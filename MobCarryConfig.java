package com.example.mobcarry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MobCarryConfig {
    public boolean enabled = true;
    public boolean requireSneaking = true;
    public boolean requireEmptyHand = true;
    public boolean allowPassiveMobs = false;
    public boolean makePersistent = true;
    public boolean protectFromSun = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MobCarryConfig instance = new MobCarryConfig();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("mobcarry.json");
    }

    public static MobCarryConfig get() {
        return instance;
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try {
                MobCarryConfig loaded = GSON.fromJson(Files.readString(p), MobCarryConfig.class);
                if (loaded != null) instance = loaded;
            } catch (Exception e) {
                System.err.println("[mobcarry] Failed to read config, using defaults: " + e);
            }
        }
        save();
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(instance));
        } catch (IOException e) {
            System.err.println("[mobcarry] Failed to save config: " + e);
        }
    }
}
