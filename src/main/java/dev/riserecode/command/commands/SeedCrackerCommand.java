package dev.riserecode.command.commands;

import dev.riserecode.command.Command;
import dev.riserecode.command.CommandManager;
import dev.riserecode.module.modules.SeedCrackerModule;

/** Local command prefix; no slash command or packet is sent to the server. */
public final class SeedCrackerCommand extends Command {
    private final SeedCrackerModule module;
    private final CommandManager commands;
    public SeedCrackerCommand(SeedCrackerModule module, CommandManager commands) {
        super("seedcracker","Collect structures and manually crack native 26.2 seeds","seedcracker <on|off|start|stop|rescan|clear|restore|copy|status|lookup|submit|set>");
        this.module=module; this.commands=commands;
    }
    @Override public void execute(String[] args) {
        String action=args.length==0?"status":args[0].toLowerCase(java.util.Locale.ROOT);
        switch (action) {
            case "on" -> module.setEnabled(true);
            case "off" -> module.setEnabled(false);
            case "start" -> module.start(); case "stop" -> module.stop(); case "rescan" -> module.rescan();
            case "clear" -> module.clearData(); case "restore" -> module.restore(); case "copy" -> module.copySeed();
            case "lookup" -> module.lookup();
            case "submit" -> { commands.reply(SeedCrackerModule.submissionDetails()); module.submit(); }
            case "status" -> { }
            case "set" -> {
                if (args.length!=3) { throw new IllegalArgumentException("seedcracker set \"setting name\" <value>; settings include each Find switch, Debug, Outlines, Cracking Workers and database controls"); }
                var setting=module.getSettings().stream().filter(s -> s.getName().equalsIgnoreCase(args[1])).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown SeedCracker setting"));
                var value=new com.google.gson.JsonPrimitive(args[2]);
                if (setting instanceof dev.riserecode.setting.BooleanSetting) {
                    if (!args[2].equalsIgnoreCase("true") && !args[2].equalsIgnoreCase("false")) { throw new IllegalArgumentException("Use true or false"); }
                    value=new com.google.gson.JsonPrimitive(Boolean.parseBoolean(args[2]));
                } else if (setting instanceof dev.riserecode.setting.DoubleSetting) { value=new com.google.gson.JsonPrimitive(Double.parseDouble(args[2])); }
                dev.riserecode.config.SettingCodecs.apply(setting,value);
            }
            default -> throw new IllegalArgumentException(getUsage());
        }
        module.statusLines().forEach(commands::reply);
    }
}
