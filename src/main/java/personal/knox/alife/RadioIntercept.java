package personal.knox.alife;

/** Whether a base operator catches nearby faction traffic. */
public final class RadioIntercept {
    private RadioIntercept() {}

    public static boolean intercepts(RadioOperator operator, double distanceMeters, int roll) {
        if (operator == null) throw new IllegalArgumentException("Missing operator");
        if (roll < 0 || roll > 99) throw new IllegalArgumentException("Invalid roll");
        return operator.canContact(distanceMeters) && roll < operator.interceptChance() * 100;
    }
}
