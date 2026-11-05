package personal.knox.alife;

/** Base radio operator; skill affects range and interception. */
public record RadioOperator(String npcId, String baseId, int skill, boolean onDuty) {
    public RadioOperator {
        if (npcId == null || npcId.isBlank() || baseId == null || baseId.isBlank())
            throw new IllegalArgumentException("Missing radio operator");
        if (skill < 1 || skill > 5) throw new IllegalArgumentException("Invalid radio skill");
    }

    public int rangeMeters() { return onDuty ? 200 + skill * 100 : 0; }

    public boolean canContact(double distanceMeters) {
        if (!Double.isFinite(distanceMeters) || distanceMeters < 0)
            throw new IllegalArgumentException("Invalid distance");
        return onDuty && distanceMeters <= rangeMeters();
    }

    /** 0..1 chance to catch nearby faction traffic. */
    public double interceptChance() { return onDuty ? Math.min(1.0, 0.1 + skill * 0.15) : 0; }
}
