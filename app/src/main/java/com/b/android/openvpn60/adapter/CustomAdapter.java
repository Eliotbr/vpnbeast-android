package com.b.android.openvpn60.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.VpnProfile;

import java.util.List;

/**
 * Created by b on 6/6/17.
 */

public class CustomAdapter extends ArrayAdapter<VpnProfile> {
    private int mResourceId = 0;
    private LayoutInflater mLayoutInflater;
    public RadioButton mSelectedRB;
    public TextView mSelectedTxt;
    private int mSelectedTxtPosition = -1;
    private int mSelectedPosition = -1;
    private List<VpnProfile> profiles;
    public VpnProfile mProfile2;
    private CustomAdapter mInstance;
    //private VpnProfile mSelected;


    public CustomAdapter(Context context, int resource, int textViewResourceId, List<VpnProfile> objects) {
        super(context, resource, textViewResourceId, objects);
        mResourceId = resource;
        profiles = objects;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInstance = this;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        final ViewHolder holder;
        if(view == null) {
            view = mLayoutInflater.inflate(mResourceId, parent, false);
            holder = new ViewHolder();
            holder.name = view.findViewById(R.id.name);
            holder.radioBtn = view.findViewById(R.id.radio);
            view.setTag(holder);
        } else
            holder = (ViewHolder)view.getTag();
        holder.name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(position != mSelectedPosition && mSelectedRB != null)
                    mSelectedRB.setChecked(false);
                else if (mSelectedRB != null)
                    mSelectedRB.setChecked(true);
                mSelectedPosition = position;
                mSelectedRB = holder.radioBtn;
                mSelectedTxt = (TextView)v;
            }
        });
        holder.radioBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(position != mSelectedPosition && mSelectedRB != null)
                    mSelectedRB.setChecked(false);
                mSelectedPosition = position;
                mSelectedRB = (RadioButton)v;
                //mSelected = mSelectedPosition
            }
        });
        if(mSelectedPosition != position)
            holder.radioBtn.setChecked(false);
        else {
            holder.radioBtn.setChecked(true);
            if(mSelectedRB != null && holder.radioBtn != mSelectedRB)
                mSelectedRB = holder.radioBtn;
        }
        holder.name.setText(profiles.get(position).getName());
        holder.name.setTextColor(Color.WHITE);
        //this.mProfile2 = CustomAdapter.this.getItem(position);
        mProfile2 = profiles.get(position);
        //holder.name.setText(getItem(position).getName());
        return view;
    }

    public class ViewHolder {
        TextView        name;
        RadioButton     radioBtn;
    }
}