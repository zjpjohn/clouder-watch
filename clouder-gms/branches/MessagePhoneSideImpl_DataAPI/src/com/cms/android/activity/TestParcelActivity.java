package com.cms.android.activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.cms.android.wearable.Asset;
import com.cms.android.wearable.PutDataRequest;

public class TestParcelActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d("spencer", "onCreate...");
		PutDataRequest parcelable = (PutDataRequest) getIntent().getParcelableExtra("key");
		Log.d("spencer", "Parcelable -> " + parcelable.toString());
		
//		Asset asset = (Asset) getIntent().getParcelableExtra("key");
//		Log.d("spencer", "Asset -> " + asset.toString());
	}

}
