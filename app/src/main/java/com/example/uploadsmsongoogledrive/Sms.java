package com.example.uploadsmsongoogledrive;

public class Sms {
    private String address;
    private String date;
    private String body;

    public Sms(String address, String date, String body) {
        this.address = address;
        this.date = date;
        this.body = body;
    }

    public String getAddress() {
        return address;
    }

    public String getDate() {
        return date;
    }

    public String getBody() {
        return body;
    }
}