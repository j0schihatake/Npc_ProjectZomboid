package personal.knox.quest;

/** Exact item/count delivery for a quest; partial deliveries accumulate. */
public final class QuestDelivery {
    private final String itemId;
    private final int required;
    private int delivered;

    public QuestDelivery(String itemId, int required) {
        if (itemId == null || itemId.isBlank() || required <= 0)
            throw new IllegalArgumentException("Invalid delivery");
        this.itemId = itemId;
        this.required = required;
    }

    public String itemId() { return itemId; }
    public int required() { return required; }
    public int delivered() { return delivered; }
    public int remaining() { return required - delivered; }
    public boolean complete() { return delivered >= required; }

    /** Returns how many units were accepted; extra units are ignored. */
    public int deliver(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Invalid delivery amount");
        int accepted = Math.min(amount, remaining());
        delivered += accepted;
        return accepted;
    }
}
