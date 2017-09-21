package com.b.android.openvpn60.fragment;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.VpnProfile;
import com.b.android.openvpn60.activity.MainActivity;
import com.b.android.openvpn60.enums.Constants;

import java.util.ArrayList;

/**
 * Created by b on 8/13/17.
 */

public class ServerSelectFragment extends ListFragment {
    private Context context;
    private static final String SERVICE_URL_GET_PROFILES = Constants.URL_GET_PROFILES.toString();
    private ArrayList<VpnProfile> profiles;
    private static final String RESULT_PROFILE = Constants.RESULT_PROFILE.toString();
    private RelativeLayout pnlRelative = null;
    private ArrayAdapter<VpnProfile> adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        profiles = (ArrayList<VpnProfile>) MainActivity.profiles;
        View v = inflater.inflate(R.layout.list_server_fragment, container, false);
        adapter = new ArrayAdapter<VpnProfile>(inflater.getContext(),
                R.layout.list_server_text, R.id.list_content,
                    profiles);
        context = getActivity().getApplicationContext();
        pnlRelative = (RelativeLayout) getActivity().findViewById(R.id.activity_main);
        pnlRelative.setVisibility(View.INVISIBLE);
        setListAdapter(adapter);

        return v;
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        getActivity().getIntent().putExtra(RESULT_PROFILE, profiles.get(position));
        getActivity().getFragmentManager().beginTransaction().remove(ServerSelectFragment.this).commit();
        ((MainActivity) getActivity()).updateViews();
        pnlRelative.setVisibility(View.VISIBLE);
    }


}
