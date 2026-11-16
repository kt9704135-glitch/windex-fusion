package dev.windex.fusion.analysis;

/** Converts a weighted underground evidence centroid to the same X/Z on the surface. */
public final class Projection {
    public record Target(int x, int y, int z) {}
    private Projection() {}

    public static Target surface(long weightedX, long weightedZ, int weight,
                                 java.util.function.IntBinaryOperator heightAt) {
        if (weight <= 0) throw new IllegalArgumentException("weight must be positive");
        int x = Math.toIntExact(Math.floorDiv(weightedX, weight));
        int z = Math.toIntExact(Math.floorDiv(weightedZ, weight));
        return new Target(x, heightAt.applyAsInt(x, z), z);
    }
}
