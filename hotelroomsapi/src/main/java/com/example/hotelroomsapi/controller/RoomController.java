package com.example.hotelroomsapi.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static final Set<String> VALID_TYPES =
            Set.of("Single", "Double", "Suite", "Deluxe", "Family");

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // ---------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------

    @PostMapping
    public ResponseEntity<?> create(@RequestBody(required = false) Map<String, Object> body) {

        if (body == null) {
            return badRequest("Invalid request body");
        }

        try {
            Room room = buildRoom(body);

            if (roomService.roomNumberExists(room.getRoomNumber(), null)) {
                return errorResponse(
                        HttpStatus.CONFLICT,
                        "roomNumber already exists");
            }

            Room created = roomService.createRoom(room);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(created);

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // GET ONE
    // ---------------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable String id) {

        Long numericId;

        try {
            numericId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            return notFound("Record not found");
        }

        if (numericId <= 0) {
            return notFound("Record not found");
        }

        return roomService.getRoomById(numericId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() ->
                        notFound("Record not found"));
    }

    // ---------------------------------------------------------
    // LIST
    // ---------------------------------------------------------

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Double minPricePerNight,
            @RequestParam(required = false) Double maxPricePerNight,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {

        int actualPage = page == null ? 1 : page;
        int actualLimit = limit == null ? 10 : limit;

        if (actualPage < 1 || actualLimit < 1 || actualLimit > 50) {
            return badRequest("Invalid page or limit");
        }

        if (type != null && !VALID_TYPES.contains(type)) {
            return badRequest("Invalid type");
        }

        if (available != null &&
                !(available.equals(Boolean.TRUE)
                        || available.equals(Boolean.FALSE))) {
            return badRequest("Invalid available");
        }

        if (minPricePerNight != null
                && (!Double.isFinite(minPricePerNight)
                || minPricePerNight < 0)) {
            return badRequest("Invalid minPricePerNight");
        }

        if (maxPricePerNight != null
                && (!Double.isFinite(maxPricePerNight)
                || maxPricePerNight < 0)) {
            return badRequest("Invalid maxPricePerNight");
        }

        // The question says min > max returns an empty result.
        if (minPricePerNight != null
                && maxPricePerNight != null
                && minPricePerNight > maxPricePerNight) {

            return emptyList(actualPage, actualLimit);
        }

        if (sort != null &&
                !Set.of("name", "pricePerNight", "capacity")
                        .contains(sort)) {
            return badRequest("Invalid sort field");
        }

        String actualOrder = order == null ? "asc" : order;

        if (!actualOrder.equalsIgnoreCase("asc")
                && !actualOrder.equalsIgnoreCase("desc")) {
            return badRequest("Invalid order");
        }

        List<Room> rooms = roomService.getAllRooms();

        // Search
        if (search != null && !search.isEmpty()) {
            String searchText = search.toLowerCase();

            rooms = rooms.stream()
                    .filter(room ->
                            room.getName().toLowerCase()
                                    .contains(searchText)
                            ||
                            room.getLocation().toLowerCase()
                                    .contains(searchText))
                    .collect(Collectors.toList());
        }

        // Type
        if (type != null) {
            rooms = rooms.stream()
                    .filter(room -> room.getType().equals(type))
                    .collect(Collectors.toList());
        }

        // Available
        if (available != null) {
            rooms = rooms.stream()
                    .filter(room -> room.isAvailable() == available)
                    .collect(Collectors.toList());
        }

        // Minimum price
        if (minPricePerNight != null) {
            rooms = rooms.stream()
                    .filter(room ->
                            room.getPricePerNight() >= minPricePerNight)
                    .collect(Collectors.toList());
        }

        // Maximum price
        if (maxPricePerNight != null) {
            rooms = rooms.stream()
                    .filter(room ->
                            room.getPricePerNight() <= maxPricePerNight)
                    .collect(Collectors.toList());
        }

        // Sort only when requested.
        if (sort != null) {

            Comparator<Room> comparator;

            switch (sort) {
                case "name":
                    comparator = Comparator.comparing(
                            Room::getName,
                            String.CASE_INSENSITIVE_ORDER);
                    break;

                case "pricePerNight":
                    comparator = Comparator.comparingDouble(
                            Room::getPricePerNight);
                    break;

                case "capacity":
                    comparator = Comparator.comparingInt(
                            Room::getCapacity);
                    break;

                default:
                    return badRequest("Invalid sort field");
            }

            if (actualOrder.equalsIgnoreCase("desc")) {
                comparator = comparator.reversed();
            }

            rooms.sort(comparator);
        }

        int total = rooms.size();

        int totalPages =
                total == 0 ? 0 : (int) Math.ceil((double) total / actualLimit);

        int start = (actualPage - 1) * actualLimit;

        List<Room> data;

        if (start >= total) {
            data = new ArrayList<>();
        } else {
            int end = Math.min(start + actualLimit, total);
            data = new ArrayList<>(rooms.subList(start, end));
        }

        Map<String, Object> response = new HashMap<>();

        response.put("data", data);
        response.put("page", actualPage);
        response.put("limit", actualLimit);
        response.put("total", total);
        response.put("totalPages", totalPages);

        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------
    // PUT
    // ---------------------------------------------------------

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body) {

        Long numericId;

        try {
            numericId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            return notFound("Record not found");
        }

        if (!roomService.getRoomById(numericId).isPresent()) {
            return notFound("Record not found");
        }

        if (body == null) {
            return badRequest("Invalid request body");
        }

        try {
            Room updated = buildRoom(body);

            if (roomService.roomNumberExists(
                    updated.getRoomNumber(), numericId)) {

                return errorResponse(
                        HttpStatus.CONFLICT,
                        "roomNumber already exists");
            }

            return roomService.updateRoom(numericId, updated)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() ->
                            notFound("Record not found"));

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // PATCH
    // ---------------------------------------------------------

    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> updates) {

        Long numericId;

        try {
            numericId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            return notFound("Record not found");
        }

        Room room = roomService.getRoomById(numericId).orElse(null);

        if (room == null) {
            return notFound("Record not found");
        }

        if (updates == null || updates.isEmpty()) {
            return badRequest("At least one field is required");
        }

        // id can never be changed.
        updates.remove("id");

        boolean recognised = false;

        try {

            for (Map.Entry<String, Object> entry : updates.entrySet()) {

                String field = entry.getKey();
                Object value = entry.getValue();

                switch (field) {

                    case "name":
                        recognised = true;
                        room.setName(validateName(value));
                        break;

                    case "location":
                        recognised = true;
                        room.setLocation(validateLocation(value));
                        break;

                    case "type":
                        recognised = true;
                        room.setType(validateType(value));
                        break;

                    case "pricePerNight":
                        recognised = true;
                        room.setPricePerNight(
                                validatePrice(value));
                        break;

                    case "capacity":
                        recognised = true;
                        room.setCapacity(
                                validateCapacity(value));
                        break;

                    case "available":
                        recognised = true;
                        room.setAvailable(
                                validateBoolean(value));
                        break;

                    case "roomNumber":
                        recognised = true;

                        String newRoomNumber =
                                validateRoomNumber(value);

                        if (roomService.roomNumberExists(
                                newRoomNumber, numericId)) {

                            return errorResponse(
                                    HttpStatus.CONFLICT,
                                    "roomNumber already exists");
                        }

                        room.setRoomNumber(newRoomNumber);
                        break;

                    default:
                        // Unknown fields are intentionally ignored.
                        break;
                }
            }

            if (!recognised) {
                return badRequest(
                        "At least one recognised field is required");
            }

            return ResponseEntity.ok(room);

        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {

        Long numericId;

        try {
            numericId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            return notFound("Record not found");
        }

        if (roomService.deleteRoom(numericId)) {
            return ResponseEntity.noContent().build();
        }

        return notFound("Record not found");
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private Room buildRoom(Map<String, Object> body) {

        // id is deliberately ignored.

        String name = requiredString(body, "name");
        String location = requiredString(body, "location");
        String type = requiredString(body, "type");

        double price = requiredNumber(body, "pricePerNight");
        int capacity = requiredInteger(body, "capacity");
        boolean available = requiredBoolean(body, "available");

        String roomNumber = requiredString(body, "roomNumber");

        Room room = new Room();

        room.setName(validateName(name));
        room.setLocation(validateLocation(location));
        room.setType(validateType(type));
        room.setPricePerNight(validatePrice(price));
        room.setCapacity(validateCapacity(capacity));
        room.setAvailable(available);
        room.setRoomNumber(validateRoomNumber(roomNumber));

        return room;
    }

    private String requiredString(
            Map<String, Object> body, String field) {

        if (!body.containsKey(field)
                || !(body.get(field) instanceof String)) {
            throw new IllegalArgumentException(
                    field + " must be a string");
        }

        return (String) body.get(field);
    }

    private double requiredNumber(
            Map<String, Object> body, String field) {

        if (!body.containsKey(field)
                || !(body.get(field) instanceof Number)) {
            throw new IllegalArgumentException(
                    field + " must be a number");
        }

        return ((Number) body.get(field)).doubleValue();
    }

    private int requiredInteger(
            Map<String, Object> body, String field) {

        if (!body.containsKey(field)
                || !(body.get(field) instanceof Number)) {
            throw new IllegalArgumentException(
                    field + " must be an integer");
        }

        Number number = (Number) body.get(field);

        double value = number.doubleValue();

        if (value != Math.floor(value)) {
            throw new IllegalArgumentException(
                    field + " must be an integer");
        }

        return number.intValue();
    }

    private boolean requiredBoolean(
            Map<String, Object> body, String field) {

        if (!body.containsKey(field)
                || !(body.get(field) instanceof Boolean)) {
            throw new IllegalArgumentException(
                    field + " must be a boolean");
        }

        return (Boolean) body.get(field);
    }

    private String validateName(Object value) {

        if (!(value instanceof String)) {
            throw new IllegalArgumentException(
                    "name must be a string");
        }

        String text = ((String) value).trim();

        if (text.length() < 3 || text.length() > 60) {
            throw new IllegalArgumentException(
                    "name must be between 3 and 60 characters");
        }

        return text;
    }

    private String validateLocation(Object value) {

        if (!(value instanceof String)) {
            throw new IllegalArgumentException(
                    "location must be a string");
        }

        String text = ((String) value).trim();

        if (text.length() < 2 || text.length() > 40) {
            throw new IllegalArgumentException(
                    "location must be between 2 and 40 characters");
        }

        return text;
    }

    private String validateType(Object value) {

        if (!(value instanceof String)) {
            throw new IllegalArgumentException(
                    "type must be a string");
        }

        String type = (String) value;

        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException(
                    "Invalid type");
        }

        return type;
    }

    private double validatePrice(Object value) {

        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(
                    "pricePerNight must be a number");
        }

        double price = ((Number) value).doubleValue();

        if (!Double.isFinite(price)
                || price <= 0
                || price > 100000) {
            throw new IllegalArgumentException(
                    "pricePerNight must be greater than 0");
        }

        double rounded = Math.round(price * 100.0) / 100.0;

        if (Math.abs(price - rounded) > 0.0000001) {
            throw new IllegalArgumentException(
                    "pricePerNight may have at most 2 decimal places");
        }

        return price;
    }

    private int validateCapacity(Object value) {

        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(
                    "capacity must be an integer");
        }

        Number number = (Number) value;

        double valueAsDouble = number.doubleValue();

        if (valueAsDouble != Math.floor(valueAsDouble)) {
            throw new IllegalArgumentException(
                    "capacity must be an integer");
        }

        int capacity = number.intValue();

        if (capacity < 1 || capacity > 10) {
            throw new IllegalArgumentException(
                    "capacity must be between 1 and 10");
        }

        return capacity;
    }

    private boolean validateBoolean(Object value) {

        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException(
                    "available must be a boolean");
        }

        return (Boolean) value;
    }

    private String validateRoomNumber(Object value) {

        if (!(value instanceof String)) {
            throw new IllegalArgumentException(
                    "roomNumber must be a string");
        }

        String text = ((String) value).trim();

        if (text.length() < 4 || text.length() > 20) {
            throw new IllegalArgumentException(
                    "roomNumber must be between 4 and 20 characters");
        }

        return text;
    }

    // =========================================================
    // RESPONSE HELPERS
    // =========================================================

    private ResponseEntity<Map<String, String>> badRequest(
            String message) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }

    private ResponseEntity<Map<String, String>> notFound(
            String message) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", message));
    }

    private ResponseEntity<Map<String, String>> errorResponse(
            HttpStatus status,
            String message) {

        return ResponseEntity
                .status(status)
                .body(Map.of("error", message));
    }

    private ResponseEntity<Map<String, Object>> emptyList(
            int page,
            int limit) {

        Map<String, Object> response = new HashMap<>();

        response.put("data", new ArrayList<>());
        response.put("page", page);
        response.put("limit", limit);
        response.put("total", 0);
        response.put("totalPages", 0);

        return ResponseEntity.ok(response);
    }
}