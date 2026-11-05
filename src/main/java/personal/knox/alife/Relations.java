package personal.knox.alife;

import java.util.HashMap;
import java.util.Map;

/** Reputation and encounter memory owned by this project; no game state. */
public final class Relations {
    public enum Event { ATTACKED, ROBBED, HELPED, TRADED, IGNORED }

    public static final long DAY = 24L * 60 * 60 * 1000;
    private static final long REVENGE_DELAY = 2 * DAY;
    private static final long REVENGE_MEMORY = 7 * DAY;
    private static final double REVENGE_SCORE = -10;

    private static final class Data {
        double score;
        long attackedAt = Long.MIN_VALUE;
        long lastEventAt = Long.MIN_VALUE;
    }

    private final Map<String, Data> records = new HashMap<>();

    public void record(String factionId, Event event, long now) {
        if (factionId == null || factionId.isBlank()) throw new IllegalArgumentException("Missing faction id");
        if (event == null) throw new IllegalArgumentException("Missing event");
        if (now < 0) throw new IllegalArgumentException("Invalid time");
        var data = records.computeIfAbsent(factionId, key -> new Data());
        data.score = Math.max(-100, Math.min(100, data.score + delta(event)));
        data.lastEventAt = now;
        if (event == Event.ATTACKED) data.attackedAt = now;
    }

    private static int delta(Event event) {
        return switch (event) {
            case ATTACKED -> -25;
            case ROBBED -> -15;
            case HELPED -> 20;
            case TRADED -> 8;
            case IGNORED -> 0;
        };
    }

    public int score(String factionId) {
        var data = records.get(factionId);
        return data == null ? 0 : (int) Math.round(data.score);
    }

    public Faction.Stance stance(String factionId) {
        int value = score(factionId);
        if (value <= -25) return Faction.Stance.HOSTILE;
        if (value >= 25) return Faction.Stance.FRIENDLY;
        return Faction.Stance.NEUTRAL;
    }

    /** A survivor group may return after a delay, but not forever. */
    public boolean wantsRevenge(String factionId, long now) {
        var data = records.get(factionId);
        if (data == null || data.attackedAt == Long.MIN_VALUE || now < 0) return false;
        long elapsed = now - data.attackedAt;
        return elapsed >= REVENGE_DELAY && elapsed <= REVENGE_MEMORY && data.score <= REVENGE_SCORE;
    }

    /** Scores drift one point per full day toward neutral; never past it. */
    public void decay(long now) {
        for (var data : records.values()) {
            if (data.lastEventAt == Long.MIN_VALUE || now <= data.lastEventAt) continue;
            long days = (now - data.lastEventAt) / DAY;
            if (days <= 0) continue;
            double step = Math.min(days, 10);
            data.score = data.score > 0 ? Math.max(0, data.score - step)
                    : Math.min(0, data.score + step);
            data.lastEventAt += days * DAY;
        }
    }

    public int size() { return records.size(); }

    public void reset() { records.clear(); }
}
