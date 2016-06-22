package com.hoperun.message.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import java.util.List;

@SuppressWarnings("deprecation")
public class ImageAdapter extends BaseAdapter {

	private Context context;

	private List<Bitmap> bitmapList;

	public ImageAdapter(Context context, List<Bitmap> bitmapList) {
		this.context = context;
		this.bitmapList = bitmapList;
	}

	@Override
	public int getCount() {
		return bitmapList == null ? 0 : bitmapList.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView = new ImageView(context);
		imageView.setImageBitmap(bitmapList.get(position));
		imageView.setScaleType(ImageView.ScaleType.FIT_XY);
		imageView.setLayoutParams(new Gallery.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		return imageView;
	}
}
