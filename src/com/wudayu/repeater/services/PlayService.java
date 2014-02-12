package com.wudayu.repeater.services;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.wudayu.repeater.R;
import com.wudayu.repeater.activities.MainActivity;


/**
 *
 * @author: Wu Dayu
 * @En_Name: David Wu
 * @E-mail: wudayu@gmail.com
 * @version: 1.0
 * @Created Time: Jan 20, 2014 10:20:05 AM
 * @Description: This is David Wu's property.
 *
 **/
public class PlayService extends Service {

	public final static String TAG = "com.wudayu.repeater.services.PlayService";
	public final static int NOTIFY_ID = 0xB1;
	public final static int PLAYMODE_NORMAL = 0x11;
	public final static int PLAYMODE_LOOP = 0x12;
	public final static int PLAYMODE_SECTION_LOOP = 0x13;
	public final static int[] PLAYMODE = { PLAYMODE_NORMAL, PLAYMODE_LOOP,
			PLAYMODE_SECTION_LOOP };

	private Timer sectionLoopTimer = new Timer();
	private TimerTask sectionLoopTimerTask;
	private IBinder playBinder = new PlayBinder();
	private MediaPlayer mPlayer;
	private Uri mUri;

	private boolean isRunning;
	private boolean isSame;
	private int mDuration;
	private int mPlaymode;
	private int pointA;
	private int pointB;
    private int playModeIter;

	@Override
	public void onCreate() {
		mPlayer = new MediaPlayer();
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		isRunning = true;
		isSame = false;

		if (mUri != null && (mUri.equals(intent.getData()) || (intent.getData() == null)))
			isSame = true;
		if (intent.getData() != null)
			mUri = intent.getData();

		if (mUri == null) {
			SharedPreferences sharedPreferences = getSharedPreferences("playUri", MODE_PRIVATE);
			mUri = new Uri.Builder().encodedPath(sharedPreferences.getString("mUri", null)).build();
		} else {
			SharedPreferences sharedPreferences = getSharedPreferences("playUri", MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString("mUri", mUri.toString());
			editor.commit();
		}

		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		if (!isSame) {
			prepare(mUri);
			playModeIter = 0;
			pointA = 0;
			pointB = mDuration;
		}

		startTimerTask();
		return playBinder;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		isRunning = false;
		if (mPlayer != null)
			mPlayer.release();
		mPlayer = null;

		stopForeground(true);
		super.onDestroy();
	}

	/*
	 * The PlayService interface for client activities
	 */
	public class PlayBinder extends Binder {

		/**
		 * The format of play back functions
		 * public Boolean playBackXXX() {
		 * 		return false();
		 * }
		 */

		public int playBackGetDuration() {
			return mDuration;
		}

		public int playBackGetCurrentPosition() {
			return getCurrentPosition();
		}

		public void playBackSeekTo(int msec) {
			if (mPlayer != null)
				mPlayer.seekTo(msec);
		}

		public void playBackPause() {
			if (mPlayer != null)
				mPlayer.pause();
		}

		public void playBackContinue() {
			if (mPlayer != null)
				mPlayer.start();
		}

		public boolean playBackIsPlaying() {
			return mPlayer != null ? mPlayer.isPlaying() : false;
		}

		public void setPlayMode(int mode) {
			mPlaymode = mode;
		}

		public int getPlayMode() {
			return mPlaymode;
		}

		public void setPointA(int val) {
			pointA = val;
		}

		public void setPointB(int val) {
			pointB = val;
		}

		public int getPoingA() {
			return pointA;
		}

		public int getPointB() {
			return pointB;
		}

		public Uri getUri() {
			return mUri;
		}

		public int incPlayModeIter() {
			playModeIter = (playModeIter + 1) % PlayService.PLAYMODE.length;

			return playModeIter;
		}

		public int getPlayModeIter() {
			return playModeIter;
		}

		public boolean isPlaying() {
			return mPlayer.isPlaying();
		}

		@SuppressWarnings("deprecation")
		public void useForeground(CharSequence tickerText, String currSong) {
			Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
			/* Method 01
			 * this method can't do what we want in Android 4.4 KitKat
			 * it can only show the application info page which contains the 'Force Close' button.
	 		NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(PlayService.this)
					.setContentTitle(getString(R.string.app_name))
					.setContentText(currSong)
					.setTicker(tickerText)
					.setContentIntent(pendingIntent);
			Notification notification = mNotifyBuilder.build();
			*/
			/* Method 02 */
			Notification notification = new Notification(R.drawable.ic_launcher, tickerText,
			        System.currentTimeMillis());
			notification.setLatestEventInfo(PlayService.this, getText(R.string.app_name),
					currSong, pendingIntent);
			
			startForeground(NOTIFY_ID, notification);
		}

	}

	public int getCurrentPosition() {
		if (mPlayer != null)
			return mPlayer.getCurrentPosition();

		return 0;
	}

	private Boolean prepare(Uri mUri) {
		mDuration = 0;
		mPlaymode = PLAYMODE[0];
		mPlayer.stop();
		mPlayer.reset();

		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				switch (mPlaymode) {
					case PlayService.PLAYMODE_LOOP:
						mp.start();
						break;
					case PlayService.PLAYMODE_SECTION_LOOP:
						mp.start();
						mp.seekTo(pointA);
						break;
					case PlayService.PLAYMODE_NORMAL:
						mp.start();
						mp.seekTo(0);
						mp.pause();
						break;
				}
			}
		});

		try {
			mPlayer.setDataSource(getApplicationContext(), mUri);
			mPlayer.prepare();
			mDuration = mPlayer.getDuration();
			mPlayer.start();
		} catch (Exception e) {
			Log.w(TAG, e.toString());
			e.printStackTrace();
			Toast.makeText(getApplicationContext(), getString(R.string.str_prepare_failed), Toast.LENGTH_LONG).show();
			return false;
		}

		return true;
	}

	private void startTimerTask() {
		sectionLoopTimer = new Timer();
		sectionLoopTimerTask = new TimerTask() {
			@Override
			public void run() {
				if (!isRunning) {
					sectionLoopTimer.cancel();
					sectionLoopTimer.purge();
					return;
				}

				if ((mPlaymode == PlayService.PLAYMODE_SECTION_LOOP)
						&& (getCurrentPosition() >= pointB || getCurrentPosition() < pointA))
					mPlayer.seekTo(pointA);
			}
		};

		sectionLoopTimer.schedule(sectionLoopTimerTask, 0, 50);
	}
}
