package com.clouder.watch.call.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.clouder.watch.call.R;

import java.util.List;
import java.util.Map;

/**
 * Created by zhou_wenchong on 11/16/2015.
 */
public class ContactSimpleAdapter extends BaseAdapter {
    private List<Map<String, Object>> mData;
    private LayoutInflater mInflater;

    public ContactSimpleAdapter(Context context, List<Map<String, Object>> mData) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = mData;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.list_contacts, null);
            holder.img = (RoundImageView) convertView.findViewById(R.id.img);
            holder.title = (TextView) convertView.findViewById(R.id.name);
            holder.info = (TextView) convertView.findViewById(R.id.num);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
//        holder.img.setBackgroundResource((Integer) mData.get(position).get("img"));
        holder.img.setImageBitmap((Bitmap) mData.get(position).get("img"));
        holder.title.setText((String) mData.get(position).get("name"));
        holder.info.setText((String) mData.get(position).get("num"));

        return convertView;
    }

    public final class ViewHolder {
        public RoundImageView img;
        public TextView title;
        public TextView info;
    }
}
