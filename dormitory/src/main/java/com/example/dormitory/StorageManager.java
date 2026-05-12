package com.example.dormitory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StorageManager {
    private static final String DATA_DIR = "dormitory_data";
    private static final String TENANTS_FILE = DATA_DIR + File.separator + "tenants.json";
    private static final String ROOMS_FILE = DATA_DIR + File.separator + "rooms.json";

    private static StorageManager instance;
    private Gson gson;

    private StorageManager() {
        // Create GSON instance with LocalDate adapter
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.setPrettyPrinting();
        gson = gsonBuilder.create();

        // Create data directory if it doesn't exist
        createDataDirectory();
    }

    public static StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    private void createDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Save tenants to file
    public void saveTenants(List<Tenant> tenants) {
        try (Writer writer = new FileWriter(TENANTS_FILE)) {
            gson.toJson(tenants, writer);
            System.out.println("Tenants saved successfully: " + tenants.size() + " tenants");
        } catch (IOException e) {
            System.err.println("Error saving tenants: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Load tenants from file
    public List<Tenant> loadTenants() {
        File file = new File(TENANTS_FILE);
        if (!file.exists()) {
            System.out.println("No existing tenants file found. Starting with empty list.");
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(file)) {
            Type tenantListType = new TypeToken<ArrayList<Tenant>>(){}.getType();
            List<Tenant> tenants = gson.fromJson(reader, tenantListType);
            System.out.println("Tenants loaded successfully: " + tenants.size() + " tenants");
            return tenants;
        } catch (IOException e) {
            System.err.println("Error loading tenants: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Save rooms to file
    public void saveRooms(List<Room> rooms) {
        try (Writer writer = new FileWriter(ROOMS_FILE)) {
            gson.toJson(rooms, writer);
            System.out.println("Rooms saved successfully: " + rooms.size() + " rooms");
        } catch (IOException e) {
            System.err.println("Error saving rooms: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Load rooms from file
    public List<Room> loadRooms() {
        File file = new File(ROOMS_FILE);
        if (!file.exists()) {
            System.out.println("No existing rooms file found. Creating default rooms.");
            return createDefaultRooms();
        }

        try (Reader reader = new FileReader(file)) {
            Type roomListType = new TypeToken<ArrayList<Room>>(){}.getType();
            List<Room> rooms = gson.fromJson(reader, roomListType);
            System.out.println("Rooms loaded successfully: " + rooms.size() + " rooms");
            return rooms;
        } catch (IOException e) {
            System.err.println("Error loading rooms: " + e.getMessage());
            e.printStackTrace();
            return createDefaultRooms();
        }
    }

    private List<Room> createDefaultRooms() {
        List<Room> defaultRooms = new ArrayList<>();
        defaultRooms.add(new Room("R001", "101", "VACANT", 5000));
        defaultRooms.add(new Room("R002", "102", "VACANT", 4500));
        defaultRooms.add(new Room("R003", "103", "VACANT", 5500));
        defaultRooms.add(new Room("R004", "104", "MAINTENANCE", 5000));
        defaultRooms.add(new Room("R005", "105", "VACANT", 4800));
        defaultRooms.add(new Room("R006", "201", "VACANT", 6000));
        defaultRooms.add(new Room("R007", "202", "VACANT", 5200));
        defaultRooms.add(new Room("R008", "203", "VACANT", 5500));
        defaultRooms.add(new Room("R009", "204", "VACANT", 5000));
        defaultRooms.add(new Room("R010", "205", "VACANT", 5800));

        // Save default rooms
        saveRooms(defaultRooms);
        return defaultRooms;
    }

    // LocalDate adapter for GSON
    private static class LocalDateAdapter implements com.google.gson.JsonSerializer<LocalDate>, com.google.gson.JsonDeserializer<LocalDate> {
        @Override
        public com.google.gson.JsonElement serialize(LocalDate date, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        @Override
        public LocalDate deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context) throws com.google.gson.JsonParseException {
            return LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }
}