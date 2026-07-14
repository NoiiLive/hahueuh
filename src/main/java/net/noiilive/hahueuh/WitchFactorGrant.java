package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.WitchFactorAuthority;
import net.noiilive.hahueuh.snapshot.PlayerAuthorityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public final class WitchFactorGrant {
    private WitchFactorGrant() {}

    public static void grant(ServerPlayer player, WitchFactorAuthority authority) {
        if (authority == WitchFactorAuthority.NONE) return;
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        UUID uuid = player.getUUID();
        MinecraftServer server = player.serverLevel().getServer();

        if (ConfigMain.SINGLE_AUTHORITY_HOLDER.get()) {
            List<UUID> currentHolders = switch (authority) {
                case SLOTH -> am.holdersOfWitchFactorSloth();
                case GREED -> am.holdersOfWitchFactorGreed();
                case NONE -> List.of();
            };
            for (UUID holder : currentHolders) {
                if (holder.equals(uuid)) continue;
                switch (authority) {
                    case SLOTH -> am.setWitchFactorSloth(holder, false);
                    case GREED -> am.setWitchFactorGreed(holder, false);
                    case NONE -> {}
                }
                ServerPlayer previous = server.getPlayerList().getPlayer(holder);
                if (previous != null) {
                    previous.displayClientMessage(Component.translatable("hahueuh.message.witch_factor_reassigned",
                            Component.translatable(authority.translationKey), player.getName())
                            .withStyle(ChatFormatting.RED), true);
                }
            }
        }

        switch (authority) {
            case SLOTH -> {
                am.setSloth(uuid, true);
                am.setWitchFactorSloth(uuid, true);
                am.setSlothVariant(uuid, SlothVariant.randomWeighted(player.getRandom()));
                HahUeuh.SLOTH_COMPAT.ensureStartingScore(uuid);
            }
            case GREED -> {
                am.setGreed(uuid, true);
                am.setWitchFactorGreed(uuid, true);
                am.setGreedVariant(uuid, GreedVariant.randomWeighted(player.getRandom()));
                HahUeuh.GREED_COMPAT.ensureStartingScore(uuid);
            }
            case NONE -> {}
        }
        HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(player);
    }

    public static void revokeAllForRelease(MinecraftServer server, WitchFactorAuthority authority) {
        if (!ConfigMain.SINGLE_AUTHORITY_HOLDER.get() || authority == WitchFactorAuthority.NONE) return;
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        List<UUID> currentHolders = switch (authority) {
            case SLOTH -> am.holdersOfWitchFactorSloth();
            case GREED -> am.holdersOfWitchFactorGreed();
            case NONE -> List.of();
        };
        for (UUID holder : currentHolders) {
            switch (authority) {
                case SLOTH -> am.setWitchFactorSloth(holder, false);
                case GREED -> am.setWitchFactorGreed(holder, false);
                case NONE -> {}
            }
            ServerPlayer previous = server.getPlayerList().getPlayer(holder);
            if (previous != null) {
                previous.displayClientMessage(Component.translatable("hahueuh.message.witch_factor_revoked_for_release",
                        Component.translatable(authority.translationKey)).withStyle(ChatFormatting.RED), true);
            }
        }
    }
}
