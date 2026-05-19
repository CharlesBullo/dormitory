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

    // User account fields
    private String username;
    private String temporaryPassword;
    private boolean hasResetPassword;

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
        this.hasResetPassword = false;

        // Generate username from email or name
        this.username = generateUsername(tenantName, email);

        // Fixed temporary password "123456"
        this.temporaryPassword = "123456";
    }

    private String generateUsername(String name, String email) {
        // Try to use email prefix first
        if (email != null && email.contains("@")) {
            String prefix = email.split("@")[0];
            if (prefix.length() >= 4) {
                return prefix;
            }
        }
        // Otherwise generate from name
        String cleanName = name.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (cleanName.length() > 10) {
            cleanName = cleanName.substring(0, 10);
        }
        return cleanName + System.currentTimeMillis() % 1000;
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
    public String getUsername() { return username; }
    public String getTemporaryPassword() { return temporaryPassword; }
    public boolean isHasResetPassword() { return hasResetPassword; }

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
    public void setUsername(String username) { this.username = username; }
    public void setTemporaryPassword(String temporaryPassword) { this.temporaryPassword = temporaryPassword; }
    public void setHasResetPassword(boolean hasResetPassword) { this.hasResetPassword = hasResetPassword; }
}