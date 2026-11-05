package personal.knox.quest;

import java.util.ArrayList;
import java.util.List;

/** UI-agnostic dialogue state: lines, options and open/closed flag. */
public final class DialogueSession {
    public record Line(String speaker, String text) {
        public Line {
            if (speaker == null || speaker.isBlank() || text == null || text.isBlank())
                throw new IllegalArgumentException("Missing line");
        }
    }

    public record Option(String id, String label, Kind kind) {
        public enum Kind { INFO, QUEST, TRADE, JOIN, HELP, LEAVE }
        public Option {
            if (id == null || id.isBlank() || label == null || label.isBlank() || kind == null)
                throw new IllegalArgumentException("Missing option");
        }
    }

    private final String npcId;
    private final String factionId;
    private final List<Line> history = new ArrayList<>();
    private List<Option> options = List.of();
    private boolean open = true;

    public DialogueSession(String npcId, String factionId) {
        if (npcId == null || npcId.isBlank() || factionId == null || factionId.isBlank())
            throw new IllegalArgumentException("Missing dialogue participant");
        this.npcId = npcId;
        this.factionId = factionId;
    }

    public String npcId() { return npcId; }
    public String factionId() { return factionId; }
    public boolean open() { return open; }
    public List<Line> history() { return List.copyOf(history); }
    public List<Option> options() { return options; }

    public void say(String speaker, String text) {
        if (!open) throw new IllegalStateException("Dialogue is closed");
        history.add(new Line(speaker, text));
    }

    public void setOptions(List<Option> next) {
        if (next == null) throw new IllegalArgumentException("Missing options");
        options = List.copyOf(next);
    }

    public Option choose(String id) {
        if (!open) throw new IllegalStateException("Dialogue is closed");
        for (var option : options) if (option.id().equals(id)) return option;
        return null;
    }

    public void close() { open = false; }
}
