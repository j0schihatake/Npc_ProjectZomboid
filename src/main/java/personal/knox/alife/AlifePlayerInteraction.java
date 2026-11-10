package personal.knox.alife;

/** Player-facing decisions for a faction encounter. */
public final class AlifePlayerInteraction {
    public record Response(String greeting, boolean hostile, boolean trade, boolean recruit,
                           boolean tribute, boolean help) {}
    private AlifePlayerInteraction() {}

    public static Response decide(int standing, boolean armed, boolean groupNear) {
        boolean hostile = standing < -25;
        boolean friendly = standing > 25;
        String greeting = hostile ? "Уходи." : friendly ? "Свои, проходи." : armed ? "Опусти оружие." : "Стой.";
        return new Response(greeting, hostile, standing > 0 && !hostile,
                friendly && groupNear, standing < 0 && !hostile, friendly);
    }

    public static String tribute(int food, int water, int meds) {
        return "tribute food=" + Math.max(0, food) + " water=" + Math.max(0, water)
                + " meds=" + Math.max(0, meds);
    }

    public static String trade(int standing, boolean hasStock) {
        return standing > 0 && hasStock ? "trade-open" : "trade-closed";
    }

    public static String help(int standing, boolean wounded) {
        return standing > 25 && wounded ? "help-sent" : "no-help";
    }

    public static String leave(boolean hostile) { return hostile ? "leave-or-fight" : "leave-safe"; }
}
