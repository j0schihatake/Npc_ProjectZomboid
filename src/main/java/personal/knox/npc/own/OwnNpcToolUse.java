package personal.knox.npc.own;

import java.util.EnumSet;
import java.util.Set;

/** Tool-gated obstacle removal for own NPCs; pure model. */
public final class OwnNpcToolUse {
    public enum Tool { HAMMER, SAW, CROWBAR, SCREWDRIVER }
    public enum Obstacle { WINDOW, FENCE, WALL, DOOR, BARRICADE }

    private OwnNpcToolUse() {}

    public static boolean canDismantle(Set<Tool> tools, Obstacle obstacle) {
        if (tools == null || obstacle == null) throw new IllegalArgumentException("Missing tools or obstacle");
        return tools.containsAll(required(obstacle));
    }

    public static int seconds(Set<Tool> tools, Obstacle obstacle) {
        if (!canDismantle(tools, obstacle)) throw new IllegalStateException("Missing tools");
        return switch (obstacle) {
            case WINDOW -> 3;
            case FENCE -> 6;
            case DOOR -> 8;
            case BARRICADE -> 10;
            case WALL -> 20;
        };
    }

    public static Set<Tool> required(Obstacle obstacle) {
        if (obstacle == null) throw new IllegalArgumentException("Missing obstacle");
        return switch (obstacle) {
            case WINDOW -> EnumSet.of(Tool.SCREWDRIVER);
            case FENCE -> EnumSet.of(Tool.HAMMER);
            case DOOR -> EnumSet.of(Tool.SCREWDRIVER, Tool.HAMMER);
            case BARRICADE -> EnumSet.of(Tool.CROWBAR);
            case WALL -> EnumSet.of(Tool.CROWBAR, Tool.HAMMER);
        };
    }
}
