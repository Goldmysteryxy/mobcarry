package com.example.mobcarry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MobCarry implements ModInitializer {
    public static final String MOD_ID = "mobcarry";

    /** UUIDs of mobs currently being carried by a player. */
    private static final Set<UUID> CARRIED = new HashSet<>();

    @Override
    public void onInitialize() {
        MobCarryConfig.load();

        // Pick up: sneak + right-click a mob
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(player instanceof ServerPlayer sp) || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            MobCarryConfig cfg = MobCarryConfig.get();
            if (!cfg.enabled) return InteractionResult.PASS;
            if (cfg.requireSneaking && !sp.isShiftKeyDown()) return InteractionResult.PASS;
            if (cfg.requireEmptyHand && !sp.getMainHandItem().isEmpty()) return InteractionResult.PASS;
            if (!(entity instanceof Mob mob) || !canCarry(mob, cfg)) return InteractionResult.PASS;

            if (!carriedBy(sp).isEmpty()) {
                sp.sendSystemMessage(Component.literal("You're already carrying a mob"), true);
                return InteractionResult.SUCCESS;
            }
            mob.setNoAi(true);
            mob.setTarget(null);
            if (cfg.makePersistent) mob.setPersistenceRequired();
            if (mob.startRiding(sp, true)) {
                CARRIED.add(mob.getUUID());
                sp.sendSystemMessage(Component.literal("Picked up mob. Sneak + right-click to drop"), true);
            } else {
                mob.setNoAi(false);
            }
            return InteractionResult.SUCCESS;
        });

        // Drop on a block: sneak + right-click a block
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(player instanceof ServerPlayer sp) || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!sp.isShiftKeyDown() || !sp.getMainHandItem().isEmpty()) return InteractionResult.PASS;
            List<Mob> carried = carriedBy(sp);
            if (carried.isEmpty()) return InteractionResult.PASS;
            Vec3 pos = Vec3.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection()));
            carried.forEach(m -> release(m, pos));
            return InteractionResult.SUCCESS;
        });

        // Drop in front of you: sneak + right-click air
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!(player instanceof ServerPlayer sp) || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!sp.isShiftKeyDown() || !sp.getMainHandItem().isEmpty()) return InteractionResult.PASS;
            List<Mob> carried = carriedBy(sp);
            if (carried.isEmpty()) return InteractionResult.PASS;
            Vec3 look = sp.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            Vec3 pos = sp.position().add(flat.lengthSqr() > 1.0E-4 ? flat.normalize().scale(1.0) : Vec3.ZERO);
            carried.forEach(m -> release(m, pos));
            return InteractionResult.SUCCESS;
        });

        // Keep carried mobs from burning in daylight
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!MobCarryConfig.get().protectFromSun || CARRIED.isEmpty()) return;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                for (Entity e : p.getPassengers()) {
                    if (CARRIED.contains(e.getUUID())) e.clearFire();
                }
            }
        });

        // Never leave frozen mobs behind
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> releaseAll(handler.getPlayer()));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer sp) releaseAll(sp);
        });
    }

    private static boolean canCarry(Mob mob, MobCarryConfig cfg) {
        if (mob instanceof WitherBoss || mob instanceof EnderDragon || mob instanceof Warden) return false;
        if (mob.isNoAi() || mob.isPassenger() || mob.isVehicle()) return false;
        return mob instanceof Enemy || cfg.allowPassiveMobs;
    }

    private static List<Mob> carriedBy(ServerPlayer sp) {
        List<Mob> list = new ArrayList<>();
        for (Entity e : sp.getPassengers()) {
            if (e instanceof Mob m && CARRIED.contains(m.getUUID())) list.add(m);
        }
        return list;
    }

    private static void releaseAll(ServerPlayer sp) {
        Vec3 pos = sp.position();
        carriedBy(sp).forEach(m -> release(m, pos));
    }

    private static void release(Mob mob, Vec3 pos) {
        mob.stopRiding();
        CARRIED.remove(mob.getUUID());
        mob.setNoAi(false);
        if (pos != null) {
            mob.setPos(pos.x, pos.y, pos.z);
            mob.setDeltaMovement(Vec3.ZERO);
            mob.resetFallDistance();
        }
    }
}
