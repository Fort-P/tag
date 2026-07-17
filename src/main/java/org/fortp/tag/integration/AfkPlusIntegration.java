package org.fortp.tag.integration;

import com.sakuraryoko.afkplus.api.AfkPlusAPI;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

public class AfkPlusIntegration {
    public static boolean isPlayerAfk(ServerPlayer player) {
        if (FabricLoader.getInstance().isModLoaded("afkplus")) {
            return AfkPlusAPI.isPlayerAfk(player);
        }
        return false;
    }
}
