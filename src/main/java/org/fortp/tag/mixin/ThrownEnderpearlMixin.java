package org.fortp.tag.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.fortp.tag.game.GameManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin {

    @Inject(method = "isAllowedToTeleportOwner", at = @At("HEAD"), cancellable = true)
    private static void isAllowedToTeleportOwner(Entity owner, Level newLevel, CallbackInfoReturnable<Boolean> cir) {
        if (GameManager.getRunner() == owner.getUUID()) {
            if (owner instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.literal("Enderpearls are disabled!").withStyle(ChatFormatting.RED));
            }
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void checkPearlDistance(HitResult hitResult, CallbackInfo ci) {
        ThrownEnderpearl pearl = (ThrownEnderpearl) (Object) this;
        Entity owner = pearl.getOwner();

        if (owner != null && GameManager.isGameActive() && GameManager.isEligibleRunner((ServerPlayer) owner)) {
            if (pearl.distanceTo(owner) > 500.0F) {
                if (owner instanceof ServerPlayer player) {
                    player.sendSystemMessage(Component.literal("You can't teleport that far!").withStyle(ChatFormatting.RED));
                }

                pearl.discard();
                ci.cancel();
            }
        }
    }
}
