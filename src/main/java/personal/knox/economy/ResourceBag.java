package personal.knox.economy;

import personal.knox.alife.FactionEconomy;

/** Long round resource bag: one of three kinds, finite units. */
public final class ResourceBag {
    public enum Kind { MEDICAL, FOOD, AMMO }
    public static final int DEFAULT_CAPACITY = 6;

    private final Kind kind;
    private final int capacity;
    private int units;

    public ResourceBag(Kind kind, int units) { this(kind, units, DEFAULT_CAPACITY); }

    public ResourceBag(Kind kind, int units, int capacity) {
        if (kind == null) throw new IllegalArgumentException("Missing bag kind");
        if (capacity <= 0 || units < 0 || units > capacity)
            throw new IllegalArgumentException("Invalid bag units");
        this.kind = kind;
        this.capacity = capacity;
        this.units = units;
    }

    public Kind kind() { return kind; }
    public int capacity() { return capacity; }
    public int units() { return units; }
    public boolean empty() { return units == 0; }

    public int take(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Invalid amount");
        int taken = Math.min(units, amount);
        units -= taken;
        return taken;
    }

    /** Deposits all units into the matching faction resource; returns amount. */
    public int depositInto(FactionEconomy economy) {
        if (economy == null) throw new IllegalArgumentException("Missing economy");
        int amount = units;
        economy.add(map(kind), amount);
        units = 0;
        return amount;
    }

    static FactionEconomy.Resource map(Kind kind) {
        return switch (kind) {
            case MEDICAL -> FactionEconomy.Resource.MEDS;
            case FOOD -> FactionEconomy.Resource.FOOD;
            case AMMO -> FactionEconomy.Resource.AMMO;
        };
    }
}
