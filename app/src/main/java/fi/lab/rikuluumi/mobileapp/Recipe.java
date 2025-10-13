package fi.lab.rikuluumi.mobileapp;

public class Recipe {
    private String title;
    private String info;
    private String imageUrl;

    public Recipe(String title, String info, String imageUrl) {
        this.title = title;
        this.info = info;
        this.imageUrl = imageUrl;
    }

    public String getTitle() { return title; }
    public String getInfo() { return info; }
    public String getImageUrl() { return imageUrl; }
}
