package net.noiilive.hahueuh.command;

import net.noiilive.hahueuh.ConfigSloth;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.SlothVariant;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.Set;

public class RezeroCommand {
    private static final float HALF_HEART = 1.0f;

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rezero")
                .then(Commands.literal("checkpoint")
                        .requires(source -> source.hasPermission(2))
                        .executes(RezeroCommand::runCheckpoint))
                .then(Commands.literal("halfheart")
                        .executes(RezeroCommand::runHalfHeart))
                .then(Commands.literal("authority")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("returnbydeath")
                                        .then(Commands.literal("acquired")
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(RezeroCommand::runAuthority))))
                                .then(Commands.literal("domain")
                                        .then(Commands.literal("acquired")
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(RezeroCommand::runDomainAuthority))))
                                .then(Commands.literal("sloth")
                                        .then(Commands.literal("acquired")
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(RezeroCommand::runSlothAuthority)))
                                        .then(Commands.literal("variant")
                                                .then(Commands.literal("invisibleprovidence")
                                                        .executes(ctx -> runSlothVariant(ctx, SlothVariant.INVISIBLE_PROVIDENCE)))
                                                .then(Commands.literal("unseenhands")
                                                        .executes(ctx -> runSlothVariant(ctx, SlothVariant.UNSEEN_HANDS)))
                                                .then(Commands.literal("sekhmet")
                                                        .executes(ctx -> runSlothVariant(ctx, SlothVariant.SEKHMET))))
                                        .then(Commands.literal("compatibility")
                                                .then(Commands.literal("get")
                                                        .executes(RezeroCommand::runGetSlothCompat))
                                                .then(Commands.literal("set")
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                                .executes(RezeroCommand::runSetSlothCompat)))))
                                .then(Commands.literal("greed")
                                        .then(Commands.literal("acquired")
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(RezeroCommand::runGreedAuthority)))
                                        .then(Commands.literal("variant")
                                                .then(Commands.literal("lionsheart")
                                                        .executes(ctx -> runGreedVariant(ctx, GreedVariant.LIONSHEART)))
                                                .then(Commands.literal("corleonis")
                                                        .executes(ctx -> runGreedVariant(ctx, GreedVariant.CORLEONIS))))
                                        .then(Commands.literal("compatibility")
                                                .then(Commands.literal("get")
                                                        .executes(RezeroCommand::runGetGreedCompat))
                                                .then(Commands.literal("set")
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                                .executes(RezeroCommand::runSetGreedCompat)))))))
                .then(Commands.literal("revive")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(RezeroCommand::runRevive)))
                .then(Commands.literal("ally")
                        .then(Commands.literal("request")
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .executes(RezeroCommand::runAllyRequest)))
                        .then(Commands.literal("accept")
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .executes(ctx -> runAllyResponse(ctx, true))))
                        .then(Commands.literal("decline")
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .executes(ctx -> runAllyResponse(ctx, false))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .executes(RezeroCommand::runAllyRemove)))));
    }

    private static int runCheckpoint(CommandContext<CommandSourceStack> ctx) {
        HahUeuh.SNAPSHOT_MANAGER.createSnapshot("command");
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.checkpoint_created")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int runHalfHeart(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        player.setHealth(HALF_HEART);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.half_heart")
                .withStyle(ChatFormatting.RED), false);
        return 1;
    }

    private static int runAuthority(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().setReturnByDeath(target.getUUID(), value);
        HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(target);

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.rbd_authority_set",
                target.getName(), value
        ).withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return 1;
    }

    private static int runDomainAuthority(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().setDomain(target.getUUID(), value);
        HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(target);

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.domain_authority_set",
                target.getName(), value
        ).withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return 1;
    }

    private static int runSlothAuthority(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().setSloth(target.getUUID(), value);
        HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(target);

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.sloth_authority_set",
                target.getName(), value
        ).withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return 1;
    }

    private static int runSlothVariant(CommandContext<CommandSourceStack> ctx, SlothVariant variant) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().setSloth(target.getUUID(), true);
        HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().setSlothVariant(target.getUUID(), variant);
        HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(target);

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.sloth_variant_set",
                target.getName(), Component.translatable(variant.translationKey)
        ).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return 1;
    }

    private static int runGetSlothCompat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int score = HahUeuh.SLOTH_COMPAT.getScore(target.getUUID());
        int threshold = ConfigSloth.SLOTH_COMPAT_THRESHOLD.getAsInt();
        ctx.getSource().sendSuccess(() -> Component.translatable(
                score >= threshold ? "hahueuh.command.sloth_compat_get_compatible" : "hahueuh.command.sloth_compat_get",
                target.getName(), score, threshold
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return score;
    }

    private static int runSetSlothCompat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        HahUeuh.SLOTH_COMPAT.setScore(target.getUUID(), amount);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.sloth_compat_set",
                target.getName(), amount
        ).withStyle(ChatFormatting.GREEN), true);
        return amount;
    }

    private static int runGreedAuthority(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().setGreed(target.getUUID(), value);
        if (!value) {
            HahUeuh.LITTLE_KING.releaseAllImplants(target.getUUID());
        }
        HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(target);

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.greed_authority_set",
                target.getName(), value
        ).withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return 1;
    }

    private static int runGreedVariant(CommandContext<CommandSourceStack> ctx, GreedVariant variant) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().setGreed(target.getUUID(), true);
        HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().setGreedVariant(target.getUUID(), variant);
        if (variant != GreedVariant.LIONSHEART) {
            HahUeuh.LITTLE_KING.releaseAllImplants(target.getUUID());
        }
        HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(target);

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.greed_variant_set",
                target.getName(), Component.translatable(variant.translationKey)
        ).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return 1;
    }

    private static int runGetGreedCompat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int score = HahUeuh.GREED_COMPAT.getScore(target.getUUID());
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.greed_compat_get",
                target.getName(), score
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return score;
    }

    private static int runSetGreedCompat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        HahUeuh.GREED_COMPAT.setScore(target.getUUID(), amount);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.greed_compat_set",
                target.getName(), amount
        ).withStyle(ChatFormatting.GREEN), true);
        return amount;
    }

    private static int runAllyRequest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer requester = ctx.getSource().getPlayerOrException();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
        for (GameProfile profile : profiles) {
            HahUeuh.PLAYER_ALLIES.requestAlly(requester, profile);
        }
        return profiles.size();
    }

    private static int runAllyResponse(CommandContext<CommandSourceStack> ctx, boolean accept) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
        for (GameProfile profile : profiles) {
            if (accept) {
                HahUeuh.PLAYER_ALLIES.acceptRequest(target, profile);
            } else {
                HahUeuh.PLAYER_ALLIES.declineRequest(target, profile);
            }
        }
        return profiles.size();
    }

    private static int runAllyRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
        for (GameProfile profile : profiles) {
            HahUeuh.PLAYER_ALLIES.removeAlly(player, profile);
        }
        return profiles.size();
    }

    private static int runRevive(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        boolean keepInventory = target.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        DimensionTransition transition = target.findRespawnPositionAndUseSpawnBlock(keepInventory, DimensionTransition.DO_NOTHING);

        target.setGameMode(GameType.SURVIVAL);
        target.teleportTo(transition.newLevel(), transition.pos().x, transition.pos().y, transition.pos().z,
                Set.of(), transition.yRot(), transition.xRot());
        target.setHealth(target.getMaxHealth());

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.revived", target.getName())
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
