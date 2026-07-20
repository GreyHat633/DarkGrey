package com.greyhat.dark_grey.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import com.google.gson.JsonObject;
import com.greyhat.dark_grey.DarkGrey;
import com.greyhat.dark_grey.api.IRPGComponent;
import com.greyhat.dark_grey.api.capability.IHasTooltip;
import com.greyhat.dark_grey.api.capability.IOnHeldTick;

public class ComponentTorchAfterglow implements IRPGComponent, IOnHeldTick, IHasTooltip {

    private static final Set<Integer> REPORTED_INVALID_POTION_IDS = Collections.synchronizedSet(new HashSet<Integer>());
    private static final Set<Integer> REPORTED_BROKEN_POTION_IDS = Collections.synchronizedSet(new HashSet<Integer>());
    private static final Set<String> REPORTED_RUNTIME_FAILURES = Collections.synchronizedSet(new HashSet<String>());

    @Override
    public String getComponentId() {
        return "炬火残光";
    }

    @Override
    public void configure(JsonObject params) {}

    @Override
    public void addTooltipLines(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add(
            "\u00A76\u263C \u70AC\u706B\u6B8B\u5149 \u00A77| \u00A7e\u6301\u6709\u65F6\u514D\u75AB\u4E00\u5207\u8D1F\u9762\u72B6\u6001");
    }

    @Override
    public void onHeldTick(ItemStack weaponStack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return;
        }

        // Immunity must feel immediate. This runs only for the selected RPG item,
        // and the empty-effect fast path keeps the per-tick cost negligible.
        final Collection<?> activeEffects;
        try {
            activeEffects = new ArrayList<Object>(player.getActivePotionEffects());
        } catch (Throwable failure) {
            reportRuntimeFailure(player, "snapshot", -1, failure);
            return;
        }
        if (activeEffects.isEmpty()) return;

        List<Integer> toRemove = new ArrayList<>();
        for (Object rawEffect : activeEffects) {
            if (!(rawEffect instanceof PotionEffect)) {
                reportRuntimeFailure(
                    player,
                    "invalid-entry",
                    -1,
                    new ClassCastException(
                        "Expected PotionEffect but found " + (rawEffect == null ? "null"
                            : rawEffect.getClass()
                                .getName())));
                continue;
            }

            PotionEffect effect = (PotionEffect) rawEffect;
            int potionId = -1;
            try {
                potionId = effect.getPotionID();
                Potion[] potionTypes = Potion.potionTypes;
                if (potionTypes == null || potionId < 0 || potionId >= potionTypes.length) {
                    if (REPORTED_INVALID_POTION_IDS.add(potionId)) {
                        DarkGrey.LOG.warn(
                            "Torch Afterglow ignored out-of-range potion ID " + potionId
                                + " instead of disconnecting the holder.");
                    }
                    continue;
                }

                Potion potion = potionTypes[potionId];
                if (potion != null && potion.isBadEffect()) {
                    toRemove.add(potionId);
                }
            } catch (Throwable failure) {
                rethrowFatal(failure);
                if (REPORTED_BROKEN_POTION_IDS.add(potionId)) {
                    DarkGrey.LOG.error(
                        "Torch Afterglow could not inspect potion ID " + potionId
                            + "; the effect was left unchanged to keep the player connected.",
                        failure);
                }
            }
        }

        for (Integer id : toRemove) {
            try {
                player.removePotionEffect(id);
            } catch (Throwable failure) {
                reportRuntimeFailure(player, "remove", id, failure);
            }
        }
    }

    private static void reportRuntimeFailure(EntityPlayer player, String stage, int potionId, Throwable failure) {
        rethrowFatal(failure);
        String failureType = failure == null ? "unknown"
            : failure.getClass()
                .getName();
        String key = stage + ":" + potionId + ":" + failureType;
        if (REPORTED_RUNTIME_FAILURES.add(key)) {
            DarkGrey.LOG.error(
                "Torch Afterglow isolated a potion failure during " + stage
                    + " for player "
                    + (player == null ? "<unknown>" : player.getCommandSenderName())
                    + " (potionId="
                    + potionId
                    + "). The holder remains connected.",
                failure);
        }
    }

    private static void rethrowFatal(Throwable failure) {
        if (failure instanceof ThreadDeath) {
            throw (ThreadDeath) failure;
        }
        if (failure instanceof VirtualMachineError) {
            throw (VirtualMachineError) failure;
        }
    }
}
