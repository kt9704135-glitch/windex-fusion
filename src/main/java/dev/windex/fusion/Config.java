package dev.windex.fusion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("windex-fusion.json");

    public Map<Module, Boolean> enabled = new EnumMap<>(Module.class);
    public int scanRadiusChunks = 4;
    public int scanBlocksPerTick = 650;
    public int susMinEvidence = 7;
    public int spawnerMinEachParticle = 4;
    public int spawnerWindowSeconds = 30;
    public int lightUpdatesForAlert = 9;
    public int lightWindowSeconds = 25;
    public int renderDistanceBlocks = 192;
    public int maxHudMarkers = 8;
    public boolean showHud = true;
    public boolean showCoordinates = true;
    public int accentRgb = 0x6AD5FF;
    public int panelRgb = 0x101D2A;
    public int textRgb = 0xF4FBFF;
    public int particleRgb = 0xC6F2FF;
    public int analyzerRgb = 0x57D1FF;
    public int autoTotemDelayTicks = 4;
    public boolean arcticParticles = true;
    public String preset = "DONUT";

    public Config() {
        for (Module module : Module.values()) enabled.put(module,
                module != Module.TRIGGER_BOT && module != Module.TARGET_HUD);
    }

    public static Config load() {
        try {
            if (Files.exists(PATH)) {
                Config config = GSON.fromJson(Files.readString(PATH), Config.class);
                if (config != null) { config.validate(); return config; }
            }
        } catch (Exception e) {
            WindexFusion.LOGGER.warn("Could not read Windex Fusion config", e);
        }
        Config config = new Config();
        config.save();
        return config;
    }

    public boolean on(Module module) { return enabled.getOrDefault(module, true); }

    public void toggle(Module module) {
        enabled.put(module, !on(module));
        save();
    }

    public void preset(String name) {
        preset = name;
        switch (name) {
            case "DONUT" -> {
                // Conservative activity thresholds: no claim of DonutSMP bypass.
                scanRadiusChunks = 4; scanBlocksPerTick = 650; susMinEvidence = 7;
                lightUpdatesForAlert = 9; spawnerMinEachParticle = 4;
                spawnerWindowSeconds = 30; lightWindowSeconds = 25;
                maxHudMarkers = 8;
            }
            case "PERFORMANCE" -> {
                scanRadiusChunks = 2; scanBlocksPerTick = 200; susMinEvidence = 7;
                lightUpdatesForAlert = 9; spawnerMinEachParticle = 4; maxHudMarkers = 5;
                spawnerWindowSeconds = 30; lightWindowSeconds = 25;
            }
            case "DEEP" -> {
                scanRadiusChunks = 5; scanBlocksPerTick = 1000; susMinEvidence = 4;
                lightUpdatesForAlert = 5; spawnerMinEachParticle = 3; maxHudMarkers = 10;
                spawnerWindowSeconds = 25; lightWindowSeconds = 25;
            }
            default -> {
                preset = "BALANCED"; scanRadiusChunks = 3; scanBlocksPerTick = 500;
                susMinEvidence = 5; lightUpdatesForAlert = 6;
                spawnerMinEachParticle = 3; maxHudMarkers = 7;
                spawnerWindowSeconds = 25; lightWindowSeconds = 25;
            }
        }
        save();
    }

    private void validate() {
        if (enabled == null) enabled = new EnumMap<>(Module.class);
        for (Module module : Module.values()) enabled.putIfAbsent(module,
                module != Module.TRIGGER_BOT && module != Module.TARGET_HUD);
        scanRadiusChunks = clamp(scanRadiusChunks, 1, 6);
        scanBlocksPerTick = clamp(scanBlocksPerTick, 100, 1500);
        susMinEvidence = clamp(susMinEvidence, 2, 30);
        spawnerMinEachParticle = clamp(spawnerMinEachParticle, 2, 15);
        spawnerWindowSeconds = clamp(spawnerWindowSeconds, 5, 120);
        lightUpdatesForAlert = clamp(lightUpdatesForAlert, 3, 30);
        lightWindowSeconds = clamp(lightWindowSeconds, 10, 90);
        renderDistanceBlocks = clamp(renderDistanceBlocks, 24, 512);
        maxHudMarkers = clamp(maxHudMarkers, 2, 12);
        accentRgb &= 0xFFFFFF;
        panelRgb &= 0xFFFFFF;
        textRgb &= 0xFFFFFF;
        particleRgb &= 0xFFFFFF;
        analyzerRgb &= 0xFFFFFF;
        autoTotemDelayTicks = clamp(autoTotemDelayTicks, 1, 20);
        if (preset == null) preset = "DONUT";
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    public void save() {
        validate();
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException e) {
            WindexFusion.LOGGER.warn("Could not write Windex Fusion config", e);
        }
    }
}
