package model;

public class Art extends Item {
    private String artistName;

    public Art(String id, String name, String description, double startingPrice, String artistName) {
        super(id, name, description, startingPrice);
        this.artistName = artistName;
    }

    public String getArtistName() { return artistName; }

    @Override
    public String getSpecificDetails() {
        return "Tác phẩm nghệ thuật - Tác giả: " + artistName;
    }
}