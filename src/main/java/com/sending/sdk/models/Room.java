package com.sending.sdk.models;

public class Room {
    private final String id;

    public Room(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Room{" +
                "id='" + id + '\'' +
                '}';
    }
}
