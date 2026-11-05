package personal.knox.economy;

/** Specialized station: no input resources, only a worker of the right profession. */
public final class ResourceStation {
    public enum Profession { MEDIC, COOK, ARMORER }

    private final Profession profession;
    private final int level;

    public ResourceStation(Profession profession, int level) {
        if (profession == null) throw new IllegalArgumentException("Missing profession");
        if (level < 1 || level > 7) throw new IllegalArgumentException("Invalid station level");
        this.profession = profession;
        this.level = level;
    }

    public Profession profession() { return profession; }
    public int level() { return level; }

    public ResourceBag.Kind output() {
        return switch (profession) {
            case MEDIC -> ResourceBag.Kind.MEDICAL;
            case COOK -> ResourceBag.Kind.FOOD;
            case ARMORER -> ResourceBag.Kind.AMMO;
        };
    }

    /** Worker skill 1..5; a station with no worker produces nothing. */
    public ResourceBag craft(int workerSkill, int workerCount) {
        if (workerSkill < 1 || workerSkill > 5) throw new IllegalArgumentException("Invalid worker");
        if (workerCount <= 0) return new ResourceBag(output(), 0);
        int units = Math.min(ResourceBag.DEFAULT_CAPACITY, 1 + (workerSkill + level) / 2);
        return new ResourceBag(output(), units);
    }
}
