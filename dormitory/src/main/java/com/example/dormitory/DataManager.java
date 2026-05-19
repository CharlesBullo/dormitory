package com.example.dormitory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDate;

public class DataManager {
    private static DataManager instance;

    private ObservableList<Tenant> tenantList;
    private ObservableList<Room> roomList;
    private boolean databaseAvailable;

    private DataManager() {
        tenantList = FXCollections.observableArrayList();
        roomList = FXCollections.observableArrayList();

        databaseAvailable = DatabaseConfig.testConnection();

        if (databaseAvailable) {
            loadAllRooms();
            loadAllTenants();
        } else {
            System.err.println("Database not available. Using empty lists.");
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

                String tenantName = rs.getString("tenant_name");
                if (tenantName != null && !tenantName.isEmpty()) {
                    room.setTenantName(tenantName);
                }
                String tenantId = rs.getString("tenant_id");
                if (tenantId != null && !tenantId.isEmpty()) {
                    room.setTenantId(tenantId);
                }
                roomList.add(room);
            }
            System.out.println("✓ Loaded " + roomList.size() + " rooms");
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

        String sql = "UPDATE rooms SET room_number = ?, status = ?, monthly_rent = ?, rent_due = ?, tenant_name = ?, tenant_id = ? WHERE room_id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, room.getRoomNumber());
            pstmt.setString(2, room.getStatus());
            pstmt.setDouble(3, room.getMonthlyRent());
            pstmt.setDouble(4, room.getRentDue());
            pstmt.setString(5, room.getTenantName());
            pstmt.setString(6, room.getTenantId());
            pstmt.setString(7, room.getRoomId());
            pstmt.executeUpdate();

            // Update local list
            int index = roomList.indexOf(room);
            if (index >= 0) {
                roomList.set(index, room);
            }
            System.out.println("✓ Room updated: " + room.getRoomNumber() + " - Tenant: " + room.getTenantName());
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
        String sql = "SELECT * FROM tenants ORDER BY tenant_name";

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

                loadTenantUserAccount(tenant);
                tenantList.add(tenant);
            }
            System.out.println("✓ Loaded " + tenantList.size() + " tenants from database");
        } catch (SQLException e) {
            System.err.println("Error loading tenants: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadTenantUserAccount(Tenant tenant) {
        String sql = "SELECT username FROM users WHERE tenant_id = ? AND role = 'TENANT'";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenant.getTenantId());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                tenant.setUsername(rs.getString("username"));
            }

        } catch (SQLException e) {
            System.err.println("Error loading tenant user account: " + e.getMessage());
        }
    }

    public void addTenant(Tenant tenant) {
        if (!databaseAvailable) return;

        String sql = "INSERT INTO tenants (tenant_id, tenant_name, contact_number, email, address, " +
                "room_id, room_number, monthly_rent, move_in_date, due_date, is_paid, total_debt, missed_months, is_evicted, paid_streak) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
            pstmt.setBoolean(11, tenant.getIsPaid());
            pstmt.setDouble(12, tenant.getTotalDebt());
            pstmt.setInt(13, tenant.getMissedMonths());
            pstmt.setBoolean(14, tenant.getIsEvicted());
            pstmt.setInt(15, tenant.getPaidStreak());
            pstmt.executeUpdate();

            // Update room status and tenant name in rooms table
            Room room = findRoomById(tenant.getRoomId());
            if (room != null) {
                room.setStatus("OCCUPIED");
                room.setTenantName(tenant.getTenantName());
                room.setTenantId(tenant.getTenantId());
                updateRoom(room); // This will update the rooms table in database
            }

            // Create user account for the tenant
            createUserAccountForTenant(tenant);

            // Reload both lists to ensure consistency
            loadAllRooms();
            loadAllTenants();

            System.out.println("✓ Tenant added to database: " + tenant.getTenantName());
            System.out.println("✓ Room " + tenant.getRoomNumber() + " is now occupied by " + tenant.getTenantName());
        } catch (SQLException e) {
            System.err.println("Error adding tenant: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void createUserAccountForTenant(Tenant tenant) {
        if (!databaseAvailable) return;

        String hashedPassword = BCrypt.hashpw(tenant.getTemporaryPassword(), BCrypt.gensalt(10));

        String sql = "INSERT INTO users (username, password, full_name, email, role, tenant_id, is_active) " +
                "VALUES (?, ?, ?, ?, 'TENANT', ?, TRUE)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tenant.getUsername());
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, tenant.getTenantName());
            pstmt.setString(4, tenant.getEmail());
            pstmt.setString(5, tenant.getTenantId());
            pstmt.executeUpdate();

            System.out.println("✓ User account created for tenant: " + tenant.getUsername());

        } catch (SQLException e) {
            System.err.println("Error creating user account: " + e.getMessage());
        }
    }

    public void updateTenant(Tenant tenant, int index) {
        if (!databaseAvailable) return;

        String sql = "UPDATE tenants SET tenant_name = ?, contact_number = ?, email = ?, address = ?, " +
                "is_paid = ?, total_debt = ?, missed_months = ?, is_evicted = ?, paid_streak = ?, due_date = ? " +
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
            pstmt.setDate(10, Date.valueOf(tenant.getDueDate()));
            pstmt.setString(11, tenant.getTenantId());
            pstmt.executeUpdate();

            tenantList.set(index, tenant);
            System.out.println("✓ Tenant updated in database: " + tenant.getTenantName());
        } catch (SQLException e) {
            System.err.println("Error updating tenant: " + e.getMessage());
        }
    }

    public void removeTenant(Tenant tenant) {
        if (tenant == null) {
            System.err.println("Cannot remove null tenant");
            return;
        }

        System.out.println("Attempting to remove tenant: " + tenant.getTenantName() + " (ID: " + tenant.getTenantId() + ")");

        if (!databaseAvailable) {
            tenantList.remove(tenant);
            System.out.println("✓ Tenant removed from local list (database not available)");
            return;
        }

        try {
            Connection conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            try {
                // 1. Delete payments
                String deletePaymentsSql = "DELETE FROM payments WHERE tenant_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deletePaymentsSql)) {
                    pstmt.setString(1, tenant.getTenantId());
                    pstmt.executeUpdate();
                }

                // 2. Delete user account
                String deleteUserSql = "DELETE FROM users WHERE tenant_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteUserSql)) {
                    pstmt.setString(1, tenant.getTenantId());
                    pstmt.executeUpdate();
                }

                // 3. Update room to VACANT and clear tenant info
                String updateRoomSql = "UPDATE rooms SET status = 'VACANT', rent_due = 0, tenant_name = NULL, tenant_id = NULL WHERE room_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateRoomSql)) {
                    pstmt.setString(1, tenant.getRoomId());
                    pstmt.executeUpdate();

                    // Update local room object
                    Room room = findRoomById(tenant.getRoomId());
                    if (room != null) {
                        room.setStatus("VACANT");
                        room.setTenantName("");
                        room.setTenantId("");
                        room.setRentDue(0);
                    }
                }

                // 4. Delete tenant
                String deleteTenantSql = "DELETE FROM tenants WHERE tenant_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteTenantSql)) {
                    pstmt.setString(1, tenant.getTenantId());
                    pstmt.executeUpdate();
                }

                conn.commit();
                tenantList.remove(tenant);
                loadAllRooms();
                loadAllTenants();

                System.out.println("✓ Tenant completely removed from database: " + tenant.getTenantName());

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error during tenant deletion, rolling back: " + e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(true);
                conn.close();
            }

        } catch (SQLException e) {
            System.err.println("Error removing tenant from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveData() {
        System.out.println("Data already persisted to database");
    }

    public boolean isDatabaseAvailable() {
        return databaseAvailable;
    }

    public void refreshData() {
        if (databaseAvailable) {
            loadAllRooms();
            loadAllTenants();
            System.out.println("✓ Data refreshed from database");
        }
    }
}