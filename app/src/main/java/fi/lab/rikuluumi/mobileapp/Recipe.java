package fi.lab.rikuluumi.mobileapp;

public class Recipe {
    private String title;
    private String info;
    private int imageResId;

    public Recipe(String title, String info, int imageResId) {
        this.title = title;
        this.info = info;
        this.imageResId = imageResId;
    }

    public String getTitle() { return title; }
    public String getInfo() { return info; }
    public int getImageResId() { return imageResId; }
}
