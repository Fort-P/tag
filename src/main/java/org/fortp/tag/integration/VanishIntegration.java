package org.fortp.tag.integration;

import me.drex.vanish.api.VanishAPI;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class VanishIntegration {
    public static boolean IsPlayerVanished(MinecraftServer server, ServerPlayer player) {
        if (FabricLoader.getInstance().isModLoaded("melius-vanish")) {
            return VanishAPI.isVanished(server, player.getUUID());
        }

        return false;
    }
}
