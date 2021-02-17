package com.timothydillan.circles.Models;


public class Post {

    private final String id;

    private final String title;

    private final String description;

    private final String sentiment;

    public Post(final String id, final String title, String description, final String sentiment) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.sentiment = sentiment;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public String getSentiment() {
        return sentiment;
    }

}

