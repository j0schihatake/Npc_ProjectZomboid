package personal.knox.quest;

/** A quest taken over by a faction group instead of the player. */
public record QuestAssignment(String questId, String assigneeGroupId, long startedAtMillis, State state) {
    public enum State { ASSIGNED, EN_ROUTE, NEEDS_RESCUE, COMPLETED, FAILED }

    public QuestAssignment {
        if (questId == null || questId.isBlank() || assigneeGroupId == null || assigneeGroupId.isBlank()
                || state == null || startedAtMillis < 0)
            throw new IllegalArgumentException("Missing assignment data");
    }

    public QuestAssignment withState(State next) {
        if (next == null) throw new IllegalArgumentException("Missing assignment state");
        return new QuestAssignment(questId, assigneeGroupId, startedAtMillis, next);
    }
}
