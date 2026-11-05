package personal.knox.alife;

public final class SpecialistLoanTest {
    public static void run() {
        var loan = new SpecialistLoan("doc", SpecialistLoan.Skill.DOCTOR, 4, 7);
        assert loan.dailyRelationGain() == 8;
        loan.dayPassed();
        assert loan.daysRemaining() == 6;
        loan.returnHome();
        assert loan.returned();
        try { loan.dayPassed(); throw new AssertionError("Returned loan advanced"); }
        catch (IllegalStateException expected) { }
        try { new SpecialistLoan("x", SpecialistLoan.Skill.DOCTOR, 0, 7); throw new AssertionError("Zero level accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new SpecialistLoan("x", SpecialistLoan.Skill.DOCTOR, 1, 0); throw new AssertionError("Zero duration accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Specialist-loan tests passed: daily gain, duration, return and invalid data.");
    }
}
