package net.noiilive.hahueuh.command;

import net.noiilive.hahueuh.BookOfLifeStats;
import net.noiilive.hahueuh.ConfigMagic;
import net.noiilive.hahueuh.ConfigMain;
import net.noiilive.hahueuh.ConfigSloth;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.BookOfLifeAging;
import net.noiilive.hahueuh.ChunkManaData;
import net.noiilive.hahueuh.ChunkMiasmaData;
import net.noiilive.hahueuh.GateDefectiveState;
import net.noiilive.hahueuh.GateStrain;
import net.noiilive.hahueuh.MagicSchool;
import net.noiilive.hahueuh.ModAttachments;
import net.noiilive.hahueuh.ConfigPlayer;
import net.noiilive.hahueuh.PlayerStats;
import net.noiilive.hahueuh.StatBonuses;
import net.noiilive.hahueuh.network.PlayerStat;
import net.noiilive.hahueuh.network.StatEntry;
import net.noiilive.hahueuh.PlayerLifespan;
import net.noiilive.hahueuh.network.GateDefectiveVariant;
import net.noiilive.hahueuh.network.GateStatus;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.PlayerRace;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.WitchFactorAuthority;
import net.noiilive.hahueuh.snapshot.PlayerAuthorityManager;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

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
                                        .then(Commands.literal("witchfactor")
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(RezeroCommand::runSlothWitchFactor)))
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
                                        .then(Commands.literal("witchfactor")
                                                .then(Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(RezeroCommand::runGreedWitchFactor)))
                                        .then(Commands.literal("variant")
                                                .then(Commands.literal("lionsheart")
                                                        .executes(ctx -> runGreedVariant(ctx, GreedVariant.LIONSHEART)))
                                                .then(Commands.literal("corleonis")
                                                        .executes(ctx -> runGreedVariant(ctx, GreedVariant.CORLEONIS)))
                                                .then(Commands.literal("echidna")
                                                        .executes(ctx -> runGreedVariant(ctx, GreedVariant.ECHIDNA))))
                                        .then(Commands.literal("compatibility")
                                                .then(Commands.literal("get")
                                                        .executes(RezeroCommand::runGetGreedCompat))
                                                .then(Commands.literal("set")
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                                .executes(RezeroCommand::runSetGreedCompat)))))))
                .then(magicCommand())
                .then(Commands.literal("sagecandidate")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(RezeroCommand::runSageCandidate))))
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
                                        .executes(RezeroCommand::runAllyRemove)))
                        .then(Commands.literal("attacks")
                                .executes(RezeroCommand::runAllyAttacksGet)
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(RezeroCommand::runAllyAttacksSet))))
                .then(Commands.literal("race")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("get")
                                        .executes(RezeroCommand::runGetRace))
                                .then(Commands.literal("set")
                                        .then(Commands.literal("human")
                                                .executes(ctx -> runSetRace(ctx, PlayerRace.HUMAN)))
                                        .then(Commands.literal("elf")
                                                .executes(ctx -> runSetRace(ctx, PlayerRace.ELF)))
                                        .then(Commands.literal("half_elf")
                                                .executes(ctx -> runSetRace(ctx, PlayerRace.HALF_ELF))))))
                .then(Commands.literal("gate")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("get")
                                        .executes(RezeroCommand::runGetGate))
                                .then(Commands.literal("set")
                                        .then(Commands.literal("open")
                                                .executes(ctx -> runSetGate(ctx, GateStatus.OPEN)))
                                        .then(Commands.literal("partly_open")
                                                .executes(ctx -> runSetGate(ctx, GateStatus.PARTLY_OPEN)))
                                        .then(Commands.literal("damaged")
                                                .executes(ctx -> runSetGate(ctx, GateStatus.DAMAGED)))
                                        .then(Commands.literal("destroyed")
                                                .executes(ctx -> runSetGate(ctx, GateStatus.DESTROYED)))
                                        .then(Commands.literal("defective")
                                                .executes(ctx -> runSetGate(ctx, GateStatus.DEFECTIVE))))
                                .then(Commands.literal("defective")
                                        .then(Commands.literal("get")
                                                .executes(RezeroCommand::runGetGateDefective))
                                        .then(Commands.literal("set")
                                                .then(Commands.literal("no_absorption")
                                                        .executes(ctx -> runSetGateDefective(ctx, GateDefectiveVariant.NO_ABSORPTION)))
                                                .then(Commands.literal("no_release")
                                                        .executes(ctx -> runSetGateDefective(ctx, GateDefectiveVariant.NO_RELEASE)))))
                                .then(Commands.literal("output")
                                        .then(Commands.literal("get")
                                                .executes(RezeroCommand::runGetGateOutput))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(RezeroCommand::runSetGateOutput))))
                                .then(Commands.literal("efficiency")
                                        .then(Commands.literal("get")
                                                .executes(RezeroCommand::runGetGateEfficiency))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(RezeroCommand::runSetGateEfficiency))))
                                .then(Commands.literal("strain")
                                        .then(Commands.literal("get")
                                                .executes(RezeroCommand::runGetGateStrain))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 100))
                                                        .executes(RezeroCommand::runSetGateStrain))))))
                .then(Commands.literal("age")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("get")
                                        .executes(RezeroCommand::runGetAge))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(RezeroCommand::runSetAge)))))
                .then(Commands.literal("lifespan")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("get")
                                        .executes(RezeroCommand::runGetLifespan))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(RezeroCommand::runSetLifespan)))))
                .then(buildStatsNode())
                .then(Commands.literal("od")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("get")
                                        .executes(RezeroCommand::runGetOd))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(RezeroCommand::runSetOd)))
                                .then(Commands.literal("max")
                                        .executes(RezeroCommand::runMaxOd))))
                .then(Commands.literal("mana")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(RezeroCommand::runSetMana)))))
                .then(Commands.literal("chunkmana")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("get")
                                .executes(RezeroCommand::runGetChunkMana))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(RezeroCommand::runSetChunkMana))))
                .then(Commands.literal("chunkmiasma")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("get")
                                .executes(RezeroCommand::runGetChunkMiasma))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(RezeroCommand::runSetChunkMiasma)))));
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

    private static void enforceSingleHolder(CommandContext<CommandSourceStack> ctx, ServerPlayer target,
            List<UUID> currentHolders, BiConsumer<UUID, Boolean> revoke, String messageKey, String authorityNameKey) {
        if (!ConfigMain.SINGLE_AUTHORITY_HOLDER.get()) return;
        MinecraftServer server = ctx.getSource().getServer();
        for (UUID holder : currentHolders) {
            if (holder.equals(target.getUUID())) continue;
            revoke.accept(holder, false);
            ServerPlayer previous = server.getPlayerList().getPlayer(holder);
            if (previous == null) continue;
            HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(previous);
            previous.displayClientMessage(Component.translatable(messageKey,
                    Component.translatable(authorityNameKey), target.getName()).withStyle(ChatFormatting.RED), true);
        }
    }

    private static void defaultGrantWitchFactor(ServerPlayer target, List<UUID> currentWitchFactorHolders,
            BiConsumer<UUID, Boolean> setWitchFactor, WitchFactorAuthority sin) {
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        UUID uuid = target.getUUID();
        if (!am.isSageCandidate(uuid) && am.hasOtherWitchFactor(uuid, sin)) return;
        boolean conflict = ConfigMain.SINGLE_AUTHORITY_HOLDER.get()
                && currentWitchFactorHolders.stream().anyMatch(u -> !u.equals(uuid));
        if (!conflict) setWitchFactor.accept(uuid, true);
    }

    private static int runAuthority(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        if (value) {
            enforceSingleHolder(ctx, target, am.holdersOfReturnByDeath(), am::setReturnByDeath,
                    "hahueuh.message.authority_reassigned", "hahueuh.authority.return_by_death");
        }
        am.setReturnByDeath(target.getUUID(), value);
        HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(target);

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.rbd_authority_set",
                target.getName(), String.valueOf(value)
        ).withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return 1;
    }

    private static int runDomainAuthority(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        if (value) {
            enforceSingleHolder(ctx, target, am.holdersOfDomain(), am::setDomain,
                    "hahueuh.message.authority_reassigned", "hahueuh.authority.domain");
        }
        am.setDomain(target.getUUID(), value);
        HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(target);

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.domain_authority_set",
                target.getName(), String.valueOf(value)
        ).withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return 1;
    }

    private static int runSlothAuthority(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        if (value) {
            HahUeuh.SLOTH_COMPAT.ensureStartingScore(target.getUUID());
            defaultGrantWitchFactor(target, am.holdersOfWitchFactorSloth(), am::setWitchFactorSloth, WitchFactorAuthority.SLOTH);
        } else {
            am.setWitchFactorSloth(target.getUUID(), false);
        }
        am.setSloth(target.getUUID(), value);
        HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(target);

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.sloth_authority_set",
                target.getName(), String.valueOf(value)
        ).withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return 1;
    }

    private static int runSlothWitchFactor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        if (value && !am.canUseSloth(target.getUUID())) {
            ctx.getSource().sendFailure(Component.translatable("hahueuh.command.witch_factor_needs_authority",
                    target.getName(), Component.translatable("hahueuh.authority.sloth")));
            return 0;
        }
        if (value && !am.isSageCandidate(target.getUUID()) && am.hasOtherWitchFactor(target.getUUID(), WitchFactorAuthority.SLOTH)) {
            ctx.getSource().sendFailure(Component.translatable("hahueuh.command.witch_factor_needs_sage_candidate",
                    target.getName()));
            return 0;
        }
        if (value) {
            enforceSingleHolder(ctx, target, am.holdersOfWitchFactorSloth(), am::setWitchFactorSloth,
                    "hahueuh.message.witch_factor_reassigned", "hahueuh.authority.sloth");
        }
        am.setWitchFactorSloth(target.getUUID(), value);

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.sloth_witch_factor_set",
                target.getName(), String.valueOf(value)
        ).withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return 1;
    }

    private static int runSlothVariant(CommandContext<CommandSourceStack> ctx, SlothVariant variant) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        HahUeuh.SLOTH_COMPAT.ensureStartingScore(target.getUUID());
        defaultGrantWitchFactor(target, am.holdersOfWitchFactorSloth(), am::setWitchFactorSloth, WitchFactorAuthority.SLOTH);
        am.setSloth(target.getUUID(), true);
        am.setSlothVariant(target.getUUID(), variant);
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
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        if (value) {
            HahUeuh.GREED_COMPAT.ensureStartingScore(target.getUUID());
            defaultGrantWitchFactor(target, am.holdersOfWitchFactorGreed(), am::setWitchFactorGreed, WitchFactorAuthority.GREED);
        } else {
            am.setWitchFactorGreed(target.getUUID(), false);
        }
        am.setGreed(target.getUUID(), value);
        if (!value) {
            HahUeuh.LITTLE_KING.releaseAllImplants(target.getUUID());
        }
        HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(target);

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.greed_authority_set",
                target.getName(), String.valueOf(value)
        ).withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return 1;
    }

    private static int runGreedWitchFactor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        if (value && !am.canUseGreed(target.getUUID())) {
            ctx.getSource().sendFailure(Component.translatable("hahueuh.command.witch_factor_needs_authority",
                    target.getName(), Component.translatable("hahueuh.authority.greed")));
            return 0;
        }
        if (value && !am.isSageCandidate(target.getUUID()) && am.hasOtherWitchFactor(target.getUUID(), WitchFactorAuthority.GREED)) {
            ctx.getSource().sendFailure(Component.translatable("hahueuh.command.witch_factor_needs_sage_candidate",
                    target.getName()));
            return 0;
        }
        if (value) {
            enforceSingleHolder(ctx, target, am.holdersOfWitchFactorGreed(), am::setWitchFactorGreed,
                    "hahueuh.message.witch_factor_reassigned", "hahueuh.authority.greed");
        }
        am.setWitchFactorGreed(target.getUUID(), value);

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.greed_witch_factor_set",
                target.getName(), String.valueOf(value)
        ).withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return 1;
    }

    private static int runGreedVariant(CommandContext<CommandSourceStack> ctx, GreedVariant variant) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        HahUeuh.GREED_COMPAT.ensureStartingScore(target.getUUID());
        defaultGrantWitchFactor(target, am.holdersOfWitchFactorGreed(), am::setWitchFactorGreed, WitchFactorAuthority.GREED);
        am.setGreed(target.getUUID(), true);
        am.setGreedVariant(target.getUUID(), variant);
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

    private static int runSageCandidate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().setSageCandidate(target.getUUID(), value);

        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.sage_candidate_set",
                target.getName(), String.valueOf(value)
        ).withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return 1;
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

    private static int runAllyAttacksGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean enabled = HahUeuh.PLAYER_ALLIES.allyAttacksEnabled(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.translatable(enabled
                ? "hahueuh.command.ally_attacks_on" : "hahueuh.command.ally_attacks_off")
                .withStyle(enabled ? ChatFormatting.YELLOW : ChatFormatting.GREEN), false);
        return enabled ? 1 : 0;
    }

    private static int runAllyAttacksSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean enabled = BoolArgumentType.getBool(ctx, "value");
        HahUeuh.PLAYER_ALLIES.setAllyAttacks(player.getUUID(), enabled);
        ctx.getSource().sendSuccess(() -> Component.translatable(enabled
                ? "hahueuh.command.ally_attacks_set_on" : "hahueuh.command.ally_attacks_set_off")
                .withStyle(enabled ? ChatFormatting.YELLOW : ChatFormatting.GREEN), false);
        return enabled ? 1 : 0;
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

    private static int runGetRace(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PlayerRace race = target.getData(ModAttachments.PLAYER_RACE.get());
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.race_get",
                target.getName(), Component.translatable(race.translationKey)
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int runSetRace(CommandContext<CommandSourceStack> ctx, PlayerRace race) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PlayerRace previousRace = target.getData(ModAttachments.PLAYER_RACE.get());
        target.setData(ModAttachments.PLAYER_RACE.get(), race);
        if (previousRace != race) {
            PlayerLifespan.reroll(target, race);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.race_set",
                target.getName(), Component.translatable(race.translationKey)
        ).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int runGetGate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        GateStatus gate = target.getData(ModAttachments.PLAYER_GATE_STATUS.get());
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.gate_get",
                target.getName(), Component.translatable(gate.translationKey)
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int runSetGate(CommandContext<CommandSourceStack> ctx, GateStatus gate) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        GateStatus previousGate = target.getData(ModAttachments.PLAYER_GATE_STATUS.get());
        target.setData(ModAttachments.PLAYER_GATE_STATUS.get(), gate);
        if (gate == GateStatus.DEFECTIVE && previousGate != GateStatus.DEFECTIVE) {
            GateDefectiveState.reroll(target);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.gate_set",
                target.getName(), Component.translatable(gate.translationKey)
        ).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int runGetGateDefective(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        GateDefectiveState.ensureRolled(target);
        GateDefectiveVariant variant = GateDefectiveVariant.byOrdinal(
                target.getData(ModAttachments.PLAYER_GATE_DEFECTIVE_VARIANT.get()));
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.gate_defective_get",
                target.getName(), Component.translatable(variant.translationKey)
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int runSetGateDefective(CommandContext<CommandSourceStack> ctx, GateDefectiveVariant variant) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        target.setData(ModAttachments.PLAYER_GATE_DEFECTIVE_VARIANT.get(), variant.ordinal());
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.gate_defective_set",
                target.getName(), Component.translatable(variant.translationKey)
        ).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int runGetGateOutput(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        GateStrain.ensureRolled(target);
        int output = target.getData(ModAttachments.PLAYER_GATE_OUTPUT.get());
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.gate_output_get",
                target.getName(), output
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return output;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> magicCommand() {
        var playerArg = Commands.argument("player", EntityArgument.player());
        for (MagicSchool school : MagicSchool.values()) {
            playerArg.then(Commands.literal(school.id)
                    .then(Commands.literal("acquired")
                            .then(Commands.argument("value", BoolArgumentType.bool())
                                    .executes(ctx -> runMagicAcquired(ctx, school)))));
        }
        return Commands.literal("magic").requires(source -> source.hasPermission(2)).then(playerArg);
    }

    private static int runMagicAcquired(CommandContext<CommandSourceStack> ctx, MagicSchool school)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean value = BoolArgumentType.getBool(ctx, "value");
        school.grant(target, value);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.magic_acquired_set",
                target.getName(), Component.translatable(school.translationKey), String.valueOf(value)
        ).withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return 1;
    }

    private static int runSetGateOutput(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        final int amount = Math.min(IntegerArgumentType.getInteger(ctx, "amount"),
                ConfigMagic.GATE_OUTPUT_MAX.getAsInt());
        target.setData(ModAttachments.PLAYER_GATE_OUTPUT.get(), amount);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.gate_output_set",
                target.getName(), amount
        ).withStyle(ChatFormatting.GREEN), true);
        return amount;
    }

    private static int runGetGateEfficiency(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        GateStrain.ensureRolled(target);
        int efficiency = target.getData(ModAttachments.PLAYER_GATE_EFFICIENCY.get());
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.gate_efficiency_get",
                target.getName(), efficiency
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return efficiency;
    }

    private static int runSetGateEfficiency(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        final int amount = Math.min(IntegerArgumentType.getInteger(ctx, "amount"),
                ConfigMagic.GATE_EFFICIENCY_MAX.getAsInt());
        target.setData(ModAttachments.PLAYER_GATE_EFFICIENCY.get(), amount);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.gate_efficiency_set",
                target.getName(), amount
        ).withStyle(ChatFormatting.GREEN), true);
        return amount;
    }

    private static int runGetGateStrain(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int strain = target.getData(ModAttachments.PLAYER_GATE_STRAIN.get());
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.gate_strain_get",
                target.getName(), strain
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return strain;
    }

    private static int runSetGateStrain(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        GateStrain.setStrain(target, amount);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.gate_strain_set",
                target.getName(), amount
        ).withStyle(ChatFormatting.GREEN), true);
        return amount;
    }

    private static int runGetAge(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int age = target.getData(ModAttachments.PLAYER_AGE.get());
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.age_get",
                target.getName(), age
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return age;
    }

    private static int runSetAge(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        target.setData(ModAttachments.PLAYER_AGE.get(), amount);
        BookOfLifeAging.checkOldAge(target);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.age_set",
                target.getName(), amount
        ).withStyle(ChatFormatting.GREEN), true);
        return amount;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildStatsNode() {
        com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> playerNode =
                Commands.argument("player", EntityArgument.player());

        for (PlayerStat stat : PlayerStat.ORDERED) {
            playerNode = playerNode.then(Commands.literal(stat.id)
                    .then(Commands.literal("get")
                            .executes(ctx -> runGetStat(ctx, stat)))
                    .then(Commands.literal("set")
                            .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                    .executes(ctx -> runSetStatLevel(ctx, stat))))
                    .then(Commands.literal("proficiency")
                            .then(Commands.literal("get")
                                    .executes(ctx -> runGetStatRoll(ctx, stat, true)))
                            .then(Commands.literal("set")
                                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(ctx -> runSetStatRoll(ctx, stat, true)))))
                    .then(Commands.literal("capacity")
                            .then(Commands.literal("get")
                                    .executes(ctx -> runGetStatRoll(ctx, stat, false)))
                            .then(Commands.literal("set")
                                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(ctx -> runSetStatRoll(ctx, stat, false))))));
        }

        return Commands.literal("stats")
                .requires(source -> source.hasPermission(2))
                .then(playerNode);
    }

    private static int runGetStat(CommandContext<CommandSourceStack> ctx, PlayerStat stat)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PlayerStats.ensureRolled(target);
        StatEntry entry = PlayerStats.get(target, stat);
        int perLevel = ConfigPlayer.STAT_PROGRESS_PER_LEVEL.getAsInt();
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.stat_get",
                target.getName(), Component.translatable(stat.translationKey),
                entry.level(), StatBonuses.levelCap(entry), entry.progress(), perLevel,
                entry.proficiency(), entry.capacity()
        ).withStyle(ChatFormatting.AQUA), false);
        return entry.level();
    }

    private static int runSetStatLevel(CommandContext<CommandSourceStack> ctx, PlayerStat stat)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        PlayerStats.setLevel(target, stat, amount);
        StatEntry entry = PlayerStats.get(target, stat);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.stat_level_set",
                target.getName(), Component.translatable(stat.translationKey),
                entry.level(), StatBonuses.levelCap(entry)
        ).withStyle(ChatFormatting.GREEN), true);
        return entry.level();
    }

    private static int runGetStatRoll(CommandContext<CommandSourceStack> ctx, PlayerStat stat, boolean proficiency)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PlayerStats.ensureRolled(target);
        StatEntry entry = PlayerStats.get(target, stat);
        int value = proficiency ? entry.proficiency() : entry.capacity();
        ctx.getSource().sendSuccess(() -> Component.translatable(
                proficiency ? "hahueuh.command.stat_proficiency_get" : "hahueuh.command.stat_capacity_get",
                target.getName(), Component.translatable(stat.translationKey), value
        ).withStyle(ChatFormatting.AQUA), false);
        return value;
    }

    private static int runSetStatRoll(CommandContext<CommandSourceStack> ctx, PlayerStat stat, boolean proficiency)
            throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        int value = proficiency
                ? PlayerStats.setProficiency(target, stat, amount)
                : PlayerStats.setCapacity(target, stat, amount);
        StatEntry entry = PlayerStats.get(target, stat);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                proficiency ? "hahueuh.command.stat_proficiency_set" : "hahueuh.command.stat_capacity_set",
                target.getName(), Component.translatable(stat.translationKey), value,
                entry.level(), StatBonuses.levelCap(entry)
        ).withStyle(ChatFormatting.GREEN), true);
        return value;
    }

    private static int runGetLifespan(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PlayerLifespan.ensureRolled(target);
        int lifespan = target.getData(ModAttachments.PLAYER_LIFESPAN.get());
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.lifespan_get",
                target.getName(), lifespan
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return lifespan;
    }

    private static int runSetLifespan(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        target.setData(ModAttachments.PLAYER_LIFESPAN.get(), amount);
        BookOfLifeStats.clampToMax(target);
        BookOfLifeStats.setOdToMax(target);
        BookOfLifeAging.checkOldAge(target);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.lifespan_set",
                target.getName(), amount
        ).withStyle(ChatFormatting.GREEN), true);
        return amount;
    }

    private static int runGetOd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int od = target.getData(ModAttachments.PLAYER_OD_CURRENT.get());
        int max = BookOfLifeStats.maxOd(target);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.od_get",
                target.getName(), od, max
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return od;
    }

    private static int runSetOd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        int max = BookOfLifeStats.maxOd(target);
        int clamped = Math.min(amount, max);
        target.setData(ModAttachments.PLAYER_OD_CURRENT.get(), clamped);
        HahUeuh.CRIPPLED_STATE.checkRecovery(target);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.od_set",
                target.getName(), clamped, max
        ).withStyle(ChatFormatting.GREEN), true);
        return clamped;
    }

    private static int runSetMana(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        target.setData(ModAttachments.PLAYER_MANA_CURRENT.get(), amount);
        BookOfLifeStats.clampToMax(target);
        int current = target.getData(ModAttachments.PLAYER_MANA_CURRENT.get());
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.mana_set",
                target.getName(), current, BookOfLifeStats.maxMana(target)
        ).withStyle(ChatFormatting.GREEN), true);
        return current;
    }

    private static int runMaxOd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        BookOfLifeStats.setOdToMax(target);
        int max = BookOfLifeStats.maxOd(target);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.od_max",
                target.getName(), max
        ).withStyle(ChatFormatting.GREEN), true);
        return max;
    }

    private static int runGetChunkMana(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        LevelChunk chunk = player.serverLevel().getChunkAt(player.blockPosition());
        int mana = ChunkManaData.available(chunk);
        int cap = ConfigMagic.CHUNK_AMBIENT_MANA_CAP.getAsInt();
        ChunkPos pos = chunk.getPos();
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.chunk_mana_get",
                pos.x, pos.z, mana, cap
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return mana;
    }

    private static int runSetChunkMana(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        LevelChunk chunk = player.serverLevel().getChunkAt(player.blockPosition());
        ChunkManaData.set(chunk, amount);
        ChunkPos pos = chunk.getPos();
        int now = ChunkManaData.available(chunk);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.chunk_mana_set",
                pos.x, pos.z, now
        ).withStyle(ChatFormatting.GREEN), true);
        return now;
    }

    private static int runGetChunkMiasma(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        LevelChunk chunk = player.serverLevel().getChunkAt(player.blockPosition());
        int miasma = ChunkMiasmaData.get(chunk);
        int cap = ConfigMagic.MIASMA_CAP.getAsInt();
        ChunkPos pos = chunk.getPos();
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.chunk_miasma_get",
                pos.x, pos.z, miasma, cap
        ).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return miasma;
    }

    private static int runSetChunkMiasma(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        LevelChunk chunk = player.serverLevel().getChunkAt(player.blockPosition());
        ChunkMiasmaData.set(chunk, amount);
        ChunkPos pos = chunk.getPos();
        int now = ChunkMiasmaData.get(chunk);
        ctx.getSource().sendSuccess(() -> Component.translatable("hahueuh.command.chunk_miasma_set",
                pos.x, pos.z, now
        ).withStyle(ChatFormatting.GREEN), true);
        return now;
    }
}
