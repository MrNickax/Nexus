package com.nickax.nexus.bukkit;

import com.nickax.nexus.common.cache.LocalCacheExecutor;
import com.nickax.nexus.common.storage.JsonStorageExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class NexusBukkit extends JavaPlugin {

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
        LocalCacheExecutor.shutdown();
        JsonStorageExecutor.shutdown();
    }
}
