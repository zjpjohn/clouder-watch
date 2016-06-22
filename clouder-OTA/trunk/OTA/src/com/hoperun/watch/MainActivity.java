package com.hoperun.watch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {

	private static final String TAG = "watch";

	private Button mStartBtn;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initViews();
	}

	private void initViews() {
		mStartBtn = (Button) findViewById(R.id.btn_start);
		mStartBtn.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_start:
			Log.d(TAG, "start service");
			Intent intent = new Intent(DownloadService.INTENT);
			startService(intent);
			break;

		default:
			break;
		}
	}

}
