package com.example.hahueuh.client;

import com.example.hahueuh.HahUeuh;
import com.example.hahueuh.HahUeuhAbilities;
import com.example.hahueuh.api.Ability;
import com.example.hahueuh.api.event.RegisterAbilitiesEvent;
import com.example.hahueuh.network.ActivateAuthorityPayload;
import com.example.hahueuh.network.ClientSlothState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class HahUeuhClientAbilities {
    private HahUeuhClientAbilities() {}

    private static final SlothHandBehavior SLOTH_HAND_BEHAVIOR = new SlothHandBehavior();

    @SubscribeEvent
    static void onRegisterAbilities(RegisterAbilitiesEvent event) {
        event.register(Ability.builder(HahUeuhAbilities.DOMAIN_ABILITY, HahUeuhAbilities.DOMAIN_AUTHORITY)
                .translationKey("hahueuh.authority.domain")
                .onActivate(ctx -> PacketDistributor.sendToServer(ActivateAuthorityPayload.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.RETURN_BY_DEATH_ABILITY, HahUeuhAbilities.RETURN_BY_DEATH_AUTHORITY)
                .translationKey("hahueuh.authority.return_by_death")
                .shortLabel(() -> "RBD")
                .onActivate(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.getConnection() != null) {
                        mc.getConnection().sendChat("I can use Return by Death");
                    }
                })
                .build());

        event.register(Ability.builder(HahUeuhAbilities.SLOTH_HAND_ABILITY, HahUeuhAbilities.SLOTH_AUTHORITY)
                .holdBased()
                .translationKey(() -> ClientSlothState.slothVariant().translationKey)
                .shortLabel(() -> ClientSlothState.slothVariant().id
                        .substring(0, Math.min(3, ClientSlothState.slothVariant().id.length()))
                        .toUpperCase(Locale.ROOT))
                .icon(() -> ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID,
                        "textures/gui/icons/" + ClientSlothState.slothVariant().id + ".png"))
                .onHeldTick(SLOTH_HAND_BEHAVIOR)
                .build());
    }
}
