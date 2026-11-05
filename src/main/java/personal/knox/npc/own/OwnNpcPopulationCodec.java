package personal.knox.npc.own;

import java.util.ArrayList;
import java.util.List;

/** Text snapshot of strategic groups; no game objects. */
public final class OwnNpcPopulationCodec {
    private static final String SEP = "|";
    private static final String NL = "\n";

    private OwnNpcPopulationCodec() {}

    public static String encode(List<OwnNpcPopulation.Group> groups) {
        if (groups == null) throw new IllegalArgumentException("Missing groups");
        var text = new StringBuilder();
        for (var g : groups) {
            if (g.id().contains(SEP) || g.factionId().contains(SEP))
                throw new IllegalArgumentException("Unsupported group id");
            text.append(g.id()).append(SEP).append(g.factionId()).append(SEP).append(g.kind().name()).append(SEP)
                    .append(g.members()).append(SEP).append(g.x()).append(SEP).append(g.y()).append(SEP)
                    .append(g.targetX()).append(SEP).append(g.targetY()).append(NL);
        }
        return text.toString();
    }

    public static List<OwnNpcPopulation.Group> decode(String text) {
        if (text == null) throw new IllegalArgumentException("Missing snapshot");
        var result = new ArrayList<OwnNpcPopulation.Group>();
        for (var line : text.split(NL)) {
            if (line.isBlank()) continue;
            var p = line.split("\\|", -1);
            if (p.length != 8) throw new IllegalArgumentException("Invalid group line");
            try {
                result.add(new OwnNpcPopulation.Group(p[0], p[1],
                        OwnNpcPopulation.Group.Kind.valueOf(p[2]), Integer.parseInt(p[3]),
                        Double.parseDouble(p[4]), Double.parseDouble(p[5]),
                        Double.parseDouble(p[6]), Double.parseDouble(p[7])));
            } catch (RuntimeException invalid) {
                throw new IllegalArgumentException("Invalid group line", invalid);
            }
        }
        return List.copyOf(result);
    }
}
