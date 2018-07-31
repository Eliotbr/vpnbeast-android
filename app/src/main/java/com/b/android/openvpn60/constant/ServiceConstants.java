package com.b.android.openvpn60.constant;

/**
 * Created by b on 1/8/2018.
 */

public enum ServiceConstants {
    URL_INSERT_USER("http://139.59.160.203:8080/rest/user-controller/insert-user"),
    URL_INSERT_MEMBER("http://139.59.160.203:8080/rest/member-controller/insert-member"),
    URL_LOGIN("http://139.59.160.203:8080/rest/user-controller/do-login"),
    URL_UPDATE_LAST_LOGIN("http://139.59.160.203:8080/rest/user-controller/update-last-login"),
    URL_GET_PROFILES("http://139.59.160.203:8080/rest/server-controller/get-all-servers"),
    URL_GET_MEMBER_BY_USERNAME("http://139.59.160.203:8080/rest/member-controller/members");


    private final String text;

    ServiceConstants(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
