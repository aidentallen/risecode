package dev.riserecode.ui.component;

import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.setting.ColorPicker;
import dev.riserecode.ui.component.setting.EnumSelector;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

/** Scrollable two-column appearance controls with live preview. Profiles live in the Profiles tab. */
public final class ThemeEditor extends UIComponent {
    private record Control(ThemeManager.Control model, UIComponent view) { }
    private final ThemeManager manager;
    private final Map<String, List<Control>> groups = new LinkedHashMap<>();
    private final List<ActionButton> actions = new ArrayList<>();
    private final Animation scroll = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.SCROLLING, 0, 0.2, MathUtil.Easing.OUT_QUART);
    private UIComponent captured;
    public record CardTransform(double cx,double cy,double scale){
        public double localX(double value){return cx+(value-cx)/scale;}
        public double localY(double value){return cy+(value-cy)/scale;}
        public void apply(GuiGraphicsExtractor g){g.pose().translate((float)cx,(float)cy);g.pose().scale((float)scale,(float)scale);g.pose().translate((float)-cx,(float)-cy);}
    }
    private static final CardTransform IDENTITY=new CardTransform(0,0,1);
    private final Map<UIComponent,CardTransform> transforms=new java.util.IdentityHashMap<>();
    private CardTransform drawingTransform=IDENTITY;
    public CardTransform transform(UIComponent component){return transforms.getOrDefault(component,IDENTITY);}
    private String keyboardSetting;
    public String keyboardSetting(){return keyboardSetting;}
    private boolean click(UIComponent component,double mx,double my,int button){var t=transform(component);boolean used=component.mouseClicked(t.localX(mx),t.localY(my),button);if(used&&FocusedInput.active()){FocusedInput.pointerMapping(v->transform(component).localX(v),v->transform(component).localY(v));}return used;}

    private double maxScroll;
    private double viewportY;
    private final Map<String, List<ActionButton>> sectionActions = new LinkedHashMap<>();
    private final Map<String, List<ActionButton>> controlActions = new LinkedHashMap<>();
    private final List<ActionButton> visibleActions = new ArrayList<>();
    private String pendingReveal;
    private String sectionKey="appearance";
    private final boolean cards;
    private double sectionHeight(){return cards?ModuleButton.HEADER_HEIGHT:SettingsSection.HEADER;}
    private final Map<String,SettingsSection> sections=new LinkedHashMap<>();
    private final List<Control> visibleControls=new ArrayList<>();
    private final List<SettingsSection> visibleSections=new ArrayList<>();
    public ThemeEditor sectionKey(String value){sectionKey=value;sections.clear();return this;}
    public SettingsSection section(String name){return sections.computeIfAbsent(name,key->new SettingsSection(sectionKey+":"+key,key,this::unfocus).card(cards));}
    private final Map<String,dev.riserecode.setting.Setting<?>> settingsByName=new LinkedHashMap<>();
    private dev.riserecode.setting.Setting<?> setting(String name){return settingsByName.getOrDefault(name,manager.getSetting(name));}
    private String query="";
    private String[] queryTerms=new String[0];
    private boolean matchingInvalid=true,matchingGerman;
    private final Map<Control,String> searchText=new java.util.IdentityHashMap<>();
    private final Map<Control,Boolean> visibility=new java.util.IdentityHashMap<>();
    private final Map<String,List<Control>> matchingGroups=new LinkedHashMap<>();
    private final Map<List<Control>,List<List<Control>>> rowCache=new java.util.IdentityHashMap<>();
    private double rowsWidth=Double.NaN,rowsPad=Double.NaN,rowsFont=Double.NaN;
    private void prepareMatches(){
        boolean changed=matchingInvalid||matchingGerman!=Theme.isGerman();
        if(matchingGerman!=Theme.isGerman()){matchingGerman=Theme.isGerman();searchText.clear();}
        for(var values:groups.values()){for(var control:values){boolean visible=control.model.setting().isVisible();Boolean previous=visibility.put(control,visible);if(previous==null||previous!=visible){changed=true;}}}
        if(!changed){return;}matchingInvalid=false;matchingGroups.clear();rowCache.clear();
        for(var entry:groups.entrySet()){var values=new ArrayList<Control>();for(var control:entry.getValue()){if(matches(entry.getKey(),control)){values.add(control);}}if(!values.isEmpty()){matchingGroups.put(entry.getKey(),List.copyOf(values));}}
    }

    private final Map<String,Boolean> beforeSearch=new LinkedHashMap<>();
    public void setQuery(String value){
        String next=value.strip().toLowerCase(java.util.Locale.ROOT);if(next.equals(query)){return;}
        unfocus();
        if(matchingGerman!=Theme.isGerman()){matchingGerman=Theme.isGerman();searchText.clear();}
        if(query.isEmpty()&&!next.isEmpty()){groups.keySet().forEach(name->beforeSearch.put(name,section(name).isExpanded()));}
        query=next;queryTerms=next.isEmpty()?new String[0]:next.split("\\s+");matchingInvalid=true;
        if(query.isEmpty()){beforeSearch.forEach((name,expanded)->section(name).setExpanded(expanded));beforeSearch.clear();}
        else{groups.forEach((name,values)->{if(values.stream().anyMatch(c->matches(name,c))){section(name).setExpanded(true);}});}
        scroll.reset(0);
    }
    private boolean matches(String group,Control control){
        if(!control.model.setting().isVisible()){return false;}if(queryTerms.length==0){return true;}
        String text=searchText.computeIfAbsent(control,key->(group+" "+control.model.setting().getName()+" "+Theme.displayName(control.model.setting().getName())+" "+control.model.setting().getDescription()).toLowerCase(java.util.Locale.ROOT));
        for(String term:queryTerms){if(!text.contains(term)){return false;}}return true;
    }
    public double preferredHeight(double available){
        prepareMatches();double result=0;for(var entry:matchingGroups.entrySet()){
            var values=entry.getValue();
            result+=sectionHeight()+section(entry.getKey()).bodyHeight(groupContent(values,entry.getKey(),available))+manager.number("Content Padding");
        }return Math.max(1,result);
    }
    private List<List<Control>> rows(List<Control> values,double available){
        double pad=manager.number("Content Padding"),font=manager.number("Font Scale"),column=(available-pad*3)/2;
        if(rowsWidth!=available||rowsPad!=pad||rowsFont!=font){rowCache.clear();rowsWidth=available;rowsPad=pad;rowsFont=font;}var cached=rowCache.get(values);if(cached!=null){return cached;}
        List<List<Control>> rows=new ArrayList<>();for(int i=0;i<values.size();){Control first=values.get(i++);if(!cards&&column>=130*manager.number("Font Scale")&&!wide(first.view())&&i<values.size()&&!wide(values.get(i).view())){rows.add(List.of(first,values.get(i++)));}else{rows.add(List.of(first));}}rowCache.put(values,rows);return rows;
    }
    private double groupContent(List<Control> values,String group,double available){
        double inner=available-manager.number("Content Padding")*2,row=Math.max(28,18*manager.number("Font Scale")+12);
        double result=group.equals("Typography")?15:0;
        for(var controls:rows(values,available)){result+=rowHeight(controls,row)+3+toolsHeight(controlActions.getOrDefault(controls.getFirst().model().setting().getName(),List.of()),inner);}
        return result+toolsHeight(sectionActions.getOrDefault(group,List.of()),inner)+manager.number("Content Padding");
    }
    public void replaceSectionActions(String group,List<ActionButton> tools){sectionActions.put(group,List.copyOf(tools));}
    public void replaceActions(String setting, List<ActionButton> value) { controlActions.put(setting, value); }

    public ThemeEditor(ThemeManager manager, Consumer<String> notice) {
        this(manager,notice,manager.getControls().stream().filter(c->!c.group().equals("Interface Sounds")&&!c.setting().getName().equals("Interface Scale")).toList(),false);
    }
    public ThemeEditor(ThemeManager manager, Consumer<String> notice,List<ThemeManager.Control> models,boolean scoped) {
        this.manager=manager;this.cards=!scoped;
        for (ThemeManager.Control model : models) {
            var setting = model.setting(); settingsByName.put(setting.getName(),setting); UIComponent view;
            if (!scoped && setting.getName().equals("Background")) { view = new BackgroundGallery(manager); }
            else if (!scoped && setting.getName().equals("Appearance Preset")) { view = new PresetGallery(manager); }
            else if (setting instanceof BooleanSetting bool) { view = new ToggleSwitch(bool, manager::changed); }
            else if (setting instanceof dev.riserecode.setting.StringSetting text) { view = new dev.riserecode.ui.component.setting.StringControl(text, manager::changed); }
            else if (setting instanceof DoubleSetting number) { view = new Slider(number, manager::changed, model.step()); }
            else if (setting instanceof EnumSetting<?> option) { view = enumView(option, manager::changed); }
            else if (setting instanceof ColorSetting color) { view = new ColorPicker(color, manager::changed); }
            else { throw new IllegalArgumentException("Missing theme control " + setting.getName()); }
            groups.computeIfAbsent(model.group(), ignored -> new ArrayList<>()).add(new Control(model, view));
        }
        actions.add(new ActionButton("Reset Theme", () -> { manager.reset(); notice.accept("Default theme restored"); }));
        actions.add(new ActionButton("Undo Preset", () -> notice.accept(manager.undoPreset() ? "Preset undone" : "No preset to undo")));
        actions.add(new ActionButton("Import Logo", () -> { dev.riserecode.ui.component.FocusedInput.commit(); try { var file=(dev.riserecode.setting.StringSetting)setting("Logo File");file.setValue(dev.riserecode.ui.theme.BrandingLogo.importPng(manager.getDirectory(),java.nio.file.Path.of(file.getValue())));manager.changed(); notice.accept("Logo imported"); } catch (java.io.IOException | RuntimeException invalid) { notice.accept(invalid.getMessage()); } }));
        actions.add(new ActionButton("Import Image", () -> { FocusedInput.commit(); try { var file=(dev.riserecode.setting.StringSetting)setting("Background File");file.setValue(dev.riserecode.ui.theme.BrandingLogo.importPng(manager.getDirectory(),java.nio.file.Path.of(file.getValue())));ThemeManager.assign(setting("Background"),ThemeManager.Background.LOCAL_IMAGE);manager.changed(); notice.accept("Background imported"); } catch (java.io.IOException | RuntimeException invalid) { notice.accept(invalid.getMessage()); } }));
        actions.add(new ActionButton("Import Sound", () -> { FocusedInput.commit(); try { dev.riserecode.ui.sound.UiSounds.importOgg(manager,this::setting); notice.accept("Sound imported; reloading sounds"); } catch (java.io.IOException | RuntimeException invalid) { notice.accept(invalid.getMessage()); } }));
        actions.add(new ActionButton("Preview Sound", dev.riserecode.ui.sound.UiSounds::play).sound(false));
        actions.add(new ActionButton("Reset Layout", () -> { manager.resetLayout(); notice.accept("Layout reset"); }));
        actions.add(new ActionButton("Reload Fonts", () -> { CustomTextRenderer.close(); notice.accept("Fonts reloaded from resources and local files"); }));
        if(!scoped){sectionActions.put("Presets", List.of(actions.get(0), actions.get(1)));}
        controlActions.put("Logo File", List.of(actions.get(2)));
        controlActions.put("Background File", List.of(actions.get(3)));
        controlActions.put("Sound File", List.of(actions.get(4), actions.get(5)));
        if(!scoped){sectionActions.put("Geometry & Layout", List.of(actions.get(6)));}
        var optionalFont = new ActionButton("Download Font", () -> { CustomTextRenderer.retryOptionalFont(); notice.accept(CustomTextRenderer.status()); });
        actions.add(optionalFont);
        sectionActions.put("Typography", List.of(actions.get(7), optionalFont));

    }
    private List<Bounds> toolBounds(List<ActionButton> tools,double available){var mc=net.minecraft.client.Minecraft.getInstance();return ButtonMetrics.flow(mc==null?null:mc.font,tools.stream().map(ActionButton::label).toList(),available);}
    private double toolsHeight(List<ActionButton> tools,double available){return tools.isEmpty()?0:ButtonMetrics.flowHeight(toolBounds(tools,available))+7;}
    private void renderTools(List<ActionButton> tools,double left,double top,double available,GuiGraphicsExtractor graphics,Font font,double mouseX,double mouseY,double seconds,double opacity){
        if(tools.isEmpty()){return;}var positions=toolBounds(tools,available);
        for(int i=0;i<tools.size();i++){var action=tools.get(i);var b=positions.get(i);action.setBounds(left+b.x(),top+b.y()+3,b.width(),b.height());transforms.put(action,drawingTransform);
            if(KeyboardNavigation.collectingOffscreen()||action.getBounds().y()+ButtonMetrics.HEIGHT>viewportY&&action.getBounds().y()<y+height){action.render(graphics,font,mouseY<viewportY||mouseY>=y+height?-1e6:mouseX,mouseY<viewportY||mouseY>=y+height?-1e6:mouseY,seconds,opacity);visibleActions.add(action);}
        }
    }
    private static boolean wide(UIComponent view) { return view instanceof PresetGallery || view instanceof BackgroundGallery || view instanceof dev.riserecode.ui.component.setting.StringControl; }
    private static double rowHeight(List<Control> controls,double fallback){double result=0;for(var control:controls){result=Math.max(result,controlHeight(control.view(),fallback));}return controls.isEmpty()?fallback:result;}
    private static double controlHeight(UIComponent view, double row) { return view instanceof BackgroundGallery ? Math.ceil(ThemeManager.Background.values().length/4.0)*41 : view instanceof PresetGallery ? 96 : SettingMetrics.height(view,row); }
    private static <E extends Enum<E>> UIComponent enumView(EnumSetting<E> option, Runnable changed) { return new EnumSelector<>(option, changed); }
    public UIComponent getControl(String label) {
        return groups.values().stream().flatMap(List::stream).filter(control -> control.model.setting().getName().equals(label))
                .map(Control::view).findFirst().orElseThrow();
    }
    public double getScrollOffset() { return scroll.getValue(); }
    public double getMaxScroll() { return maxScroll; }
    public List<ActionButton> getActions() { return List.copyOf(actions); }
    public ActionButton getAction(String label) { return actions.stream().filter(action -> action.label().equals(label)).findFirst().orElseThrow(); }
    public void reveal(String label) {
        getControl(label);
        groups.forEach((name,values)->{if(values.stream().anyMatch(c->c.model.setting().getName().equals(label))){section(name).setExpanded(true);}});
        pendingReveal = label;
    }
    @Override public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        if(captured!=null&&groups.values().stream().flatMap(List::stream).anyMatch(c->c.view()==captured&&!c.model().setting().isVisible())){FocusedInput.commit();mouseReleased(0,0,0);}
        double pad=manager.number("Content Padding");visibleActions.clear();visibleControls.clear();visibleSections.clear();
        viewportY=y;double viewportHeight=Math.max(1,height);
        maxScroll=Math.max(0,preferredHeight(width)-viewportHeight);
        if(scroll.getValue()>maxScroll){scroll.reset(maxScroll);}scroll.animateTo(Math.clamp(scroll.getTarget(),0,maxScroll));double offset=Math.max(0,scroll.update(seconds));
        double top=y-offset;
        if(!query.isEmpty()&&matchingGroups.isEmpty()){
            RenderUtil.text(graphics,font,Theme.isGerman()?"Keine passenden Einstellungen":"No matching settings",x+6,y+10,.85,Theme.opacity(Theme.TEXT,opacity));
            RenderUtil.text(graphics,font,Theme.isGerman()?"Suche kürzen oder löschen":"Try fewer words or Clear the search.",x+6,y+27,.7,Theme.opacity(Theme.SECONDARY,opacity));
        }
        boolean ownsReveal=maxScroll>0;if(ownsReveal){KeyboardNavigation.pushScrollRegion(this::revealControl);}
        RenderUtil.scissorPush(graphics,x,y,width,height);
        try{
            for(var entry:matchingGroups.entrySet()){
                String group=entry.getKey();var values=entry.getValue();
                var header=section(group);double content=groupContent(values,group,width),body=header.bodyHeight(content),cardHeight=sectionHeight()+body;
                header.setBounds(x,top,width-4,sectionHeight());
                drawingTransform=new CardTransform(x+(width-4)/2,top+cardHeight/2,header.feedbackScale());
                transforms.put(header,drawingTransform);
                double localMouseX=drawingTransform.localX(mouseX),localMouseY=drawingTransform.localY(mouseY);
                graphics.pose().pushMatrix();drawingTransform.apply(graphics);
                try{
                if(KeyboardNavigation.collectingOffscreen()||top+cardHeight>=y&&top<y+height){
                    RenderUtil.drawSurfaceTransition(graphics,x,top,width-4,cardHeight,Theme.BUTTON_RADIUS,header.surfaceHover()*.65,opacity);header.render(graphics,font,localMouseX,localMouseY,seconds,opacity);visibleSections.add(header);
                }
                if(body>.01){
                    RenderUtil.scissorPush(graphics,x,top+sectionHeight(),width,body);
                    try{
                        double controlY=top+sectionHeight(),innerWidth=width-pad*2,columnWidth=(innerWidth-pad)/2,row=Math.max(28,18*manager.number("Font Scale")+12);
                        if(group.equals("Typography")){RenderUtil.text(graphics,font,RenderUtil.ellipsize(font,CustomTextRenderer.status(),innerWidth/.68),x+pad,controlY,.68,Theme.opacity(Theme.SECONDARY,opacity));controlY+=15;}
                        for(var rowControls:rows(values,width)){
                            double rowHeight=rowHeight(rowControls,row);
                            for(int column=0;column<rowControls.size();column++){
                                var control=rowControls.get(column);var view=control.view();transforms.put(view,drawingTransform);view.setBounds(x+pad+column*(columnWidth+pad),controlY,rowControls.size()==1?innerWidth-4:columnWidth-4,rowHeight);
                                boolean fullyVisible=header.isExpanded()&&controlY+rowHeight<=top+sectionHeight()+body+.01;
                                if(KeyboardNavigation.collectingOffscreen()||controlY+rowHeight>=y&&controlY<y+height){view.render(graphics,font,fullyVisible&&localMouseY>=Math.max(y,top+sectionHeight())&&localMouseY<Math.min(y+height,top+cardHeight)?localMouseX:-1e6,fullyVisible?localMouseY:-1e6,seconds,opacity*header.amount(content));if(fullyVisible){visibleControls.add(control);if(control.model.setting().getName().equals(keyboardSetting)){var bounds=view.getBounds();RenderUtil.drawRoundedBorder(graphics,bounds.x()-2,bounds.y(),bounds.width()+4,bounds.height(),3,Theme.opacity(Theme.accent(),opacity*.7));}}}
                            }
                            controlY+=rowHeight+3;var tools=controlActions.getOrDefault(rowControls.getFirst().model().setting().getName(),List.of());
                            int before=visibleActions.size();renderTools(tools,x+pad,controlY,innerWidth,graphics,font,localMouseX,localMouseY,seconds,opacity*header.amount(content));
                            if(!header.isExpanded()||controlY+toolsHeight(tools,innerWidth)>top+cardHeight){visibleActions.subList(before,visibleActions.size()).clear();}controlY+=toolsHeight(tools,innerWidth);
                        }
                        var tools=sectionActions.getOrDefault(group,List.of());int before=visibleActions.size();renderTools(tools,x+pad,controlY,innerWidth,graphics,font,localMouseX,localMouseY,seconds,opacity*header.amount(content));
                        if(!header.isExpanded()||controlY+toolsHeight(tools,innerWidth)>top+cardHeight+.01){visibleActions.subList(before,visibleActions.size()).clear();}
                    }finally{RenderUtil.scissorPop(graphics);}
                }
                }finally{graphics.pose().popMatrix();}
                top+=cardHeight+pad;
            }
        }finally{RenderUtil.scissorPop(graphics);if(ownsReveal){KeyboardNavigation.popScrollRegion();}}
        if(pendingReveal!=null){var control=getControl(pendingReveal);double desired=control.getBounds().y()-viewportY-4+scroll.getValue();scroll.animateTo(Math.clamp(desired,0,maxScroll));if(visibleControls.stream().anyMatch(c->c.view()==control)){pendingReveal=null;}}
        if(maxScroll>0){double thumb=Math.max(18,viewportHeight*viewportHeight/(viewportHeight+maxScroll));RenderUtil.drawRoundedRect(graphics,x+width-2,y+offset/maxScroll*(viewportHeight-thumb),1.5,thumb,.75,Theme.opacity(Theme.accent(),opacity*.65));}
    }
    private void revealControl(Bounds bounds){
        double shift=bounds.y()<y?bounds.y()-y:Math.max(0,bounds.y()+bounds.height()-y-height);
        if(shift!=0){scroll.reset(Math.clamp(scroll.getValue()+shift,0,maxScroll));}
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        groups.values().forEach(group -> group.forEach(control -> { if (control.view instanceof ColorPicker picker) { var t=transform(picker);picker.pointerFocus(t.localX(mouseX),t.localY(mouseY)); } }));
        if (!contains(mouseX, mouseY)) { return false; }
        if (mouseY < viewportY || mouseY >= y + height) { return true; }
        for (ActionButton action : visibleActions) { if (click(action,mouseX,mouseY,button)) { return true; } }
        for(var header:visibleSections){if(click(header,mouseX,mouseY,button)){return true;}}
        for(var control:List.copyOf(visibleControls)){
            if(control.model.setting().isVisible()&&click(control.view,mouseX,mouseY,button)){captured=control.view;return true;}
        }
        return true;
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button) { return captured != null && captured.mouseDragged(transform(captured).localX(mouseX), transform(captured).localY(mouseY), button); }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (captured == null) { return false; } boolean result = captured.mouseReleased(transform(captured).localX(mouseX), transform(captured).localY(mouseY), button); captured = null; return result;
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        if (!contains(mouseX, mouseY)) { return false; }
        mouseReleased(0, 0, 0); scroll.animateTo(Math.max(0, Math.min(maxScroll, scroll.getTarget() - vertical * 32))); return true;
    }
    public void unfocus() { groups.values().forEach(group -> group.forEach(c -> { if (c.view instanceof ColorPicker picker) { picker.unfocus(); } })); mouseReleased(0, 0, 0); }
    public boolean charTyped(CharacterEvent event) { return visibleControls.stream().anyMatch(c -> c.view instanceof ColorPicker picker && picker.charTyped(event)); }
    public boolean keyPressed(KeyEvent event, Supplier<String> clipboard, Consumer<String> copy) {
        if(sectionKey.equals("client:PERFORMANCE")){
            var options=groups.entrySet().stream().flatMap(entry->entry.getValue().stream().filter(c->matches(entry.getKey(),c))).toList();
            if(!options.isEmpty()){
                int index=0;for(int i=0;i<options.size();i++){if(options.get(i).model.setting().getName().equals(keyboardSetting)){index=i;break;}}
                int key=event.key();
                if(key==258||key==264||key==265){int step=key==265||key==258&&event.hasShiftDown()?-1:1;if(keyboardSetting==null){index=step>0?0:options.size()-1;}else{index=Math.floorMod(index+step,options.size());}keyboardSetting=options.get(index).model.setting().getName();reveal(keyboardSetting);return true;}
                if(key==262||key==263||key==32||event.isConfirmation()){
                    var setting=options.get(index).model.setting();keyboardSetting=setting.getName();reveal(keyboardSetting);
                    if(setting instanceof BooleanSetting b){b.setValue(!b.getValue());}else if(setting instanceof EnumSetting<?> e){cycle(e,key==263?-1:1);}dev.riserecode.ui.sound.UiSounds.play();return true;
                }
            }
        }
        return visibleControls.stream().anyMatch(c -> c.view instanceof ColorPicker picker && picker.keyPressed(event, clipboard, copy));
    }
    private static <E extends Enum<E>> void cycle(EnumSetting<E> setting,int step){var options=setting.getOptions();setting.setValue(options.get(Math.floorMod(options.indexOf(setting.getValue())+step,options.size())));}

}
