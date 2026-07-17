package org.fortp.tag;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.fortp.tag.game.GameManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tag implements ModInitializer {
    public static final String MOD_ID = "tag";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        TickScheduler.init();
        CommandRegistrationCallback.EVENT.register(TagCommands::registerCommands);
        ServerLifecycleEvents.SERVER_STARTED.register(GameManager::initialize);
        ServerTickEvents.END_SERVER_TICK.register(GameManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                GameManager.onPlayerDisconnect(handler.player));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
                GameManager.onPlayerChangedLevel(player, destination));

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            if (player instanceof ServerPlayer && entity instanceof ServerPlayer) {
                if (GameManager.checkTag(player.getUUID(), entity.getUUID())) {
                    return InteractionResult.SUCCESS;
                } else if (player.getUUID().equals(GameManager.getRunner())) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });


        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (player instanceof ServerPlayer serverPlayer
                    && !serverPlayer.getCooldowns().isOnCooldown(stack)
                    && GameManager.shouldBlockMobilityItem(serverPlayer, stack)) {
                if (stack.is(Items.ENDER_PEARL)) {
                    player.sendSystemMessage(Component.literal("Enderpearls are disabled near the runner"));
                } else if (stack.is(Items.TRIDENT)) {
                    player.sendSystemMessage(Component.literal("Riptide is disabled near the runner"));
                } else {
                    player.sendSystemMessage(Component.literal("Mobility items are disabled near the runner."));
                }
//                serverPlayer.stopUsingItem();
                // Cancel the use event on the client
                level.broadcastEntityEvent(serverPlayer, (byte) 9);
                serverPlayer.getCooldowns().addCooldown(stack, 40);
                // Make sure the client knows it didn't use a pearl
                serverPlayer.inventoryMenu.sendAllDataToRemote();
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof HappyGhast && GameManager.isGameActive() && player.getUUID().equals(GameManager.getRunner())) {
                player.sendSystemMessage(Component.literal("Happy ghasts are disabled for the runner").withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                GameManager.onPlayerDeath(player);
            }
        });
    }
}
