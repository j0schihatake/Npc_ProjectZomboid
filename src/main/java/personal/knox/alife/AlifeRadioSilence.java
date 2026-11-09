package personal.knox.alife;

import java.util.HashMap;
import java.util.Map;

/** Outpost radio state: silence stops dispatch and schedules a response patrol. */
public final class AlifeRadioSilence {
    private final Map<String, Integer> silentUntilDay = new HashMap<>();

    public String silence(String outpostId, int day, int days) {
        if (outpostId == null || outpostId.isBlank() || days <= 0) return "invalid";
        silentUntilDay.put(outpostId, day + days);
        return "silenced=" + outpostId + " until=" + (day + days);
    }

    public boolean silent(String outpostId, int day) {
        return silentUntilDay.getOrDefault(outpostId, -1) > day;
    }

    public boolean canDispatch(String outpostId, int day) { return !silent(outpostId, day); }

    public String responsePatrol(String outpostId, int day) {
        return silent(outpostId, day) ? "patrol-sent=" + outpostId + " day=" + day : "no-patrol";
    }

    public int silenceCount() { return silentUntilDay.size(); }
}
