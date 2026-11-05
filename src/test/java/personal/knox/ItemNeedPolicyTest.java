package personal.knox;

public final class ItemNeedPolicyTest {
    private static ItemNeedPolicy.State s(double hunger, double thirst, double health, int slots, int ammo) {
        return new ItemNeedPolicy.State(hunger, thirst, health, slots, ammo);
    }

    public static void run() {
        var full = s(1, 1, 20, 0, 0);
        assert !ItemNeedPolicy.objects(ItemNeedPolicy.Kind.FOOD, full) : "Full slots do not object";
        assert !ItemNeedPolicy.objects(ItemNeedPolicy.Kind.MEDICAL, full) : "Full slots do not object to medicine";
        var hungry = s(0.7, 0.1, 100, 3, 50);
        assert ItemNeedPolicy.objects(ItemNeedPolicy.Kind.FOOD, hungry);
        assert !ItemNeedPolicy.objects(ItemNeedPolicy.Kind.WATER, hungry);
        assert ItemNeedPolicy.weight(ItemNeedPolicy.Kind.FOOD, hungry) > 0.5;
        var hurt = s(0.1, 0.1, 30, 2, 50);
        assert ItemNeedPolicy.objects(ItemNeedPolicy.Kind.MEDICAL, hurt);
        assert ItemNeedPolicy.weight(ItemNeedPolicy.Kind.MEDICAL, hurt) > 0.5;
        var lowAmmo = s(0, 0, 100, 2, 5);
        assert ItemNeedPolicy.objects(ItemNeedPolicy.Kind.AMMO, lowAmmo);
        assert !ItemNeedPolicy.objects(ItemNeedPolicy.Kind.OTHER, lowAmmo) : "Other items are not contested";
        assert ItemNeedPolicy.weight(ItemNeedPolicy.Kind.FOOD, s(0.1, 0, 100, 2, 50)) == 0;
        try { s(-0.1, 0, 100, 1, 1); throw new AssertionError("Bad hunger accepted"); }
        catch (IllegalArgumentException expected) { }
        try { ItemNeedPolicy.objects(null, hungry); throw new AssertionError("Null kind accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Item-need tests passed: full slots, food/water, medical, ammo, other and invalid state.");
    }
}
