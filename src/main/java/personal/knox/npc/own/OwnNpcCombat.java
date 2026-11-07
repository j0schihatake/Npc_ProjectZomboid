package personal.knox.npc.own;

import java.util.List;

/** Target selection and damage for own NPCs; no game objects. */
public final class OwnNpcCombat {
    public record Target(String id, double x, double y, double health, boolean visible) {
        public Target {
            if (id == null || id.isBlank() || !Double.isFinite(x + y + health) || health < 0)
                throw new IllegalArgumentException("Invalid target");
        }
    }

    public record Result(String targetId, double damage, boolean killed, int ammoLeft) {
        public Result {
            if (targetId == null || !Double.isFinite(damage) || damage < 0 || ammoLeft < 0)
                throw new IllegalArgumentException("Invalid combat result");
        }
    }

    private OwnNpcCombat() {}

    /** Ranged engagement distance: weapon reach, optics and weather. */
    public record Point(double x, double y) {}

    /** Step directly away from the enemy, keeping the desired distance. */
    public static Point retreatPoint(double selfX, double selfY, double enemyX, double enemyY,
                                     double desired, double step) {
        if (!Double.isFinite(selfX + selfY + enemyX + enemyY + desired + step)
                || desired < 0 || step <= 0) throw new IllegalArgumentException("Invalid retreat");
        double dx = selfX - enemyX;
        double dy = selfY - enemyY;
        double distance = Math.hypot(dx, dy);
        if (distance < 1e-9) return new Point(selfX, selfY);
        if (distance >= desired) return new Point(selfX, selfY);
        double move = Math.min(step, desired - distance);
        return new Point(selfX + dx / distance * move, selfY + dy / distance * move);
    }

    public static double engagementRange(int remoteRange, boolean scope, String weather) {
        double range = Math.max(8, Math.min(40, remoteRange > 0 ? remoteRange : 8));
        if (scope) range *= 1.5;
        String w = weather == null ? "" : weather.toLowerCase(java.util.Locale.ROOT);
        if (w.contains("storm") || w.contains("thunder")) range *= 0.45;
        else if (w.contains("rain") || w.contains("fog") || w.contains("snow")) range *= 0.65;
        return Math.max(4, Math.min(40, range));
    }

    /** Weapon and skill damage; condition reduces it, never below 5. */
    public static double damage(int weaponLevel, int skill, int condition, int conditionMax, boolean ranged) {
        double base = (ranged ? 10 : 8) + Math.max(0, weaponLevel) * 2 + Math.max(0, skill) * 2;
        if (conditionMax > 0) {
            double ratio = Math.max(0, Math.min(1, condition / (double) conditionMax));
            base *= 0.5 + 0.5 * ratio;
        }
        return Math.max(5, Math.min(60, base));
    }

    public static Target nearest(List<Target> targets, double x, double y, double range) {
        if (targets == null || range < 0 || !Double.isFinite(x + y + range))
            throw new IllegalArgumentException("Invalid target query");
        Target best = null;
        double bestDistance = Double.MAX_VALUE;
        for (var target : targets) {
            if (target.health() <= 0 || !target.visible()) continue;
            double distance = Math.hypot(target.x() - x, target.y() - y);
            if (distance <= range && distance < bestDistance) {
                best = target;
                bestDistance = distance;
            }
        }
        return best;
    }

    public static Result melee(Target target, double damage) {
        return hit(target, damage, 0);
    }

    public static Result ranged(Target target, double damage, int ammo) {
        if (ammo <= 0) throw new IllegalStateException("No ammo");
        return hit(target, damage, ammo - 1);
    }

    private static Result hit(Target target, double damage, int ammoLeft) {
        if (target == null || !Double.isFinite(damage) || damage <= 0)
            throw new IllegalArgumentException("Invalid attack");
        double applied = Math.min(damage, target.health());
        return new Result(target.id(), applied, target.health() - applied <= 0, ammoLeft);
    }
}
