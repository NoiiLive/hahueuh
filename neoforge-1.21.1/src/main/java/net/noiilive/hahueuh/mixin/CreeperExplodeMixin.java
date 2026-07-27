package net.noiilive.hahueuh.mixin;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Creeper.class)
public abstract class CreeperExplodeMixin {
    @Shadow private int swell;
    @Shadow private int explosionRadius;

    @Invoker("spawnLingeringCloud")
    protected abstract void hahueuh$spawnLingeringCloud();

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void hahueuh$creeperExplodeGuard(CallbackInfo ci) {
        Creeper self = (Creeper) (Object) this;
        if (self.level().isClientSide) return;
        UUID uuid = self.getUUID();

        if (HahUeuh.LIONS_HEART.isActive(uuid)) {
            float f = self.isPowered() ? 2.0F : 1.0F;
            self.level().explode(self, self.getX(), self.getY(), self.getZ(),
                    (float) this.explosionRadius * f, Level.ExplosionInteraction.MOB);
            hahueuh$spawnLingeringCloud();
            this.swell = 0;
            self.setSwellDir(-1);
            ci.cancel();
            return;
        }

        if (HahUeuh.SNAPSHOT_MANAGER.onEntityWouldSelfDestruct(self)) {
            this.swell = 0;
            self.setSwellDir(-1);
            ci.cancel();
        }
    }

    @Inject(method = "explodeCreeper", at = @At("TAIL"))
    private void hahueuh$creeperDropWitchFactor(CallbackInfo ci) {
        Creeper self = (Creeper) (Object) this;
        if (self.level().isClientSide) return;
        HahUeuh.MOB_WITCH_FACTOR.dropWitchFactor(self);
    }
}
