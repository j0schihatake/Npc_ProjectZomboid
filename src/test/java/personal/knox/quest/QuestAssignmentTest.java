package personal.knox.quest;

public final class QuestAssignmentTest {
    public static void run() {
        var assignment = new QuestAssignment("q1", "group-a", 100, QuestAssignment.State.ASSIGNED);
        assert assignment.state() == QuestAssignment.State.ASSIGNED;
        assert assignment.withState(QuestAssignment.State.EN_ROUTE).state() == QuestAssignment.State.EN_ROUTE;
        assert assignment.withState(QuestAssignment.State.NEEDS_RESCUE).state() == QuestAssignment.State.NEEDS_RESCUE;
        try { new QuestAssignment("", "g", 0, QuestAssignment.State.ASSIGNED); throw new AssertionError("Empty quest accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new QuestAssignment("q", "g", -1, QuestAssignment.State.ASSIGNED); throw new AssertionError("Negative time accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Quest-assignment tests passed: assignment, states and invalid data.");
    }
}
