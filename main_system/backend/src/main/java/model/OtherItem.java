package model;

public class OtherItem extends Item {
    public OtherItem(Long id, String name, String description, double startingPrice) {
        super(id, name, description, startingPrice);
    }

    @Override
    public String getSpecificDetails() {
        return "Sản phẩm khác";
    }
}
