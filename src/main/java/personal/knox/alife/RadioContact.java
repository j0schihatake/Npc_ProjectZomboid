package personal.knox.alife;

/** Incoming radio contact from a group the player met. */
public record RadioContact(String groupId, Stance stance, Kind kind, String text, long atMillis) {
    public enum Stance { ENEMY, NEUTRAL, ALLY }
    public enum Kind { WARNING, QUESTION, CHAT, JOKE, STORY, DISTRESS }

    public RadioContact {
        if (groupId == null || groupId.isBlank() || stance == null || kind == null
                || text == null || text.isBlank() || atMillis < 0)
            throw new IllegalArgumentException("Missing radio contact");
    }
}
