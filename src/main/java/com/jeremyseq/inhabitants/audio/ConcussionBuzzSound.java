package com.jeremyseq.inhabitants.audio;

import com.jeremyseq.inhabitants.audio.ModSoundEvents;
import com.jeremyseq.inhabitants.effects.ModEffects;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.resources.sounds.SoundInstance;

public class ConcussionBuzzSound extends AbstractTickableSoundInstance {
    private final Player player;

    public ConcussionBuzzSound(Player player) {
        super(ModSoundEvents.CONCUSSION_BUZZ.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 3.0F;
        this.pitch = 1.25F;
        this.relative = true;
    }

    @Override
    public void tick() {
        if (!player.isAlive() ||
        !player.hasEffect(ModEffects.CONCUSSION.get())) {
            this.stop();
        }
    }
}
