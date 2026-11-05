package personal.knox;

/** Whether an NPC objects to the player taking an item because they need it more. */
public final class ItemNeedPolicy {
    public enum Kind { FOOD, WATER, MEDICAL, AMMO, OTHER }

    public record State(double hunger, double thirst, double health, int freeSlots, int ammo) {
        public State {
            if (!Double.isFinite(hunger) || hunger < 0 || hunger > 1
                    || !Double.isFinite(thirst) || thirst < 0 || thirst > 1
                    || !Double.isFinite(health) || health < 0 || health > 100
                    || freeSlots < 0 || ammo < 0)
                throw new IllegalArgumentException("Invalid item-need state");
        }
    }

    private ItemNeedPolicy() {}

    /** A full inventory means "take it, I have no room"; only real need objects. */
    public static boolean objects(Kind kind, State state) {
        if (kind == null || state == null) throw new IllegalArgumentException("Missing kind or state");
        if (state.freeSlots() <= 0) return false;
        return switch (kind) {
            case FOOD -> state.hunger() >= 0.50;
            case WATER -> state.thirst() >= 0.50;
            case MEDICAL -> state.health() < 60;
            case AMMO -> state.ammo() < 30;
            case OTHER -> false;
        };
    }

    /** 0 when there is no objection; otherwise how urgent the need is. */
    public static double weight(Kind kind, State state) {
        if (!objects(kind, state)) return 0;
        return switch (kind) {
            case FOOD -> state.hunger();
            case WATER -> state.thirst();
            case MEDICAL -> (100 - state.health()) / 100.0;
            case AMMO -> (30 - state.ammo()) / 30.0;
            case OTHER -> 0;
        };
    }
}
