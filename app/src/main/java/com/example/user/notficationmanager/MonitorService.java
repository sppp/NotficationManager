package com.example.user.notficationmanager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;



/*
    MonitorService is the background service for listening notifications.
    It handles the timed delaying and works while MainActivity is paused.
 */
public class MonitorService extends NotificationListenerService {

    private SharedPreferences preferences;
    private static boolean is_enabled = false;
    private static boolean enable_timer = false;
    private static final String TAG = "MonitorService";
    private static final String TAG_PRE = "[" + MonitorService.class.getSimpleName() + "] ";
    public static final String ACTION_NLS_CONTROL = "com.example.user.notficationmanager.NLSCONTROL";
    public static List<StatusBarNotification[]> current_notifications = new ArrayList<StatusBarNotification[]>();
    public static int current_notification_count = 0;
    public static StatusBarNotification posted_notification;
    public static List<QueuedNotification> queued_nots = new ArrayList<QueuedNotification>();
    public static MonitorService self;
    public static int begin_hour = 0;
    public static int begin_minute = 0;
    public static int end_hour = 23;
    public static int end_minute = 55;
    public static int countdown_begin_hour = 23;
    public static int countdown_begin_minute = 55;
    public static int countdown_end_hour = 23;
    public static int countdown_end_minute = 59;
    public static int countdown_end_second = 59;
    public static boolean[] enabled_day = new boolean[7];// constant
    public static final long NOTIFY_INTERVAL = 10 * 1000; // 10 seconds
    private Handler mHandler = new Handler();
    private Timer mTimer = null;
    private Notification icon;
    private int icon_id = 0;


    // GetCountdown returns time in seconds until queue is released.
    static int GetCountdown() {
        if (!is_enabled) return 0;
        Calendar c = Calendar.getInstance();
        int second = c.get(Calendar.SECOND);
        int minute = c.get(Calendar.MINUTE);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int second_of_day = hour * 60 * 60 + minute * 60 + second;
        int end_of_day = enable_timer == false ?
                end_hour * 60 * 60 + end_minute * 60:
                countdown_end_hour * 60 * 60 + countdown_end_minute * 60 + countdown_end_second;
        int seconds_left = end_of_day - second_of_day;
        if (seconds_left < 0) seconds_left += 24 * 60 * 60;
        return seconds_left;
    }

    // IsDelaying checks if delaying of notifications is currently active.
    static boolean IsDelaying() {
        if (!is_enabled)
            return false;

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        if (enable_timer == false) {

            // Check if it is time to delay notifications
            int wday = c.get(Calendar.DAY_OF_WEEK);
            if (wday == 7) wday = 0;

            if (enabled_day[wday] == true &&
                    (hour > begin_hour || (hour == begin_hour && minute >= begin_minute)) &&
                    (hour < end_hour || (hour == end_hour && minute < end_minute))) {
                return true;
            }
        }
        else {
            if (
                    (hour > countdown_begin_hour || (hour == countdown_begin_hour && minute >= countdown_begin_minute)) &&
                    (hour < countdown_end_hour || (hour == countdown_end_hour && minute < countdown_end_minute))) {
                return true;
            }
        }

        return false;
    }

    // TimeDisplayTimerTask periodically checks for begin or end of notification delay.
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

    // Routine function to check the delaying.
    public void Refresh() {
        LOG("MonitorService Refresh");

        // Release notification queue if delaying is not active.
        if (is_enabled) {
            if (!IsDelaying()) {
                ReleaseQueue();
            }
        }

        // Show icon in status bar while delaying is active.
        RefreshStatusIcon();

        // Refresh GUI's countdown if it is running. (Could be done in MainActivity thread, but this works too)
        if (MainActivity.self != null)
            MainActivity.self.RefreshCountdown();
    }

    // RefreshStatusIcon shows icon in status bar while delaying is active.
    public void RefreshStatusIcon() {
        if (IsDelaying()) {
            ShowIcon();
        } else {
            HideIcon();
        }
    }

    // HideIcon cancels the icon-notification.
    public void HideIcon() {
        if (icon == null) return;

        android.app.NotificationManager notificationManager = (android.app.NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(icon_id);

        icon_id = 0;
        icon = null;
    }

    // ShowIcon sends a notification without auto-cancel and with on-going attribute.
    // The icon notification is not added to the delaying queue.
    public void ShowIcon() {
        if (icon != null) return;

        android.app.NotificationManager nmgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        android.support.v7.app.NotificationCompat.Builder ncomp = new android.support.v7.app.NotificationCompat.Builder(this);
        ncomp.setContentTitle("Notification delaying is enabled");
        ncomp.setContentText("");
        ncomp.setTicker("Notification delaying is enabled");
        ncomp.setSmallIcon(R.drawable.status_bar_icon);
        ncomp.setAutoCancel(false);
        ncomp.setOngoing(true);

        icon = ncomp.build();
        icon_id = (int)System.currentTimeMillis();
        nmgr.notify(icon_id, icon);
    }

    // CancelLast removes the last notification.
    public void CancelLast() {
        if (posted_notification != null) {
            android.app.NotificationManager notificationManager = (android.app.NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(posted_notification.getId());
        }
    }

    // CancelAll removes all open notifications.
    public void CancelAll() {
        cancelAllNotifications();
    }

    // StartTimer starts delaying of notifications instantly, until given time is passed.
    public void StartTimer(int minutes) {
        is_enabled = true;
        enable_timer = true;

        Calendar c = Calendar.getInstance();
        int second = c.get(Calendar.SECOND);
        int minute = c.get(Calendar.MINUTE);
        int hour = c.get(Calendar.HOUR_OF_DAY);

        int second_of_day  = second + minute * 60 + hour * 60 * 60;
        second_of_day += minutes * 60;

        while (second_of_day  >= 24*60*60) second_of_day  -= 24*60*60;

        countdown_begin_hour = hour;
        countdown_begin_minute = minute;
        countdown_end_hour = second_of_day / 60 / 60;
        countdown_end_minute = (second_of_day / 60) % 60;
        countdown_end_second = second_of_day % 60;

        RefreshStatusIcon();
    }

    // Enable starts or stops the timed delaying, which uses the begin and end times and weekdays.
    public void Enable(boolean enabled) {
        if (is_enabled == enabled && enable_timer == false) return; // Useless

        // Don't enable instant delaying.
        enable_timer = false;

        // Queuing was enabled, release the queue
        if (is_enabled) {
            ReleaseQueue();
        }

        // Queuing was disabled, clear existing notifications
        else {
            CancelAll();

            // Store settings
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("saturday", enabled_day[0]);
            editor.putBoolean("sunday", enabled_day[1]);
            editor.putBoolean("monday", enabled_day[2]);
            editor.putBoolean("tuesday", enabled_day[3]);
            editor.putBoolean("wednesday", enabled_day[4]);
            editor.putBoolean("thursday", enabled_day[5]);
            editor.putBoolean("friday", enabled_day[6]);
            editor.putInt("begin_hour", begin_hour);
            editor.putInt("begin_minute", begin_minute);
            editor.putInt("end_hour", end_hour);
            editor.putInt("end_minute", end_minute);
            editor.commit();
        }

        is_enabled = enabled;

        RefreshStatusIcon();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Send START_STICKY to left service in "started" state and allow restarting.
        return START_STICKY;
    }

    // onCreate is being called after the service has been created.
    @Override
    public void onCreate() {
        self = this;
        super.onCreate();

        // Get persistent settings
        preferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        enabled_day[0] = preferences.getBoolean("saturday", false);
        enabled_day[1] = preferences.getBoolean("sunday", false);
        enabled_day[2] = preferences.getBoolean("monday", true);
        enabled_day[3] = preferences.getBoolean("tuesday", true);
        enabled_day[4] = preferences.getBoolean("wednesday", true);
        enabled_day[5] = preferences.getBoolean("thursday", true);
        enabled_day[6] = preferences.getBoolean("friday", true);
        begin_hour = preferences.getInt("begin_hour", 0);
        begin_minute = preferences.getInt("begin_minute", 0);
        end_hour = preferences.getInt("end_hour", 23);
        end_minute = preferences.getInt("end_minute", 59);


        // Start periodical refresher
        if(mTimer != null) {
            mTimer.cancel();
        } else {
            mTimer = new Timer();
        }
        mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(), 0, NOTIFY_INTERVAL);

        // Test listening a notification.
        // After updating to new version, the previous permission is not transferred to the new app,
        // and requires user to re-enable the permission.
        if (MainActivity.self != null) {
            MainActivity.self.RefreshData();
            MainActivity.self.TestListenerAndConfirm();
        }

    }

    // These functions are here to avoid some weird bug, which is now unfortunately forgotten.
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }


    // onNotificationPosted is being called when the application has permission to listen notifications
    // and a notification is posted to the system.
    // In older Android systems (< 23 API) the notification have already been shown to the user,
    // and it is too late to cancel them in this point.
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        // Skip this service's notification
        if (sbn.getId() == icon_id)
            return;

        // If delaying is active, then add the notification to the queue and cancel current notification.
        if (IsDelaying()) {
            QueuedNotification qn = new QueuedNotification();
            qn.Set(sbn);
            queued_nots.add(qn);

            // Cancel notification
            android.app.NotificationManager notificationManager = (android.app.NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(sbn.getId());
        }

        UpdateCurrentNotifications();
        posted_notification = sbn;
    }

    // ReleaseQueue shows all delayed notifications.
    public void ReleaseQueue() {
        LOG("Releasing Queue");

        // Get notifications to separate list to avoid getting pushed notifications into queue gain.
        List<Notification> release_list = new ArrayList<Notification>();
        for (int i = 0; i < queued_nots.size(); i++) {
            QueuedNotification qn = queued_nots.get(i);
            Notification not = qn.Get();
            release_list.add(not);
        }

        // Show all queued notifications
        android.app.NotificationManager nmgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        for (int i = 0; i < release_list.size(); i++) {
            nmgr.notify((int)System.currentTimeMillis(), release_list.get(i));
        }

        // Queue might have been re-filled with notifications, so clear it again.
        // This is not clean as it could be, as re-filling could be avoided with other means, but it works.
        queued_nots.clear();

    }

    // UpdateCurrentNotifications gets the current notification list, which is required for
    // testing the listening of notifications.
    private void UpdateCurrentNotifications() {
        try {
            StatusBarNotification[] activeNos = getActiveNotifications();
            if (activeNos == null) {
                LOG("ERROR: activeNos == null");
                return;
            }
            if (current_notifications.size() == 0) {
                current_notifications.add(null);
            }
            current_notifications.set(0,activeNos );
            current_notification_count = activeNos.length;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // GetCurrentNotifications returns the current list of notifications.
    public static StatusBarNotification[] GetCurrentNotifications() {
        if (current_notifications.size() == 0) {
            return null;
        }
        return current_notifications.get(0);
    }


    // Utility function for logging purposes.
    private static void LOG(Object object) {
        Log.i(TAG, TAG_PRE+object);
    }

}
