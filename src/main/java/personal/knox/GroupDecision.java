package personal.knox;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Consensus for squad decisions when the leader does not answer. No game objects. */
public final class GroupDecision {
    public enum Choice { ATTACK, HOLD, RETREAT }

    public record Vote(Choice choice, double weight) {
        public Vote {
            if (choice == null) throw new IllegalArgumentException("Missing vote");
            if (!Double.isFinite(weight) || weight <= 0) throw new IllegalArgumentException("Invalid vote weight");
        }
    }

    private GroupDecision() {}

    /**
     * A leader's explicit answer wins. Otherwise the weighted majority decides;
     * a tie falls back to the safest choice (retreat), never to an accidental attack.
     */
    public static Choice decide(List<Vote> votes, Choice leaderChoice) {
        if (leaderChoice != null) return leaderChoice;
        if (votes == null || votes.isEmpty()) return Choice.HOLD;
        Map<Choice, Double> totals = new EnumMap<>(Choice.class);
        for (var vote : votes) {
            if (vote == null) throw new IllegalArgumentException("Null vote");
            totals.merge(vote.choice(), vote.weight(), Double::sum);
        }
        Choice best = Choice.HOLD;
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
        return tie ? Choice.RETREAT : best;
    }
}
