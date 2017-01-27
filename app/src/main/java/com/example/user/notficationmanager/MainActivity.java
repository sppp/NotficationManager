package com.example.user.notificationmanager;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener, PopupMenu.OnMenuItemClickListener {
    private EditText editDelay;
    private EditText editFirstDate;
    private EditText editSecondDate;
    private Button defaultOptions;
    private TextView manageApps;
    private Calendar calendar = Calendar.getInstance();

    private final String[] helps = {"Q: How do I add a new application to the notification manager?\n" +
            "\n" +
            "A: Press manage apps at the bottom of the main screen and choose the applications which notifications you want to manage.", "Q: How do I disable the notification management?\n" +
            "\n" +
            "A: Press the off button in the main screen."};


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        editDelay = (EditText) findViewById(R.id.editDelay);
        editDelay.setOnClickListener(this);

        editFirstDate = (EditText) findViewById(R.id.editFirstDate);
        editFirstDate.setOnClickListener(this);
        editFirstDate.setOnLongClickListener(this);

        editSecondDate = (EditText) findViewById(R.id.editSecondDate);
        editSecondDate.setOnClickListener(this);
        editSecondDate.setOnLongClickListener(this);

        defaultOptions = (Button) findViewById(R.id.btnDefaultOptions);


        manageApps = (TextView) findViewById(R.id.txtManageApps);
        manageApps.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == this.editFirstDate || view == this.editSecondDate) {
            new DatePickerDialog(MainActivity.this, date, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        } else if (view == this.manageApps) {
            Intent intent = new Intent(getBaseContext(), AppMenuActivity.class);
            startActivity(intent);
        }
    }


    DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel();
        }
    };

    private void updateLabel() {

        String myFormat = "dd/MM/yy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, getResources().getConfiguration().locale);


    }

    @Override
    public boolean onLongClick(View view) {
        if (view == this.editFirstDate || view == this.editSecondDate) {
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
                editDelay.setText("00:15");
                return true;
            case R.id.popup_second:
                editDelay.setText("00:30");
                return true;
            case R.id.popup_third:
                editDelay.setText("01:00");
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
        helpList.setAnchorView(editDelay);
        helpList.setWidth(800);
        helpList.setHeight(1200);

        helpList.setModal(true);
        helpList.show();
    }

}
