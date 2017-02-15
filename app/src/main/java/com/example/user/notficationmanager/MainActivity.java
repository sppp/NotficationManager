package com.example.user.notficationmanager;

import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;
import android.provider.Settings.Secure;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    // Access MainActivity object from global context to keep things simple enough
    static public MainActivity self;

    // Used variables
    private Handler countdown_handler = new Handler();
    private EditText countdown;
    private EditText begin;
    private EditText end;
    private TextView start_survey;
    private int begin_hour = 0;
    private int begin_minute = 0;
    private int end_hour = 23;
    private int end_minute = 59;
    private int view_type = 0;
    private int countdown_delay = 500; //milliseconds
    private boolean is_enabled = false;
    private String survey_str = "";
    private String answer_url = "";
    private Thread answer_thread;
    private Thread survey_thread;


    // Tags for logging purposes
    private static final String TAG = "Notification Delayer";
    private static final String TAG_PRE = "["+MainActivity.class.getSimpleName()+"] ";
    private static final String ENABLED_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";


    // Refresh countdown label, which shows how much time is left before notifications are released
    void RefreshCountdown() {

        // Require running service
        if (MonitorService.self == null)
            return;

        // Update time only if delaying is currently active
        if (MonitorService.IsDelaying()) {

            // Get components of countdown time
            int seconds_left = MonitorService.self.GetCountdown();
            int hour_left = seconds_left / 60 / 60;
            int minute_left = (seconds_left / 60) % 60;
            int second_left = seconds_left % 60;

            // Get countdown time as string
            String countdown_str =
                    Integer.toString(hour_left) + ":" +
                    (minute_left < 10 ? "0" : "") + Integer.toString(minute_left) + ":" +
                    (second_left < 10 ? "0" : "") + Integer.toString(second_left);
            countdown.setText(countdown_str);

            // Call this RefreshCountdown function periodically while delaying is active
            countdown_handler.postDelayed(new Runnable() {
                public void run() {
                    if (MainActivity.self != null)
                        MainActivity.self.RefreshCountdown();
                }
            }, countdown_delay);
        }
        else {

            // Reset label and don't call this function again
            countdown.setText("0:00");
        }
    }

    // onCreate is the first function to be called and it sets the main view.
    protected void onCreate(Bundle savedInstanceState) {
        self = this;
        super.onCreate(savedInstanceState);
        SetMainView();
    }

    // SetMainView gets references to GUI widgets and sets their functionality.
    // Xml attributes could also be used.
    private void SetMainView() {
        setContentView(R.layout.activity_main);

        countdown = (EditText) findViewById(R.id.countdown);
        countdown.setOnClickListener(this);

        begin = (EditText) findViewById(R.id.begin);
        begin.setOnClickListener(this);

        end = (EditText) findViewById(R.id.end);
        end.setOnClickListener(this);

        start_survey = (TextView) findViewById(R.id.start_survey);
        start_survey.setOnClickListener(this);

        // Check that background service is running and start it if it's not.
        if (MonitorService.self == null) {
            startService(new Intent(MainActivity.this, MonitorService.class));
        }

        // Refresh labels in the GUI
        RefreshData();
    }

    // Reset reference to this object when closing the application, so background service doesn't use it.
    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()){
            self = null;
        }

    }

    // Test if the application has permissions to listen notification and prompt user if not.
    // Also, the permission becomes invalid when the application is being upgraded, so
    // check that the listening works by sending test notification and checking that it is received.
    public void TestListenerAndConfirm() {
        is_enabled = IsEnabled();
        if (!is_enabled) {
            ShowConfirmDialog(false);
        }
        else if (!TestListener()) {
            ShowConfirmDialog(true);
        }
    }

    // Refresh GUI widgets when the application window have been restored.
    @Override
    protected void onResume() {
        super.onResume();

        // Check permissions once again.
        is_enabled = IsEnabled();
        if (!is_enabled) {
            ShowConfirmDialog(false);
        }
        else {
            // Start service if it is not running.
            if (MonitorService.self == null) {
                startService(new Intent(MainActivity.this, MonitorService.class));
            }
        }

        // Refresh GUI widgets
        DefaultSettings();
        RefreshData();
    }


    // The function to handle clicking interaction.
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
            survey_thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try  {
                        StartSurvey();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            survey_thread.start();
        }
        else if (view.getId() == R.id.answer_survey) {
            AnswerSurvey();
        }
        else if (view.getId() == R.id.survey_back) {
            SetMainView();
        }
    }

    // Default settings are values that are being set while service is starting and
    // stored settings haven't been loaded yet. This is optional.
    public void DefaultSettings() {
        ToggleButton toggle;
        toggle = (ToggleButton) findViewById(R.id.monday);
        toggle.setChecked(true);
        toggle = (ToggleButton) findViewById(R.id.tuesday);
        toggle.setChecked(true);
        toggle = (ToggleButton) findViewById(R.id.wednesday);
        toggle.setChecked(true);
        toggle = (ToggleButton) findViewById(R.id.thursday);
        toggle.setChecked(true);
        toggle = (ToggleButton) findViewById(R.id.friday);
        toggle.setChecked(true);
    }

    // RefreshData sets the value of GUI widgets.
    // It loads settings from background service, which have loaded settings from persistent storage.
    public void RefreshData() {
        if (MonitorService.self == null)
            return;

        begin_hour = MonitorService.begin_hour;
        begin_minute = MonitorService.begin_minute;
        end_hour = MonitorService.end_hour;
        end_minute = MonitorService.end_minute;

        begin.setText(Integer.toString(begin_hour) + ":" + (begin_minute < 10 ? "0" : "") + Integer.toString(begin_minute));
        end.setText(Integer.toString(end_hour) + ":" + (end_minute < 10 ? "0" : "") + Integer.toString(end_minute));

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


    // Function to handle new begin/end time of day for notification delaying.
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

    // showPopup function shows the "Delay Now" menu
    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        popup.setOnMenuItemClickListener(this);
        inflater.inflate(R.menu.popup_delay, popup.getMenu());
        popup.show();
    }

    // onMenuItemClick function handles the functionality of "Delay Now" menu items.
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.popup_first:
                StartTimerDelay(15);
                return true;
            case R.id.popup_second:
                StartTimerDelay(30);
                return true;
            case R.id.popup_third:
                StartTimerDelay(60);
                return true;
            default:
                return false;
        }
    }

    // StartTimerDelay function handles the "Delay Now" functionality.
    public void StartTimerDelay(int minutes) {
        int hours = minutes / 60;
        minutes = minutes % 60;
        if (MonitorService.self == null) {LOG("ERROR: Service is not running."); return;}
        String timestr = Integer.toString(hours) + ":" + (minutes < 10 ? "0" : "") + Integer.toString(minutes);
        LOG("Starting timed delay of " + timestr);
        countdown.setText(timestr);
        MonitorService.self.StartTimer(minutes);
        RefreshCountdown();
        ToggleButton toggle = (ToggleButton) findViewById(R.id.enable);
        toggle.setChecked(true);
    }

    // onCreateOptionsMenu enables the main menu of the application.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_menu, menu);
        return true;
    }

    // onOptionsItemSelected handles the functionality of the main menu.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dbg_create_test:
                DebugCreateNotification(this);
                return true;
            case R.id.dbg_remove_last:
                RemoveLastNotification();
                return true;
            case R.id.dbg_cancel_all:
                ClearAllNotifications();
                return true;
            case R.id.dbg_dump:
                ListCurrentNotification();
                return true;
            case R.id.dbg_release:
                MonitorService.self.ReleaseQueue();
                return true;
            case R.id.sys_perms:
                OpenNotificationAccess();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // getResString gets the translated text
    public String getResString(String name) {
        Context context = this;
        return getString(context.getResources().getIdentifier(name, "string", context.getPackageName()));
    }

    // buttonOnClicked handles the functionality of GUI buttons.
    public void buttonOnClicked(View view) {

        // Check notification access
        if (!is_enabled)
            is_enabled = IsEnabled();

        // All buttons requires service running in the background.
        if (MonitorService.self == null) {
            LOG("Unknown error in MonitorService");
            startService(new Intent(MainActivity.this, MonitorService.class));
            return;
        }

        ToggleButton toggle;
        Button btn;
        switch (view.getId()) {

            // ToggleButtons for weekdays.
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

            // The main "Enable" button for timed delaying.
            case R.id.enable:
                toggle = (ToggleButton) findViewById(view.getId());
                if (toggle.isChecked()) {
                    SetTimerange();
                    MonitorService.self.Enable(true);
                }
                else {
                    MonitorService.self.Enable(false);
                }
                RefreshCountdown();
                break;
            default:
                break;
        }
    }

    // SetTimerange assigns GUI's settings to service's settings.
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

    // IsEnabled checks that the application has permissions to listen notifications.
    private boolean IsEnabled() {
        String name = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), ENABLED_LISTENERS);
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

    // TestListener tests that listening really works by sending a notification and checking that it is received.
    private boolean TestListener() {

        // Create a test notification
        android.app.NotificationManager nmgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this);
        ncomp.setContentTitle(getResString("app_name"));
        ncomp.setContentText(getResString("app_starting"));
        ncomp.setTicker(getResString("app_starting"));
        ncomp.setSmallIcon(R.drawable.status_bar_icon);
        ncomp.setAutoCancel(true);
        int id = (int) System.currentTimeMillis();
        nmgr.notify(id, ncomp.build());

        // Require non-empty notification list
        boolean success = GetCurrentNotificationString() != "";

        // Cancel the test notification
        android.app.NotificationManager notificationManager = (android.app.NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);

        return success;
    }

    // DebugCreateNotification creates a test notification for debugging purposes.
    private void DebugCreateNotification(Context context) {
        android.app.NotificationManager nmgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this);
        ncomp.setContentTitle("Test Notification");
        ncomp.setContentText("Notification Listener Service Example");
        ncomp.setTicker("Notification Listener Service Example");
        ncomp.setSmallIcon(android.R.drawable.sym_def_app_icon);
        ncomp.setAutoCancel(true);
        nmgr.notify((int)System.currentTimeMillis(), ncomp.build());
    }

    // OpenNotificationAccess opens the notification listening permissions window.
    private void OpenNotificationAccess() {
        startActivity(new Intent(ACTION_SETTINGS));
    }

    // ShowConfirmDialog requests user to give this application the permission for listening notifications.
    private void ShowConfirmDialog(boolean re_enable_msg) {
        new AlertDialog.Builder(this)
                .setMessage(
                        re_enable_msg == false ?
                                getResString("enable_access") :
                                getResString("reenable_access"))
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

    // Remove latest notification for debugging purposes.
    private void RemoveLastNotification() {
        if (is_enabled) {
            MonitorService.self.CancelLast();
        }else {
            LOG("Please Enable Notification Access");
        }
    }

    // Clear all notifications for debugging purposes.
    private void ClearAllNotifications() {
        if (is_enabled) {
            MonitorService.self.CancelAll();
        }else {
            LOG("Please Enable Notification Access");
        }
    }

    // Get raw text about all active notifications for debugging purposes and for testing listening.
    private String GetCurrentNotificationString() {
        String list = "";
        StatusBarNotification[] current = MonitorService.GetCurrentNotifications();
        if (current != null) {
            for (int i = 0; i < current.length; i++) {
                list = i + " " + current[i].getPackageName() + "\n" + list;
            }
        }
        return list;
    }

    // Put list of all notifications to console for debugging purposes.
    private void ListCurrentNotification() {
        if (is_enabled) {
            if (MonitorService.GetCurrentNotifications() == null)
                return;
            String result = GetCurrentNotificationString();
            LOG("\n" + result);
        }else {
            LOG("Please Enable Notification Access");
        }
    }

    // Easy-to-use logging function
    private void LOG(Object object) {
        Log.i(TAG, TAG_PRE+object);
    }

    // Utility function for fecthing webpage.
    public static String HttpRequest(String url_str) {
        URL url;
        String response = "";
        try {
            url = new URL(url_str);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));

            writer.flush();
            writer.close();
            os.close();
            int responseCode=conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }
            else {
                response="";

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    // StartSurvey starts the switching to the survey view by requesting survey questions first.
    private void StartSurvey() {

        String lang = Locale.getDefault().getLanguage();

        survey_str = HttpRequest("https://toimisto.zzz.fi/survey.php?action=survey&lang=" + lang);
        if (survey_str == "") return;
        LOG(survey_str);

        runOnUiThread(new Runnable() {
            public void run() {
                ViewSurvey();
            }
        });
    }

    // AnswerSurvey gets the survey answer values from the GUI and sends them to the server.
    private void AnswerSurvey() {

        RatingBar rb;
        Switch sw;
        String ans_str = Secure.getString(getContentResolver(), Secure.ANDROID_ID) + ";";

        try {

            sw = (Switch) findViewById(R.id.q_bool_0);
            ans_str += (sw.isChecked() ? "1;" : "0;");
            sw = (Switch) findViewById(R.id.q_bool_1);
            ans_str += (sw.isChecked() ? "1;" : "0;");
            sw = (Switch) findViewById(R.id.q_bool_2);
            ans_str += (sw.isChecked() ? "1;" : "0;");

            rb = (RatingBar) findViewById(R.id.q_rate_0_ans);
            ans_str += Integer.toString((int)rb.getRating()) + ";";
            rb = (RatingBar) findViewById(R.id.q_rate_1_ans);
            ans_str += Integer.toString((int)rb.getRating()) + ";";
            rb = (RatingBar) findViewById(R.id.q_rate_2_ans);
            ans_str += Integer.toString((int)rb.getRating()) + ";";
            rb = (RatingBar) findViewById(R.id.q_rate_3_ans);
            ans_str += Integer.toString((int)rb.getRating()) + ";";
            rb = (RatingBar) findViewById(R.id.q_rate_4_ans);
            ans_str += Integer.toString((int)rb.getRating()) + ";";

            LOG("Answer string: " + ans_str);

            // Encode answer string to the url
            answer_url = "https://toimisto.zzz.fi/survey.php?action=answer&answer=" + URLEncoder.encode(ans_str, "utf-8");
            LOG("Answer url: " + answer_url);

            // Networking must be done in non-GUI thread.
            answer_thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    HttpRequest(answer_url);
                }
            });
            answer_thread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Switch back to the main view
        SetMainView();
    }

    // ViewSurvey switches main view to the survey view and sets received survey questions to labels.
    private void ViewSurvey() {

        String[] parts = survey_str.split(";");
        for (int i = 0; i < parts.length; i++) {
            LOG(Integer.toString(i) + ": " + parts[i]);
        }

        if (parts.length != 9) {
            LOG("ERROR: Survey doesn't have 9 lines");
            return;
        }

        TextView tw;
        Switch sw;
        Button btn;

        try {
            setContentView(R.layout.survey);

            tw = (TextView) findViewById(R.id.survey_title);
            tw.setText(parts[0]);
            sw = (Switch) findViewById(R.id.q_bool_0);
            sw.setText(parts[1]);
            sw = (Switch) findViewById(R.id.q_bool_1);
            sw.setText(parts[2]);
            sw = (Switch) findViewById(R.id.q_bool_2);
            sw.setText(parts[3]);
            tw = (TextView) findViewById(R.id.q_rate_0);
            tw.setText(parts[4]);
            tw = (TextView) findViewById(R.id.q_rate_1);
            tw.setText(parts[5]);
            tw = (TextView) findViewById(R.id.q_rate_2);
            tw.setText(parts[6]);
            tw = (TextView) findViewById(R.id.q_rate_3);
            tw.setText(parts[7]);
            tw = (TextView) findViewById(R.id.q_rate_4);
            tw.setText(parts[8]);

            btn = (Button) findViewById(R.id.answer_survey);
            btn.setOnClickListener(this);
            btn = (Button) findViewById(R.id.survey_back);
            btn.setOnClickListener(this);

        } catch (Exception e) {
            e.printStackTrace();
            setContentView(R.layout.activity_main);
        }
    }
}
