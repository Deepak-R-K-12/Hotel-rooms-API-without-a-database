package com.example.hotelroomsapi.model;

public class Room {

    private Long id;
    private String name;
    private String location;
    private String type;
    private double pricePerNight;
    private int capacity;
    private boolean available;
    private String roomNumber;

    public Room() {
    }

    public Room(Long id, String name, String location, String type,
                double pricePerNight, int capacity,
                boolean available, String roomNumber) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.type = type;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.available = available;
        this.roomNumber = roomNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }
}