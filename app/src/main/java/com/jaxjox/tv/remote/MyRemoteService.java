package com.jaxjox.tv.remote;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.android.appremote.api.error.CouldNotFindSpotifyApp;
import com.spotify.android.appremote.api.error.NotLoggedInException;
import com.spotify.android.appremote.api.error.UserNotAuthorizedException;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.ErrorCallback;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.Empty;
import com.spotify.protocol.types.Image;
import com.spotify.protocol.types.ImageUri;
import com.spotify.protocol.types.PlayerState;


import java.io.ByteArrayOutputStream;

public class MyRemoteService extends Service {
    private final String TAG = this.getClass().getSimpleName();

    public MyRemoteService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        logMessage("onCreate MyRemoteService");
        SpotifyAppRemote.setDebugMode(true);
        connect(true);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        logMessage("onUnbind MyRemoteService");
        if (mSpotifyAppRemote != null)
            SpotifyAppRemote.disconnect(mSpotifyAppRemote);
        MyRemoteService.this.mOutgoingMessenger = null;
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logMessage("onStartCommand MyRemoteService");
        return super.onStartCommand(intent, flags, startId);

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static SpotifyAppRemote mSpotifyAppRemote;

    private final ErrorCallback mErrorCallback = this::logError;

    private void logError(Throwable throwable) {
        Log.e(TAG, "", throwable);
    }

    private void logMessage(String s) {
        Log.d(TAG, "" + s);
    }


    public void playUri(String uri) {
        if (mSpotifyAppRemote != null)
            mSpotifyAppRemote
                    .getPlayerApi()
                    .play(uri)
                    .setResultCallback(new CallResult.ResultCallback<Empty>() {
                        @Override
                        public void onResult(Empty empty) {
                            logMessage("play");
                        }
                    })
                    .setErrorCallback(mErrorCallback);
    }

    private void connect(boolean showAuthView) {

        SpotifyAppRemote.disconnect(mSpotifyAppRemote);

        SpotifyAppRemote.connect(MyRemoteService.this,
                new ConnectionParams.Builder(MyRemoteService.this.getString(R.string.client_id))
                        .setRedirectUri(MyRemoteService.this.getString(R.string.redirect_uri))
                        .showAuthView(showAuthView)
                        .build(),
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        onSubscribedToPlayerStateButtonClicked();
                    }

                    @Override
                    public void onFailure(Throwable error) {

                        if (error instanceof NotLoggedInException || error instanceof UserNotAuthorizedException) {
                            // Show login button and trigger the login flow from auth library when clicked
                            Log.e(TAG, error.getMessage() + "Show login button and trigger the login flow from auth library when clicked", error);
                        } else if (error instanceof CouldNotFindSpotifyApp) {
                            // Show button to download Spotify
                            Log.e(TAG, error.getMessage() + " Show button to download Spotify", error);
                        }
                        // Something went wrong when attempting to connect! Handle errors here

                    }
                });
    }

    private static final int MSG_FROM_MSG_PLAY_URL = 0x10001;
    private static final int MSG_FROM_MSG_PLAYPAUSE = 0x10002;
    private static final int MSG_FROM_MSG_SKIPPREVIOUS = 0x10003;
    private static final int MSG_FROM_MSG_SKIPNEXT = 0x10004;
    private static final int MSG_FROM_MSG_SEEKTO = 0x10005;
    private static final int MSG_FROM_MSG_CONNECT = 0x10006;
    private static final int MSG_FROM_MSG_DISCONNECT = 0x10007;
    private static final int MSG_FROM_MSG_PAUSE = 0x11008;

    private static final int MSG_FROM_MSG_PLAYSTATE = 0x11000;
    private static final int MSG_FROM_MSG_PLAYIMG = 0x11001;


    private Messenger mOutgoingMessenger;
    @SuppressLint("HandlerLeak")
    private final Messenger mMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msgfromClient) {
            switch (msgfromClient.what) {
                //msg 客户端传来的消息
                case MSG_FROM_MSG_PLAY_URL:
                    Bundle bundle = msgfromClient.getData();
                    if (bundle.getString("playUri") != null)
                        playUri(bundle.getString("playUri"));
                    break;
                case MSG_FROM_MSG_PAUSE:
                    onPauseButtonClicked();

                    break;

                case MSG_FROM_MSG_PLAYPAUSE:
                    onPlayPauseButtonClicked();

                    break;
                case MSG_FROM_MSG_SKIPPREVIOUS:
                    onSkipPreviousButtonClicked();

                    break;
                case MSG_FROM_MSG_SKIPNEXT:
                    onSkipNextButtonClicked();

                    break;
                case MSG_FROM_MSG_SEEKTO:
                    Bundle bundleS = msgfromClient.getData();
                    seekTo(bundleS.getInt("progress"));

                    break;
                case MSG_FROM_MSG_CONNECT:
                    connect(true);
                    if (msgfromClient.replyTo != null) {
                        MyRemoteService.this.mOutgoingMessenger = msgfromClient.replyTo;
                    } else {
                    }
                    break;
                case MSG_FROM_MSG_DISCONNECT:
                    SpotifyAppRemote.disconnect(mSpotifyAppRemote);
                    MyRemoteService.this.mOutgoingMessenger = null;
                    break;


                default:
                    if (msgfromClient.replyTo != null) {
                        MyRemoteService.this.mOutgoingMessenger = msgfromClient.replyTo;
                    } else {
                    }
                    break;
            }
            super.handleMessage(msgfromClient);
        }


    });

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    String trackName;
    String artistName;
    ImageUri imageUri;
    long max;
    long duration;
    long playbackPosition;
    boolean isPaused;
    float playbackSpeed;

    /**
     * 图片转换成base64字符串
     *
     * @param bitmap
     * @return
     */
    public static String bitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imgBytes = baos.toByteArray();// 转为byte数组
        return Base64.encodeToString(imgBytes, Base64.DEFAULT);
    }

    Subscription<PlayerState> mPlayerStateSubscription;
    private final Subscription.EventCallback<PlayerState> mPlayerStateEventCallback =
            new Subscription.EventCallback<PlayerState>() {
                @Override
                public void onEvent(PlayerState playerState) {

                    if (playerState.track != null) {
                        logMessage("playerState.track.toString()" + playerState.track.toString());
                        trackName = playerState.track.name;
                        artistName = playerState.track.artist.name;
                        max = playerState.track.duration;
                        duration = playerState.track.duration;
                        playbackPosition = playerState.playbackPosition;
                        isPaused = playerState.isPaused;
                        playbackSpeed = playerState.playbackSpeed;
                        imageUri = playerState.track.imageUri;
                        sendPlayerState();
                    }

                }

            };

    private void sendPlayerState() {
        logMessage("mOutgoingMessenger" + mOutgoingMessenger);
        if (mSpotifyAppRemote != null) {
            try {
                Message message = Message.obtain();
                Bundle bundleTo = new Bundle();
                bundleTo.putFloat("playbackSpeed", playbackSpeed);
                bundleTo.putBoolean("isPaused", isPaused);
                bundleTo.putString("trackName", trackName);
                bundleTo.putString("artistName", artistName);
                bundleTo.putLong("max", max);
                bundleTo.putLong("duration", duration);
                bundleTo.putLong("playbackPosition", playbackPosition);
                message.setData(bundleTo);
                message.what = MSG_FROM_MSG_PLAYSTATE;
                mOutgoingMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (imageUri != null)
                mSpotifyAppRemote
                        .getImagesApi()
                        .getImage(imageUri, Image.Dimension.THUMBNAIL)
                        .setResultCallback(new CallResult.ResultCallback<Bitmap>() {
                            @Override
                            public void onResult(Bitmap bitmap) {
                                if (mOutgoingMessenger != null)
                                    try {
//                                                    Log.d("imageUri", bitmapToString(bitmap));
                                        Message message = Message.obtain();
                                        Bundle bundleTo = new Bundle();
                                        bundleTo.putString("imageUri", bitmapToString(bitmap));
                                        message.setData(bundleTo);
                                        message.what = MSG_FROM_MSG_PLAYIMG;
                                        mOutgoingMessenger.send(message);
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                            }
                        });

        }
    }

    public void onSubscribedToPlayerStateButtonClicked() {
        if (mPlayerStateSubscription != null && !mPlayerStateSubscription.isCanceled()) {
            mPlayerStateSubscription.cancel();
            mPlayerStateSubscription = null;
        }
        if (mSpotifyAppRemote != null)
            mPlayerStateSubscription =
                    (Subscription<PlayerState>)
                            mSpotifyAppRemote
                                    .getPlayerApi()
                                    .subscribeToPlayerState()
                                    .setEventCallback(mPlayerStateEventCallback)
                                    .setLifecycleCallback(
                                            new Subscription.LifecycleCallback() {
                                                @Override
                                                public void onStart() {
                                                    logMessage("Event: start");
                                                    sendPlayerState();

                                                }

                                                @Override
                                                public void onStop() {
                                                    logMessage("Event: end");
                                                }
                                            })
                                    .setErrorCallback(
                                            throwable -> {
                                                logError(throwable);
                                            });
    }


    public void onSkipPreviousButtonClicked() {
        if (mSpotifyAppRemote != null)
            mSpotifyAppRemote
                    .getPlayerApi()
                    .skipPrevious()
                    .setResultCallback(new CallResult.ResultCallback<Empty>() {
                        @Override
                        public void onResult(Empty empty) {
                            logMessage("skip previous");
                        }
                    })
                    .setErrorCallback(mErrorCallback);
    }

    public void onPlayPauseButtonClicked() {
        if (mSpotifyAppRemote != null)
            mSpotifyAppRemote
                    .getPlayerApi()
                    .getPlayerState()
                    .setResultCallback(
                            playerState -> {
                                if (playerState.isPaused) {
                                    mSpotifyAppRemote
                                            .getPlayerApi()
                                            .resume()
                                            .setResultCallback(new CallResult.ResultCallback<Empty>() {
                                                @Override
                                                public void onResult(Empty empty) {
                                                    logMessage("play");
                                                }
                                            })
                                            .setErrorCallback(mErrorCallback);
                                } else {
                                    mSpotifyAppRemote
                                            .getPlayerApi()
                                            .pause()
                                            .setResultCallback(new CallResult.ResultCallback<Empty>() {
                                                @Override
                                                public void onResult(Empty empty) {
                                                    logMessage("pause");
                                                }
                                            })
                                            .setErrorCallback(mErrorCallback);
                                }
                            });
    }


    public void onPauseButtonClicked() {
        if (mSpotifyAppRemote != null)
            mSpotifyAppRemote
                    .getPlayerApi()
                    .getPlayerState()
                    .setResultCallback(
                            playerState -> {
                                if (playerState.isPaused) {

                                } else {
                                    mSpotifyAppRemote
                                            .getPlayerApi()
                                            .pause()
                                            .setResultCallback(new CallResult.ResultCallback<Empty>() {
                                                @Override
                                                public void onResult(Empty empty) {
                                                    logMessage("pause");
                                                }
                                            })
                                            .setErrorCallback(mErrorCallback);
                                }
                            });
    }

    public void onSkipNextButtonClicked() {
        if (mSpotifyAppRemote != null)
            mSpotifyAppRemote
                    .getPlayerApi()
                    .skipNext()
                    .setResultCallback(new CallResult.ResultCallback<Empty>() {
                        @Override
                        public void onResult(Empty empty) {
                            logMessage("skip next");
                        }
                    })
                    .setErrorCallback(mErrorCallback);
    }

    public void seekTo(int progress) {
        if (mSpotifyAppRemote != null)
            mSpotifyAppRemote
                    .getPlayerApi()
                    .seekTo(progress)
                    .setErrorCallback(mErrorCallback);
    }


}
