package personal.knox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Universal weighted survey for one NPC asking others. No game objects. */
public final class Survey {
    public record Answer(String option, double weight) {
        public Answer {
            if (option == null || option.isBlank()) throw new IllegalArgumentException("Missing option");
            if (!Double.isFinite(weight) || weight <= 0) throw new IllegalArgumentException("Invalid weight");
        }
    }

    private Survey() {}

    /** Leader answer wins; otherwise weighted majority; a tie returns the caller's safe fallback. */
    public static String result(List<Answer> answers, String leaderChoice, String fallback) {
        if (fallback == null || fallback.isBlank()) throw new IllegalArgumentException("Missing fallback");
        if (leaderChoice != null && !leaderChoice.isBlank()) return leaderChoice;
        if (answers == null || answers.isEmpty()) return fallback;
        Map<String, Double> totals = new LinkedHashMap<>();
        for (var answer : answers) {
            if (answer == null) throw new IllegalArgumentException("Null answer");
            totals.merge(answer.option(), answer.weight(), Double::sum);
        }
        String best = null;
        double bestWeight = -1;
        boolean tie = false;
        for (var entry : totals.entrySet()) {
            if (entry.getValue() > bestWeight) {
                best = entry.getKey();
                bestWeight = entry.getValue();
                tie = false;
            } else if (entry.getValue() == bestWeight) {
                tie = true;
            }
        }
        return tie ? fallback : best;
    }
}
