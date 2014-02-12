package com.wudayu.repeater.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wudayu.repeater.R;
import com.wudayu.repeater.services.PlayService;
import com.wudayu.repeater.utils.FormatHelper;

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
    TextView txtCurrentTime;
    TextView txtDurationTime;
    TextView txtPointA;
    TextView txtPointB;

    private String currSong;
    private int mDuration;
    private boolean mSeeking = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent intent = getIntent();
        initializeService(intent);

		btnPlayHold = (Button) findViewById(R.id.btn_play_hold);
		btnAPoint = (Button) findViewById(R.id.btn_apoint);
		btnBPoint = (Button) findViewById(R.id.btn_bpoint);
		btnPlayMode = (Button) findViewById(R.id.btn_playmode);
		processBar = (SeekBar) findViewById(R.id.progress);
		txtCurrentTime = (TextView) findViewById(R.id.txt_current_time);
		txtDurationTime = (TextView) findViewById(R.id.txt_duration_time);
		txtPointA = (TextView) findViewById(R.id.txt_point_a);
		txtPointB = (TextView) findViewById(R.id.txt_point_b);

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
			if (playBinder.playBackIsPlaying()) {
				pauseThePlay();
			} else {
				continueThePlay();
			}
		}
	}

	private void pauseThePlay() {
		playBinder.playBackPause();
		btnPlayHold.setText(R.string.btn_play_hold_state_hold);
	}

	private void continueThePlay() {
		playBinder.playBackContinue();
		btnPlayHold.setText(R.string.btn_play_hold_state_playing);
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
			changePlayMode(PlayService.PLAYMODE[playBinder.incPlayModeIter()]);
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

                txtCurrentTime.setText(FormatHelper.timeFormatter(playBinder.playBackGetCurrentPosition()));
                txtDurationTime.setText(FormatHelper.timeFormatter(playBinder.playBackGetDuration()));
                txtPointA.setText(FormatHelper.timeFormatter(playBinder.getPoingA()));
                txtPointB.setText(FormatHelper.timeFormatter(playBinder.getPointB()));

                if (playBinder.playBackIsPlaying()) {
                	continueThePlay();
    			} else {
    				pauseThePlay();
    			}
            }

            mProgressRefresher.removeCallbacksAndMessages(null);
            mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
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
				playBinder.useForeground(getText(R.string.str_ticker_now_playing) + currSong, currSong);
				mDuration = playBinder.playBackGetDuration();
				processBar.setMax(mDuration);
				changePlayMode(PlayService.PLAYMODE[playBinder.getPlayModeIter()]);
				mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
			}
		};

		bindService(playServiceIntent, sConnection, Context.BIND_IMPORTANT);

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
			case R.id.action_helps:
				helpsInfo();
				break;
			case R.id.action_rate:
				rateme();
				break;
			case R.id.action_exit:
				stopService(playServiceIntent);
				MainActivity.this.finish();
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	private void helpsInfo() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this)
				.setTitle(getString(R.string.action_helps))
				.setMessage(
						getString(R.string.app_name) + " "
								+ getString(R.string.version) + "\n\n"
								+ getString(R.string.str_helps_tip_hint) + "\n\t"
								+ getString(R.string.str_helps_tip_1) + "\n\n\t"
								+ getString(R.string.str_helps_tip_2) + "\n\n\t"
								+ getString(R.string.str_helps_tip_3) + "\n\n"
								+ getString(R.string.author_hint)
								+ getString(R.string.author) + "\n"
								+ getString(R.string.email_hint)
								+ getString(R.string.email))
				.setIcon(android.R.drawable.ic_dialog_info)
				.setPositiveButton(getString(R.string.str_dialog_btn_positive), null);

		dialog.show();
	}

	private void rateme() {
		Uri uri = Uri.parse("market://details?id=" + getPackageName());
		Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
		try {
			startActivity(goToMarket);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, "Could not connect the Play",
					Toast.LENGTH_LONG).show();
		}
	}

	private void changePlayMode(int mode) {
		switch (mode) {
			case PlayService.PLAYMODE_LOOP:
				btnPlayMode.setText(R.string.btn_playmode_loop);
				playBinder.setPlayMode(PlayService.PLAYMODE_LOOP);
				break;
			case PlayService.PLAYMODE_SECTION_LOOP:
				btnPlayMode.setText(R.string.btn_playmode_section_loop);
				playBinder.setPlayMode(PlayService.PLAYMODE_SECTION_LOOP);
				break;
			case PlayService.PLAYMODE_NORMAL:
				btnPlayMode.setText(R.string.btn_playmode_normal);
				playBinder.setPlayMode(PlayService.PLAYMODE_NORMAL);
				break;
		}
	}

}
