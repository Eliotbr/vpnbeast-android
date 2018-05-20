package com.b.android.openvpn60.model;

import java.io.Serializable;
import java.util.UUID;


public class Member implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID uuid;
    private int id;
    private String userName;
    private String email;
    private String firstName;
    private String lastName;


    public Member(String userName, String firstName, String lastName, String email) {
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        uuid = UUID.randomUUID();
    }

    public Member() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID mUuid) {
        this.uuid = mUuid;
    }
}
