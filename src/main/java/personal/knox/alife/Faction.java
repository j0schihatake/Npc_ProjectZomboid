package personal.knox.alife;

/** Own faction data; no A-Life code, identifiers or content is used. */
public record Faction(String id, String name, Stance stance, int reputation) {
    public enum Stance { HOSTILE, NEUTRAL, FRIENDLY }

    public Faction {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Missing faction id");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Missing faction name");
        if (stance == null) throw new IllegalArgumentException("Missing faction stance");
        if (reputation < -100 || reputation > 100) throw new IllegalArgumentException("Invalid reputation");
    }
}
