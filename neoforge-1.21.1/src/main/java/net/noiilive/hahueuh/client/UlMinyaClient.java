package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.ConfigMagicYin;
import net.noiilive.hahueuh.network.UlMinyaActivatePayload;
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
import net.neoforged.neoforge.network.PacketDistributor;

public final class UlMinyaClient {
    private UlMinyaClient() {}

    public static void activate() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        Entity target = raycastEntity(player, ConfigMagicYin.UL_MINYA_RANGE.get());
        PacketDistributor.sendToServer(new UlMinyaActivatePayload(target != null ? target.getId() : -1));
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
