package fi.lab.rikuluumi.mobileapp;

public class Recipe {
    private int id;
    private String title;
    private String info;
    private String imageUrl;

    public Recipe(String title, String info, String imageUrl, int id) {
        this.id = id;
        this.title = title;
        this.info = info;
        this.imageUrl = imageUrl;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getInfo() { return info; }
    public String getImageUrl() { return imageUrl; }
}
