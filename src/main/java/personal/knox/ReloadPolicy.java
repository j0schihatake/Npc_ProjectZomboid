package personal.knox;

/** Whether a character should start a native weapon reload now. */
public final class ReloadPolicy {
    private ReloadPolicy() {}

    public static boolean shouldReload(int current, int max, int reserve,
                                       boolean armed, boolean busy, boolean driving) {
        if (current < 0 || max <= 0 || current > max || reserve < 0)
            throw new IllegalArgumentException("Invalid reload state");
        return armed && !busy && !driving && reserve > 0 && current < max;
    }
}
