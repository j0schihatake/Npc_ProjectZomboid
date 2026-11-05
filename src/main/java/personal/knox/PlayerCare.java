package personal.knox;

import java.util.HashMap;
import java.util.Map;

/** Player self-care policy. No game objects, inventory or timed actions. */
public final class PlayerCare {
    public static final String NONE = "none";
    public static final String BANDAGE = "bandage";
    public static final String PAINKILLER = "painkiller";
    public static final String DRINK = "drink";
    public static final String EAT = "eat";

    private static final long ACTION_COOLDOWN = 3_000;
    private static final long RETRY_UNAVAILABLE = 30_000;

    private final Map<String, Long> unavailableUntil = new HashMap<>();
    private long lastActionAt = Long.MIN_VALUE;

    public String decide(long now, double health, int bleedingParts, double pain,
                         double thirst, double hunger, boolean busy, boolean danger,
                         boolean medic, boolean drink, boolean eat) {
        if (now < 0 || bleedingParts < 0
                || !Double.isFinite(health + pain + thirst + hunger)) return NONE;
        if (lastActionAt != Long.MIN_VALUE && now >= lastActionAt && now - lastActionAt < ACTION_COOLDOWN)
            return NONE;
        String action = choose(health, bleedingParts, pain, thirst, hunger, busy, danger, medic, drink, eat);
        if (action == null) return NONE;
        Long until = unavailableUntil.get(action);
        return until != null && now < until ? NONE : action;
    }

    private static String choose(double health, int bleedingParts, double pain, double thirst,
                                 double hunger, boolean busy, boolean danger,
                                 boolean medic, boolean drink, boolean eat) {
        // Bleeding outranks everything and is allowed even while a threat is near.
        if (medic && bleedingParts > 0 && !busy) return BANDAGE;
        if (medic && pain >= 40 && !busy && !danger) return PAINKILLER;
        if (drink && thirst >= 0.20 && !busy && !danger) return DRINK;
        if (eat && hunger >= 0.25 && !busy && !danger) return EAT;
        return null;
    }

    public void done(String action, long now) {
        if (action == null || NONE.equals(action) || now < 0) return;
        lastActionAt = now;
        unavailableUntil.remove(action);
    }

    public void unavailable(String action, long now) {
        if (action == null || NONE.equals(action) || now < 0) return;
        unavailableUntil.put(action, now + RETRY_UNAVAILABLE);
    }

    public long retryAt(String action) {
        Long value = unavailableUntil.get(action);
        return value == null ? Long.MIN_VALUE : value;
    }

    public void reset() {
        unavailableUntil.clear();
        lastActionAt = Long.MIN_VALUE;
    }
}
