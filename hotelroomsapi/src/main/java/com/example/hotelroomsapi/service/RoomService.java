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

    public Room createRoom(Room room) {
        Long id = idGenerator.getAndIncrement();
        room.setId(id);
        rooms.put(id, room);
        return room;
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Optional<Room> getRoomById(Long id) {
        return Optional.ofNullable(rooms.get(id));
    }

    public Optional<Room> updateRoom(Long id, Room room) {
        if (!rooms.containsKey(id)) {
            return Optional.empty();
        }

        room.setId(id);
        rooms.put(id, room);

        return Optional.of(room);
    }

    public boolean deleteRoom(Long id) {
        return rooms.remove(id) != null;
    }

    public boolean roomNumberExists(String roomNumber, Long excludeId) {
        for (Room room : rooms.values()) {
            if (excludeId != null && room.getId().equals(excludeId)) {
                continue;
            }

            if (room.getRoomNumber() != null
                    && room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return true;
            }
        }

        return false;
    }
}