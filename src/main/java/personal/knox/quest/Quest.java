package personal.knox.quest;

public record Quest(String id, Kind kind, String giverFactionId, String targetId, int stages, State state) {
    public enum Kind { RESCUE, SEARCH, RESOURCES, SPECIAL_ZOMBIE, ESCORT, PATROL, DEFEND, DELIVER, REPAIR, TRADE }
    public enum State { OFFERED, ACTIVE, COMPLETED, FAILED, DECLINED }

    public Quest {
        if (id == null || id.isBlank() || giverFactionId == null || giverFactionId.isBlank()
                || targetId == null || targetId.isBlank() || kind == null || state == null)
            throw new IllegalArgumentException("Missing quest data");
        if (stages <= 0) throw new IllegalArgumentException("Invalid quest stages");
    }

    public Quest withState(State next) {
        if (next == null) throw new IllegalArgumentException("Missing quest state");
        return new Quest(id, kind, giverFactionId, targetId, stages, next);
    }
}
