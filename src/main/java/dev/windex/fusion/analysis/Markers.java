package dev.windex.fusion.analysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Markers {
    public enum Kind {
        SPAWNER("Spawner", 0xFFB86B),
        PARTICLE("Spawner?", 0xFFC46B),
        PLAYER("Player", 0xFF6D8B),
        ACTIVITY("Activity?", 0xFFAB68),
        SUS("Sus above", 0xB58AFF),
        CAVE("Cave", 0x75B8FF),
        TERRAIN("Terrain", 0x78D7AD),
        STORAGE("Storage", 0xE6C57F);

        public final String title;
        public final int rgb;
        Kind(String title, int rgb) { this.title = title; this.rgb = rgb; }
    }

    public record Marker(Kind kind, String key, int x, int y, int z,
                         int confidence, String evidence, long expiresAt) {
        public long squaredDistance(double px, double pz) {
            double dx = x - px, dz = z - pz;
            return (long)(dx * dx + dz * dz);
        }
    }

    private final Map<String, Marker> markers = new LinkedHashMap<>();

    public synchronized void upsert(Kind kind, String id, int x, int y, int z,
                                    int confidence, String evidence, long ttlMs) {
        String key = kind.name() + ":" + id;
        markers.put(key, new Marker(kind, key, x, y, z,
                Math.max(0, Math.min(100, confidence)), evidence,
                System.currentTimeMillis() + ttlMs));
        if (markers.size() > 600) {
            purge();
            if (markers.size() > 600) markers.remove(markers.keySet().iterator().next());
        }
    }

    public synchronized List<Marker> nearby(double x, double z, int maxRange) {
        purge();
        long maxSq = (long)maxRange * maxRange;
        List<Marker> list = new ArrayList<>();
        for (Marker marker : markers.values()) {
            if (marker.squaredDistance(x,z) <= maxSq) list.add(marker);
        }
        list.sort(Comparator.comparingInt(Marker::confidence).reversed()
                .thenComparingLong(m -> m.squaredDistance(x,z)));
        return list;
    }

    public synchronized void purge() {
        long now = System.currentTimeMillis();
        markers.values().removeIf(m -> m.expiresAt() <= now);
    }

    public synchronized void clear() { markers.clear(); }
}
