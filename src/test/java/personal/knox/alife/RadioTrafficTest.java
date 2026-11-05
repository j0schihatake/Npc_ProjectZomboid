package personal.knox.alife;

public final class RadioTrafficTest {
    public static void run() {
        var traffic = new RadioTraffic();
        traffic.add(new RadioMessage("a", "b", "How are you?", 10, RadioMessage.Kind.CHAT));
        traffic.add(new RadioMessage("a", "b", "Need help", 20, RadioMessage.Kind.DISTRESS));
        assert traffic.size() == 2 && traffic.from("a").size() == 2;
        for (int i = 0; i < RadioTraffic.MAX_MESSAGES + 5; i++) {
            traffic.add(new RadioMessage("b", "a", "m" + i, i, RadioMessage.Kind.CHAT));
        }
        assert traffic.size() == RadioTraffic.MAX_MESSAGES : "Traffic is bounded";
        assert traffic.recent().get(traffic.size() - 1).text().equals("m24");
        try { new RadioMessage("", "b", "x", 0, RadioMessage.Kind.CHAT); throw new AssertionError("Empty sender accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new RadioMessage("a", "b", "", 0, RadioMessage.Kind.CHAT); throw new AssertionError("Empty text accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Radio-traffic tests passed: add, recent, filter, bound and invalid data.");
    }
}
