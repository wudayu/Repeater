package com.wudayu.repeater.activities;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.wudayu.repeater.R;
import com.wudayu.repeater.services.PlayService;

public class MainActivity extends Activity {

	public final static String TAG = "com.wudayu.repeater.activities.MainActivity";

    private Uri mUri;
    private Intent playServiceIntent;
    private Handler mProgressRefresher;
    private ServiceConnection sConnection;
    private PlayService.PlayBinder playBinder;

    Button btnPlayHold;
    Button btnAPoint;
    Button btnBPoint;
    Button btnPlayMode;
    SeekBar processBar;

    private String currSong;
    private int mDuration;
    private boolean mSeeking = false;
    private int playModeIter = 0;

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
		btnPlayMode = (Button) findViewById(R.id.btn_playmode);
		processBar = (SeekBar) findViewById(R.id.progress);

		btnPlayHold.setOnClickListener(new BtnPlayHoldOnClickListener());
		btnAPoint.setOnClickListener(new BtnAPointOnClickListener());
		btnBPoint.setOnClickListener(new BtnBPointOnClickListener());
		btnPlayMode.setOnClickListener(new BtnPlayModeOnClickListener());
		processBar.setOnSeekBarChangeListener(new ProgressOnSeekBarChangeListener());

		mProgressRefresher = new Handler();
	}

	protected void initializeService(Intent intent) {
		if (intent == null) {
            finish();
            return;
        }
        mUri = intent.getData();
        if (mUri != null && mUri.getScheme().equals("http")) {
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
			if (playBinder != null)
				playBinder.setPointA(playBinder.playBackGetCurrentPosition());
			else
				Toast.makeText(MainActivity.this, getString(R.string.str_set_pointa_failed), Toast.LENGTH_SHORT).show();
		}
	}

	private class BtnBPointOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (playBinder != null) {
				playBinder.setPointB(playBinder.playBackGetCurrentPosition());
				changePlayMode(PlayService.PLAYMODE_SECTION_LOOP);
			} else
				Toast.makeText(MainActivity.this, getString(R.string.str_set_pointb_failed), Toast.LENGTH_SHORT).show();
		}
	}

	private class BtnPlayModeOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			changePlayMode(PlayService.PLAYMODE[(++playModeIter) % PlayService.PLAYMODE.length]);
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
			mSeeking = true;
			playBinder.playBackPause();
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
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

				mUri = playBinder.getUri();
				currSong = mUri.toString().substring(mUri.toString().lastIndexOf('/') + 1);
				mDuration = playBinder.playBackGetDuration();
				processBar.setMax(mDuration);
				playBinder.setPointA(0);
				playBinder.setPointB(mDuration);
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

	private void changePlayMode(int mode) {
		switch (mode) {
			case PlayService.PLAYMODE_LOOP:
				btnPlayMode.setText(getString(R.string.btn_playmode_loop));
				playBinder.setPlayMode(PlayService.PLAYMODE_LOOP);
				break;
			case PlayService.PLAYMODE_SECTION_LOOP:
				btnPlayMode.setText(getString(R.string.btn_playmode_section_loop));
				playBinder.setPlayMode(PlayService.PLAYMODE_SECTION_LOOP);
				break;
			case PlayService.PLAYMODE_NORMAL:
				btnPlayMode.setText(getString(R.string.btn_playmode_normal));
				playBinder.setPlayMode(PlayService.PLAYMODE_NORMAL);
				break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showNotification();
		}

		return super.onKeyDown(keyCode, event);
	}

	private void showNotification() {
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		int notifyID = 1;
		Intent resultIntent = new Intent(this, MainActivity.class);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(MainActivity.this, 0, resultIntent, 0);
		NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(this)
				.setContentTitle(getString(R.string.app_name))
				.setContentText(currSong)
				.setTicker(currSong)
				.setContentIntent(resultPendingIntent)
				.setSmallIcon(R.drawable.ic_launcher)
				.setAutoCancel(true)
				.setOngoing(true);

		mNotificationManager.notify(notifyID, mNotifyBuilder.build());
	}
}
