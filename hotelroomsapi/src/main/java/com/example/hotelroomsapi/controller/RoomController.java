
package com.example.hotelroomsapi.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelroomsapi.model.Room;
import com.example.hotelroomsapi.service.RoomService;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // Create a room with validation
    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody Room room) {

        String error = validateRoom(room);

        if (error != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", error));
        }

        Room createdRoom = roomService.createRoom(room);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createdRoom);
    }

    // Get all rooms with search, filtering, sorting and pagination
    @GetMapping
    public ResponseEntity<?> getAllRooms(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "roomNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "page must be >= 0 and size must be between 1 and 100"));
        }

        if ((minPrice != null && minPrice < 0)
                || (maxPrice != null && maxPrice < 0)
                || (minPrice != null && maxPrice != null
                && minPrice > maxPrice)) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Invalid price range"));
        }

        if (!List.of("roomNumber", "roomType", "pricePerNight")
                .contains(sortBy)) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Invalid sortBy field"));
        }

        if (!"asc".equalsIgnoreCase(direction)
                && !"desc".equalsIgnoreCase(direction)) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "direction must be asc or desc"));
        }

        String searchText = search == null ? "" : search.trim();

        List<Room> rooms = roomService.getAllRooms().stream()
                // Search by room number or room type
                .filter(room -> searchText.isEmpty()
                        || (room.getRoomNumber() != null
                        && room.getRoomNumber().toLowerCase()
                                .contains(searchText.toLowerCase()))
                        || (room.getRoomType() != null
                        && room.getRoomType().toLowerCase()
                                .contains(searchText.toLowerCase())))

                // Filter by room type
                .filter(room -> roomType == null
                        || (room.getRoomType() != null
                        && room.getRoomType().equalsIgnoreCase(roomType.trim())))

                // Filter by availability
                .filter(room -> available == null
                        || room.isAvailable() == available)

                // Filter by minimum price
                .filter(room -> minPrice == null
                        || room.getPricePerNight() >= minPrice)

                // Filter by maximum price
                .filter(room -> maxPrice == null
                        || room.getPricePerNight() <= maxPrice)

                .collect(Collectors.toList());

        // Sort the results
        Comparator<Room> comparator;

        comparator = switch (sortBy) {
            case "roomType" -> Comparator.comparing(
                    Room::getRoomType,
                    Comparator.nullsLast(
                            String.CASE_INSENSITIVE_ORDER));
            case "pricePerNight" -> Comparator.comparingDouble(
                    Room::getPricePerNight);
            default -> Comparator.comparing(
                    Room::getRoomNumber,
                    Comparator.nullsLast(
                            String.CASE_INSENSITIVE_ORDER));
        };

        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }

        rooms.sort(comparator);

        // Calculate pagination
        int totalElements = rooms.size();

        int totalPages = (int) Math.ceil(
                (double) totalElements / size);

        int start = (int) Math.min(
                (long) page * size, totalElements);

        int end = Math.min(start + size, totalElements);

        List<Room> pageContent = rooms.subList(start, end);

        return ResponseEntity.ok(Map.of(
                "content", pageContent,
                "page", page,
                "size", size,
                "totalElements", totalElements,
                "totalPages", totalPages
        ));
    }

    // Get a room by ID
    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        return roomService.getRoomById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Replace a room with validation
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoom(
            @PathVariable Long id,
            @RequestBody Room room) {

        String error = validateRoom(room);

        if (error != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", error));
        }

        return roomService.updateRoom(id, room)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Partially update a room
    @PatchMapping("/{id}")
    public ResponseEntity<?> patchRoom(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        if (updates == null || updates.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "At least one field is required"));
        }

        try {
            return roomService.patchRoom(id, updates)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    // Delete a room
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {

        if (roomService.deleteRoom(id)) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();
    }

    // Validate room details before saving
    private String validateRoom(Room room) {

        if (room == null) {
            return "Room details are required";
        }

        if (room.getRoomNumber() == null
                || room.getRoomNumber().isBlank()) {
            return "Room number is required";
        }

        if (room.getRoomType() == null
                || room.getRoomType().isBlank()) {
            return "Room type is required";
        }

        if (!Double.isFinite(room.getPricePerNight())
                || room.getPricePerNight() <= 0) {
            return "Price per night must be a positive number";
        }

        return null;
    }
}