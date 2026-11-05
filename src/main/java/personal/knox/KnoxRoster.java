package personal.knox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Parsed Knox group roster; data only. */
public final class KnoxRoster {
    public record Member(String id, double x, double y, double z) {
        public Member {
            if (id == null || id.isBlank() || !Double.isFinite(x + y + z))
                throw new IllegalArgumentException("Invalid member");
        }
    }

    private KnoxRoster() {}

    public static List<Member> parse(String text) {
        if (text == null) throw new IllegalArgumentException("Missing roster");
        var result = new ArrayList<Member>();
        for (var entry : text.split(";")) {
            if (entry.isBlank()) continue;
            int at = entry.indexOf('@');
            if (at <= 0) throw new IllegalArgumentException("Invalid member entry");
            var parts = entry.substring(at + 1).split(",", -1);
            if (parts.length != 3) throw new IllegalArgumentException("Invalid member position");
            try {
                result.add(new Member(entry.substring(0, at),
                        Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
            } catch (RuntimeException invalid) {
                throw new IllegalArgumentException("Invalid member entry", invalid);
            }
        }
        return List.copyOf(result);
    }

    public static String format(List<Member> members) {
        if (members == null) throw new IllegalArgumentException("Missing members");
        var text = new StringBuilder();
        for (var member : members) {
            text.append(member.id()).append('@')
                    .append(String.format(Locale.ROOT, "%.1f,%.1f,%.1f", member.x(), member.y(), member.z()))
                    .append(';');
        }
        return text.toString();
    }
}
