package com.wudayu.repeater.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.wudayu.repeater.R;
import com.wudayu.repeater.services.PlayService;

public class MainActivity extends Activity {

	public final static String TAG = "MainActivity";

    private Uri mUri;
    private Intent playServiceIntent;
    private Handler mProgressRefresher;
    private ServiceConnection sConnection;
    private PlayService.PlayBinder playBinder;

    Button btnPlayHold;
    Button btnAPoint;
    Button btnBPoint;
    SeekBar processBar;

    private int mDuration;
    private boolean mSeeking = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
        initializeService(intent);

		btnPlayHold = (Button) findViewById(R.id.btn_play_hold);
		btnAPoint = (Button) findViewById(R.id.btn_apoint);
		btnBPoint = (Button) findViewById(R.id.btn_bpoint);
		processBar = (SeekBar) findViewById(R.id.progress);

		btnPlayHold.setOnClickListener(new BtnPlayHoldOnClickListener());
		btnAPoint.setOnClickListener(new BtnAPointOnClickListener());
		btnBPoint.setOnClickListener(new BtnBPointOnClickListener());
		processBar.setOnSeekBarChangeListener(new ProgressOnSeekBarChangeListener());

		mProgressRefresher = new Handler();
	}

	protected void initializeService(Intent intent) {
		if (intent == null) {
            finish();
            return;
        }
        mUri = intent.getData();
        if (mUri == null) {
            finish();
            return;
        }

		if (mUri.getScheme().equals("http")) {
            Toast.makeText(this, "Host : " + mUri.getHost(), Toast.LENGTH_SHORT).show();
            finish();
        }

		playServiceIntent = new Intent(this, PlayService.class);
		playServiceIntent.setData(mUri);

		startService(playServiceIntent);
	}

	private class BtnPlayHoldOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			
		}
	}

	private class BtnAPointOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			
		}
	}

	private class BtnBPointOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			
		}
	}

	private class ProgressOnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if (fromUser) {
				playBinder.playBackSeekTo(progress);
            }
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			Log.i(TAG, "Start Seeking = " + mSeeking);
			mSeeking = true;
			playBinder.playBackPause();
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			Log.i(TAG, "Stop Seeking = " + mSeeking);
			mSeeking = false;
			playBinder.playBackContinue();
		}
	}

	class ProgressRefresher implements Runnable {
        public void run() {
            if (!mSeeking && mDuration != 0) {
                processBar.setProgress(playBinder.playBackGetCurrentPosition());
            }

            mProgressRefresher.removeCallbacksAndMessages(null);
            mProgressRefresher.postDelayed(new ProgressRefresher(), 100);
        }
    }

	@Override
	protected void onStart() {
		sConnection = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
			}
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				playBinder = (PlayService.PlayBinder) service;

				mDuration = playBinder.playBackGetDuration();
				processBar.setMax(mDuration);
				mProgressRefresher.postDelayed(new ProgressRefresher(), 100);
			}
		};
		
		bindService(playServiceIntent, sConnection, Context.BIND_AUTO_CREATE);

		super.onStart();
	}

	@Override
	protected void onStop() {
		unbindService(sConnection);

		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings:
				break;
			case R.id.action_exit:
				stopService(playServiceIntent);
				MainActivity.this.finish();
				break;
		}

		return super.onOptionsItemSelected(item);
	}

}
