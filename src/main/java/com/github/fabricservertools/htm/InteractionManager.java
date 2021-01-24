package com.github.fabricservertools.htm;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.stream.Collectors;

public class InteractionManager {
    public static HashMap<ServerPlayerEntity, HTMInteractAction> pendingActions = new HashMap<>();

    public static void execute(ServerPlayerEntity player, World world, BlockPos pos) {
        HTMInteractAction action = pendingActions.get(player);
        switch (action.getType()) {
            case SET:
                set(action, player, world, pos);
                break;
            case TRUST:
                trust(action, player, world, pos);
                break;
            case REMOVE:
                remove(action, player, world, pos);
                break;
            case INFO:
                info(action, player, world, pos);
                break;
        }

        pendingActions.remove(player);
    }

    private static void info(HTMInteractAction action, ServerPlayerEntity player, World world, BlockPos pos) {
        HTMContainerLock lock = getLock(player, pos);
        if (lock == null) return;

        if (lock.getType() == null) {
            player.sendMessage(new TranslatableText("text.htm.error.no_lock"), false);
            return;
        }

        GameProfile owner = world.getServer().getUserCache().getByUuid(lock.getOwner());

        player.sendMessage(new TranslatableText("text.htm.divider"), false);
        player.sendMessage(new TranslatableText("text.htm.type", lock.getType().name()), false);
        player.sendMessage(new TranslatableText("text.htm.owner", owner.getName()), false);
        if (owner.getId().equals(player.getUuid())) {
            String trustedList = lock.getTrusted()
                    .stream()
                    .map(a -> world.getServer().getUserCache().getByUuid(a).getName())
                    .collect(Collectors.joining(", "));

            player.sendMessage(new TranslatableText("text.htm.trusted", trustedList), false);
        }
        player.sendMessage(new TranslatableText("text.htm.divider"), false);
    }

    public static void trust(HTMInteractAction action, ServerPlayerEntity player, World world, BlockPos pos) {
        HTMContainerLock lock = getLock(player, pos);
        if (lock == null) return;

        if (lock.getType() == null) {
            player.sendMessage(new TranslatableText("text.htm.error.no_lock"), false);
            return;
        }

        if (lock.getOwner() != player.getUuid()) {
            player.sendMessage(new TranslatableText("text.htm.error.not_owner"), false);
            return;
        }

        if (lock.getOwner() == action.getTrustPlayer().getId()) {
            player.sendMessage(new TranslatableText("text.htm.error.trust_self"), false);
            return;
        }

        if (lock.addTrust(action.getTrustPlayer().getId())){
            player.sendMessage(new TranslatableText("text.htm.trust", action.getTrustPlayer().getName()), false);
        } else {
            player.sendMessage(new TranslatableText("text.htm.error.already_trusted", action.getTrustPlayer().getName()), false);
        }
    }

    public static void remove(HTMInteractAction action, ServerPlayerEntity player, World world, BlockPos pos) {
        HTMContainerLock lock = getLock(player, pos);
        if (lock == null) return;

        if (lock.getType() == null) {
            player.sendMessage(new TranslatableText("text.htm.error.no_lock"), false);
            return;
        }

        if (lock.getOwner() != player.getUuid()) {
            player.sendMessage(new TranslatableText("text.htm.error.not_owner"), false);
            return;
        }

        lock.remove();
        player.sendMessage(new TranslatableText("text.htm.unlocked"), false);
    }

    public static void set(HTMInteractAction action, ServerPlayerEntity player, World world, BlockPos pos) {
        HTMContainerLock lock = getLock(player, pos);
        if (lock == null) return;

        if (lock.getType() != null) {
            player.sendMessage(new TranslatableText("text.htm.error.already_locked"), false);
            return;
        }

        lock.setType(action.getSetType(), player);
        player.sendMessage(new TranslatableText("text.htm.set", action.getSetType().name()), false);
    }

    @Nullable
    private static HTMContainerLock getLock(ServerPlayerEntity player, BlockPos pos) {
        BlockEntity blockEntity = player.getServerWorld().getBlockEntity(pos);
        if (!(blockEntity instanceof LockableContainerBlockEntity) || blockEntity == null) {
            player.sendMessage(new TranslatableText("text.htm.error.unlockable"), false);
            return null;
        }

        HTMContainerLock lock = ((LockableObject) blockEntity).getLock();
        return lock;
    }
}
