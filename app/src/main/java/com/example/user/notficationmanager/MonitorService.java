package com.example.user.notficationmanager;

import java.util.ArrayList;
import java.util.List;

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
    private CancelNotificationReceiver receiver = new CancelNotificationReceiver();

    public static List<QueuedNotification> queued_nots = new ArrayList<QueuedNotification>();
    public static MonitorService self;

    private Handler monitor_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_UPDATE_CURRENT_NOS:
                    UpdateCurrentNotifications();
                    break;
                default:
                    break;
            }
        }
    };

    class CancelNotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action;
            if (intent != null && intent.getAction() != null) {
                action = intent.getAction();
                if (action.equals(ACTION_NLS_CONTROL)) {
                    String command = intent.getStringExtra("command");
                    if (TextUtils.equals(command, "cancel_last")) {
                        if (current_notifications != null && current_notification_count >= 1) {
                            StatusBarNotification sbnn = GetCurrentNotifications()[current_notification_count - 1];
                            cancelNotification(sbnn.getPackageName(), sbnn.getTag(), sbnn.getId());
                        }
                    }
                    else if (TextUtils.equals(command, "cancel_all")) {
                        cancelAllNotifications();
                    }
                    else if (TextUtils.equals(command, "release_queue")) {
                        MonitorService.self.ReleaseQueue();
                    }
                }
            }
        }

    }

    @Override
    public void onCreate() {
        self = this;
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NLS_CONTROL);
        registerReceiver(receiver, filter);
        monitor_handler.sendMessage(monitor_handler.obtainMessage(EVENT_UPDATE_CURRENT_NOS));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {


        QueuedNotification qn = new QueuedNotification();
        qn.Set(sbn);
        queued_nots.add(qn);

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
            current_notifications.set(0, activeNos);
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
