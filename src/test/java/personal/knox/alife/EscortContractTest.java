package personal.knox.alife;

public final class EscortContractTest {
    public static void run() {
        var contract = new EscortContract("ally", 2);
        contract.wound();
        assert contract.wounded() == 1;
        contract.returnHome();
        assert contract.state() == EscortContract.State.RETURNED;
        assert contract.relationDelta() > 0;
        var failed = new EscortContract("ally", 2);
        failed.kill();
        failed.abandon();
        assert failed.relationDelta() < -40 : "Death is costly even when abandoned";
        try { new EscortContract("ally", 0); throw new AssertionError("Zero escorts accepted"); }
        catch (IllegalArgumentException expected) { }
        try { contract.returnHome(); throw new AssertionError("Closed contract changed"); }
        catch (IllegalStateException expected) { }
        System.out.println("Escort-contract tests passed: wound, return, death/abandon, closed state and invalid size.");
    }
}
