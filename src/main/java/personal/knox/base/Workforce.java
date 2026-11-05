package personal.knox.base;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Allocates only free, healthy-enough members; capacity depends on their health. */
public final class Workforce {
    public record Member(String id, double health, boolean free, boolean wounded) {
        public Member {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Missing member id");
            if (!Double.isFinite(health) || health < 0 || health > 100)
                throw new IllegalArgumentException("Invalid health");
        }
    }

    public record Task(String id, int required, int priority) {
        public Task {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Missing task id");
            if (required <= 0) throw new IllegalArgumentException("Invalid required workers");
        }
    }

    public record Assignment(String taskId, List<String> memberIds) {
        public Assignment {
            if (taskId == null || memberIds == null) throw new IllegalArgumentException("Missing assignment");
            memberIds = List.copyOf(memberIds);
        }
    }

    private Workforce() {}

    public static boolean able(Member member) {
        return member != null && member.free() && !member.wounded() && member.health() >= 40;
    }

    public static int available(List<Member> members) {
        if (members == null) return 0;
        int count = 0;
        for (var member : members) if (able(member)) count++;
        return count;
    }

    /** Sum of health/100 for able members: fewer wounded means more usable capacity. */
    public static double capacity(List<Member> members) {
        if (members == null) return 0;
        double total = 0;
        for (var member : members) if (able(member)) total += member.health() / 100.0;
        return total;
    }

    public static List<Assignment> allocate(List<Member> members, List<Task> tasks) {
        if (members == null || tasks == null) throw new IllegalArgumentException("Missing members or tasks");
        var pool = new ArrayList<Member>();
        for (var member : members) if (able(member)) pool.add(member);
        pool.sort(Comparator.comparingDouble(Member::health).reversed());
        var ordered = new ArrayList<>(tasks);
        ordered.sort(Comparator.comparingInt(Task::priority).reversed());
        var result = new ArrayList<Assignment>();
        int next = 0;
        for (var task : ordered) {
            if (task == null) throw new IllegalArgumentException("Null task");
            var assigned = new ArrayList<String>();
            while (assigned.size() < task.required() && next < pool.size()) {
                assigned.add(pool.get(next++).id());
            }
            if (!assigned.isEmpty()) result.add(new Assignment(task.id(), assigned));
        }
        return List.copyOf(result);
    }
}
