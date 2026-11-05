package personal.knox;

/** Speaker-relative world-plane sectors. Isometric screen coordinates are irrelevant. */
public final class ThreatDirection {
    private ThreatDirection() {}

    public static String topic(String original, double facingX, double facingY, double dx, double dy) {
        if (!"react_zombies".equals(original)) return original;
        double facingLength = Math.hypot(facingX, facingY);
        double distance = Math.hypot(dx, dy);
        if (!Double.isFinite(facingLength) || !Double.isFinite(distance)
                || facingLength < 1e-6 || distance < 1e-6) return original;
        double fx = facingX / facingLength, fy = facingY / facingLength;
        double tx = dx / distance, ty = dy / distance;
        double forward = fx * tx + fy * ty;
        double right = fx * ty - fy * tx;
        if (Math.abs(forward) >= Math.abs(right))
            return forward >= 0 ? "react_zombies_front" : "react_zombies_behind";
        return right > 0 ? "react_zombies_right" : "react_zombies_left";
    }
}
