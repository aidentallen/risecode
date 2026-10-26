package dev.riserecode;

import com.google.gson.JsonObject;
import dev.riserecode.command.*;
import dev.riserecode.setting.*;
import dev.riserecode.ui.theme.*;
import java.nio.file.Files;
import java.util.List;
import org.slf4j.LoggerFactory;

/** Migration, independently scoped effects, and completion replacement contracts. */
public final class Release020Checks {
    private static int checks;
    private Release020Checks() { }
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    public static void main(String[] args)throws Exception{
        var manager=new ThemeManager(Files.createTempDirectory("risecode-020-"),LoggerFactory.getLogger("020-check"));
        check(manager.choice("Container Style",ThemeManager.ContainerStyle.class)==ThemeManager.ContainerStyle.RISECODE,"container default is risecode");
        check(java.util.Arrays.asList(ThemeManager.Background.values()).contains(ThemeManager.Background.NONE),"None is available");
        var page=manager.page("test.container");
        page.style.setValue(PageAppearance.ScreenStyle.MINECRAFT);check(!page.enabled(true),"Minecraft overrides global risecode");
        page.style.setValue(PageAppearance.ScreenStyle.RISECODE);check(page.enabled(false),"risecode overrides global Minecraft");
        ((DoubleSetting)manager.getSetting("Particle Opacity")).setValue(.7);page.inherit.setValue(false);
        check(page.inheritParticles.getValue()&&page.effective("Particle Opacity")==null,"background detach does not detach particles");
        page.inheritParticles.setValue(false);check(((DoubleSetting)page.get("Particle Opacity")).getValue()==.7,"particle detach seeds current global value");
        ((DoubleSetting)page.get("Particle Opacity")).setValue(.2);page.inherit.setValue(true);page.inherit.setValue(false);
        check(((DoubleSetting)page.get("Particle Opacity")).getValue()==.2,"background changes preserve local particles");
        ((DoubleSetting)page.get("GUI Blur Radius")).setValue(31.0);page.inheritParticles.setValue(true);page.inheritParticles.setValue(false);
        check(((DoubleSetting)page.get("GUI Blur Radius")).getValue()==31,"particle changes preserve local background");
        var snapshot=manager.snapshot();var other=new ThemeManager(Files.createTempDirectory("risecode-020-load-"),LoggerFactory.getLogger("020-check"));other.apply(snapshot);
        check(other.page("test.container").style.getValue()==PageAppearance.ScreenStyle.RISECODE,"screen style round trips");
        check(!other.page("test.container").inheritParticles.getValue(),"independent particle inheritance round trips");
        JsonObject legacy=new JsonObject();legacy.addProperty("Page Theme","DISABLED");legacy.addProperty("Inherit Background",false);legacy.addProperty("Particle Opacity",.41);
        var migrated=new PageAppearance(manager);migrated.load(legacy);
        check(migrated.style.getValue()==PageAppearance.ScreenStyle.MINECRAFT,"legacy opt-out remains Minecraft");check(!migrated.inheritParticles.getValue(),"legacy local particle ownership preserved");check(((DoubleSetting)migrated.get("Particle Opacity")).getValue()==.41,"legacy local particle value preserved");
        var appearance=new dev.riserecode.ui.component.ThemeEditor(manager,ignored->{});
        try{appearance.getControl("Click Sound");throw new AssertionError("sound controls duplicated in Appearance");}catch(java.util.NoSuchElementException expected){checks++;}
        try{appearance.getControl("Interface Scale");throw new AssertionError("scale controls duplicated in Appearance");}catch(java.util.NoSuchElementException expected){checks++;}
        var commands=new CommandManager(()->".",ignored->{},ignored->{},LoggerFactory.getLogger("020-command"));
        commands.register(new Command("toggle","","toggle <module>","t"){public void execute(String[] values){throw new AssertionError("completion executed a command");}});
        commands.register(new Command("profile","","profile load <name>"){public void execute(String[] values){throw new AssertionError("completion executed a command");}});
        var names=CommandCompletions.suggest(commands,".to",3,(c,a)->List.of());check(names.size()==1&&names.getFirst().start()==1&&names.getFirst().text().equals("toggle"),"prefix names have correct replacement range");
        var module=CommandCompletions.suggest(commands,".t Au",5,(c,a)->List.of("Auto Sprint","Fly"));check(module.size()==1&&module.getFirst().text().equals("\"Auto Sprint\"")&&module.getFirst().start()==3,"module names with spaces are quoted");
        var quoted=CommandCompletions.suggest(commands,".toggle \"Auto S",15,(c,a)->List.of("Auto Sprint"));check(quoted.size()==1&&quoted.getFirst().start()==8,"incomplete quote is tolerated");
        check(CommandCompletions.suggest(commands,"/to",3,(c,a)->List.of()).isEmpty(),"slash commands remain vanilla");
        check(CommandCompletions.suggest(commands,".unknown x",10,(c,a)->List.of()).isEmpty(),"unknown command has no argument suggestions");
        var cursor=CommandCompletions.suggest(commands,".toggle Au later",10,(c,a)->List.of("Auto Sprint"));check(cursor.size()==1&&cursor.getFirst().end()==10,"completion respects cursor position");
        var middle=CommandCompletions.suggest(commands,".toggle \"Auto Sprint\" next",15,(c,a)->List.of("Auto Sprint"));
        check(middle.size()==1&&middle.getFirst().end()==21,"completion in a quoted argument replaces its remaining text");
        migrated.style.setValue(PageAppearance.ScreenStyle.INHERIT);check(migrated.enabled(true),"choosing Inherit clears an old hidden opt-out");
        AppearancePolishChecks.run();
        System.out.println("Release 0.20 pure checks passed ("+checks+" assertions).");
        if(args.length==1&&args[0].equals("--pure")){return;}
        dev.riserecode.integration.DiscordActivityChecks.run();
        System.out.println("Release 0.20 checks passed ("+checks+" assertions, plus Discord IPC lifecycle checks).");
    }
}
