package personal.knox.alife;

public final class VehicleTripTest {
    public static void run() {
        var trip = new VehicleTrip("car", "driver", 4, 2.0);
        trip.drive(5);
        assert trip.state() == VehicleTrip.State.DRIVING && trip.fuel() < 2.0;
        trip.block();
        assert trip.state() == VehicleTrip.State.BLOCKED;
        trip.unblock();
        assert trip.state() == VehicleTrip.State.DRIVING;
        trip.drive(100);
        assert trip.state() == VehicleTrip.State.BROKEN && trip.fuel() == 0;
        var arrived = new VehicleTrip("car", "driver", 2, 5);
        arrived.arrive();
        assert arrived.state() == VehicleTrip.State.ARRIVED;
        try { new VehicleTrip("", "d", 1, 1); throw new AssertionError("Empty vehicle accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new VehicleTrip("v", "d", 0, 1); throw new AssertionError("Zero seats accepted"); }
        catch (IllegalArgumentException expected) { }
        try { trip.drive(1); throw new AssertionError("Broken car drove"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Vehicle-trip tests passed: fuel, distance, block/unblock, breakdown, arrival and invalid data.");
    }
}
