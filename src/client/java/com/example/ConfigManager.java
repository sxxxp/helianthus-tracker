package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.transformer.Config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("helianthus-tracker.json").toFile();
    public static ModConfig config = new ModConfig();

    public static void load() {
        if(!CONFIG_FILE.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)){
            config = GSON.fromJson(reader,ModConfig.class);
        } catch (IOException e) {
            e.fillInStackTrace();
        }
    }
    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.fillInStackTrace();
        }
    }
    public static void reset() {
        config = new ModConfig();
        save();
    }

}
