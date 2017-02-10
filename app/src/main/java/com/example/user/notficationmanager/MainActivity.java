package com.example.user.notficationmanager;

import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.user.notficationmanager.R;



public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener, PopupMenu.OnMenuItemClickListener {
    private EditText countdown;
    private EditText begin;
    private EditText end;
    private TextView start_survey;
    private Button delay_now;

    private int begin_hour = 0;
    private int begin_minute = 0;
    private int end_hour = 23;
    private int end_minute = 59;
    private int view_type = 0;


    private static final String TAG = "NotificationManager";
    private static final String TAG_PRE = "["+MainActivity.class.getSimpleName()+"] ";
    private static final String ENABLED_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private static final int EVENT_SHOW_CREATE = 0;
    private static final int EVENT_LIST_CURRENT = 1;
    private boolean is_enabled = false;
    private boolean check_service = false;

    private Handler countdown_handler = new Handler();
    int countdown_delay = 500; //milliseconds

    //private Calendar calendar = Calendar.getInstance();

    private final String[] helps = {"Q: How do I add a new application to the notification manager?\n" +
            "\n" +
            "A: Press manage apps at the bottom of the main screen and choose the applications which notifications you want to manage.", "Q: How do I disable the notification management?\n" +
            "\n" +
            "A: Press the off button in the main screen."};

    void RefreshCountdown() {
        if (MonitorService.self == null)
            return;

        if (MonitorService.IsDelaying()) {



            countdown_handler.postDelayed(new Runnable() {
                public void run() {
                    //do something
                    countdown_handler.postDelayed(this, countdown_delay);
                }
            }, countdown_delay);
        }
        else {


        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        countdown = (EditText) findViewById(R.id.countdown);
        countdown.setOnClickListener(this);

        begin = (EditText) findViewById(R.id.begin);
        begin.setOnClickListener(this);
        begin.setOnLongClickListener(this);

        end = (EditText) findViewById(R.id.end);
        end.setOnClickListener(this);
        end.setOnLongClickListener(this);

        delay_now = (Button) findViewById(R.id.delay_now);

        start_survey = (TextView) findViewById(R.id.start_survey);
        start_survey.setOnClickListener(this);

        if (MonitorService.self == null) {
            startService(new Intent(MainActivity.this, MonitorService.class));
        }

        RefreshData();
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
            //if (check_service == true && MonitorService.self == null) {
            if (MonitorService.self == null) {
                startService(new Intent(MainActivity.this, MonitorService.class));
                check_service = false;
            }
        }

        RefreshData();
    }

    @Override
    public void onClick(View view) {
        if (view == this.begin) {
            view_type = 0;
            new TimePickerDialog(MainActivity.this, time, begin_hour, begin_minute, true).show();
        }
        else if (view == this.end) {
            view_type = 1;
            new TimePickerDialog(MainActivity.this, time, end_hour, end_minute, true).show();
        }
        else if (view == this.start_survey) {
            //Intent intent = new Intent(getBaseContext(), AppMenuActivity.class);
            //startActivity(intent);
        }
    }

    public void RefreshData() {
        if (MonitorService.self == null)
            return;

        begin_hour = MonitorService.begin_hour;
        begin_minute = MonitorService.begin_minute;
        end_hour = MonitorService.end_hour;
        end_minute = MonitorService.end_minute;

        ToggleButton toggle;
        toggle = (ToggleButton) findViewById(R.id.monday);
        toggle.setChecked(MonitorService.enabled_day[2]);
        toggle = (ToggleButton) findViewById(R.id.tuesday);
        toggle.setChecked(MonitorService.enabled_day[3]);
        toggle = (ToggleButton) findViewById(R.id.wednesday);
        toggle.setChecked(MonitorService.enabled_day[4]);
        toggle = (ToggleButton) findViewById(R.id.thursday);
        toggle.setChecked(MonitorService.enabled_day[5]);
        toggle = (ToggleButton) findViewById(R.id.friday);
        toggle.setChecked(MonitorService.enabled_day[6]);
        toggle = (ToggleButton) findViewById(R.id.saturday);
        toggle.setChecked(MonitorService.enabled_day[0]);
        toggle = (ToggleButton) findViewById(R.id.sunday);
        toggle.setChecked(MonitorService.enabled_day[1]);
    }


    TimePickerDialog.OnTimeSetListener time = new TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            if (view_type == 0) {
                begin_hour = hourOfDay;
                begin_minute = minute;
                String str = Integer.toString(begin_hour) + ":" + (begin_minute < 10 ? "0" : "") + Integer.toString(begin_minute);
                begin.setText(str);
            }
            else {
                end_hour = hourOfDay;
                end_minute = minute;
                String str = Integer.toString(end_hour) + ":" + (end_minute < 10 ? "0" : "") + Integer.toString(end_minute);
                end.setText(str);
            }
        }
    };


    @Override
    public boolean onLongClick(View view) {
        if (view == this.begin || view == this.end) {
            Toast.makeText(view.getContext(), "Use the format date/month/year to enter the dates when you want to delay your notifications", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return false;
        }
    }

    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        popup.setOnMenuItemClickListener(this);
        inflater.inflate(R.menu.popup_delay, popup.getMenu());
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.popup_first:
                countdown.setText("00:15");
                return true;
            case R.id.popup_second:
                countdown.setText("00:30");
                return true;
            case R.id.popup_third:
                countdown.setText("01:00");
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bar_help:
                displayHelpList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void displayHelpList() {
        ListPopupWindow helpList;
        helpList = new ListPopupWindow(MainActivity.this);
        helpList.setAdapter(new ArrayAdapter(
                MainActivity.this,
                R.layout.help, helps));
        helpList.setAnchorView(countdown);
        helpList.setWidth(800);
        helpList.setHeight(600);

        helpList.setModal(true);
        helpList.show();
    }




    public void buttonOnClicked(View view) {

        switch (view.getId()) {
            case R.id.debug_notification_settings:
                OpenNotificationAccess();
                return;
            case R.id.debug_create_notification:
                DebugCreateNotification(this);
                //handler.sendMessageDelayed(handler.obtainMessage(EVENT_SHOW_CREATE), 50);
                return;
        }

        // Check notification access
        if (!is_enabled)
            is_enabled = IsEnabled();

        if (MonitorService.self == null) {
            LOG("Unknown error in MonitorService");
            startService(new Intent(MainActivity.this, MonitorService.class));
            return;
        }

        // All buttons, which can be pressed AFTER enabling notification access
        ToggleButton toggle;
        switch (view.getId()) {
            /*
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
                break;*/
            case R.id.monday:
                toggle = (ToggleButton) findViewById(view.getId());
                MonitorService.enabled_day[2] = toggle.isChecked();
                break;
            case R.id.tuesday:
                toggle = (ToggleButton) findViewById(view.getId());
                MonitorService.enabled_day[3] = toggle.isChecked();
                break;
            case R.id.wednesday:
                toggle = (ToggleButton) findViewById(view.getId());
                MonitorService.enabled_day[4] = toggle.isChecked();
                break;
            case R.id.thursday:
                toggle = (ToggleButton) findViewById(view.getId());
                MonitorService.enabled_day[5] = toggle.isChecked();
                break;
            case R.id.friday:
                toggle = (ToggleButton) findViewById(view.getId());
                MonitorService.enabled_day[6] = toggle.isChecked();
                break;
            case R.id.saturday:
                toggle = (ToggleButton) findViewById(view.getId());
                MonitorService.enabled_day[0] = toggle.isChecked();
                break;
            case R.id.sunday:
                toggle = (ToggleButton) findViewById(view.getId());
                MonitorService.enabled_day[1] = toggle.isChecked();
                break;

            case R.id.start_survey:

                break;

            case R.id.enable:
                toggle = (ToggleButton) findViewById(view.getId());
                if (toggle.isChecked()) {
                    SetTimerange();
                    MonitorService.self.Enable(true);
                }
                else {
                    MonitorService.self.Enable(false);
                }
                break;
            default:
                break;
        }
    }

    void SetTimerange() {
        MonitorService.self.begin_hour = begin_hour;
        MonitorService.self.begin_minute = begin_minute;
        MonitorService.self.end_hour = end_hour;
        MonitorService.self.end_minute = end_minute;

        ToggleButton toggle;
        toggle = (ToggleButton) findViewById(R.id.monday);
        MonitorService.self.enabled_day[2] = toggle.isChecked();
        toggle = (ToggleButton) findViewById(R.id.tuesday);
        MonitorService.self.enabled_day[3] = toggle.isChecked();
        toggle = (ToggleButton) findViewById(R.id.wednesday);
        MonitorService.self.enabled_day[4] = toggle.isChecked();
        toggle = (ToggleButton) findViewById(R.id.thursday);
        MonitorService.self.enabled_day[5] = toggle.isChecked();
        toggle = (ToggleButton) findViewById(R.id.friday);
        MonitorService.self.enabled_day[6] = toggle.isChecked();
        toggle = (ToggleButton) findViewById(R.id.saturday);
        MonitorService.self.enabled_day[0] = toggle.isChecked();
        toggle = (ToggleButton) findViewById(R.id.sunday);
        MonitorService.self.enabled_day[1] = toggle.isChecked();
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
