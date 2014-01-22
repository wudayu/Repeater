package com.wudayu.repeater.services;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


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

		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		if (!isSame)
			prepare(mUri);

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
		mPlayer.release();
		mPlayer = null;

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

		public boolean isPlaying() {
			if (mPlayer != null)
				return mPlayer.isPlaying();
			else
				return false;
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

		public Uri getUri() {
			return mUri;
		}

	}

	public int getCurrentPosition() {
		if (mPlayer != null && mPlayer.isPlaying())
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
						break;
				}
			}
		});

		try {
			mPlayer.setDataSource(getApplicationContext(), mUri);
			mPlayer.prepare();
			mDuration = mPlayer.getDuration();
			mPlayer.start();
		} catch (IllegalArgumentException e) {
			Log.w(TAG, e.toString());
			e.printStackTrace();
			return false;
		} catch (SecurityException e) {
			Log.w(TAG, e.toString());
			e.printStackTrace();
			return false;
		} catch (IllegalStateException e) {
			Log.w(TAG, e.toString());
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			Log.w(TAG, e.toString());
			e.printStackTrace();
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

		sectionLoopTimer.scheduleAtFixedRate(sectionLoopTimerTask, 0, 10);
	}
}
