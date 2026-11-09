package personal.knox.alife;

import java.util.ArrayList;
import java.util.List;

/** Longer-lived squad data: leader, route, wounded, morale and last battle. */
public record AlifeSquadState(String squadId, String leader, List<double[]> route, int wounded, int morale,
                              String lastBattle) {
    public AlifeSquadState {
        if (squadId == null || squadId.isBlank()) throw new IllegalArgumentException("Missing squad");
        leader = leader == null ? "" : leader;
        route = route == null ? List.of() : List.copyOf(route);
        if (wounded < 0 || morale < 0 || morale > 100) throw new IllegalArgumentException("Invalid state");
        lastBattle = lastBattle == null ? "" : lastBattle;
    }

    public static AlifeSquadState from(Squad squad, String leader) {
        return new AlifeSquadState(squad.id(), leader, List.of(), 0, 70, "");
    }

    public AlifeSquadState withWounded(int value) {
        return new AlifeSquadState(squadId, leader, route, Math.max(0, value), morale, lastBattle);
    }

    public AlifeSquadState withMorale(int value) {
        return new AlifeSquadState(squadId, leader, route, wounded, Math.max(0, Math.min(100, value)), lastBattle);
    }

    public AlifeSquadState afterBattle(Squad squad, String summary) {
        return new AlifeSquadState(squadId, leader, List.of(), wounded, Math.max(0, morale - 10), summary);
    }

    public AlifeSquadState route(double x, double y) {
        var next = new ArrayList<>(route);
        if (next.size() >= 32) next.remove(0);
        next.add(new double[]{x, y});
        return new AlifeSquadState(squadId, leader, next, wounded, morale, lastBattle);
    }

    public double[] nextPoint() {
        return route.isEmpty() ? null : route.get(0);
    }
}
