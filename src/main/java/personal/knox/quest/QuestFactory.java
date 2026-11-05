package personal.knox.quest;

import java.util.Locale;

/** Deterministic quest creation from a faction offer. */
public final class QuestFactory {
    private QuestFactory() {}

    public static Quest create(Quest.Kind kind, String giverFactionId, String targetId, int stages) {
        if (kind == null) throw new IllegalArgumentException("Missing quest kind");
        if (giverFactionId == null || giverFactionId.isBlank()
                || targetId == null || targetId.isBlank() || stages <= 0)
            throw new IllegalArgumentException("Invalid quest data");
        String id = kind.name().toLowerCase(Locale.ROOT) + "-" + giverFactionId + "-" + targetId;
        return new Quest(id, kind, giverFactionId, targetId, stages, Quest.State.OFFERED);
    }
}
