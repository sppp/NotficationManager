package com.example.user.notficationmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import com.example.user.notficationmanager.R;

public class NotificationManager extends Activity {

    private static final String TAG = "NotificationManager";
    private static final String TAG_PRE = "["+NotificationManager.class.getSimpleName()+"] ";
    private static final String ENABLED_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private static final int EVENT_SHOW_CREATE = 0;
    private static final int EVENT_LIST_CURRENT = 1;
    private boolean is_enabled = false;
    private TextView text_view;
    private boolean check_service = false;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SHOW_CREATE:
                    ShowCreateNotification();
                    break;
                case EVENT_LIST_CURRENT:
                    ListCurrentNotification();
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (false) {
            setContentView(R.layout.activity_main);
        }
        else {
            setContentView(R.layout.startup);
            text_view = (TextView) findViewById(R.id.debug_log);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        is_enabled = IsEnabled();
        if (!is_enabled) {
            ShowConfirmDialog();
        }
        else {
            // Restart service (crashes)
            if (check_service == true && MonitorService.self == null) {
                startService(new Intent(NotificationManager.this, MonitorService.class));
                check_service = false;
            }
        }
    }

    void SetTimerange() {
        TimePicker tp;
        tp = (TimePicker) findViewById(R.id.begin);
        MonitorService.self.begin_hour = tp.getHour();
        MonitorService.self.begin_minute = tp.getMinute();
        tp = (TimePicker) findViewById(R.id.end);
        MonitorService.self.end_hour = tp.getHour();
        MonitorService.self.end_minute = tp.getMinute();
    }

    public void buttonOnClicked(View view) {

        // All buttons, which can be pressed BEFORE enabling notification access
        text_view.setTextColor(Color.BLACK);
        switch (view.getId()) {
            case R.id.debug_notification_settings:
                OpenNotificationAccess();
                return;
            case R.id.debug_create_notification:
                DebugCreateNotification(this);
                handler.sendMessageDelayed(handler.obtainMessage(EVENT_SHOW_CREATE), 50);
                return;
        }

        // Check notification access
        if (!is_enabled)
            is_enabled = IsEnabled();
        if (!is_enabled) {
            text_view.setTextColor(Color.RED);
            text_view.setText("Please Enable Notification Access");
            return;
        }

        if (MonitorService.self == null) {
            text_view.setTextColor(Color.RED);
            text_view.setText("Unknown error in MonitorService");
            startService(new Intent(NotificationManager.this, MonitorService.class));
            return;
        }

        // All buttons, which can be pressed AFTER enabling notification access
        CheckBox cb;
        switch (view.getId()) {
            case R.id.debug_cancel_last:
                RemoveLastNotification();
                break;
            case R.id.debug_cancel_all:
                ClearAllNotifications();
                break;
            case R.id.debug_refresh_log:
                ListCurrentNotification();
                break;
            case R.id.debug_release_queue:
                MonitorService.self.ReleaseQueue();
                break;
            case R.id.monday:
                cb = (CheckBox) findViewById(view.getId());
                MonitorService.self.enabled_day[2] = cb.isChecked();
                break;
            case R.id.tuesday:
                cb = (CheckBox) findViewById(view.getId());
                MonitorService.self.enabled_day[3] = cb.isChecked();
                break;
            case R.id.wednesday:
                cb = (CheckBox) findViewById(view.getId());
                MonitorService.self.enabled_day[4] = cb.isChecked();
                break;
            case R.id.thursday:
                cb = (CheckBox) findViewById(view.getId());
                MonitorService.self.enabled_day[5] = cb.isChecked();
                break;
            case R.id.friday:
                cb = (CheckBox) findViewById(view.getId());
                MonitorService.self.enabled_day[6] = cb.isChecked();
                break;
            case R.id.saturday:
                cb = (CheckBox) findViewById(view.getId());
                MonitorService.self.enabled_day[0] = cb.isChecked();
                break;
            case R.id.sunday:
                cb = (CheckBox) findViewById(view.getId());
                MonitorService.self.enabled_day[1] = cb.isChecked();
                break;
            case R.id.enable:
                SetTimerange();
                Switch sw = (Switch) findViewById(R.id.enable);
                MonitorService.self.Enable(sw.isChecked());
                break;
            default:
                break;
        }
    }

    private boolean IsEnabled() {
        String name = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(name, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void DebugCreateNotification(Context context) {
        android.app.NotificationManager nmgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this);
        ncomp.setContentTitle("My Notification");
        ncomp.setContentText("Notification Listener Service Example");
        ncomp.setTicker("Notification Listener Service Example");
        ncomp.setSmallIcon(android.R.drawable.sym_def_app_icon);
        ncomp.setAutoCancel(true);
        nmgr.notify((int)System.currentTimeMillis(), ncomp.build());
    }

    private void DebugCommand(Context context, String cmd) {
        Intent intent = new Intent();
        intent.setAction(MonitorService.ACTION_NLS_CONTROL);
        intent.putExtra("command", cmd);
        context.sendBroadcast(intent);
    }

    private String GetCurrentNotificationString() {
        String list = "";
        StatusBarNotification[] current = MonitorService.GetCurrentNotifications();
        if (current != null) {
            for (int i = 0; i < current.length; i++) {
                list = i +" " + current[i].getPackageName() + "\n" + list;
            }
        }
        return list;
    }

    private void ListCurrentNotification() {
        String result = "";
        if (is_enabled) {
            if (MonitorService.GetCurrentNotifications() == null)
                return;
            result = GetCurrentNotificationString();
            text_view.setText(result);
        }else {
            text_view.setTextColor(Color.RED);
            text_view.setText("Please Enable Notification Access");
        }
    }

    private void RemoveLastNotification() {
        if (is_enabled) {
            MonitorService.self.CancelLast();
        }else {
            text_view.setTextColor(Color.RED);
            text_view.setText("Please Enable Notification Access");
        }
    }

    private void ClearAllNotifications() {
        if (is_enabled) {
            MonitorService.self.CancelAll();
        }else {
            text_view.setTextColor(Color.RED);
            text_view.setText("Please Enable Notification Access");
        }
    }

    private void ShowCreateNotification() {
        if (MonitorService.posted_notification != null) {
            String result = MonitorService.posted_notification.getPackageName()+"\n"
                    + MonitorService.posted_notification.getTag()+"\n"
                    + MonitorService.posted_notification.getId()+"\n"+"\n"
                    + text_view.getText();
            result = "Create notification:"+"\n"+result;
            text_view.setText(result);
        }
    }

    private void OpenNotificationAccess() {

        // Check service after settings
        check_service = true;

        startActivity(new Intent(ACTION_SETTINGS));
    }

    private void ShowConfirmDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Please enable NotficationManager access")
                .setTitle("Notification Access")
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                OpenNotificationAccess();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // do nothing
                            }
                        })
                .create().show();
    }

    private void LOG(Object object) {
        Log.i(TAG, TAG_PRE+object);
    }
}
