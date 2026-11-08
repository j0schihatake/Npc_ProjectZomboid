package personal.knox.npc.own;

/** Small starting kit per faction; equipment progression will extend this. */
public final class OwnNpcFactionKit {
    public record Kit(String weapon, String accessory) {}
    private OwnNpcFactionKit() {}

    public static Kit kit(String faction) {
        String value = faction == null ? "" : faction.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("military")) return new Kit("Base.Pistol", "Base.Hat_Army");
        if (value.contains("raider")) return new Kit("Base.BaseballBat", "Base.Bandana");
        if (value.contains("trader")) return new Kit("Base.Hammer", "Base.Bag_Schoolbag");
        return new Kit("Base.BaseballBat", "Base.Hat_BaseballCap");
    }
}
