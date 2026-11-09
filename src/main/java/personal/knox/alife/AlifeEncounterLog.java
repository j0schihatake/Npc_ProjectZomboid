package personal.knox.alife;

import java.util.ArrayList;
import java.util.List;

/** Bounded memory of encounters: who, where, when and result. */
public final class AlifeEncounterLog {
    public record Encounter(int day, String factionId, double x, double y, String outcome) {}
    private final int max;
    private final List<Encounter> entries = new ArrayList<>();

    public AlifeEncounterLog(int max) { this.max = Math.max(1, Math.min(256, max)); }

    public void add(int day, String factionId, double x, double y, String outcome) {
        if (factionId == null || factionId.isBlank()) return;
        entries.add(new Encounter(day, factionId, x, y, outcome == null ? "" : outcome));
        while (entries.size() > max) entries.remove(0);
    }

    public int size() { return entries.size(); }
    public List<Encounter> recent(int day, int limit) {
        var out = new ArrayList<Encounter>();
        for (int i = entries.size() - 1; i >= 0 && out.size() < limit; i--)
            if (entries.get(i).day() >= day) out.add(entries.get(i));
        return List.copyOf(out);
    }

    public boolean seenSince(String factionId, int day) {
        for (var e : entries) if (e.factionId().equals(factionId) && e.day() >= day) return true;
        return false;
    }

    public String summary() { return "encounters=" + entries.size(); }
}
