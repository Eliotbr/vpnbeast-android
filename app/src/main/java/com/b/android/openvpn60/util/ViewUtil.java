package com.b.android.openvpn60.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.activity.LoginActivity;
import com.b.android.openvpn60.activity.MainActivity;
import com.b.android.openvpn60.activity.StatusActivity;
import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.core.VpnStatus;
import com.b.android.openvpn60.model.VpnProfile;

/**
 * Created by b on 1/6/2018.
 */

public class ViewUtil {

    public static void showErrorDialog(Context context, String string) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle("Error");
        //mBuilder.setMessage(R.string.state_msg_disconnected);
        //final View disconnectLayout = getLayoutInflater().inflate(R.layout.disconnect_dialog, null, false);
        //mBuilder.setView(disconnectLayout);
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.colorAccent));
        // Initialize a new spannable string builder instance
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(context.getString(R.string.state_msg_disconnected));
        ssBuilder.setSpan(
                foregroundColorSpan,
                0,
                context.getString(R.string.state_msg_disconnected).length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        alertDialog.setMessage(ssBuilder);
        alertDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alertDialog.show();
    }
}
