package personal.knox;

public final class ThreatDirectionTest {
    public static void run() {
        // Rotate both vectors through every facing: the relative sector must stay unchanged.
        for (int i = 0; i < 360; i++) {
            double angle = Math.toRadians(i), x = Math.cos(angle), y = Math.sin(angle);
            assert ThreatDirection.topic("react_zombies", x, y, x * 3, y * 3).equals("react_zombies_front");
            assert ThreatDirection.topic("react_zombies", x, y, -y * 2, x * 2).equals("react_zombies_right");
            assert ThreatDirection.topic("react_zombies", x, y, y * 2, -x * 2).equals("react_zombies_left");
            assert ThreatDirection.topic("react_zombies", x, y, -x * 3, -y * 3).equals("react_zombies_behind");
        }
        assert ThreatDirection.topic("react_zombies", 0, 0, 2, 3).equals("react_zombies");
        assert ThreatDirection.topic("react_zombies", 1, 0, 0, 0).equals("react_zombies");
        assert ThreatDirection.topic("react_zombies", Double.NaN, 0, 2, 3).equals("react_zombies");
        assert ThreatDirection.topic("react_zombies", 1, 0, Double.POSITIVE_INFINITY, 3).equals("react_zombies");
        assert ThreatDirection.topic("react_horde", 1, 0, 0, 2).equals("react_horde");
        var api = PersonalKnoxAPI.getInstance();
        for (String sector : new String[]{"front", "behind", "right", "left"})
            for (int roll = 0; roll < 5; roll++)
                assert api.chooseDialogue("react_zombies_" + sector, .5, 50, 50, roll) > 0;
        var director = new ReactionDirector();
        director.observe("speaker", 100_000, 100, 100, false, 0, true, false, false);
        director.observe("speaker", 180_000, 100, 100, false, 1, false, false, false);
        assert director.observe("speaker", 191_000, 100, 100, false, 0, false, false, false).isEmpty()
            : "A pending warning must be cancelled when all targets leave sight";
    }
}
