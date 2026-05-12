package com.example.dormitory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static DataManager instance;

    private ObservableList<Tenant> tenantList;
    private ObservableList<Room> roomList;
    private boolean databaseAvailable;

    private DataManager() {
        tenantList = FXCollections.observableArrayList();
        roomList = FXCollections.observableArrayList();

        // Check if database is available
        databaseAvailable = DatabaseConfig.testConnection();

        if (databaseAvailable) {
            // Load data from database
            loadAllRooms();
            loadAllTenants();
        } else {
            System.err.println("Database not available. Using empty lists.");
            // Create default rooms if no database (fallback)
            createDefaultRooms();
        }
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    private void createDefaultRooms() {
        // Fallback if database is not available
        roomList.addAll(
                new Room("R001", "101", "VACANT", 5000),
                new Room("R002", "102", "VACANT", 4500),
                new Room("R003", "103", "VACANT", 5500),
                new Room("R004", "104", "MAINTENANCE", 5000),
                new Room("R005", "105", "VACANT", 4800)
        );
    }

    // ============ ROOM METHODS ============

    public ObservableList<Room> getRoomList() {
        return roomList;
    }

    public void loadAllRooms() {
        if (!databaseAvailable) return;

        roomList.clear();
        String sql = "SELECT * FROM rooms ORDER BY room_number";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Room room = new Room(
                        rs.getString("room_id"),
                        rs.getString("room_number"),
                        rs.getString("status"),
                        rs.getDouble("monthly_rent")
                );
                room.setRentDue(rs.getDouble("rent_due"));
                roomList.add(room);
            }
            System.out.println("✓ Loaded " + roomList.size() + " rooms from database");
        } catch (SQLException e) {
            System.err.println("Error loading rooms: " + e.getMessage());
        }
    }

    public void addRoom(Room room) {
        if (!databaseAvailable) {
            roomList.add(room);
            return;
        }

        String sql = "INSERT INTO rooms (room_id, room_number, status, monthly_rent) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, room.getRoomId());
            pstmt.setString(2, room.getRoomNumber());
            pstmt.setString(3, room.getStatus());
            pstmt.setDouble(4, room.getMonthlyRent());
            pstmt.executeUpdate();

            roomList.add(room);
            System.out.println("✓ Room added to database: " + room.getRoomNumber());
        } catch (SQLException e) {
            System.err.println("Error adding room: " + e.getMessage());
        }
    }

    public void updateRoom(Room room) {
        if (!databaseAvailable) return;

        String sql = "UPDATE rooms SET room_number = ?, status = ?, monthly_rent = ?, rent_due = ? WHERE room_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, room.getRoomNumber());
            pstmt.setString(2, room.getStatus());
            pstmt.setDouble(3, room.getMonthlyRent());
            pstmt.setDouble(4, room.getRentDue());
            pstmt.setString(5, room.getRoomId());
            pstmt.executeUpdate();

            int index = roomList.indexOf(room);
            if (index >= 0) {
                roomList.set(index, room);
            }
            System.out.println("✓ Room updated in database: " + room.getRoomNumber());
        } catch (SQLException e) {
            System.err.println("Error updating room: " + e.getMessage());
        }
    }

    public void removeRoom(Room room) {
        if (!databaseAvailable) {
            roomList.remove(room);
            return;
        }

        String sql = "DELETE FROM rooms WHERE room_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, room.getRoomId());
            pstmt.executeUpdate();

            roomList.remove(room);
            System.out.println("✓ Room deleted from database: " + room.getRoomNumber());
        } catch (SQLException e) {
            System.err.println("Error deleting room: " + e.getMessage());
        }
    }

    public Room findRoomByNumber(String roomNumber) {
        for (Room room : roomList) {
            if (room.getRoomNumber().equals(roomNumber)) {
                return room;
            }
        }
        return null;
    }

    public Room findRoomById(String roomId) {
        for (Room room : roomList) {
            if (room.getRoomId().equals(roomId)) {
                return room;
            }
        }
        return null;
    }

    public ObservableList<Room> getVacantRooms() {
        ObservableList<Room> vacantRooms = FXCollections.observableArrayList();
        for (Room room : roomList) {
            if (room.getStatus().equals("VACANT")) {
                vacantRooms.add(room);
            }
        }
        return vacantRooms;
    }

    // ============ TENANT METHODS ============

    public ObservableList<Tenant> getTenantList() {
        return tenantList;
    }

    public void loadAllTenants() {
        if (!databaseAvailable) return;

        tenantList.clear();
        String sql = "SELECT * FROM tenants WHERE is_evicted = FALSE ORDER BY tenant_name";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Tenant tenant = new Tenant(
                        rs.getString("tenant_id"),
                        rs.getString("tenant_name"),
                        rs.getString("contact_number"),
                        rs.getString("email"),
                        rs.getString("room_number"),
                        rs.getString("room_id"),
                        rs.getDouble("monthly_rent"),
                        rs.getDate("move_in_date").toLocalDate()
                );

                tenant.setAddress(rs.getString("address"));
                tenant.setPaid(rs.getBoolean("is_paid"));
                tenant.setTotalDebt(rs.getDouble("total_debt"));
                tenant.setMissedMonths(rs.getInt("missed_months"));
                tenant.setEvicted(rs.getBoolean("is_evicted"));
                tenant.setPaidStreak(rs.getInt("paid_streak"));

                tenantList.add(tenant);
            }
            System.out.println("✓ Loaded " + tenantList.size() + " tenants from database");
        } catch (SQLException e) {
            System.err.println("Error loading tenants: " + e.getMessage());
        }
    }

    public void addTenant(Tenant tenant) {
        if (!databaseAvailable) return;

        String sql = "INSERT INTO tenants (tenant_id, tenant_name, contact_number, email, address, " +
                "room_id, room_number, monthly_rent, move_in_date, due_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenant.getTenantId());
            pstmt.setString(2, tenant.getTenantName());
            pstmt.setString(3, tenant.getContactNumber());
            pstmt.setString(4, tenant.getEmail());
            pstmt.setString(5, tenant.getAddress());
            pstmt.setString(6, tenant.getRoomId());
            pstmt.setString(7, tenant.getRoomNumber());
            pstmt.setDouble(8, tenant.getMonthlyRent());
            pstmt.setDate(9, Date.valueOf(tenant.getMoveInDate()));
            pstmt.setDate(10, Date.valueOf(tenant.getDueDate()));
            pstmt.executeUpdate();

            tenantList.add(tenant);

            // Update room status
            Room room = findRoomById(tenant.getRoomId());
            if (room != null) {
                room.setStatus("OCCUPIED");
                room.setTenantName(tenant.getTenantName());
                room.setTenantId(tenant.getTenantId());
                updateRoom(room);
            }

            System.out.println("✓ Tenant added to database: " + tenant.getTenantName());
        } catch (SQLException e) {
            System.err.println("Error adding tenant: " + e.getMessage());
        }
    }

    public void updateTenant(Tenant tenant, int index) {
        if (!databaseAvailable) return;

        String sql = "UPDATE tenants SET tenant_name = ?, contact_number = ?, email = ?, address = ?, " +
                "is_paid = ?, total_debt = ?, missed_months = ?, is_evicted = ?, paid_streak = ? " +
                "WHERE tenant_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenant.getTenantName());
            pstmt.setString(2, tenant.getContactNumber());
            pstmt.setString(3, tenant.getEmail());
            pstmt.setString(4, tenant.getAddress());
            pstmt.setBoolean(5, tenant.getIsPaid());
            pstmt.setDouble(6, tenant.getTotalDebt());
            pstmt.setInt(7, tenant.getMissedMonths());
            pstmt.setBoolean(8, tenant.getIsEvicted());
            pstmt.setInt(9, tenant.getPaidStreak());
            pstmt.setString(10, tenant.getTenantId());
            pstmt.executeUpdate();

            tenantList.set(index, tenant);
            System.out.println("✓ Tenant updated in database: " + tenant.getTenantName());
        } catch (SQLException e) {
            System.err.println("Error updating tenant: " + e.getMessage());
        }
    }

    public void removeTenant(Tenant tenant) {
        if (!databaseAvailable) {
            tenantList.remove(tenant);
            return;
        }

        // First, vacate the room
        Room room = findRoomById(tenant.getRoomId());
        if (room != null) {
            room.setStatus("VACANT");
            room.setTenantName("");
            room.setTenantId("");
            updateRoom(room);
        }

        // Then delete the tenant
        String sql = "DELETE FROM tenants WHERE tenant_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenant.getTenantId());
            pstmt.executeUpdate();

            tenantList.remove(tenant);
            System.out.println("✓ Tenant deleted from database: " + tenant.getTenantName());
        } catch (SQLException e) {
            System.err.println("Error deleting tenant: " + e.getMessage());
        }
    }

    public void saveData() {
        // Data is automatically saved to database in each operation
        System.out.println("Data already persisted to database");
    }

    public boolean isDatabaseAvailable() {
        return databaseAvailable;
    }
}