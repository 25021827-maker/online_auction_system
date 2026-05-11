package model;

public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(String id, String name, String description,
                       double startingPrice, int warrantyMonths) {
        super(id, name, description, startingPrice);
        this.warrantyMonths = warrantyMonths;
    }

    public int getWarrantyMonths() { return warrantyMonths; }

    @Override
    public String getSpecificDetails() {
        return "Đồ điện tử - Bảo hành: " + warrantyMonths + " tháng";
    }
}