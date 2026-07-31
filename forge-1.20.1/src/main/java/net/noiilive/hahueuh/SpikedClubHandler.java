package net.noiilive.hahueuh;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class SpikedClubHandler {
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        Entity attacker = event.getSource().getDirectEntity();
        if (!(attacker instanceof LivingEntity living)) return;
        if (!(living.getMainHandItem().getItem() instanceof SpikedClubItem)) return;
        if (!SpikedClubItem.canSmashAttack(living)) return;

        float bonus = SpikedClubItem.smashBonusDamage(living.fallDistance);
        bonus += SpikedClubItem.densityBonus(living.getMainHandItem(), living.fallDistance);
        event.setAmount(event.getAmount() + bonus);
    }
}
