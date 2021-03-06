package com.b.android.openvpn60.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import com.b.android.openvpn60.R;

/**
 * Created by b on 1/6/2018.
 */

public class ViewUtil {

    private ViewUtil() {

    }

    public static AlertDialog.Builder showErrorDialog(Context context, String string) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle("Error");
        //mBuilder.setMessage(R.string.state_msg_disconnected);
        //final View disconnectLayout = getLayoutInflater().inflate(R.layout.disconnect_dialog, null, false);
        //mBuilder.setView(disconnectLayout);
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(context.getResources().getColor(R.color.colorAccent));
        // Initialize a new spannable string builder instance
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(string);
        ssBuilder.setSpan(
                foregroundColorSpan,
                0,
                string.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        alertDialog.setMessage(ssBuilder);
        alertDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // nothing is going to happen when clicked that button
            }
        });
        return alertDialog;
    }
}
