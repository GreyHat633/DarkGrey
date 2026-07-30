//
// Decompiled by Procyon v0.6.0
//

package com.greyhat.dark_grey.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.greyhat.dark_grey.api.capability.ISetComponent;
import com.greyhat.dark_grey.item.ItemRPGArmor;
import com.greyhat.dark_grey.mark.api.MarkApplyContext;

public class SetBonusManager {

    private static final WeakHashMap<EntityPlayer, Map<String, SetInfo>> ACTIVE_SETS;

    public static void recalculateSets(final EntityPlayer player) {
        if (player.worldObj.isRemote) {
            return;
        }
        final Map<String, SetInfo> previousSets = SetBonusManager.ACTIVE_SETS.get(player);
        final Map<String, ISetComponent> representativeInstances = new HashMap<String, ISetComponent>();
        final Map<String, Integer> counts = new HashMap<String, Integer>();
        for (int slot = 0; slot < 4; ++slot) {
            final ItemStack stack = player.inventory.armorInventory[slot];
            if (stack != null) {
                if (stack.getItem() instanceof ItemRPGArmor) {
                    final ItemRPGArmor armor = (ItemRPGArmor) stack.getItem();
                    final List<IRPGComponent> components = armor.getAllComponents();
                    for (final IRPGComponent comp : components) {
                        if (comp instanceof ISetComponent) {
                            final ISetComponent setComp = (ISetComponent) comp;
                            final String setId = setComp.getSetId();
                            counts.put(setId, counts.getOrDefault(setId, 0) + 1);
                            if (representativeInstances.containsKey(setId)) {
                                continue;
                            }
                            representativeInstances.put(setId, setComp);
                        }
                    }
                }
            }
        }
        java.util.Set<String> allSetIds = new java.util.HashSet<String>();
        if (previousSets != null) {
            allSetIds.addAll(previousSets.keySet());
        }
        allSetIds.addAll(counts.keySet());

        for (String setId : allSetIds) {
            int oldPieceCount = (previousSets != null && previousSets.containsKey(setId))
                ? previousSets.get(setId).pieceCount
                : 0;
            int newPieceCount = counts.getOrDefault(setId, 0);

            if (oldPieceCount != newPieceCount) {
                ISetComponent instanceToUse = null;
                if (newPieceCount > 0) {
                    instanceToUse = representativeInstances.get(setId);
                } else if (previousSets != null && previousSets.containsKey(setId)) {
                    instanceToUse = previousSets.get(setId).instance;
                }

                if (instanceToUse != null) {
                    instanceToUse.onSetPieceCountChanged(player, oldPieceCount, newPieceCount);
                }
            }
        }

        if (counts.isEmpty()) {
            notifyRemovedSets(player, previousSets, null);
            SetBonusManager.ACTIVE_SETS.remove(player);
        } else {
            final Map<String, SetInfo> playerSets = new HashMap<String, SetInfo>();
            for (final Map.Entry<String, Integer> entry : counts.entrySet()) {
                final String setId2 = entry.getKey();
                playerSets.put(setId2, new SetInfo(entry.getValue(), representativeInstances.get(setId2)));
            }
            notifyRemovedSets(player, previousSets, playerSets);
            SetBonusManager.ACTIVE_SETS.put(player, playerSets);
        }
    }

    private static void notifyRemovedSets(final EntityPlayer player, final Map<String, SetInfo> previousSets,
        final Map<String, SetInfo> currentSets) {
        if (previousSets == null || previousSets.isEmpty()) {
            return;
        }
        for (final Map.Entry<String, SetInfo> entry : previousSets.entrySet()) {
            if (currentSets == null || !currentSets.containsKey(entry.getKey())) {
                entry.getValue().instance.onSetTick(player, 0);
            }
        }
    }

    public static int getActiveSetCount(final EntityPlayer player, final String setId) {
        if (player.worldObj.isRemote) {
            int count = 0;
            for (int i = 0; i < 4; ++i) {
                final ItemStack stack = player.inventory.armorInventory[i];
                if (stack != null && stack.getItem() instanceof ItemRPGArmor) {
                    final ItemRPGArmor armor = (ItemRPGArmor) stack.getItem();
                    for (final IRPGComponent comp : armor.getAllComponents()) {
                        if (comp instanceof ISetComponent && ((ISetComponent) comp).getSetId()
                            .equals(setId)) {
                            ++count;
                            break;
                        }
                    }
                }
            }
            return count;
        }
        final Map<String, SetInfo> playerSets = SetBonusManager.ACTIVE_SETS.get(player);
        if (playerSets != null && playerSets.containsKey(setId)) {
            return playerSets.get(setId).pieceCount;
        }
        return 0;
    }

    public static float fireOnHit(final EntityPlayer attacker, final EntityLivingBase target,
        final net.minecraft.util.DamageSource source, final float damage) {
        final Map<String, SetInfo> playerSets = SetBonusManager.ACTIVE_SETS.get(attacker);
        if (playerSets == null || playerSets.isEmpty()) {
            return damage;
        }
        float currentDamage = damage;
        for (final SetInfo info : playerSets.values()) {
            currentDamage = info.instance.onSetHit(attacker, target, source, currentDamage, info.pieceCount);
        }
        return currentDamage;
    }

    public static void fireOnKill(final EntityPlayer killer, final EntityLivingBase victim) {
        final Map<String, SetInfo> playerSets = SetBonusManager.ACTIVE_SETS.get(killer);
        if (playerSets == null || playerSets.isEmpty()) {
            return;
        }
        for (final SetInfo info : playerSets.values()) {
            info.instance.onSetKill(killer, victim, info.pieceCount);
        }
    }

    public static void fireTick(final EntityPlayer player) {
        final Map<String, SetInfo> playerSets = SetBonusManager.ACTIVE_SETS.get(player);
        if (playerSets == null || playerSets.isEmpty()) {
            return;
        }
        for (final SetInfo info : playerSets.values()) {
            info.instance.onSetTick(player, info.pieceCount);
        }
    }

    public static int modifyMarkRequestedStacks(EntityPlayer applier, EntityLivingBase target, String markId,
        MarkApplyContext context, int requestedStacks) {
        final Map<String, SetInfo> playerSets = SetBonusManager.ACTIVE_SETS.get(applier);
        if (playerSets == null || playerSets.isEmpty()) {
            return requestedStacks;
        }
        int current = requestedStacks;
        for (final SetInfo info : playerSets.values()) {
            current = info.instance
                .modifyMarkRequestedStacks(applier, target, markId, context, current, info.pieceCount);
        }
        return Math.max(0, current);
    }

    static {
        ACTIVE_SETS = new WeakHashMap<EntityPlayer, Map<String, SetInfo>>();
    }

    public static class SetInfo {

        public final int pieceCount;
        public final ISetComponent instance;

        public SetInfo(final int pieceCount, final ISetComponent instance) {
            this.pieceCount = pieceCount;
            this.instance = instance;
        }
    }
}
