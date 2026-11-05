package personal.knox.quest;

public final class QuestPolicy {
    private QuestPolicy() {}

    public static boolean canAccept(Quest quest) {
        if (quest == null) throw new IllegalArgumentException("Missing quest");
        return quest.state() == Quest.State.OFFERED;
    }

    public static Quest accept(Quest quest) {
        if (!canAccept(quest)) throw new IllegalStateException("Quest is not offered");
        return quest.withState(Quest.State.ACTIVE);
    }

    public static Quest decline(Quest quest) {
        if (!canAccept(quest)) throw new IllegalStateException("Quest is not offered");
        return quest.withState(Quest.State.DECLINED);
    }

    public static Quest complete(Quest quest) {
        if (quest == null || quest.state() != Quest.State.ACTIVE) throw new IllegalStateException("Quest is not active");
        return quest.withState(Quest.State.COMPLETED);
    }

    public static Quest fail(Quest quest) {
        if (quest == null || quest.state() != Quest.State.ACTIVE) throw new IllegalStateException("Quest is not active");
        return quest.withState(Quest.State.FAILED);
    }

    public static int relationDelta(Quest.State next) {
        if (next == null) throw new IllegalArgumentException("Missing state");
        return switch (next) {
            case DECLINED -> 0;
            case COMPLETED -> 15;
            case FAILED -> -15;
            default -> 0;
        };
    }
}
