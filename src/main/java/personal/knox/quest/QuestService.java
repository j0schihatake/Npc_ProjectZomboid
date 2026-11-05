package personal.knox.quest;

/** Ties offers to the journal and reports the relation delta of each decision. */
public final class QuestService {
    private final QuestBoard board = new QuestBoard();
    private final QuestLog log = new QuestLog();

    public Quest offer(String factionId, Quest.Kind kind, String targetId, int stages) {
        var quest = QuestFactory.create(kind, factionId, targetId, stages);
        board.offer(factionId, quest);
        return quest;
    }

    public int accept(String factionId, String questId) {
        var quest = board.take(factionId, questId);
        if (quest == null) throw new IllegalArgumentException("Unknown offer");
        log.offer(quest);
        log.accept(questId);
        return QuestPolicy.relationDelta(Quest.State.ACTIVE);
    }

    public int decline(String factionId, String questId) {
        var quest = board.take(factionId, questId);
        if (quest == null) throw new IllegalArgumentException("Unknown offer");
        log.offer(quest);
        log.decline(questId);
        return QuestPolicy.relationDelta(Quest.State.DECLINED);
    }

    public int complete(String questId) {
        log.complete(questId);
        return QuestPolicy.relationDelta(Quest.State.COMPLETED);
    }

    public int fail(String questId) {
        log.fail(questId);
        return QuestPolicy.relationDelta(Quest.State.FAILED);
    }

    public QuestLog log() { return log; }
    public QuestBoard board() { return board; }
}
