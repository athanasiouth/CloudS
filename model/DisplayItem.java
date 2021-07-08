package com.athanasioua.battleship.model.newp.model;

import androidx.annotation.NonNull;

public class DisplayItem implements Comparable {
    private String text;
    private String type;
    private String id;
    private long timestamp;
    private Repository parentRepository;

    public DisplayItem(String text, String id, String type, long timestamp,Repository parentRepository) {
        this.text = text;
        this.type = type;
        this.id = id;
        this.timestamp = timestamp;
        this.parentRepository = parentRepository;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Repository getParentRepository() {
        return parentRepository;
    }

    public void setParentRepository(Repository parentRepository) {
        this.parentRepository = parentRepository;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        return 0;
    }
}
