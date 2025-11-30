package com.example;

public class Chunk {
    private final String id;
    private final String subject;
    private final String text;

    public Chunk(String id, String subject, String text) {
        this.id = id;
        this.subject = subject;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getText() {
        return text;
    }
}
