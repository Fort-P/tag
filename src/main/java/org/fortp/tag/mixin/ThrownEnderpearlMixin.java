package org.fortp.tag.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import org.fortp.tag.game.GameManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ThrownEnderpearl.class)
public class ThrownEnderpearlMixin {

    @Inject(method = "isAllowedToTeleportOwner", at = @At("HEAD"), cancellable = true)
    private static void isAllowedToTeleportOwner(Entity owner, Level newLevel, CallbackInfoReturnable<Boolean> cir) {
        if (GameManager.getRunner() == owner.getUUID()) {
            cir.setReturnValue(false);
        }
    }
}
