package personal.knox.alife;

/** A skilled faction member loaned to an allied base for daily relation gain. */
public final class SpecialistLoan {
    public enum Skill { DOCTOR, MECHANIC, ENGINEER, FARMER, MARKSMAN }

    private final String specialistId;
    private final Skill skill;
    private final int level;
    private int daysRemaining;
    private boolean returned;

    public SpecialistLoan(String specialistId, Skill skill, int level, int days) {
        if (specialistId == null || specialistId.isBlank() || skill == null)
            throw new IllegalArgumentException("Missing specialist");
        if (level < 1 || level > 5) throw new IllegalArgumentException("Invalid skill level");
        if (days < 1 || days > 30) throw new IllegalArgumentException("Invalid loan duration");
        this.specialistId = specialistId;
        this.skill = skill;
        this.level = level;
        this.daysRemaining = days;
    }

    public String specialistId() { return specialistId; }
    public Skill skill() { return skill; }
    public int level() { return level; }
    public int daysRemaining() { return daysRemaining; }
    public boolean returned() { return returned; }

    /** Higher skill is worth more to the ally. */
    public int dailyRelationGain() { return level * 2; }

    public void dayPassed() {
        if (returned || daysRemaining <= 0) throw new IllegalStateException("Loan is over");
        daysRemaining--;
    }

    public void returnHome() {
        if (returned) throw new IllegalStateException("Already returned");
        returned = true;
    }
}
