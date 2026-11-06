package personal.knox.npc.own;

import java.util.EnumSet;
import java.util.Set;

/** Tool-gated obstacle removal for own NPCs; pure model. */
public final class OwnNpcToolUse {
    public enum Tool { HAMMER, SAW, CROWBAR, SCREWDRIVER }
    public enum Obstacle { WINDOW, FENCE, WALL, DOOR, BARRICADE }

    public record Plan(Obstacle obstacle, Set<Tool> tools, int seconds, double noise, double wear) {
        public Plan {
            if (obstacle == null || tools == null || tools.isEmpty() || seconds <= 0 || noise < 0 || wear < 0)
                throw new IllegalArgumentException("Invalid dismantle plan");
            tools = Set.copyOf(tools);
        }
    }

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

    public static Obstacle classify(String className) {
        if (className == null || className.isBlank()) throw new IllegalArgumentException("Missing class name");
        String lower = className.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("barricade")) return Obstacle.BARRICADE;
        if (lower.contains("window")) return Obstacle.WINDOW;
        if (lower.contains("fence")) return Obstacle.FENCE;
        if (lower.contains("door")) return Obstacle.DOOR;
        if (lower.contains("wall")) return Obstacle.WALL;
        throw new IllegalArgumentException("Unknown obstacle: " + className);
    }

    public static Set<Tool> fromItems(java.util.Collection<String> itemTypes) {
        if (itemTypes == null) throw new IllegalArgumentException("Missing item types");
        var tools = EnumSet.noneOf(Tool.class);
        for (var type : itemTypes) {
            if (type == null) continue;
            String lower = type.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("hammer") || lower.contains("молот")) tools.add(Tool.HAMMER);
            if (lower.contains("saw") || lower.contains("пила")) tools.add(Tool.SAW);
            if (lower.contains("crowbar") || lower.contains("лом")) tools.add(Tool.CROWBAR);
            if (lower.contains("screwdriver") || lower.contains("отверт")) tools.add(Tool.SCREWDRIVER);
        }
        return tools;
    }

    public static Plan plan(Set<Tool> tools, Obstacle obstacle) {
        int seconds = seconds(tools, obstacle);
        double noise = switch (obstacle) {
            case WINDOW -> 2;
            case FENCE -> 4;
            case DOOR -> 5;
            case BARRICADE -> 6;
            case WALL -> 9;
        };
        return new Plan(obstacle, required(obstacle), seconds, noise, 1);
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
