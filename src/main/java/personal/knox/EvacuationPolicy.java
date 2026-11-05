package personal.knox;

/** Decides whether a rescue group drives in, asks the player to come closer, or refuses. */
public final class EvacuationPolicy {
    public enum Decision { COME_TO_YOU, YOU_COME_TO_US, REFUSE }

    public record Situation(int zombiesAround, int rescuers, int armedRescuers,
                            boolean vehicle, double routeRisk) {
        public Situation {
            if (zombiesAround < 0 || rescuers < 0 || armedRescuers < 0 || armedRescuers > rescuers)
                throw new IllegalArgumentException("Invalid rescue group");
            if (!Double.isFinite(routeRisk) || routeRisk < 0 || routeRisk > 1)
                throw new IllegalArgumentException("Invalid route risk");
        }
    }

    private EvacuationPolicy() {}

    public static Decision decide(Situation situation) {
        if (situation == null) throw new IllegalArgumentException("Missing situation");
        if (situation.rescuers() == 0) return Decision.REFUSE;
        double strength = situation.rescuers()
                + situation.armedRescuers() * 1.5
                + (situation.vehicle() ? 3 : 0)
                - situation.routeRisk() * 4;
        if (situation.zombiesAround() <= strength) return Decision.COME_TO_YOU;
        if (situation.zombiesAround() <= strength * 2) return Decision.YOU_COME_TO_US;
        return Decision.REFUSE;
    }
}
