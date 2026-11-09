package personal.knox.alife;

/** Editable squad template: appearance, gear, behavior and relations. */
public record AlifeSquadEditor(String id, String appearance, String weapon, String behavior, String relation) {
    public AlifeSquadEditor {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Missing id");
        appearance = safe(appearance);
        weapon = safe(weapon);
        behavior = safe(behavior);
        relation = safe(relation);
    }

    private static String safe(String value) { return value == null || value.isBlank() ? "default" : value; }

    public String export() { return id + "|" + appearance + "|" + weapon + "|" + behavior + "|" + relation; }

    public static AlifeSquadEditor parse(String text) {
        if (text == null) return null;
        String[] p = text.split("\\|");
        if (p.length != 5) return null;
        try { return new AlifeSquadEditor(p[0], p[1], p[2], p[3], p[4]); }
        catch (IllegalArgumentException invalid) { return null; }
    }
}
