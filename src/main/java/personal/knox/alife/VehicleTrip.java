package personal.knox.alife;

/** A group vehicle trip: fuel, distance, seats and blocking/broken states. */
public final class VehicleTrip {
    public enum State { DRIVING, BLOCKED, BROKEN, ARRIVED }

    private final String vehicleId;
    private final String driverId;
    private final int seats;
    private double fuel;
    private double distanceKm;
    private State state = State.DRIVING;

    public VehicleTrip(String vehicleId, String driverId, int seats, double fuel) {
        if (vehicleId == null || vehicleId.isBlank() || driverId == null || driverId.isBlank())
            throw new IllegalArgumentException("Missing vehicle or driver");
        if (seats < 1 || seats > 6) throw new IllegalArgumentException("Invalid seats");
        if (!Double.isFinite(fuel) || fuel < 0) throw new IllegalArgumentException("Invalid fuel");
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.seats = seats;
        this.fuel = fuel;
    }

    public String vehicleId() { return vehicleId; }
    public String driverId() { return driverId; }
    public int seats() { return seats; }
    public double fuel() { return fuel; }
    public double distanceKm() { return distanceKm; }
    public State state() { return state; }

    public void drive(double km) {
        if (state != State.DRIVING || !Double.isFinite(km) || km <= 0)
            throw new IllegalArgumentException("Invalid drive");
        double needed = km * 0.1;
        distanceKm += km;
        fuel = Math.max(0, fuel - needed);
        if (fuel == 0) state = State.BROKEN;
    }

    public void block() {
        if (state == State.DRIVING) state = State.BLOCKED;
    }

    public void unblock() {
        if (state == State.BLOCKED) state = State.DRIVING;
    }

    public void arrive() {
        if (state == State.DRIVING || state == State.BLOCKED) state = State.ARRIVED;
    }
}
