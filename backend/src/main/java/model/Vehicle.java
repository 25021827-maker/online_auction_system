package model;

public class Vehicle extends Item {
    private double mileageKm;

    public Vehicle(String id, String name, String description, double startingPrice, double mileageKm) {
        super(id, name, description, startingPrice);
        this.mileageKm = mileageKm;
    }

    public double getMileageKm() { return mileageKm; }

    @Override
    public String getSpecificDetails() {
        return "Phương tiện đi lại - Số KM đã đi: " + mileageKm + " km";
    }
}