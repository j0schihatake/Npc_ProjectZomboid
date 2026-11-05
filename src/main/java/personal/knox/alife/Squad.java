package personal.knox.alife;

/** Simulation snapshot; real NPCs and world objects stay behind the game adapter. */
public record Squad(String id, String factionId, int members, double health, int supplies,
                    Task task, double x, double y, double z, double targetX, double targetY) {
    public enum Task { PATROL, SCAVENGE, RAID, HOLD, TRAVEL, REGROUP }
    public static final int MAX_MEMBERS = 16;

    public Squad {
        if (id == null || id.isBlank() || factionId == null || factionId.isBlank())
            throw new IllegalArgumentException("Missing squad or faction id");
        if (members < 0 || members > MAX_MEMBERS) throw new IllegalArgumentException("Invalid member count");
        if (!Double.isFinite(health) || health < 0 || health > 100) throw new IllegalArgumentException("Invalid health");
        if (supplies < 0) throw new IllegalArgumentException("Invalid supplies");
        if (task == null) throw new IllegalArgumentException("Missing task");
        if (!Double.isFinite(x + y + z + targetX + targetY)) throw new IllegalArgumentException("Invalid position");
    }

    public Squad moved(double nx, double ny, double nz) {
        return new Squad(id, factionId, members, health, supplies, task, nx, ny, nz, targetX, targetY);
    }

    public Squad withSupplies(int value) {
        return new Squad(id, factionId, members, health, value, task, x, y, z, targetX, targetY);
    }

    public Squad withMembers(int value) {
        return new Squad(id, factionId, value, health, supplies, task, x, y, z, targetX, targetY);
    }

    public double distanceTo(double px, double py) {
        return Math.hypot(x - px, y - py);
    }
}
