package com.example.user.notficationmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationListener extends NotificationListenerService {

    private ServiceReceiver service_receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        service_receiver = new ServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.user.NotificationListener");
        registerReceiver(service_receiver,filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(service_receiver);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.i("NotificationListener", ".onNotificationPosted");
        Log.i("NotificationListener","ID :" + sbn.getId() + "t" + sbn.getNotification().tickerText + "t" + sbn.getPackageName());
        Intent i = new  Intent("com.example.user.NotificationManager");
        i.putExtra("notification_event","onNotificationPosted :" + sbn.getPackageName() + "n");
        sendBroadcast(i);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i("NotificationListener", ".onNOtificationRemoved");
        Log.i("NotificationListener","ID :" + sbn.getId() + "t" + sbn.getNotification().tickerText +"t" + sbn.getPackageName());
        Intent i = new  Intent("com.example.user.NotificationManager");
        i.putExtra("notification_event",".onNotificationRemoved :" + sbn.getPackageName() + "n");
        sendBroadcast(i);
    }

    class ServiceReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("command").equals("clearall")){
                NotificationListener.this.cancelAllNotifications();
            }
            else if(intent.getStringExtra("command").equals("list")){
                Intent i1 = new  Intent("com.example.user.NotificationManager");
                i1.putExtra("notification_event","=====================");
                sendBroadcast(i1);
                int i=1;
                for (StatusBarNotification sbn : NotificationListener.this.getActiveNotifications()) {
                    Intent i2 = new  Intent("com.example.user.NotificationManager");
                    i2.putExtra("notification_event",i +" " + sbn.getPackageName() + "n");
                    sendBroadcast(i2);
                    i++;
                }
                Intent i3 = new  Intent("com.example.user.NotificationManager");
                i3.putExtra("notification_event","===== Notification List ====");
                sendBroadcast(i3);
            }
        }
    }
}
