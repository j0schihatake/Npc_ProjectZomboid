package personal.knox.alife;

/** Output for the game adapter; contains no game objects. */
public record AlifeEvent(Type type, String squadId, String factionId, double x, double y, double z, String detail) {
    public enum Type { NEAR_SQUAD, FAR_MOVE, FAR_RETURN, FAR_ENCOUNTER, OUTPOST_SILENT, FACTION_RETURN, FACTION_STARVING, FACTION_EXPAND, NODE_CAPTURED, RAID_STARTED, RAID_RESOLVED,
        SITUATION_PATROL, SITUATION_AMBUSH, SITUATION_CHECKPOINT, SITUATION_BROKEN_CAR,
        SITUATION_OCCUPIED_HOUSE, SITUATION_SNIPER, SITUATION_PUNITIVE, SITUATION_BASE_RAID }

    public AlifeEvent {
        if (type == null) throw new IllegalArgumentException("Missing event type");
        if (squadId == null || squadId.isBlank() || factionId == null || factionId.isBlank())
            throw new IllegalArgumentException("Missing squad or faction id");
        if (!Double.isFinite(x + y + z)) throw new IllegalArgumentException("Invalid position");
        detail = detail == null ? "" : detail;
    }
}
