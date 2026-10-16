
package com.example.hotelroomsapi.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.example.hotelroomsapi.model.Room;

@Service
public class RoomService {

    private final Map<Long, Room> rooms = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    // Create a new room
    public Room createRoom(Room room) {
        Long id = idGenerator.getAndIncrement();
        room.setId(id);
        rooms.put(id, room);
        return room;
    }

    // Get all rooms
    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    // Get one room by ID
    public Optional<Room> getRoomById(Long id) {
        return Optional.ofNullable(rooms.get(id));
    }

    // Replace an existing room
    public Optional<Room> updateRoom(Long id, Room updatedRoom) {
        if (!rooms.containsKey(id)) {
            return Optional.empty();
        }

        updatedRoom.setId(id);
        rooms.put(id, updatedRoom);
        return Optional.of(updatedRoom);
    }

    // Delete a room
    public boolean deleteRoom(Long id) {
        return rooms.remove(id) != null;
    }
    
    // Partially update an existing room
    public Optional<Room> patchRoom(
            Long id, Map<String, Object> updates) {

        Room room = rooms.get(id);

        if (room == null) {
            return Optional.empty();
        }

        for (Map.Entry<String, Object> entry
                : updates.entrySet()) {

            String field = entry.getKey();
            Object value = entry.getValue();

            switch (field) {
                case "roomNumber" -> {
                    if (!(value instanceof String)) {
                        throw new IllegalArgumentException(
                                "roomNumber must be text");
                    }
                    room.setRoomNumber((String) value);
                }

                case "roomType" -> {
                    if (!(value instanceof String)) {
                        throw new IllegalArgumentException(
                                "roomType must be text");
                    }
                    room.setRoomType((String) value);
                }

                case "pricePerNight" -> {
                    if (!(value instanceof Number)) {
                        throw new IllegalArgumentException(
                                "pricePerNight must be a number");
                    }
                    room.setPricePerNight(
                            ((Number) value).doubleValue());
                }

                case "available" -> {
                    if (!(value instanceof Boolean)) {
                        throw new IllegalArgumentException(
                                "available must be true or false");
                    }
                    room.setAvailable((Boolean) value);
                }

                default -> throw new IllegalArgumentException(
                            "Unknown room field: " + field);
            }
        }

        return Optional.of(room);
    }
 
}