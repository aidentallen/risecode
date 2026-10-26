package dev.riserecode.ui.animation;

import dev.riserecode.module.Module;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;

public final class PopIn {
    private PopIn(){ }
    public static double progress(double age,double duration){if(duration<=0||Theme.bool("Reduced Motion",false)){return 1;}double t=Math.clamp(age*Theme.number("Animation Speed",1)/duration,0,1);return (Theme.manager()==null?dev.riserecode.util.MathUtil.Easing.OUT_QUART:Theme.manager().choice("Opening Easing",dev.riserecode.util.MathUtil.Easing.class)).apply(t);}
    public static double windowDuration(String family){return Theme.bool(family+" Animation Override",false)?Theme.number(family+" Pop Duration",.2):Theme.manager().channelNumber(ThemeManager.Channel.OPENING,"Duration");}
    public static double scale(double strength,double progress) {
        double start=strength<=10?1-.06*strength:.4*Math.exp(-.1*(strength-10));
        return Math.clamp(1-(1-progress)*(1-start),.01,2);
    }
    public static double windowScale(String family,double progress){return scale(Theme.bool("Reduced Motion",false)?0:Theme.bool(family+" Animation Override",false)?Theme.number(family+" Pop Scale",1):Animation.scale(ThemeManager.Channel.OPENING),progress);}

    public static double moduleProgress(Module module,double age){return progress(age,module.popDuration());}
}
