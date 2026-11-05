package personal.knox.alife;

/** Escort size by faction relation and the heavy cost of losing a faction member. */
public final class EscortPolicy {
    private EscortPolicy() {}

    public static int maxEscorts(int relation) {
        if (relation < -100 || relation > 100) throw new IllegalArgumentException("Invalid relation");
        if (relation < 20) return 0;
        if (relation < 50) return 1;
        if (relation < 80) return 2;
        return 3;
    }

    /** Completed +10; wounded -5 each; dead -40 each; not returned -10. */
    public static int relationDelta(boolean returned, int wounded, int dead, boolean completed) {
        if (wounded < 0 || dead < 0) throw new IllegalArgumentException("Invalid escort losses");
        int delta = completed ? 10 : 0;
        delta -= wounded * 5;
        delta -= dead * 40;
        if (!returned) delta -= 10;
        return delta;
    }
}
