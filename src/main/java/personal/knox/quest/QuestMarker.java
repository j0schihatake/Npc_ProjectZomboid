package personal.knox.quest;

/** Map marker for an active quest objective. */
public record QuestMarker(String questId, double x, double y, double z, String label) {
    public QuestMarker {
        if (questId == null || questId.isBlank() || label == null || label.isBlank())
            throw new IllegalArgumentException("Missing marker data");
        if (!Double.isFinite(x + y + z)) throw new IllegalArgumentException("Invalid marker position");
    }
}
