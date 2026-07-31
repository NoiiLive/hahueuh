package net.noiilive.hahueuh.mixin;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.noiilive.hahueuh.ModEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MaceBreachMixin {
    @Shadow
    protected abstract void hurtArmor(DamageSource source, float damage);

    @Shadow
    public abstract int getArmorValue();

    @Shadow
    public abstract double getAttributeValue(net.minecraft.world.entity.ai.attributes.Attribute attribute);

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void hahueuh$maceBreach(DamageSource source, float amount,
                                           CallbackInfoReturnable<Float> cir) {
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return;
        int breach = ModEnchantments.breachLevel(source);
        if (breach <= 0) return;

        this.hurtArmor(source, amount);
        float armor = this.getArmorValue();
        float toughness = (float) this.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float scale = 2.0f + toughness / 4.0f;
        float clamped = Mth.clamp(armor - amount / scale, armor * 0.2f, 20.0f);
        float effectiveness = ModEnchantments.armorEffectiveness(clamped / 25.0f, breach);
        cir.setReturnValue(amount * (1.0f - effectiveness));
    }
}
