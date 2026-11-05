package personal.knox.npc.own;

import personal.knox.npc.NpcState;

/** One own NPC snapshot; no game objects, only data. */
public record OwnNpcAgent(String id, String factionId, double x, double y, double z,
                          double targetX, double targetY, int supplies, NpcState state) {
    public OwnNpcAgent {
        if (id == null || id.isBlank() || factionId == null || factionId.isBlank())
            throw new IllegalArgumentException("Missing agent id or faction");
        if (!Double.isFinite(x + y + z + targetX + targetY))
            throw new IllegalArgumentException("Invalid position");
        if (supplies < 0) throw new IllegalArgumentException("Invalid supplies");
        if (state == null) throw new IllegalArgumentException("Missing state");
    }

    public OwnNpcAgent moved(double nx, double ny, double nz) {
        return new OwnNpcAgent(id, factionId, nx, ny, nz, targetX, targetY, supplies, state);
    }

    public OwnNpcAgent withState(NpcState next) {
        return new OwnNpcAgent(id, factionId, x, y, z, targetX, targetY, supplies, next);
    }

    public OwnNpcAgent withHealth(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 100)
            throw new IllegalArgumentException("Invalid health");
        return new OwnNpcAgent(id, factionId, x, y, z, targetX, targetY, supplies,
                new personal.knox.npc.NpcState(value, state.hunger(), state.thirst(), state.fatigue(),
                        state.morale(), state.bleeding(), state.downed(), state.role(), state.task()));
    }

    public OwnNpcAgent withSupplies(int value) {
        if (value < 0) throw new IllegalArgumentException("Invalid supplies");
        return new OwnNpcAgent(id, factionId, x, y, z, targetX, targetY, value, state);
    }

    public OwnNpcAgent withTarget(double tx, double ty) {
        return new OwnNpcAgent(id, factionId, x, y, z, tx, ty, supplies, state);
    }
}
