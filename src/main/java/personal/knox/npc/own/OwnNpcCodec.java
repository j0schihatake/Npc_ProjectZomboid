package personal.knox.npc.own;

import java.util.ArrayList;
import java.util.List;
import personal.knox.npc.NpcState;

/** Compact text snapshot for ModData/save; no game objects. */
public final class OwnNpcCodec {
    private static final String SEP = "|";
    private static final String NL = "\n";

    private OwnNpcCodec() {}

    public static String encode(List<OwnNpcAgent> agents) {
        if (agents == null) throw new IllegalArgumentException("Missing agents");
        var text = new StringBuilder();
        for (var agent : agents) {
            if (agent.id().contains(SEP) || agent.factionId().contains(SEP) || agent.id().contains(NL))
                throw new IllegalArgumentException("Unsupported id character");
            var s = agent.state();
            text.append(agent.id()).append(SEP).append(agent.factionId()).append(SEP)
                    .append(agent.x()).append(SEP).append(agent.y()).append(SEP).append(agent.z()).append(SEP)
                    .append(agent.targetX()).append(SEP).append(agent.targetY()).append(SEP)
                    .append(agent.supplies()).append(SEP)
                    .append(s.health()).append(SEP).append(s.hunger()).append(SEP).append(s.thirst()).append(SEP)
                    .append(s.fatigue()).append(SEP).append(s.morale()).append(SEP)
                    .append(s.bleeding() ? 1 : 0).append(SEP).append(s.downed() ? 1 : 0).append(SEP)
                    .append(s.role().name()).append(SEP).append(s.task().name()).append(NL);
        }
        return text.toString();
    }

    public static List<OwnNpcAgent> decode(String text) {
        if (text == null) throw new IllegalArgumentException("Missing snapshot");
        var result = new ArrayList<OwnNpcAgent>();
        for (var line : text.split(NL)) {
            if (line.isBlank()) continue;
            var p = line.split("\\|", -1);
            if (p.length != 17) throw new IllegalArgumentException("Invalid agent line");
            try {
                result.add(new OwnNpcAgent(p[0], p[1],
                        Double.parseDouble(p[2]), Double.parseDouble(p[3]), Double.parseDouble(p[4]),
                        Double.parseDouble(p[5]), Double.parseDouble(p[6]), Integer.parseInt(p[7]),
                        new NpcState(Double.parseDouble(p[8]), Double.parseDouble(p[9]),
                                Double.parseDouble(p[10]), Double.parseDouble(p[11]), Double.parseDouble(p[12]),
                                "1".equals(p[13]), "1".equals(p[14]),
                                NpcState.Role.valueOf(p[15]), NpcState.Task.valueOf(p[16]))));
            } catch (RuntimeException invalid) {
                throw new IllegalArgumentException("Invalid agent line", invalid);
            }
        }
        return List.copyOf(result);
    }
}
