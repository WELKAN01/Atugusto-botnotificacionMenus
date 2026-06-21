package com.atugusto.notify.DTO;

public class messageTO {
    private String phoneNumber;
    private String phone_number_id;
    private String message;

    // Constructors
    public messageTO() {}

    public messageTO(String phone_number_id, String phoneNumber, String message) {
        this.phone_number_id = phone_number_id;
        this.phoneNumber = phoneNumber;
        this.message = message;
    }

    // Getters and Setters
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPhone_number_id() {
        return phone_number_id;
    }

    public void setPhone_number_id(String phone_number_id) {
        this.phone_number_id = phone_number_id;
    }
}
