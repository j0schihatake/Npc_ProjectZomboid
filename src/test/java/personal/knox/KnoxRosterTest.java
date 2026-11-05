package personal.knox;

public final class KnoxRosterTest {
    public static void run() {
        var text = "n1@10.0,20.0,0.0;n2@11.0,21.0,1.0;";
        var members = KnoxRoster.parse(text);
        assert members.size() == 2 && members.get(1).id().equals("n2") && members.get(1).z() == 1.0;
        assert KnoxRoster.format(members).equals(text);
        try { KnoxRoster.parse("bad"); throw new AssertionError("Bad entry accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Knox roster tests passed: parse, format and invalid entry.");
    }
}
