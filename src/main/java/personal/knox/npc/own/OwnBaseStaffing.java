package personal.knox.npc.own;

import java.util.ArrayList;
import java.util.List;

/** Data-only allocation of base workers by profession priority. */
public final class OwnBaseStaffing {
    public enum Profession {
        FARMER, WATER, FIRE, CORPSE, CLEAN, COOK, LAUNDRY, FISH, HUNT,
        SCAVENGE, CRAFT, BUILD, GUARD, PATROL, ENFORCER, DRIVER,
        MEDIC, MECHANIC, ELECTRICIAN, METALWORKER, TAILOR, BREWER,
        HERBALIST, TRAPPER, LOOKOUT, QUARTERMASTER
    }

    public record Assignment(Profession profession, int count) {}
    private OwnBaseStaffing() {}

    public static List<Assignment> plan(int total, List<Profession> priority) {
        if (total < 0 || priority == null || priority.isEmpty()) throw new IllegalArgumentException("Invalid staffing");
        int workers = total;
        var counts = new int[Profession.values().length];
        int cursor = 0;
        while (workers > 0 && cursor < priority.size()) {
            counts[priority.get(cursor).ordinal()]++;
            workers--;
            cursor = (cursor + 1) % priority.size();
        }
        var result = new ArrayList<Assignment>();
        for (var profession : priority) {
            if (counts[profession.ordinal()] > 0) result.add(new Assignment(profession, counts[profession.ordinal()]));
        }
        return List.copyOf(result);
    }

    public static List<Profession> parse(String csv) {
        if (csv == null) return defaultPriority();
        var result = new ArrayList<Profession>();
        for (String part : csv.split(",")) {
            if (part.isBlank()) continue;
            try { result.add(Profession.valueOf(part.trim().toUpperCase(java.util.Locale.ROOT))); }
            catch (IllegalArgumentException ignored) { }
        }
        return result.isEmpty() ? defaultPriority() : List.copyOf(result);
    }

    public static List<Profession> defaultPriority() {
        return List.of(Profession.GUARD, Profession.FARMER, Profession.COOK, Profession.WATER,
                Profession.BUILD, Profession.SCAVENGE, Profession.CRAFT, Profession.CLEAN,
                Profession.PATROL, Profession.ENFORCER, Profession.FIRE, Profession.CORPSE,
                Profession.LAUNDRY, Profession.FISH, Profession.HUNT, Profession.DRIVER,
                Profession.MEDIC, Profession.MECHANIC, Profession.ELECTRICIAN, Profession.METALWORKER,
                Profession.TAILOR, Profession.BREWER, Profession.HERBALIST, Profession.TRAPPER,
                Profession.LOOKOUT, Profession.QUARTERMASTER);
    }
}
