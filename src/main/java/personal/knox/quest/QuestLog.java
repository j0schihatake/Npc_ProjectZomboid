package personal.knox.quest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Own quest journal: offers, active quests, completion/failure and lookup. */
public final class QuestLog {
    private final Map<String, Quest> quests = new LinkedHashMap<>();
    private final Map<String, QuestMarker> markers = new LinkedHashMap<>();

    public void offer(Quest quest) {
        if (quest == null || quests.containsKey(quest.id()))
            throw new IllegalArgumentException("Duplicate or missing quest");
        quests.put(quest.id(), quest);
    }

    public void setMarker(QuestMarker marker) {
        if (marker == null) throw new IllegalArgumentException("Missing marker");
        markers.put(marker.questId(), marker);
    }

    public QuestMarker marker(String questId) { return markers.get(questId); }
    public List<QuestMarker> markers() { return List.copyOf(markers.values()); }

    public Quest get(String id) { return quests.get(id); }

    public List<Quest> active() {
        var result = new ArrayList<Quest>();
        for (var quest : quests.values()) if (quest.state() == Quest.State.ACTIVE) result.add(quest);
        return List.copyOf(result);
    }

    public List<Quest> all() { return List.copyOf(quests.values()); }

    public void accept(String id) { replace(id, QuestPolicy.accept(require(id))); }
    public void decline(String id) { replace(id, QuestPolicy.decline(require(id))); }
    public void complete(String id) { replace(id, QuestPolicy.complete(require(id))); markers.remove(id); }
    public void fail(String id) { replace(id, QuestPolicy.fail(require(id))); markers.remove(id); }

    private Quest require(String id) {
        var quest = quests.get(id);
        if (quest == null) throw new IllegalArgumentException("Unknown quest");
        return quest;
    }

    private void replace(String id, Quest quest) { quests.put(id, quest); }
}
