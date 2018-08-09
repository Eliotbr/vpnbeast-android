package com.b.android.openvpn60.constant;

/**
 * Created by b on 1/8/2018.
 */

public enum ServiceConstants {

    URL_LOGIN("http://139.59.160.203:7070/user-service/do-login"),
    URL_INSERT_USER("http://139.59.160.203:7070/user-service/insert-user"),
    URL_UPDATE_LAST_LOGIN("http://139.59.160.203:7070/user-service/update-last-login"),
    URL_INSERT_MEMBER("http://139.59.160.203:7070/member-service/insert-member"),
    URL_GET_MEMBER_BY_USERNAME("http://139.59.160.203:7070/member-service/members"),
    URL_GET_PROFILES("http://139.59.160.203:7070/server-service/get-all-servers");


    private final String text;

    ServiceConstants(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
