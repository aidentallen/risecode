package dev.riserecode.scanner;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import static dev.riserecode.scanner.ChunkHeuristics.*;

/** Registry IDs are resolved once; packet scanning uses a compact lookup table. */
public final class ScannerBlocks {
    private final int[] flags;
    public ScannerBlocks(){
        flags=new int[Block.BLOCK_STATE_REGISTRY.size()];
        for(BlockState state:Block.BLOCK_STATE_REGISTRY)flags[Block.getId(state)]=classify(state);
    }
    public int flags(BlockState state){int id=Block.getId(state);return id>=0&&id<flags.length?flags[id]:0;}
    private static int classify(BlockState s){
        var b=s.getBlock();String id=BuiltInRegistries.BLOCK.getKey(b).getPath();int f=0;
        if(s.isAir())return AIR|TRANSPARENT;
        if(!s.isSolidRender())f|=TRANSPARENT;
        if(b instanceof ChestBlock||b instanceof BarrelBlock||b instanceof ShulkerBoxBlock||b instanceof HopperBlock||b instanceof DispenserBlock||b instanceof AbstractFurnaceBlock||b instanceof BrewingStandBlock)f|=CONTAINER;
        if(b instanceof ShulkerBoxBlock)f|=SHULKER;if(b instanceof ChestBlock)f|=CHEST;if(b instanceof HopperBlock)f|=HOPPER;
        if(b instanceof PistonBaseBlock||b instanceof ObserverBlock)f|=MACHINERY;
        if(b instanceof HopperBlock||b instanceof DropperBlock)f|=CLOCK_PART;
        if(b instanceof RedStoneWireBlock||b instanceof RepeaterBlock||b instanceof ComparatorBlock||b instanceof LeverBlock||b instanceof TripWireBlock||b instanceof TripWireHookBlock||b instanceof DaylightDetectorBlock)f|=REDSTONE;
        if(s.is(Blocks.AMETHYST_BLOCK)||s.is(Blocks.BUDDING_AMETHYST)||s.is(Blocks.CALCITE)||s.is(Blocks.SMOOTH_BASALT))f|=GEODE;
        if(s.is(Blocks.AMETHYST_BLOCK))f|=AMETHYST;
        if(s.is(Blocks.BUDDING_AMETHYST))f|=BUDDING;if(s.is(Blocks.CALCITE)||s.is(Blocks.SMOOTH_BASALT))f|=SHELL;
        if(s.is(Blocks.AMETHYST_CLUSTER))f|=GROWTH|CLUSTER;
        if(b instanceof CropBlock crop&&crop.isMaxAge(s)||s.is(Blocks.NETHER_WART)&&s.getValue(BlockStateProperties.AGE_3)==3)f|=GROWTH|CROP;
        // Height and attached-fruit checks need neighbors and are finished after decoding.
        if(s.is(Blocks.SUGAR_CANE)||s.is(Blocks.KELP)||s.is(Blocks.KELP_PLANT))f|=TALL;
        if(b instanceof AttachedStemBlock)f|=STEM;
        if(s.is(Blocks.PUMPKIN)||s.is(Blocks.MELON))f|=FRUIT;
        if(s.is(Blocks.COBBLESTONE))f|=COBBLE;if(s.is(Blocks.MOSSY_COBBLESTONE))f|=MOSSY;
        if(s.is(Blocks.SPAWNER))f|=SPAWNER;if(b instanceof BaseRailBlock)f|=RAIL;if(s.is(Blocks.COBWEB))f|=WEB;
        if(id.startsWith("sculk"))f|=SCULK;if(s.is(Blocks.REINFORCED_DEEPSLATE)||s.is(Blocks.SOUL_LANTERN))f|=CITY;
        if(s.is(Blocks.TRIAL_SPAWNER)||s.is(Blocks.VAULT))f|=TRIAL;
        if(id.contains("tuff")||id.contains("copper"))f|=TUFF;
        if(id.endsWith("_planks")||id.endsWith("_bricks")||id.contains("torch")||s.is(Blocks.CRAFTING_TABLE))f|=BUILT;
        if(s.hasBlockEntity())f|=ENTITY;return f;
    }
    public static void growth(int[] cells){
        for(int i=0;i<cells.length;i++) {
            cells[i]&=~(DERIVED_GROWTH);int f=cells[i];
            if((f&(CLOCK_PART))!=0){int x=i&15,z=(i>>4)&15;boolean logic=false,pair=false;for(int n:new int[]{x>0?i-1:-1,x<15?i+1:-1,z>0?i-16:-1,z<15?i+16:-1,i>=256?i-256:-1,i+256<cells.length?i+256:-1})if(n>=0){logic|=(cells[n]&(REDSTONE|MACHINERY))!=0;pair|=(cells[n]&(CLOCK_PART))!=0;}if(logic&&pair)cells[i]|=CLOCK;}
            if((f&(TALL))!=0&&(i>>8)>=2&&(cells[i-256]&(TALL))!=0&&(cells[i-512]&(TALL))!=0&&(i+256>=cells.length||(cells[i+256]&(TALL))==0))cells[i]|=GROWTH|(DERIVED_GROWTH);
            if((f&(STEM))!=0){int x=i&15,z=(i>>4)&15;for(int n:new int[]{x>0?i-1:-1,x<15?i+1:-1,z>0?i-16:-1,z<15?i+16:-1})if(n>=0&&(cells[n]&(FRUIT))!=0){cells[i]|=GROWTH|(DERIVED_GROWTH);break;}}
        }
    }
}
