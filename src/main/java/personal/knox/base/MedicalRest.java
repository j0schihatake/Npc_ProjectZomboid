package personal.knox.base;

/** Rest and medic rules for wounded companions at the base. */
public final class MedicalRest {
    public enum Place {
        BED(0.80), SOFA(0.60), CHAIR(0.40), FLOOR(0.25);
        private final double healPerHour;
        Place(double healPerHour) { this.healPerHour = healPerHour; }
        public double healPerHour() { return healPerHour; }
    }

    private MedicalRest() {}

    public static boolean needsRest(double health, boolean bleeding, double fatigue) {
        if (!Double.isFinite(health) || health < 0 || health > 100
                || !Double.isFinite(fatigue) || fatigue < 0 || fatigue > 1)
            throw new IllegalArgumentException("Invalid rest state");
        return health < 60 || bleeding || fatigue >= 0.75;
    }

    public static boolean needsMedic(double health, boolean bleeding) {
        if (!Double.isFinite(health) || health < 0 || health > 100)
            throw new IllegalArgumentException("Invalid health");
        return bleeding || health < 50;
    }

    /** Best available place; heavy cases should get beds first. */
    public static Place place(boolean bed, boolean sofa, boolean chair) {
        if (bed) return Place.BED;
        if (sofa) return Place.SOFA;
        if (chair) return Place.CHAIR;
        return Place.FLOOR;
    }
}
