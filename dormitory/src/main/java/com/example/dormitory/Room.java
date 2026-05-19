package com.example.dormitory;

public class Room {
    private String roomId;
    private String roomNumber;
    private String status; // "VACANT", "OCCUPIED", "MAINTENANCE"
    private String tenantName;
    private String tenantId;
    private double monthlyRent;
    private double rentDue;
    private boolean isRentDue;

    public Room(String roomId, String roomNumber, String status, double monthlyRent) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.status = status;
        this.monthlyRent = monthlyRent;
        this.tenantName = "";
        this.tenantId = "";
        this.rentDue = 0;
        this.isRentDue = false;
    }

    // Getters
    public String getRoomId() { return roomId; }
    public String getRoomNumber() { return roomNumber; }
    public String getStatus() { return status; }
    public String getTenantName() { return tenantName; }
    public String getTenantId() { return tenantId; }
    public double getMonthlyRent() { return monthlyRent; }
    public double getRentDue() { return rentDue; }
    public boolean getIsRentDue() { return isRentDue; }

    // Setters
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setStatus(String status) { this.status = status; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public void setMonthlyRent(double monthlyRent) { this.monthlyRent = monthlyRent; }
    public void setRentDue(double rentDue) {
        this.rentDue = rentDue;
        this.isRentDue = rentDue > 0;
    }
    public void setIsRentDue(boolean isRentDue) { this.isRentDue = isRentDue; }
}