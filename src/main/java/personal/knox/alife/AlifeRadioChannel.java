package personal.knox.alife;

/** Radio channel: call a faction, ask for help, hostile call may bring reinforcements. */
public final class AlifeRadioChannel {
    private int calledDay = -1;
    private String calledFaction = "";

    public String call(String factionId, int day, int standing) {
        if (factionId == null || factionId.isBlank() || day < 0) return "invalid";
        calledDay = day;
        calledFaction = factionId;
        if (standing < -25) return "hostile-call=" + factionId;
        if (standing > 25) return "ally-answer=" + factionId;
        return "neutral-answer=" + factionId;
    }

    public boolean recentlyCalled(int day) { return calledDay >= 0 && day - calledDay <= 1; }
    public String lastFaction() { return calledFaction; }

    public String reinforcement(int day, int standing, AlifeReinforcement.Kind kind) {
        if (!recentlyCalled(day)) return "none";
        return (standing > 25 ? "reinforcement=" : "patrol=") + kind.name().toLowerCase(java.util.Locale.ROOT);
    }
}
