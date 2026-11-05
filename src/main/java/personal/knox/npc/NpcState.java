package personal.knox.npc;

/** Own NPC snapshot for the future personal NPC layer. No game objects. */
public record NpcState(double health, double hunger, double thirst, double fatigue, double morale,
                       boolean bleeding, boolean downed, Role role, Task task) {
    public enum Role { GUARD, MEDIC, SCAVENGER, FARMER, LEADER }
    public enum Task { FOLLOW, GUARD, WORK, REST, LOOT, FIGHT, FLEE, IDLE }

    public NpcState {
        if (!Double.isFinite(health) || health < 0 || health > 100)
            throw new IllegalArgumentException("Invalid health");
        if (!unit(hunger) || !unit(thirst) || !unit(fatigue) || !unit(morale))
            throw new IllegalArgumentException("Needs must be 0..1");
        if (role == null || task == null) throw new IllegalArgumentException("Missing role or task");
    }

    private static boolean unit(double value) {
        return Double.isFinite(value) && value >= 0 && value <= 1;
    }

    public NpcState withNeeds(double newHunger, double newThirst, double newFatigue) {
        return new NpcState(health, newHunger, newThirst, newFatigue, morale, bleeding, downed, role, task);
    }

    public NpcState withTask(Task value) {
        return new NpcState(health, hunger, thirst, fatigue, morale, bleeding, downed, role, value);
    }

    public NpcState withBleeding(boolean value) {
        return new NpcState(health, hunger, thirst, fatigue, morale, value, downed, role, task);
    }
}
