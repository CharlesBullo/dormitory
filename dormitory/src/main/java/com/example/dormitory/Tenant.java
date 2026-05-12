package com.example.dormitory;

import java.time.LocalDate;

public class Tenant {

    private String tenantId;
    private String tenantName;
    private String contactNumber;
    private String email;
    private String address;
    private String roomNumber;
    private String roomId;

    private boolean isPaid;
    private double monthlyRent;
    private double totalDebt;
    private int missedMonths;
    private boolean isEvicted;
    private int paidStreak;

    private LocalDate moveInDate;
    private LocalDate dueDate;

    public Tenant(String tenantId, String tenantName, String contactNumber,
                  String email, String roomNumber, String roomId,
                  double monthlyRent, LocalDate moveInDate) {
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.contactNumber = contactNumber;
        this.email = email;
        this.roomNumber = roomNumber;
        this.roomId = roomId;
        this.monthlyRent = monthlyRent;
        this.moveInDate = moveInDate;
        this.dueDate = moveInDate.plusMonths(1);
        this.isPaid = false;
        this.totalDebt = 0;
        this.missedMonths = 0;
        this.isEvicted = false;
        this.paidStreak = 0;
        this.address = "";
    }

    // Getters
    public String getTenantId() { return tenantId; }
    public String getTenantName() { return tenantName; }
    public String getContactNumber() { return contactNumber; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public String getRoomNumber() { return roomNumber; }
    public String getRoomId() { return roomId; }
    public boolean getIsPaid() { return isPaid; }
    public double getMonthlyRent() { return monthlyRent; }
    public double getTotalDebt() { return totalDebt; }
    public int getMissedMonths() { return missedMonths; }
    public boolean getIsEvicted() { return isEvicted; }
    public int getPaidStreak() { return paidStreak; }
    public LocalDate getMoveInDate() { return moveInDate; }
    public LocalDate getDueDate() { return dueDate; }

    // Setters
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public void setEmail(String email) { this.email = email; }
    public void setAddress(String address) { this.address = address; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setPaid(boolean paid) { isPaid = paid; }
    public void setMonthlyRent(double monthlyRent) { this.monthlyRent = monthlyRent; }
    public void setTotalDebt(double totalDebt) { this.totalDebt = totalDebt; }
    public void setMissedMonths(int missedMonths) { this.missedMonths = missedMonths; }
    public void setEvicted(boolean evicted) { isEvicted = evicted; }
    public void setPaidStreak(int paidStreak) { this.paidStreak = paidStreak; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}