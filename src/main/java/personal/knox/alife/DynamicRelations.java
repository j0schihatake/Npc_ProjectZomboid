package personal.knox.alife;

import java.util.HashMap;
import java.util.Map;

/** Symmetric, persistent-in-memory faction standings for A-Life diplomacy. */
public final class DynamicRelations {
    private final Map<String, Integer> values = new HashMap<>();

    public int standing(String a, String b) {
        check(a, b);
        return values.getOrDefault(key(a, b), 0);
    }

    public void set(String a, String b, int value) {
        check(a, b);
        values.put(key(a, b), Math.max(-100, Math.min(100, value)));
    }

    public void shift(String a, String b, int delta) {
        set(a, b, standing(a, b) + delta);
    }

    /** A request that was not answered hurts more than a fulfilled one helps. */
    public void onRequest(String asker, String helper, boolean fulfilled) {
        shift(asker, helper, fulfilled ? 15 : -20);
    }

    public void onAlliance(String a, String b) {
        set(a, b, 60);
    }

    public void onBetrayal(String a, String b) {
        shift(a, b, -30);
    }

    private static void check(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank() || a.equals(b))
            throw new IllegalArgumentException("Invalid faction pair");
    }

    private static String key(String a, String b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }
}
