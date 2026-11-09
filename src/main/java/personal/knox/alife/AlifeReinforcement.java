package personal.knox.alife;

/** Reinforcement transport fallback: vehicle/heli only when available, otherwise walk. */
public final class AlifeReinforcement {
    public enum Kind { WALK, CAR, HELI }
    public record Plan(Kind kind, double etaMinutes) {}
    private AlifeReinforcement() {}

    public static Plan plan(double distance, boolean car, boolean heli) {
        double d = Math.max(0, distance);
        if (heli) return new Plan(Kind.HELI, Math.max(1, d / 120));
        if (car) return new Plan(Kind.CAR, Math.max(2, d / 40));
        return new Plan(Kind.WALK, Math.max(5, d / 4));
    }
}
