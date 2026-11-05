package personal.knox.alife;

/** Own outpost model; no game objects or world writes. */
public record Outpost(String id, String factionId, Kind kind, double x, double y, double z,
                      int garrison, int supplies, boolean radio) {
    public enum Kind { BARRICADED_HOUSE, CHECKPOINT, CAMP, FARM, TOWER, WAREHOUSE, GATE, BRIDGE, COMPOUND }
    public static final int MAX_GARRISON = 64;

    public Outpost {
        if (id == null || id.isBlank() || factionId == null || factionId.isBlank())
            throw new IllegalArgumentException("Missing outpost or faction id");
        if (kind == null) throw new IllegalArgumentException("Missing outpost kind");
        if (!Double.isFinite(x + y + z)) throw new IllegalArgumentException("Invalid position");
        if (garrison < 0 || garrison > MAX_GARRISON) throw new IllegalArgumentException("Invalid garrison");
        if (supplies < 0) throw new IllegalArgumentException("Invalid supplies");
    }

    public Outpost withSupplies(int value) {
        return new Outpost(id, factionId, kind, x, y, z, garrison, value, radio);
    }

    public Outpost withRadio(boolean value) {
        return new Outpost(id, factionId, kind, x, y, z, garrison, supplies, value);
    }

    public boolean canDispatch() {
        return radio && garrison > 0 && supplies > 0;
    }
}
