package model;

public abstract class Item {
    protected Long id;
    protected String name;
    protected String description;
    protected double startingPrice;
    protected String condition; // BỔ SUNG
    protected String imagePath; // BỔ SUNG

    public Item(Long id, String name, String description, double startingPrice) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public abstract String getSpecificDetails();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}