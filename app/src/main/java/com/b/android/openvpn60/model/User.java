package com.b.android.openvpn60.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by b on 5/23/17.
 */

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID uuid;

    private int id;
    private String userName;
    private String userPass;
    private String email;
    private String firstName;
    private String lastName;

    public User(String userName, String userPass) {
        this.userName = userName;
        this.userPass = userPass;
        uuid = UUID.randomUUID();
    }

    public User() {

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

    public String getUserPass() {
        return userPass;
    }

    public void setUserPass(String userPass) {
        this.userPass = userPass;
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
