package com.b.android.openvpn60.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.constant.Constants;

public class ToStringActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_string);
        TextView txtTostring = (TextView) this.findViewById(R.id.txtTostring);
        txtTostring.setText(this.getIntent().getSerializableExtra(Constants.RESULT_PROFILE.toString()).toString());
    }
}
