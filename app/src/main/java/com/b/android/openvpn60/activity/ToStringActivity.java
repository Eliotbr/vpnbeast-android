package com.b.android.openvpn60.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.b.android.openvpn60.LaunchVPN;
import com.b.android.openvpn60.R;
import com.b.android.openvpn60.constant.Constants;
import com.b.android.openvpn60.core.ProfileManager;
import com.b.android.openvpn60.VpnProfile;

public class ToStringActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_string);
        TextView txtTostring = (TextView) this.findViewById(R.id.txtTostring);
        //txtTostring.setText(this.getIntent().getSerializableExtra(Constants.RESULT_PROFILE.toString()).toString());

        VpnProfile test = new VpnProfile("converted profile 2");
        Intent statusIntent = new Intent(ToStringActivity.this, StatusActivity.class);
        statusIntent.putExtra(Constants.RESULT_PROFILE.toString(), test);
        startOrStopVPN(test);
    }

    private void startOrStopVPN(VpnProfile profile) {
        startVPN(profile);
    }

    private void startVPN(VpnProfile profile) {
        getPM().saveProfile(this, profile);
        Intent intent = new Intent(this, LaunchVPN.class);
        intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUID().toString());
        intent.setAction(Intent.ACTION_MAIN);
        startActivity(intent);
        this.finish();
    }

    private ProfileManager getPM() {
        return ProfileManager.getInstance(this);
    }
}
