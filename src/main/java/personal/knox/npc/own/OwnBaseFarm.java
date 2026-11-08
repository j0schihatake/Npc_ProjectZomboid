package personal.knox.npc.own;

/** Data-only crop cycle: empty, tilled, planted, growing, ready. */
public final class OwnBaseFarm {
    public static final int EMPTY = 0, TILLED = 1, PLANTED = 2, GROWING = 3, READY = 4;
    private OwnBaseFarm() {}

    public static int tick(int stage, boolean watered) {
        if (!watered || stage < EMPTY || stage >= READY) return stage;
        return stage + 1;
    }
}
