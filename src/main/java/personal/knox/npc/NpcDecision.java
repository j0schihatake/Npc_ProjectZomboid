package personal.knox.npc;

/** Priorities for one NPC; the game adapter will execute the chosen action. */
public final class NpcDecision {
    public enum Action { BANDAGE, FIGHT, DRINK, EAT, REST, FOLLOW, GUARD, WORK, LOOT, FLEE, IDLE }

    private NpcDecision() {}

    public static Action next(NpcState state, boolean danger,
                              boolean hasBandage, boolean hasWater, boolean hasFood) {
        if (state == null) throw new IllegalArgumentException("Missing NPC state");
        if (state.downed()) return danger ? Action.FLEE : Action.REST;
        if (danger) return Action.FIGHT;
        if (state.bleeding() && hasBandage) return Action.BANDAGE;
        if (state.thirst() >= 0.60 && hasWater) return Action.DRINK;
        if (state.hunger() >= 0.60 && hasFood) return Action.EAT;
        if (state.fatigue() >= 0.80) return Action.REST;
        return switch (state.task()) {
            case FOLLOW -> Action.FOLLOW;
            case GUARD -> Action.GUARD;
            case WORK -> Action.WORK;
            case REST -> Action.REST;
            case LOOT -> Action.LOOT;
            case FIGHT -> Action.FIGHT;
            case FLEE -> Action.FLEE;
            case IDLE -> Action.IDLE;
        };
    }
}
