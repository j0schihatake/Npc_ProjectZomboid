package personal.knox.alife;

/** Guards and gear for a faction base, scaling with level and stock. */
public final class BaseDefense {
    private BaseDefense() {}

    public static int guards(int level, int population) {
        BaseLevel.require(level);
        if (population < 0) throw new IllegalArgumentException("Invalid population");
        return Math.min(population, level + 1);
    }

    public static int firearms(int level, int guards, int ammo) {
        BaseLevel.require(level);
        if (guards < 0 || ammo < 0) throw new IllegalArgumentException("Invalid defense state");
        if (level < 3 || ammo == 0) return 0;
        return Math.min(guards, Math.max(1, ammo / 10));
    }

    public static double gearScore(int level, int materials, int ammo) {
        BaseLevel.require(level);
        if (materials < 0 || ammo < 0) throw new IllegalArgumentException("Invalid gear state");
        double levelPart = Math.min(1, level / 7.0);
        double materialPart = Math.min(1, materials / 100.0);
        double ammoPart = Math.min(1, ammo / 60.0);
        return (levelPart + materialPart + ammoPart) / 3.0;
    }
}
