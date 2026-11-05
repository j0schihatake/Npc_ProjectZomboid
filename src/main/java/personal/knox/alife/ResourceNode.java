package personal.knox.alife;

/** A map resource point that factions can control and fight over. */
public record ResourceNode(String id, Kind kind, double x, double y, double z, String controller, int value) {
    public enum Kind { FOOD, WATER, MEDICAL, AMMO, MATERIALS, FUEL }

    public ResourceNode {
        if (id == null || id.isBlank() || kind == null) throw new IllegalArgumentException("Missing node");
        if (!Double.isFinite(x + y + z)) throw new IllegalArgumentException("Invalid node position");
        if (controller == null || controller.isBlank() || value <= 0)
            throw new IllegalArgumentException("Invalid node ownership");
    }

    public ResourceNode withController(String factionId) {
        if (factionId == null || factionId.isBlank()) throw new IllegalArgumentException("Missing controller");
        return new ResourceNode(id, kind, x, y, z, factionId, value);
    }
}
