package com.example.testasset;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class TestParcelActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d("spencer", "onCreate...");
		// PutDataRequest parcelable = (PutDataRequest)
		// getIntent().getParcelableExtra("key");
		// Log.d("spencer", "Parcelable -> " + parcelable.toString());
		// Map<String, Asset> assetMap = parcelable.getAssets();
		// Iterator<String> iterator = assetMap.keySet().iterator();
		// while (iterator.hasNext()) {
		// String key = iterator.next();
		// Log.d("spencer", "Key -> " + key);
		// Asset asset = assetMap.get(key);
		// Log.d("spencer", "asset -> " + asset);
		// }

		// DataHolder parcelable = (DataHolder)
		// getIntent().getParcelableExtra("key");
		// Log.d("spencer", "Parcelable -> " + parcelable.getStatusCode()+" "+
		// parcelable.getDataItems().size()+" "+
		// parcelable.getDataEvents().size());

		PutDataResponse response = (PutDataResponse) getIntent().getParcelableExtra("key");
		Log.d("spencer", "Parcelable -> " + response.status + " " + response.versionCode + " " + response.dataItem);

	}

}
