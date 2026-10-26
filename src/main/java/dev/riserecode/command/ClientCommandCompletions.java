package dev.riserecode.command;

import dev.riserecode.Client;
import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.module.modules.BaritoneModule;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public final class ClientCommandCompletions {
    private ClientCommandCompletions() { }
    public static List<String> arguments(Command command,String[] args){
        var client=Client.INSTANCE;
        return switch(command.getName()){
            case "toggle" -> args.length==1?client.getModuleManager().getModules().stream().map(dev.riserecode.module.Module::getName).toList():List.of();
            case "bind" -> args.length==1?client.getModuleManager().getModules().stream().map(dev.riserecode.module.Module::getName).toList():args.length==2?java.util.stream.Stream.concat(java.util.stream.Stream.of("NONE"),java.util.stream.IntStream.rangeClosed(32,348).mapToObj(KeybindManager::keyName)).toList():args.length==3?List.of("hold","toggle"):List.of();
            case "friend" -> args.length==1?List.of("add","remove","list"):args.length==2?(args[0].equalsIgnoreCase("add")?players():client.getFriendManager().getFriends().stream().map(dev.riserecode.friend.FriendManager.Friend::name).toList()):List.of();
            case "profile" -> args.length==1?List.of("save","load","list"):args.length==2?client.getProfileManager().getProfiles():List.of();
            case "help" -> client.getCommandManager().getCommands().stream().map(Command::getName).toList();
            case "baritone" -> baritone(args);
            default -> List.of();
        };
    }
    private static List<String> players(){var connection=Minecraft.getInstance().getConnection();return connection==null?List.of():connection.getOnlinePlayers().stream().map(info->info.getProfile().name()).toList();}
    private static List<String> baritone(String[] args){
        var module=Client.INSTANCE.getModuleManager().getByClass(BaritoneModule.class).orElseThrow();
        if(!module.access().available()){return List.of();}
        try{return module.access().complete(String.join(" ",args));}catch(RuntimeException failure){Client.INSTANCE.getLogger().debug("Baritone completion unavailable",failure);return List.of();}
    }
}
