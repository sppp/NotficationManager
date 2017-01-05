package com.example.user.notficationmanager;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
//import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;
import android.support.v4.app.NotificationCompat;


public class NotificationManager extends AppCompatActivity {

    private PopupWindow popupWindow;
    private LayoutInflater layoutInflater;
    private RelativeLayout relativeLayout;


    private NotificationReceiver not_recv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start NotificaionListener service
        startService(new Intent(NotificationManager.this, NotificationListener.class));

        // Create NotificationReceiver
        not_recv = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.user.NotificationManager");
        registerReceiver(not_recv,filter);




        Button btn = (Button)findViewById(R.id.create_notification);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CreateTestNotification();
            }
        });

    }

    void CreateTestNotification() {
        android.app.NotificationManager nManager = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this);
        ncomp.setContentTitle("My Notification");
        ncomp.setContentText("Notification Listener Service Example");
        ncomp.setTicker("Notification Listener Service Example");
        ncomp.setSmallIcon(android.R.drawable.sym_def_app_icon);
        ncomp.setAutoCancel(true);
        nManager.notify((int)System.currentTimeMillis(), ncomp.build());

        Log.i("NotificationManager", "created notification");
    }

    class NotificationReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String temp = intent.getStringExtra("notification_event");
            Log.i("NotificationManager", "NotificationReceiver.onReceive: " + temp);
        }
    }
}



