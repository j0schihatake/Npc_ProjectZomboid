package personal.knox.alife;

/** Active escort agreement with a faction; losses reduce the final relation delta. */
public final class EscortContract {
    public enum State { ACTIVE, RETURNED, FAILED }

    private final String factionId;
    private final int members;
    private int wounded;
    private int dead;
    private boolean returned;
    private State state = State.ACTIVE;

    public EscortContract(String factionId, int members) {
        if (factionId == null || factionId.isBlank()) throw new IllegalArgumentException("Missing faction");
        if (members < 1 || members > 3) throw new IllegalArgumentException("Invalid escort size");
        this.factionId = factionId;
        this.members = members;
    }

    public String factionId() { return factionId; }
    public int members() { return members; }
    public int wounded() { return wounded; }
    public int dead() { return dead; }
    public State state() { return state; }

    public void wound() {
        if (state != State.ACTIVE) throw new IllegalStateException("Contract is closed");
        if (wounded < members - dead) wounded++;
    }

    public void kill() {
        if (state != State.ACTIVE) throw new IllegalStateException("Contract is closed");
        if (dead < members) dead++;
    }

    public void returnHome() {
        if (state != State.ACTIVE) throw new IllegalStateException("Contract is closed");
        returned = true;
        state = State.RETURNED;
    }

    public void abandon() {
        if (state != State.ACTIVE) throw new IllegalStateException("Contract is closed");
        state = State.FAILED;
    }

    public int relationDelta() {
        return EscortPolicy.relationDelta(returned, wounded, dead, state == State.RETURNED);
    }
}
