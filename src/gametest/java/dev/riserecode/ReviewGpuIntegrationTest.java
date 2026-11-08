package dev.riserecode;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.*;
import com.mojang.blaze3d.textures.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.lwjgl.system.MemoryUtil;

/** Real backend objects with one injected failure at a device boundary; no production test hooks. */
public final class ReviewGpuIntegrationTest {
    private static int checks;
    enum Phase { NONE, VIEW, COPY }
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    static final class Fault implements AutoCloseable {
        private final GpuDevice device=RenderSystem.getDevice();
        private final Field field;
        private final Object original;
        private final Phase phase;
        boolean injected;
        final List<GpuTexture> textures=new ArrayList<>();
        final List<GpuTextureView> views=new ArrayList<>();
        final List<GpuBuffer> buffers=new ArrayList<>();
        Fault(Phase phase) throws ReflectiveOperationException {
            this.phase=phase;field=GpuDevice.class.getDeclaredField("backend");field.setAccessible(true);original=field.get(device);
            Object proxy=Proxy.newProxyInstance(GpuDeviceBackend.class.getClassLoader(),new Class<?>[]{GpuDeviceBackend.class},(ignored,method,args)->{
                if(method.getName().equals("createTextureView")&&phase==Phase.VIEW&&!injected){injected=true;throw new IllegalStateException("Injected native view creation failure");}
                Object result=forward(original,method,args);
                if(result instanceof GpuTexture value){textures.add(value);}
                if(result instanceof GpuTextureView value){views.add(value);}
                if(result instanceof GpuBuffer value){buffers.add(value);}
                if(result instanceof CommandEncoderBackend encoder&&phase==Phase.COPY){
                    return Proxy.newProxyInstance(CommandEncoderBackend.class.getClassLoader(),new Class<?>[]{CommandEncoderBackend.class},(p,operation,arguments)->{
                        if(operation.getName().equals("copyBufferToTexture")&&phase==Phase.COPY&&!injected){injected=true;throw new IllegalStateException("Injected native upload copy failure");}
                        return forward(encoder,operation,arguments);
                    });
                }
                return result;
            });
            field.set(device,proxy);
        }
        void verifyClosed(String label){
            check(phase==Phase.NONE||injected,label+" reaches injected native device boundary");
            check(!textures.isEmpty()&&textures.stream().allMatch(GpuTexture::isClosed),label+" releases every actual acquired GPU texture");
            check(views.stream().allMatch(GpuTextureView::isClosed)&&buffers.stream().allMatch(GpuBuffer::isClosed),label+" releases actual views/upload buffers");
            Client.INSTANCE.getLogger().info("GPU ownership {}: acquired/released textures={}/{}, views={}/{}, buffers={}/{}",label,textures.size(),textures.stream().filter(GpuTexture::isClosed).count(),views.size(),views.stream().filter(GpuTextureView::isClosed).count(),buffers.size(),buffers.stream().filter(GpuBuffer::isClosed).count());
        }
        long liveTextures(){return textures.stream().filter(value->!value.isClosed()).count();}
        long liveViews(){return views.stream().filter(value->!value.isClosed()).count();}
        long liveBuffers(){return buffers.stream().filter(value->!value.isClosed()).count();}
        void discardClosed(){textures.removeIf(GpuTexture::isClosed);views.removeIf(GpuTextureView::isClosed);buffers.removeIf(GpuBuffer::isClosed);}
        @Override public void close(){try{field.set(device,original);}catch(IllegalAccessException failure){throw new AssertionError(failure);}}
    }
    private static Object forward(Object target,Method method,Object[] args) throws Throwable {
        try{return method.invoke(target,args);}catch(InvocationTargetException failure){throw failure.getCause();}
    }
    private static Object invoke(Object owner,String name,Class<?>[] types,Object... args) throws ReflectiveOperationException {
        var method=owner.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(owner,args);
    }
    private static Object face(byte[] bytes) throws ReflectiveOperationException {
        var type=Class.forName("dev.riserecode.ui.font.CustomTextRenderer$Face");
        var constructor=type.getDeclaredConstructor(byte[].class,boolean.class);constructor.setAccessible(true);return constructor.newInstance(bytes,false);
    }
    private static Object bitmap(Path path) throws ReflectiveOperationException {
        var type=Class.forName("dev.riserecode.ui.font.BitmapPackFont");var constructor=type.getDeclaredConstructor(Path.class);constructor.setAccessible(true);return constructor.newInstance(path);
    }
    private static void close(Object object) throws Exception {((AutoCloseable)object).close();}
    static long fontNativeBytes(){
        var bytes=new java.util.concurrent.atomic.AtomicLong();
        MemoryUtil.memReport((address,size,thread,name,trace)->{
            if(Arrays.stream(trace).anyMatch(frame->frame.getClassName().startsWith("dev.riserecode.ui.font."))){bytes.addAndGet(size);}
        },MemoryUtil.MemoryAllocationReport.Aggregate.GROUP_BY_STACKTRACE,false);
        return bytes.get();
    }
    private static void expectedFailure(java.util.concurrent.Callable<?> action) throws Exception {
        boolean failed=false;
        try{action.call();}catch(InvocationTargetException expected){failed=expected.getCause() instanceof IllegalStateException;}
        check(failed,"Production constructor/upload propagates injected device failure");
    }
    private static Map<Object,Object> brands() throws ReflectiveOperationException {
        var field=dev.riserecode.ui.font.CustomTextRenderer.class.getDeclaredField("BRANDS");field.setAccessible(true);
        @SuppressWarnings("unchecked") var result=(Map<Object,Object>)field.get(null);return result;
    }
    private static void clearBrands() throws Exception {for(var value:brands().values()){close(value);}brands().clear();}
    private static Object brand(Object face,net.minecraft.client.gui.Font font,String text) throws ReflectiveOperationException {
        var method=dev.riserecode.ui.font.CustomTextRenderer.class.getDeclaredMethod("raster",face.getClass(),net.minecraft.client.gui.Font.class,String.class,int.class);method.setAccessible(true);return method.invoke(null,face,font,text,2);
    }
    private static Path syntheticBitmap() throws java.io.IOException {
        Path zip=Files.createTempFile("risecode-generated-bitmap-",".zip");
        var image=new java.awt.image.BufferedImage(128,128,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<128;y++){for(int x=0;x<128;x++){if(x%8<4&&y%8<6){image.setRGB(x,y,0xFFFFFFFF);}}}
        var png=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",png);image.flush();
        try(var archive=new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))){archive.putNextEntry(new java.util.zip.ZipEntry("assets/minecraft/textures/font/ascii.png"));archive.write(png.toByteArray());archive.closeEntry();}
        return zip;
    }
    public static void run(ClientGameTestContext context){
        context.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        context.runOnClient(mc->{
            check(org.lwjgl.system.Configuration.DEBUG_MEMORY_ALLOCATOR.get(false),"Native allocator reporting enabled before LWJGL initialization");
            try(var stream=ReviewGpuIntegrationTest.class.getResourceAsStream("/assets/riserecode/font/inter_regular.ttf")){
                byte[] bytes=Objects.requireNonNull(stream).readAllBytes();
                long initial=fontNativeBytes();
                for(int i=0;i<3;i++){boolean invalid=false;try{face(new byte[3]);}catch(InvocationTargetException expected){invalid=expected.getCause() instanceof IllegalArgumentException;}check(invalid,"Truncated native font retry rejects before allocation");}
                check(fontNativeBytes()==initial,"Malformed font retries retain no tracked native bytes");
                for(Phase phase:List.of(Phase.VIEW,Phase.COPY,Phase.NONE)){
                    long before=fontNativeBytes();Object face=face(bytes);boolean released=false;
                    try(var fault=new Fault(phase)){
                        if(phase==Phase.NONE){invoke(face,"upload",new Class<?>[]{});}else{expectedFailure(()->invoke(face,"upload",new Class<?>[]{}));}
                        close(face);released=true;fault.verifyClosed("Face "+phase);
                    }finally{if(!released){close(face);}}
                    check(fontNativeBytes()==before,"Face failure/success returns tracked native font bytes to baseline: "+phase);
                }
                Path bitmap=syntheticBitmap();
                try{
                    for(Phase phase:List.of(Phase.VIEW,Phase.COPY,Phase.NONE)){
                        long before=fontNativeBytes();
                        try(var fault=new Fault(phase)){
                            if(phase==Phase.NONE){close(bitmap(bitmap));}else{expectedFailure(()->bitmap(bitmap));}
                            fault.verifyClosed("Bitmap "+phase);
                        }
                        check(fontNativeBytes()==before,"Bitmap failure/success returns tracked native atlas bytes to baseline: "+phase);
                    }
                }finally{Files.deleteIfExists(bitmap);}
                Object face=face(bytes);
                try{
                    for(Phase phase:List.of(Phase.VIEW,Phase.COPY)){
                        long before=fontNativeBytes();int cached=brands().size();
                        try(var fault=new Fault(phase)){
                            expectedFailure(()->brand(face,mc.font,"Failure "+phase));fault.verifyClosed("Brand "+phase);
                        }
                        check(brands().size()==cached&&fontNativeBytes()==before,"Failed branding leaves cache/native bytes unchanged: "+phase);
                    }
                    try(var fault=new Fault(Phase.NONE)){
                        for(int i=0;i<10;i++){brand(face,mc.font,"Successful retry "+i);}
                        check(brands().size()<=8&&fault.textures.getFirst().isClosed(),"Successful branding retry retains bounded cache and evicts oldest native texture");
                        clearBrands();fault.verifyClosed("Brand success/eviction");
                    }
                }finally{close(face);}
                check(fontNativeBytes()==initial,"All isolated font owners return tracked native bytes to initial baseline");
            }catch(Exception failure){throw new AssertionError(failure);}
        });
        Client.INSTANCE.getLogger().info("Review GPU native checks passed ({} assertions)",checks);
    }
}
