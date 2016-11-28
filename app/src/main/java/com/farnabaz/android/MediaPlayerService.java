package com.farnabaz.android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.support.v7.app.NotificationCompat.MediaStyle;
import android.util.Log;

import com.farnabaz.android.radiogeek.MainActivity;
import com.farnabaz.android.radiogeek.R;

import java.io.IOException;

/**
 * Created by paulruiz on 10/28/14.
 */
public class MediaPlayerService extends Service {

	public static final String ACTION_PLAY = "action_play";
	public static final String ACTION_PAUSE = "action_pause";
	public static final String ACTION_REWIND = "action_rewind";
	public static final String ACTION_FAST_FORWARD = "action_fast_foward";
	public static final String ACTION_NEXT = "action_next";
	public static final String ACTION_PREVIOUS = "action_previous";
	public static final String ACTION_STOP = "action_stop";

	public static final String EXTRA_TITLE = "__title";
	public static final String EXTRA_PATH = "__path";
	public static final String EXTRA_NUM = "__num";

	private static final int NOTIFICATION_ID = 324812133;

	private MediaPlayer mMediaPlayer;
	private MediaSessionCompat mSession;
	private MediaControllerCompat mController;
	private String oth;
	private MediaStyle style;
	private NotificationManager notificationManager;
	private PendingIntent pendingIntent;
	private NotificationCompat.Builder mNotificationBuilder;

	private BroadcastReceiver destructorReceiver = new  BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Intent mediaIntent = new Intent(getApplicationContext(),
					MediaPlayerService.class);
			stopService(mediaIntent);
		}
	};
	private final String INTENT_FILTER = "DESTROY_MEDIA_PLAYER_NOW!";

	@Override
	public void onCreate() {
		super.onCreate();
		registerReceiver(destructorReceiver, new IntentFilter(INTENT_FILTER));
		style = new MediaStyle();
		style.setShowActionsInCompactView(0);
		style.setShowCancelButton(true);
		style.setCancelButtonIntent(PendingIntent.getBroadcast(
				getApplicationContext(), 1, new Intent(INTENT_FILTER), 0));

		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		pendingIntent = PendingIntent.getActivity(
				getApplicationContext(), 1, new Intent(getApplicationContext(), MainActivity.class), 0);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}



	private void handleIntent(Intent intent) {
		if (intent == null || intent.getAction() == null)
			return;

		String action = intent.getAction();

		if (action.equalsIgnoreCase(ACTION_PLAY)) {
			mController.getTransportControls().play();
		} else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
			mController.getTransportControls().pause();
		} else if (action.equalsIgnoreCase(ACTION_FAST_FORWARD)) {
			mController.getTransportControls().fastForward();
		} else if (action.equalsIgnoreCase(ACTION_REWIND)) {
			mController.getTransportControls().rewind();
		} else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
			mController.getTransportControls().skipToPrevious();
		} else if (action.equalsIgnoreCase(ACTION_NEXT)) {
			mController.getTransportControls().skipToNext();
		} else if (action.equalsIgnoreCase(ACTION_STOP)) {
			mController.getTransportControls().stop();
		}
	}

	private Action generateAction(int icon, String title, String intentAction) {
		return new NotificationCompat.Action.Builder(icon, title, getPendingIntent(intentAction))
				.build();
	}

	private void buildNotification(NotificationCompat.Action action) {
		if (mNotificationBuilder == null) {
			mNotificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
					.setSmallIcon(R.drawable.ic_play)
					.setShowWhen(false)
					.setOngoing(true)
					.setLargeIcon(BitmapFactory.decodeResource(getResources(),
							R.drawable.ic_player_showcase_image))
					.setContentTitle("RadioGeek " + mId).setContentText(mTitle)
					.setContentIntent(pendingIntent).setDeleteIntent(pendingIntent)
					.setStyle(style.setMediaSession(mSession.getSessionToken()));
		} else {
			mNotificationBuilder.mActions.clear();
		}

		mNotificationBuilder.addAction(action);

		notificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
	}


	@Override
	public void onDestroy() {
//		release media player
		mMediaPlayer.release();
		mSession.release();
//		cancel notification
		notificationManager.cancel(NOTIFICATION_ID);
//		unregister receiver
		unregisterReceiver(destructorReceiver);
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (mSession == null) {
			initMediaSessions();
			if (intent != null && intent.getExtras() != null) {
				oth = intent.getExtras().getString(EXTRA_PATH);
				mTitle = intent.getExtras().getString(EXTRA_TITLE);
				mId = intent.getExtras().getInt(EXTRA_NUM);
				mMediaPlayer.reset();
				try {
					mMediaPlayer.setDataSource(oth);
					mMediaPlayer.prepare();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		handleIntent(intent);
		return super.onStartCommand(intent, flags, startId);
	}

	ComponentName mEventReceiver;
	PendingIntent mMediaPendingIntent;
	String mTitle;
	int mId;

	private void initMediaSessions() {
		mMediaPlayer = new MediaPlayer();

		mEventReceiver = new ComponentName(getPackageName(),
				MediaPlayerService.class.getName());

		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setComponent(mEventReceiver);
		mMediaPendingIntent = PendingIntent.getBroadcast(this, 0,
				mediaButtonIntent, 0);
		mSession = new MediaSessionCompat(getApplicationContext(),
				"simple player session", mEventReceiver, mMediaPendingIntent);
		try {
			mController = new MediaControllerCompat(getApplicationContext(),
					mSession.getSessionToken());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
		stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY
				| PlaybackStateCompat.ACTION_PLAY_PAUSE
				| PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
				| PlaybackStateCompat.ACTION_PAUSE);
		stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 1);
		mSession.setPlaybackState(stateBuilder.build());
		mSession.setCallback(new MediaSessionCompat.Callback() {
			@Override
			public void onPlay() {
				super.onPlay();
				Log.e("MediaPlayerService", "onPlay" + mMediaPlayer.isPlaying());
				buildNotification(generateAction(R.drawable.ic_pause, "Pause",
						ACTION_PAUSE));
				mMediaPlayer.start();
			}


			@Override
			public void onPause() {
				super.onPause();
				Log.e("MediaPlayerService",
						"onPause" + mMediaPlayer.isPlaying());
				mMediaPlayer.pause();
				buildNotification(generateAction(R.drawable.ic_play, "Play",
						ACTION_PLAY));
			}

			@Override
			public void onStop() {
				super.onStop();
				Log.e("MediaPlayerService", "onStop");
				// Stop media player here

//				 Intent intent = new Intent(getApplicationContext(),
//				 MediaPlayerService.class);
//				 stopService(intent);
			}

			// @Override
			// public void onSetRating(Rating rating) {
			// super.onSetRating(rating);
			// }
		});
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mSession.release();
		return super.onUnbind(intent);
	}

	private PendingIntent getPendingIntent(String action) {
		Intent mediaIntent = new Intent(getApplicationContext(),
					MediaPlayerService.class);
		mediaIntent.setAction(action);
		return PendingIntent.getService(
				getApplicationContext(), 1, mediaIntent, 0);
	}
}