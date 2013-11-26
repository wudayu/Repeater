package com.wudayu.repeater;

import java.io.IOException;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class MainActivity extends Activity implements OnPreparedListener, OnErrorListener, OnCompletionListener {

	private final static String TAG = "Repeater MainActivity";
    private Uri mUri;
    private SeekBar mSeekBar;
    private int mDuration;
    private PreviewPlayer mPlayer;
    private Handler mProgressRefresher;
    private AudioManager mAudioManager;
    private long mMediaId = -1;
    private boolean mSeeking = false;
    private static final int OPEN_IN_MUSIC = 1;
    private boolean mPausedByTransientLossOfFocus;

    private Button btnApoint;
    private Button btnBpoint;
    private Button btnPlayHold;
    private int pointA;
    private int pointB;
    private boolean doingRepeat;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        mUri = intent.getData();
        if (mUri == null) {
            finish();
            return;
        }
        String scheme = mUri.getScheme();
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		if (scheme.equals("http")) {
            Toast.makeText(this, "Host : " + mUri.getHost(), Toast.LENGTH_SHORT).show();
            finish();
        }

        mSeekBar = (SeekBar) findViewById(R.id.progress);
        mProgressRefresher = new Handler();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        PreviewPlayer player = (PreviewPlayer) getLastNonConfigurationInstance();
        if (player == null) {
            mPlayer = new PreviewPlayer();
            mPlayer.setActivity(this);
            try {
                mPlayer.setDataSourceAndPrepare(mUri);
            } catch (Exception ex) {
                // catch generic Exception, since we may be called with a media
                // content URI, another content provider's URI, a file URI,
                // an http URI, and there are different exceptions associated
                // with failure to open each of those.
                Log.d(TAG, "Failed to open file: " + ex);
                Toast.makeText(this, R.string.playback_failed, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            mPlayer = player;
            mPlayer.setActivity(this);
            if (mPlayer.isPrepared()) {
                showPostPrepareUI();
            }
        }

        doingRepeat = false;
        btnApoint = (Button) findViewById(R.id.btnApoint);
        btnBpoint = (Button) findViewById(R.id.btnBpoint);
        btnPlayHold = (Button) findViewById(R.id.btnPlayHold);

        btnApoint.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pointA = mPlayer.getCurrentPosition();
			}
		});
        btnBpoint.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pointB = mPlayer.getCurrentPosition();
				if (pointA > 0 && pointB < mDuration) {
					doingRepeat = true;
				}
			}
		});
        btnPlayHold.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mPlayer.isPlaying()) {
					mPlayer.pause();
					doingRepeat = false;
				} else {
					mPlayer.start();
				}
			}
		});

        AsyncQueryHandler mAsyncQueryHandler = new AsyncQueryHandler(getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (cursor != null && cursor.moveToFirst()) {

                    int titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int idIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    int displaynameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                    if (idIdx >=0) {
                        mMediaId = cursor.getLong(idIdx);
                    }
                    
                    if (titleIdx >= 0) {
                        String title = cursor.getString(titleIdx);
                        //mTextLine1.setText(title);
                        if (artistIdx >= 0) {
                            String artist = cursor.getString(artistIdx);
                            //mTextLine2.setText(artist);
                        }
                    } else if (displaynameIdx >= 0) {
                        String name = cursor.getString(displaynameIdx);
                        //mTextLine1.setText(name);
                    } else {
                        // Couldn't find anything to display, what to do now?
                        Log.w(TAG, "Cursor had no names for us");
                    }
                } else {
                    Log.w(TAG, "empty cursor");
                }

                if (cursor != null) {
                    cursor.close();
                }
                //setNames();
            }
        };

        if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            if (mUri.getAuthority() == MediaStore.AUTHORITY) {
                // try to get title and artist from the media content provider
                mAsyncQueryHandler.startQuery(0, null, mUri, new String [] {
                        MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST},
                        null, null, null);
            } else {
                // Try to get the display name from another content provider.
                // Don't specifically ask for the display name though, since the
                // provider might not actually support that column.
                mAsyncQueryHandler.startQuery(0, null, mUri, null, null, null, null);
            }
        } else if (scheme.equals("file")) {
            // check if this file is in the media database (clicking on a download
            // in the download manager might follow this path
            String path = mUri.getPath();
            mAsyncQueryHandler.startQuery(0, null,  MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String [] {MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST},
                    MediaStore.Audio.Media.DATA + "=?", new String [] {path}, null);
        } else {
            // We can't get metadata from the file/stream itself yet, because
            // that API is hidden, so instead we display the URI being played
            if (mPlayer.isPrepared()) {
                //setNames();
            }
        }
	}

	@Override
    public Object onRetainNonConfigurationInstance() {
        PreviewPlayer player = mPlayer;
        mPlayer = null;
        return player;
    }

    @Override
    public void onDestroy() {
        stopPlayback();
        super.onDestroy();
    }

    private void stopPlayback() {
        if (mProgressRefresher != null) {
            mProgressRefresher.removeCallbacksAndMessages(null);
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            mAudioManager.abandonAudioFocus(mAudioFocusListener);
        }
    }

    @Override
    public void onUserLeaveHint() {
        stopPlayback();
        finish();
        super.onUserLeaveHint();
    }

	@Override
	public void onCompletion(MediaPlayer mp) {
		mSeekBar.setProgress(mDuration);
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		Toast.makeText(this, R.string.playback_failed, Toast.LENGTH_SHORT).show();
        finish();
        return true;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		if (isFinishing())
			return;

        mPlayer = (PreviewPlayer) mp;
        mPlayer.start();
        showPostPrepareUI();
	}

	private void showPostPrepareUI() {
        ProgressBar pb = (ProgressBar) findViewById(R.id.spinner);
        pb.setVisibility(View.GONE);
        mDuration = mPlayer.getDuration();
        if (mDuration != 0) {
            mSeekBar.setMax(mDuration);
            mSeekBar.setVisibility(View.VISIBLE);
        }
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);
        //mLoadingText.setVisibility(View.GONE);
        //View v = findViewById(R.id.titleandbuttons);
        //v.setVisibility(View.VISIBLE);
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        mProgressRefresher.postDelayed(new ProgressRefresher(), 50);
        //updatePlayPause();
    }

	private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (mPlayer == null) {
                // this activity has handed its MediaPlayer off to the next activity
                // (e.g. portrait/landscape switch) and should abandon its focus
                mAudioManager.abandonAudioFocus(this);
                return;
            }
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    mPausedByTransientLossOfFocus = false;
                    mPlayer.pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (mPlayer.isPlaying()) {
                        mPausedByTransientLossOfFocus = true;
                        mPlayer.pause();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mPausedByTransientLossOfFocus) {
                        mPausedByTransientLossOfFocus = false;
                        start();
                    }
                    break;
            }
            //updatePlayPause();
        }
    };

	private static class PreviewPlayer extends MediaPlayer implements OnPreparedListener {
        MainActivity mActivity;
        boolean mIsPrepared = false;

        public void setActivity(MainActivity activity) {
            mActivity = activity;
            setOnPreparedListener(this);
            setOnErrorListener(mActivity);
            setOnCompletionListener(mActivity);
        }

        public void setDataSourceAndPrepare(Uri uri) throws IllegalArgumentException,
                        SecurityException, IllegalStateException, IOException {
            setDataSource(mActivity,uri);
            prepareAsync();
        }

        /* (non-Javadoc)
         * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
         */
        @Override
        public void onPrepared(MediaPlayer mp) {
            mIsPrepared = true;
            mActivity.onPrepared(mp);
        }

        boolean isPrepared() {
            return mIsPrepared;
        }
    }
    
    private void start() {
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        mPlayer.start();
        mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
    }

    class ProgressRefresher implements Runnable {

        public void run() {
            if (mPlayer != null && !mSeeking && mDuration != 0) {
                int progress = mPlayer.getCurrentPosition() / mDuration;
                if (doingRepeat && mPlayer.getCurrentPosition() > pointB) {
                	mSeekBar.setProgress(pointA);
                	mPlayer.seekTo(pointA);
                } else
                	mSeekBar.setProgress(mPlayer.getCurrentPosition());
            }
            mProgressRefresher.removeCallbacksAndMessages(null);
            mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
        }

        /*
        public void run() {
            if (mPlayer != null && !mSeeking && mDuration != 0) {
                int progress = mPlayer.getCurrentPosition() / mDuration;
                mSeekBar.setProgress(mPlayer.getCurrentPosition());
            }
            mProgressRefresher.removeCallbacksAndMessages(null);
            mProgressRefresher.postDelayed(new ProgressRefresher(), 200);
        }
        */
    }

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mSeeking = true;
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                return;
            }
            // Protection for case of simultaneously tapping on seek bar and exit
            if (mPlayer == null) {
                return;
            }
            mPlayer.seekTo(progress);
        }
        public void onStopTrackingTouch(SeekBar bar) {
            mSeeking = false;
        }
    };

    public void playPauseClicked(View v) {
        // Protection for case of simultaneously tapping on play/pause and exit
        if (mPlayer == null) {
            return;
        }
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            btnPlayHold.setText(getString(R.string.btnPlayHoldStateHold));
        } else {
            start();
            btnPlayHold.setText(getString(R.string.btnPlayHoldStatePlaying));
        }
        //updatePlayPause();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // TODO: if mMediaId != -1, then the playing file has an entry in the media
        // database, and we could open it in the full music app instead.
        // Ideally, we would hand off the currently running mediaplayer
        // to the music UI, which can probably be done via a public static
        // getMenuInflater().inflate(R.menu.main, menu);
        menu.add(0, OPEN_IN_MUSIC, 0, "open in music");
        return true;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                } else {
                    start();
                }
                //updatePlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                start();
                //updatePlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                }
                //updatePlayPause();
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_BACK:
                stopPlayback();
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
