package personal.knox.npc.own;

/** Data-only construction blueprint: work is bounded by wall cells. */
public final class OwnBaseBlueprint {
    public record State(int width, int height, int done, int work) {
        public boolean complete() { return work > 0 && done >= work; }
    }
    private OwnBaseBlueprint() {}

    public static State start(int width, int height) {
        int w = Math.max(1, Math.min(32, width));
        int h = Math.max(1, Math.min(32, height));
        return new State(w, h, 0, w * h * 4);
    }

    public static State tick(State state, int workers) {
        if (state == null) throw new IllegalArgumentException("Missing blueprint");
        int added = Math.max(1, Math.min(8, workers)) * 4;
        return new State(state.width(), state.height(), Math.min(state.work(), state.done() + added), state.work());
    }
}
