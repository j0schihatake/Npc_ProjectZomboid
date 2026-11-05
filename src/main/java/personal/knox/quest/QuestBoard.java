package personal.knox.quest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Per-faction offers; the player can take or decline without a relation penalty. */
public final class QuestBoard {
    public static final int MAX_OFFERS = 5;
    private final Map<String, List<Quest>> offers = new LinkedHashMap<>();

    public void offer(String factionId, Quest quest) {
        if (factionId == null || factionId.isBlank() || quest == null)
            throw new IllegalArgumentException("Missing offer");
        var list = offers.computeIfAbsent(factionId, key -> new ArrayList<>());
        if (list.size() >= MAX_OFFERS) throw new IllegalStateException("Too many offers");
        for (var existing : list) {
            if (existing.id().equals(quest.id())) throw new IllegalArgumentException("Duplicate offer");
        }
        list.add(quest);
    }

    public List<Quest> offers(String factionId) {
        var list = offers.get(factionId);
        return list == null ? List.of() : List.copyOf(list);
    }

    public Quest take(String factionId, String questId) {
        var list = offers.get(factionId);
        if (list == null) return null;
        for (var quest : list) {
            if (quest.id().equals(questId)) {
                list.remove(quest);
                return quest;
            }
        }
        return null;
    }
}
