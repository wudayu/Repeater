package com.wudayu.repeater.services;

import java.io.IOException;

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

	public final static String TAG = "PlayService";

	private IBinder playBinder = new PlayBinder();
	private MediaPlayer mPlayer;
	private Uri mUri;

	private int mDuration;

	@Override
	public void onCreate() {
		// initialize the player
		mPlayer = new MediaPlayer();
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mUri = intent.getData();

		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		prepare(mUri);

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
			if (mPlayer != null && mPlayer.isPlaying())
				return mPlayer.getCurrentPosition();

			return 0;
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
	}

	private Boolean prepare(Uri mUri) {
		mDuration = 0;
		mPlayer.stop();
		mPlayer.reset();

		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
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
}
