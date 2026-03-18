package com.painless.pc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import com.painless.pc.singleton.Globals;

public abstract class PriorityService extends Service {

  public static final String CHANNEL_ID = "pt_service_channel";

  private static final String STOP_INTENT_PREFIX = "com.painless.ps.";

  private final int mNotificationId;
  private final String mAction;

  private boolean mRegistered;

  private final BroadcastReceiver mStopReceiver = new BroadcastReceiver() {
    
    @Override
    public void onReceive(Context context, Intent intent) {
      stopSelf();
    }
  };

  PriorityService(int notificationId, String action) {
    mNotificationId = notificationId;
    mAction = action;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  protected void broadcastState() {
    Globals.sendCustomAction(this, mAction);
  }

  private void ensureChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      if (nm.getNotificationChannel(CHANNEL_ID) == null) {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "PowerToggles Services",
            NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Active toggle services");
        nm.createNotificationChannel(channel);
      }
    }
  }

  protected void maybeShowNotification(String key, boolean dValue, int icon, int title, int subtitle, boolean listenToDeviceLock) {
    ensureChannel();
    IntentFilter stopFilter = new IntentFilter();
    if (listenToDeviceLock) {
      stopFilter.addAction(Intent.ACTION_SCREEN_OFF);
      mRegistered = true;
    }
    
    if (!Globals.getAppPrefs(this).getBoolean(key, dValue)) {
      String stopIntent = STOP_INTENT_PREFIX + key;

      int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
          ? PendingIntent.FLAG_IMMUTABLE : 0;

      Notification.Builder builder = new Notification.Builder(this)
          .setSmallIcon(icon)
          .setContentTitle(getString(title))
          .setContentText(getString(subtitle))
          .setContentIntent(PendingIntent.getBroadcast(this, 0, new Intent(stopIntent), piFlags))
          .setOngoing(true);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        builder.setChannelId(CHANNEL_ID);
      }

      startForeground(mNotificationId, builder.build());

      stopFilter.addAction(stopIntent);
      mRegistered = true;
    }
    if (mRegistered) {
      registerReceiver(mStopReceiver, stopFilter);
    }
  }

  protected void clearNotification() {
    if (mRegistered) {
      unregisterReceiver(mStopReceiver);
    }
    ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(mNotificationId);
  }
}
