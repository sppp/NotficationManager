package com.example.user.notficationmanager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;




public class MonitorService extends NotificationListenerService {
    private static final String TAG = "MonitorService";
    private static final String TAG_PRE = "[" + MonitorService.class.getSimpleName() + "] ";
    private static final int EVENT_UPDATE_CURRENT_NOS = 0;
    public static final String ACTION_NLS_CONTROL = "com.example.user.notficationmanager.NLSCONTROL";
    public static List<StatusBarNotification[]> current_notifications = new ArrayList<StatusBarNotification[]>();
    public static int current_notification_count = 0;
    public static StatusBarNotification posted_notification;
    public static StatusBarNotification removed_notification;

    public static List<QueuedNotification> queued_nots = new ArrayList<QueuedNotification>();
    public static MonitorService self;

    private boolean is_enabled = false;
    public int begin_hour = 0;
    public int begin_minute = 0;
    public int end_hour = 23;
    public int end_minute = 55;

    public boolean[] enabled_day = new boolean[7];

    // TIMER

    // constant
    public static final long NOTIFY_INTERVAL = 10 * 1000; // 10 seconds

    // run on another Thread to avoid crash
    private Handler mHandler = new Handler();

    // timer handling
    private Timer mTimer = null;

    class TimeDisplayTimerTask extends TimerTask {

        @Override
        public void run() {
            // run on another thread
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    MonitorService.self.Refresh();
                }

            });
        }
    }

    public void Refresh() {
        LOG("MonitorService Refresh");

        if (is_enabled) {

            // Check if it is time to release notifications
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            int wday = c.get(Calendar.DAY_OF_WEEK);
            if (    enabled_day[wday] == false ||
                    hour > end_hour ||
                    (hour == end_hour && minute >= end_minute) ) {
                ReleaseQueue();
            }
        }
    }

    public void CancelLast() {

        if (posted_notification != null) {
            android.app.NotificationManager notificationManager = (android.app.NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(posted_notification.getId());
        }
    }

    public void CancelAll() {
        cancelAllNotifications();
    }

    public void Enable(boolean enabled) {
        if (is_enabled == enabled) return; // Useless

        // Queuing was enabled, release the queue
        if (is_enabled) {
            ReleaseQueue();
        }
        // Queuing was disabled, clear existing notifications
        else {
            CancelAll();
        }

        is_enabled = enabled;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        self = this;
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NLS_CONTROL);
        UpdateCurrentNotifications();

        // Add setting slots days
        for (int i = 0; i < 7; i++) enabled_day[i] = false;

        // Start periodical refresher

        // cancel if already existed
        if(mTimer != null) {
            mTimer.cancel();
        } else {
            // recreate new
            mTimer = new Timer();
        }
        // schedule task
        mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(), 0, NOTIFY_INTERVAL);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        if (is_enabled) {

            // Check if it is time to delay notifications
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            int wday = c.get(Calendar.DAY_OF_WEEK);

            if (    enabled_day[wday] == true &&
                    (hour > begin_hour || (hour == begin_hour && minute >= begin_minute) ) &&
                    (hour < end_hour || (hour == end_hour && minute < end_minute) ) ) {
                QueuedNotification qn = new QueuedNotification();
                qn.Set(sbn);
                queued_nots.add(qn);

                // Cancel notification
                android.app.NotificationManager notificationManager = (android.app.NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(sbn.getId());
            }
        }

        UpdateCurrentNotifications();
        posted_notification = sbn;
    }


    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        UpdateCurrentNotifications();
        removed_notification = sbn;
    }

    private void UpdateCurrentNotifications() {
        try {
            StatusBarNotification[] activeNos = getActiveNotifications();
            if (current_notifications.size() == 0) {
                current_notifications.add(null);
            }
            current_notifications.set(0,activeNos );
            current_notification_count = activeNos.length;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static StatusBarNotification[] GetCurrentNotifications() {
        if (current_notifications.size() == 0) {
            return null;
        }
        return current_notifications.get(0);
    }

    public void ReleaseQueue() {
        LOG("Releasing Queue");
        List<Notification> release_list = new ArrayList<Notification>();
        for (int i = 0; i < queued_nots.size(); i++) {
            QueuedNotification qn = queued_nots.get(i);
            Notification not = qn.Get();
            release_list.add(not);
        }

        android.app.NotificationManager nmgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        for (int i = 0; i < release_list.size(); i++) {
            nmgr.notify((int)System.currentTimeMillis(), release_list.get(i));
        }

        queued_nots.clear();

    }
    private static void LOG(Object object) {
        Log.i(TAG, TAG_PRE+object);
    }

}
