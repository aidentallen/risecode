package dev.riserecode.config;

import com.google.gson.JsonObject;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.StringSetting;
import dev.riserecode.ui.theme.ThemeManager;

/** Client-wide preferences, separate from gameplay and appearance profiles. */
public final class ClientPreferences {
    public final BooleanSetting sounds=new BooleanSetting("Client Sounds","Interface click sounds",true);
    public final BooleanSetting suggestions=new BooleanSetting("Command Suggestions","Autocomplete risecode and Baritone commands",true);
    public final BooleanSetting discord=new BooleanSetting("Discord Activity","Share only Minecraft and risecode with Discord",false);
    public final StringSetting discordId=new StringSetting("Discord Application ID","Public application ID from the Discord Developer Portal","",24);
    private final ConfigManager config;
    private Runnable listener=()->{};
    public ClientPreferences(ConfigManager config){
        this.config=config;JsonObject ui=config.getUiPreferences();
        JsonObject saved=ui.has("clientPreferences")&&ui.get("clientPreferences").isJsonObject()?ui.getAsJsonObject("clientPreferences"):new JsonObject();
        for(var setting:java.util.List.of(sounds,suggestions,discord,discordId)){
            if(saved.has(setting.getName())){try{ThemeManager.assign(setting,ThemeManager.decode(setting,saved.get(setting.getName())));}catch(IllegalArgumentException ignored){/* retain the default for a malformed preference */}}
            setting.onChange(this::changed);
        }
    }
    public void onChange(Runnable action){listener=action;}
    private void changed(){
        JsonObject values=new JsonObject();values.addProperty(sounds.getName(),sounds.getValue());values.addProperty(suggestions.getName(),suggestions.getValue());values.addProperty(discord.getName(),discord.getValue());values.addProperty(discordId.getName(),discordId.getValue());
        JsonObject ui=config.getUiPreferences();ui.add("clientPreferences",values);config.setUiPreferences(ui);listener.run();
    }
}
