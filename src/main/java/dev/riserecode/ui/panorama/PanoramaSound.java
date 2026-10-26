package dev.riserecode.ui.panorama;

import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.*;

/** Own exactly three relative environmental voices. Never stop menu music or other sounds. */
public final class PanoramaSound implements AutoCloseable {
    private final ArrayList<Voice> voices=new ArrayList<>();
    private float fade;
    private static final class Voice extends AbstractTickableSoundInstance {
        private float target;
        Voice(SoundEvent event,SoundSource category){super(event,category,SoundInstance.createUnseededRandom());looping=true;relative=true;attenuation=SoundInstance.Attenuation.NONE;volume=0;}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){volume+=(target-volume)*.12f;}
        void level(float value){target=value;}
    }
    public void update(float dt,float rain,float night,PanoramaPreferences p){
        if(!Minecraft.getInstance().isWindowActive()||p.mute.getValue()||p.volume.getValue()==0){close();return;}
        if(voices.isEmpty()){
            voices.add(new Voice(SoundEvents.WATER_AMBIENT,SoundSource.AMBIENT));voices.add(new Voice(SoundEvents.WEATHER_RAIN,SoundSource.WEATHER));voices.add(new Voice(SoundEvents.FIREFLY_BUSH_IDLE,SoundSource.AMBIENT));
            for(var v:voices)Minecraft.getInstance().getSoundManager().play(v);
        }
        fade=Math.min(1,fade+dt/3);float base=(float)(p.volume.getValue()*fade);
        voices.get(0).level(base*.4f);voices.get(1).level(base*rain*.65f);voices.get(2).level(base*night*.35f);
    }
    public int owned(){return voices.size();}
    @Override public void close(){for(var v:voices)Minecraft.getInstance().getSoundManager().stop(v);voices.clear();fade=0;}
}
