package personal.knox.alife;

/** Faction recognition from template, standing and encounter history. */
public record AlifeFactionIdentity(String factionId, String templateId, int standing, boolean recognized) {
    public AlifeFactionIdentity {
        if (factionId == null || factionId.isBlank() || templateId == null || templateId.isBlank())
            throw new IllegalArgumentException("Missing faction identity");
        if (standing < -100 || standing > 100) throw new IllegalArgumentException("Invalid standing");
    }

    public static AlifeFactionIdentity of(String factionId, AlifeFactionRoster.Template template, int standing) {
        if (template == null) throw new IllegalArgumentException("Unknown template");
        return new AlifeFactionIdentity(factionId, template.id(), standing,
                standing > -50 || template.hostility() < 60);
    }

    public boolean hostile() { return standing < -25; }
    public boolean friendly() { return standing > 25; }
    public String greeting() { return hostile() ? "Уходи." : friendly() ? "Свои." : "Стой."; }
}
