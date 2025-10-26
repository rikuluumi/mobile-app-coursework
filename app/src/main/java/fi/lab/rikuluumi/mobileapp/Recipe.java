package fi.lab.rikuluumi.mobileapp;

public class Recipe {
    private int id;
    private String title;
    private String info;
    private String imageUrl;
    private boolean isFavorite;

    public Recipe(int id, String title, String info, String imageUrl, boolean isFavorite) {
        this.id = id;
        this.title = title;
        this.info = info;
        this.imageUrl = imageUrl;
        this.isFavorite = isFavorite;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getInfo() { return info; }
    public String getImageUrl() { return imageUrl; }
    public boolean getIsFavorite() { return isFavorite; }
}
