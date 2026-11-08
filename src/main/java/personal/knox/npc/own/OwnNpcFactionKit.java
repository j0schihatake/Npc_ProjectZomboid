package personal.knox.npc.own;

/** Faction starting kits with configurable ranged/melee chances. */
public final class OwnNpcFactionKit {
    public record Kit(String weapon, String accessory) {}
    private OwnNpcFactionKit() {}

    public static Kit kit(String faction) {
        int roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(100);
        return kit(faction, 40, 40, roll);
    }

    public static Kit kit(String faction, int rangedChance, int meleeChance, int roll) {
        String value = faction == null ? "" : faction.toLowerCase(java.util.Locale.ROOT);
        String accessory = value.contains("military") ? "Base.Hat_Army"
                : value.contains("raider") ? "Base.Hat_Bandana"
                : value.contains("trader") ? "Base.Bag_Schoolbag"
                : "Base.Hat_BaseballCap";
        int ranged = Math.max(0, Math.min(100, rangedChance));
        int melee = Math.max(0, Math.min(100 - ranged, meleeChance));
        String weapon = roll < ranged ? "Base.Pistol" : roll < ranged + melee ? "Base.BaseballBat" : "Base.Hammer";
        return new Kit(weapon, accessory);
    }
}
