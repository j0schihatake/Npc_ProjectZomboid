package personal.knox.npc.own;

import java.util.List;
import java.util.Locale;

/** Twenty base facilities with bounded levels and material costs. */
public final class OwnBaseFacilities {
    public static final List<String> KINDS = List.of(
            "FARM", "WATER", "KITCHEN", "WORKSHOP", "INFIRMARY",
            "LAUNDRY", "GENERATOR", "WATCHTOWER", "WALL", "GATE",
            "STORAGE", "GARAGE", "RADIO", "TRAINING", "ARMORY",
            "DORM", "LATRINE", "SHOWER", "FISH_DOCK", "HUNT_BLIND");

    private OwnBaseFacilities() {}

    public static String normalize(String kind) {
        if (kind == null) return "";
        String value = kind.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return KINDS.contains(value) ? value : "";
    }

    public static int cost(String kind, int level) {
        int index = KINDS.indexOf(normalize(kind));
        return index < 0 ? 0 : 5 + index * 2 + Math.max(0, level) * 3;
    }

    public static int maxLevel(String kind) { return normalize(kind).isEmpty() ? 0 : 3; }

    public static String output(String kind) {
        return switch (normalize(kind)) {
            case "FARM" -> "food";
            case "WATER" -> "water";
            case "KITCHEN" -> "meals";
            case "WORKSHOP" -> "materials";
            case "INFIRMARY" -> "meds";
            case "LAUNDRY" -> "clean";
            case "GENERATOR" -> "power";
            case "WATCHTOWER" -> "alarm";
            case "FISH_DOCK" -> "food";
            case "HUNT_BLIND" -> "food";
            case "ARMORY" -> "ammo";
            default -> "support";
        };
    }
}
