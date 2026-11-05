package personal.knox.alife;

import java.util.ArrayList;
import java.util.List;

/** Bounded recent radio traffic for the player-visible log. */
public final class RadioTraffic {
    public static final int MAX_MESSAGES = 20;
    private final List<RadioMessage> messages = new ArrayList<>();

    public void add(RadioMessage message) {
        if (message == null) throw new IllegalArgumentException("Missing message");
        messages.add(message);
        while (messages.size() > MAX_MESSAGES) messages.remove(0);
    }

    public List<RadioMessage> recent() { return List.copyOf(messages); }

    public List<RadioMessage> from(String fromId) {
        var result = new ArrayList<RadioMessage>();
        for (var message : messages) if (message.fromId().equals(fromId)) result.add(message);
        return List.copyOf(result);
    }

    public int size() { return messages.size(); }
    public void clear() { messages.clear(); }
}
