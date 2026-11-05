package personal.knox.npc.own;

import java.util.LinkedHashMap;
import java.util.Map;

/** Persists per-agent target floors. */
public final class OwnNpcTargetZCodec {
    private OwnNpcTargetZCodec() {}

    public static String encode(Map<String, Double> values) {
        if (values == null) throw new IllegalArgumentException("Missing values");
        var text = new StringBuilder();
        for (var entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null
                    || !Double.isFinite(entry.getValue()))
                throw new IllegalArgumentException("Invalid target floor");
            text.append(entry.getKey()).append('|').append(entry.getValue()).append('\n');
        }
        return text.toString();
    }

    public static Map<String, Double> decode(String text) {
        if (text == null) throw new IllegalArgumentException("Missing snapshot");
        var result = new LinkedHashMap<String, Double>();
        for (var line : text.split("\n")) {
            if (line.isBlank()) continue;
            var parts = line.split("\\|", -1);
            if (parts.length != 2) throw new IllegalArgumentException("Invalid target floor line");
            try {
                result.put(parts[0], Double.parseDouble(parts[1]));
            } catch (RuntimeException invalid) {
                throw new IllegalArgumentException("Invalid target floor line", invalid);
            }
        }
        return result;
    }
}
