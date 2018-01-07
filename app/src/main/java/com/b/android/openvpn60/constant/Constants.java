package com.b.android.openvpn60.constant;

/**
 * Created by b on 8/16/17.
 */

public enum Constants {
    URL_REGISTER("http://139.59.160.203:8080/UserManagement/rest/user-service/insert-user"),
    URL_REGISTER_MEMBER("http://139.59.160.203:8080/UserManagement/rest/member-service/insert-member"),
    URL_LOGIN("http://139.59.160.203:8080/UserManagement/rest/password-service/do-login"),
    URL_PUT("http://139.59.160.203:8080/UserManagement/rest/user-service/update-user"),
    URL_GET_PROFILES("http://139.59.160.203:8080/UserManagement/rest/server-service/get-all-servers"),
    URL_CHECK_MEMBERS("http://139.59.160.203:8080/UserManagement/rest/member-service/members"),
    USER_NAME("user_name"),
    USER_UUID("user_uuid"),
    TEMP_USER("temp_user"),
    USER_PASS("user_pass"),
    USER_CHOICE("user_choice"),
    SHARED_PREFS("shared_prefs"),
    MEMBER_NAME("member_name"),
    FIRST_NAME("first_name"),
    LAST_NAME("last_name"),
    EMAIL("email"),
    RESULT_PROFILE("result_profile"),
    SELECTED_PROFILE("selected_profile"),
    CMD_PING("ping -c 2 -w 2 "),
    CLASS_TAG_ACTIVITY(""),
    EXTRA_KEY("shortcut_profile_uuid"),
    EXTRA_NAME("shortcut_profile_ip"),
    EXTRA_HIDELOG("show_no_log_window"),
    CLEARLOG("clear_log_connect");


    private final String text;

    Constants(final String text) {
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
