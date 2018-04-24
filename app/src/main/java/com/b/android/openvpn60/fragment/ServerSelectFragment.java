package com.b.android.openvpn60.fragment;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.model.VpnProfile;
import com.b.android.openvpn60.activity.MainActivity;
import com.b.android.openvpn60.constant.AppConstants;

import java.util.ArrayList;

/**
 * Created by b on 8/13/17.
 */

public class ServerSelectFragment extends ListFragment {

    private Context context;
    private ArrayList<VpnProfile> profiles;
    private static final String RESULT_PROFILE = AppConstants.RESULT_PROFILE.toString();
    private RelativeLayout pnlRelative = null;
    private ArrayAdapter<VpnProfile> adapter;
    private LogHelper logHelper;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logHelper = LogHelper.getLogHelper(ServerSelectFragment.class.getName());
        //profiles = new ArrayList<>();
        profiles = (ArrayList<VpnProfile>) MainActivity.profiles;
        if (profiles == null)
            logHelper.logInfo("PROFILES ARRAYLIST IS NULL!");
        if (profiles.isEmpty())
            logHelper.logInfo("PROFILES ARRAYLIST IS EMPTY!");
        View v = inflater.inflate(R.layout.list_server_fragment, container, false);
        adapter = new ArrayAdapter<VpnProfile>(inflater.getContext(),
                R.layout.list_server_text, R.id.list_content,
                profiles);
        context = getActivity().getApplicationContext();
        pnlRelative = getActivity().findViewById(R.id.activity_main);
        pnlRelative.setVisibility(View.INVISIBLE);
        setListAdapter(adapter);
        return v;
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        getActivity().getIntent().putExtra(RESULT_PROFILE, (Parcelable) profiles.get(position));
        getActivity().getFragmentManager().beginTransaction().remove(ServerSelectFragment.this).commit();
        ((MainActivity) getActivity()).updateViews();
        pnlRelative.setVisibility(View.VISIBLE);
    }


}
