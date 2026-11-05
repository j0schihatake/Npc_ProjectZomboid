package personal.knox.alife;

/** One radio transmission visible in the live airwaves log. */
public record RadioMessage(String fromId, String toId, String text, long atMillis, Kind kind) {
    public enum Kind { CHAT, TASK, DISTRESS, ENEMY, TRADE }

    public RadioMessage {
        if (fromId == null || fromId.isBlank() || toId == null || toId.isBlank()
                || text == null || text.isBlank() || kind == null || atMillis < 0)
            throw new IllegalArgumentException("Missing radio message");
    }
}
