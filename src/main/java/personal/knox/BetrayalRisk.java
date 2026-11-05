package personal.knox;

/** Turns group history into an abstract betrayal signal; no hidden instant switch. */
public final class BetrayalRisk {
    public enum Signal { CALM, UNEASE, WARNING, CRITICAL }

    public record History(int trust, int refusedRequests, int ignoredDistress, int gifts, int sharedLoot) {
        public History {
            if (trust < 0 || trust > 100 || refusedRequests < 0 || ignoredDistress < 0
                    || gifts < 0 || sharedLoot < 0)
                throw new IllegalArgumentException("Invalid betrayal history");
        }
    }

    private BetrayalRisk() {}

    public static int score(History history) {
        if (history == null) throw new IllegalArgumentException("Missing history");
        int score = 50 - history.trust() / 2
                + history.refusedRequests() * 8
                + history.ignoredDistress() * 12
                - history.gifts() * 2
                - history.sharedLoot() * 2;
        return Math.max(0, Math.min(100, score));
    }

    public static Signal signal(int score) {
        if (score < 0 || score > 100) throw new IllegalArgumentException("Invalid score");
        if (score >= 70) return Signal.CRITICAL;
        if (score >= 40) return Signal.WARNING;
        if (score >= 20) return Signal.UNEASE;
        return Signal.CALM;
    }
}
