package com.example.user.notficationmanager;

import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Rating;
import android.os.Handler;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
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
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.provider.Settings.Secure;

import com.example.user.notficationmanager.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener, PopupMenu.OnMenuItemClickListener {

    static public MainActivity self;

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

    private static final String TAG = "Notification Delayer";
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

            int seconds_left = MonitorService.self.GetCountdown();

            int hour_left = seconds_left / 60 / 60;
            int minute_left = (seconds_left / 60) % 60;
            int second_left = seconds_left % 60;

            String countdown_str =
                    Integer.toString(hour_left) + ":" +
                    (minute_left < 10 ? "0" : "") + Integer.toString(minute_left) + ":" +
                    (second_left < 10 ? "0" : "") + Integer.toString(second_left);
            countdown.setText(countdown_str);

            // Call this function again
            countdown_handler.postDelayed(new Runnable() {
                public void run() {
                    if (MainActivity.self != null)
                        MainActivity.self.RefreshCountdown();
                }
            }, countdown_delay);
        }
        else {
            countdown.setText("0:00");
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        self = this;

        super.onCreate(savedInstanceState);

        SetMainView();
    }

    private void SetMainView() {
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
    protected void onPause() {
        super.onPause();
        if (isFinishing()){
            self = null;
        }

    }

    public void TestListenerAndConfirm() {
        is_enabled = IsEnabled();
        if (!is_enabled) {
            ShowConfirmDialog(false);
        }
        else if (!TestListener()) {
            ShowConfirmDialog(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        is_enabled = IsEnabled();
        if (!is_enabled) {
            ShowConfirmDialog(false);
        }
        else {
            // Restart service (crashes)
            //if (check_service == true && MonitorService.self == null) {
            if (MonitorService.self == null) {
                startService(new Intent(MainActivity.this, MonitorService.class));
                check_service = false;
            }
        }

        DefaultSettings();
        RefreshData();
    }


    private Thread survey_thread;

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
        /*
        //TODO: käytä @string/ohje tjsp
        if (view == this.begin || view == this.end) {
            Toast.makeText(view.getContext(), "Use the format date/month/year to enter the dates when you want to delay your notifications", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return false;
        }*/
        return false;
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


    public String getResString(String name) {
        Context context = this;
        return getString(context.getResources().getIdentifier(name, "string", context.getPackageName()));
    }

    public void buttonOnClicked(View view) {


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
        Button btn;
        switch (view.getId()) {
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


    Handler test_listener_handler = new Handler();
    int test_listener_counter;

    private boolean TestListener() {

        android.app.NotificationManager nmgr = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this);
        ncomp.setContentTitle(getResString("app_name"));
        ncomp.setContentText(getResString("app_starting"));
        ncomp.setTicker(getResString("app_starting"));
        ncomp.setSmallIcon(R.drawable.status_bar_icon);
        ncomp.setAutoCancel(true);
        int id = (int) System.currentTimeMillis();
        nmgr.notify(id, ncomp.build());

        boolean success = GetCurrentNotificationString() != "";

        android.app.NotificationManager notificationManager = (android.app.NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
        return success;
    }


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

    private void RemoveLastNotification() {
        if (is_enabled) {
            MonitorService.self.CancelLast();
        }else {
            LOG("Please Enable Notification Access");
        }
    }

    private void ClearAllNotifications() {
        if (is_enabled) {
            MonitorService.self.CancelAll();
        }else {
            LOG("Please Enable Notification Access");
        }
    }

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

    private void LOG(Object object) {
        Log.i(TAG, TAG_PRE+object);
    }

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

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    private String survey_str;

    private void StartSurvey() {

        String lang = Locale.getDefault().getLanguage();

        survey_str = HttpRequest("https://toimisto.zzz.fi/survey.php?action=survey&lang=" + lang);
        LOG(survey_str);

        runOnUiThread(new Runnable() {
            public void run() {
                ViewSurvey();
            }
        });
    }

    private String answer_url = "";
    private Thread answer_thread;

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

            answer_url = "https://toimisto.zzz.fi/survey.php?action=answer&answer=" + URLEncoder.encode(ans_str, "utf-8");
            LOG("Answer url: " + answer_url);

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

        SetMainView();
    }

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
