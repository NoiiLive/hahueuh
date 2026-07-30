package net.noiilive.hahueuh.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.noiilive.hahueuh.capability.PlayerData;
import net.noiilive.hahueuh.client.gui.SpellStorageScreen;
import net.noiilive.hahueuh.network.AlShamakActivatePacket;
import net.noiilive.hahueuh.network.ClientPlayerData;
import net.noiilive.hahueuh.network.ModNetworking;

public final class AlShamakClient {
    private AlShamakClient() {}

    public static void activate() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        PlayerData data = ClientPlayerData.of(player);
        if (data != null && (data.hasTrappedEntities() || !data.getStoredSpell().isEmpty())) {
            if (player.isShiftKeyDown()) {
                ModNetworking.CHANNEL.sendToServer(new AlShamakActivatePacket(AlShamakActivatePacket.DISCARD, -1));
                return;
            }
            Entity releaseTarget = raycastEntity(player, net.noiilive.hahueuh.network.ClientConfigValues.ulMinyaRange());
            int releaseTargetId = releaseTarget != null ? releaseTarget.getId() : -1;
            ModNetworking.CHANNEL.sendToServer(new AlShamakActivatePacket(AlShamakActivatePacket.RELEASE, releaseTargetId));
            return;
        }

        Entity target = raycastEntity(player, net.noiilive.hahueuh.network.ClientConfigValues.alShamakRange());
        if (target != null) {
            ModNetworking.CHANNEL.sendToServer(new AlShamakActivatePacket(AlShamakActivatePacket.BANISH, target.getId()));
        } else {
            mc.setScreen(new SpellStorageScreen());
        }
    }

    public static Entity raycastEntity(LocalPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range));

        BlockHitResult block = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 rayEnd = block.getType() != HitResult.Type.MISS ? block.getLocation() : end;

        AABB search = new AABB(eye, rayEnd).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, rayEnd, search,
                e -> e != player && e.isPickable() && !e.isSpectator(), eye.distanceToSqr(rayEnd));
        return hit != null ? hit.getEntity() : null;
    }
}
