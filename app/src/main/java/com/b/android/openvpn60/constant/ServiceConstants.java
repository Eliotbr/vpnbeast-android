package com.b.android.openvpn60.constant;

/**
 * Created by b on 12/30/2017.
 */

public enum ServiceConstants {

    URL_REGISTER("http://139.59.160.203:8080/UserManagement/rest/user-service/insert-user"),
    URL_REGISTER_MEMBER("http://139.59.160.203:8080/UserManagement/rest/member-service/insert-member"),
    URL_LOGIN("http://139.59.160.203:8080/UserManagement/rest/password-service/do-login"),
    URL_PUT("http://139.59.160.203:8080/UserManagement/rest/user-service/update-user"),
    URL_GET_PROFILES("http://139.59.160.203:8080/UserManagement/rest/server-service/get-all-servers"),
    URL_CHECK_MEMBERS("http://139.59.160.203:8080/UserManagement/rest/member-service/members");


    private final String text;

    ServiceConstants(final String text) {
        this.text = text;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return text;
    }
}
