package personal.knox.alife;

public final class RadioInterceptTest {
    public static void run() {
        var op = new RadioOperator("npc", "base", 5, true);
        assert RadioIntercept.intercepts(op, 100, 0);
        assert !RadioIntercept.intercepts(op, 100, 99) : "High roll misses";
        assert !RadioIntercept.intercepts(op, 1000, 0) : "Out of range";
        var off = new RadioOperator("npc", "base", 5, false);
        assert !RadioIntercept.intercepts(off, 0, 0);
        try { RadioIntercept.intercepts(null, 0, 0); throw new AssertionError("Null operator accepted"); }
        catch (IllegalArgumentException expected) { }
        try { RadioIntercept.intercepts(op, 0, 100); throw new AssertionError("Bad roll accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Radio-intercept tests passed: chance, range, duty and invalid data.");
    }
}
