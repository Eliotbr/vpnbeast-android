package com.b.android.openvpn60.constant;

/**
 * Created by b on 8/16/17.
 */

public enum AppConstants {
    USER_NAME("user_name"),
    VPN_USERNAME("vpn_username"),
    VPN_PASSWORD("vpn_password"),
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
    CLEARLOG("clear_log_connect"),
    SALT("§å¢-Ñ~”Š“+M~a98"),
    DISCONNECT_VPN("disconnect_vpn"),
    CLOSE_ACTIVITY("close_activity"),
    RESULT_DESTROYED("result_destroyed"),
    START_SERVICE("start_service"),
    START_SERVICE_STICKY("start_service_sticky"),
    NOTIFICATION_ALWAYS_VISIBLE("notification_always_visible"),
    PAUSE_VPN("pause_vpn"),
    RESUME_VPN("resume_vpn"),
    INSERT_USER("insert_user"),
    DO_LOGIN("do_login"),
    INSERT_MEMBER("insert_member"),
    CHECK_MEMBER("check_member");


    private final String text;


    AppConstants(final String text) {
        this.text = text;
    }


    @Override
    public String toString() {
        return text;
    }
}
