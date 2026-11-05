package personal.knox.npc.own;

/** Traversal rules for own NPCs: windows, high fences and ropes. */
public final class OwnNpcTraversal {
    public enum Kind { WINDOW, HIGH_FENCE, ROPE, STAIRS }
    public record Move(Kind kind, double seconds, double noise, double staminaCost) {
        public Move {
            if (kind == null || seconds <= 0 || noise < 0 || staminaCost < 0)
                throw new IllegalArgumentException("Invalid traversal move");
        }
    }

    private OwnNpcTraversal() {}

    public static boolean canUse(Kind kind, boolean hasRope, boolean handsFree, boolean injuredLegs,
                                 double stamina, boolean windowOpen, boolean fenceHeightOk) {
        if (kind == null) throw new IllegalArgumentException("Missing traversal kind");
        if (stamina < 0 || stamina > 1) throw new IllegalArgumentException("Invalid stamina");
        if (kind == Kind.ROPE) return hasRope && handsFree && stamina >= 0.15 && !injuredLegs;
        if (kind == Kind.STAIRS) return !injuredLegs && stamina >= 0.05;
        if (kind == Kind.HIGH_FENCE) return handsFree && stamina >= 0.25 && !injuredLegs && fenceHeightOk;
        return windowOpen && handsFree && !injuredLegs;
    }

    public static Move plan(Kind kind, boolean hasRope) {
        if (kind == null) throw new IllegalArgumentException("Missing traversal kind");
        return switch (kind) {
            case WINDOW -> new Move(kind, 1.2, 0.4, 0.05);
            case HIGH_FENCE -> new Move(kind, 2.0, 0.6, 0.20);
            case ROPE -> {
                if (!hasRope) throw new IllegalStateException("No rope");
                yield new Move(kind, 3.0, 0.2, 0.15);
            }
            case STAIRS -> new Move(kind, 1.0, 0.15, 0.05);
        };
    }
}
