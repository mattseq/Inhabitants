package com.jeremyseq.inhabitants.entities.bogre.utilities;

import com.jeremyseq.inhabitants.entities.bogre.BogreEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

import java.util.UUID;

public class BogreNbtHelper {
    public static final String CAULDRON_POS_KEY = "CauldronPos";
    public static final String RESULT_OWNER_UUID_KEY = "OwnerUUID";
    public static final String IS_TAMED_KEY = "IsTamed";
    public static final String TAMED_OWNER_UUID_KEY = "TamedOwnerUUID";

    public static void save(BogreEntity bogre, CompoundTag tag) {
        if (bogre.cauldronPos != null) {
            tag.put(CAULDRON_POS_KEY, NbtUtils.writeBlockPos(bogre.cauldronPos));
        }
        UUID ownerUUID = bogre.getAi().getResultOwnerUUID();
        if (ownerUUID != null) {
            tag.putUUID(RESULT_OWNER_UUID_KEY, ownerUUID);
        }

        tag.putBoolean(IS_TAMED_KEY, bogre.isTamed());

        if (bogre.getTamedOwnerUUID().isPresent()) {
            tag.putUUID(TAMED_OWNER_UUID_KEY, bogre.getTamedOwnerUUID().get());
        }
    }

    public static void load(BogreEntity bogre, CompoundTag tag) {
        if (tag.contains(CAULDRON_POS_KEY)) {
            bogre.cauldronPos = NbtUtils.readBlockPos(tag.getCompound(CAULDRON_POS_KEY));
        }
        
        if (tag.hasUUID(RESULT_OWNER_UUID_KEY)) {
            bogre.getAi().setResultOwnerUUID(tag.getUUID(RESULT_OWNER_UUID_KEY));
        }

        bogre.setTamed(tag.getBoolean(IS_TAMED_KEY));
        if (tag.hasUUID(TAMED_OWNER_UUID_KEY)) {
            bogre.setTamedOwnerUUID(tag.getUUID(TAMED_OWNER_UUID_KEY));
        }
    }
}