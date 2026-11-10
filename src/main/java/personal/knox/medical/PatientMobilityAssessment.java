package personal.knox.medical;

/** Observable mobility state of a wounded NPC; no game objects. */
public record PatientMobilityAssessment(double mobilityScore, boolean legInjured,
                                        boolean bleeding, double pain, boolean canWalk,
                                        boolean onSortie) {
    public PatientMobilityAssessment {
        if (!Double.isFinite(mobilityScore) || mobilityScore < 0 || mobilityScore > 100)
            throw new IllegalArgumentException("Invalid mobility score");
        if (!Double.isFinite(pain) || pain < 0 || pain > 1)
            throw new IllegalArgumentException("Invalid pain");
    }

    /** A minor scratch with full mobility does not justify a crutch. */
    public boolean needsAid() {
        return legInjured || !canWalk || mobilityScore < 70;
    }

    public boolean severe() {
        return !canWalk || mobilityScore < 30;
    }

    public PatientMobilityAssessment withMobility(double value) {
        return new PatientMobilityAssessment(value, legInjured, bleeding, pain, canWalk, onSortie);
    }
}
