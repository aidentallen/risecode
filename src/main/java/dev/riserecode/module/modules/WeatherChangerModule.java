package dev.riserecode.module.modules;

import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.EnumSetting;

/** Read-only local weather override; the server's weather and world fields are never changed. */
public final class WeatherChangerModule extends Module {
    public enum Weather { CLEAR, RAIN, THUNDER }
    private final EnumSetting<Weather> weather = register(new EnumSetting<>("Weather", "Local weather", Weather.CLEAR));
    public WeatherChangerModule(EventBus bus) { super("WeatherChanger", "Client-side weather and sky override.", Category.WORLD, bus); }
    public float rain() { return weather.getValue() == Weather.CLEAR ? 0 : 1; }
    public float thunder() { return weather.getValue() == Weather.THUNDER ? 1 : 0; }
}
