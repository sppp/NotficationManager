package com.example.user.notificationmanager;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListPopupWindow;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;


public class AppMenuActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private ListView appList;
    private ArrayList results = new ArrayList();

    private final String[] helps = {"Q: How do I add a new application to the notification manager?\n" +
            "\n" +
            "A: Press manage apps at the bottom of the main screen and choose the applications which notifications you want to manage.", "Q: How do I disable the notification management?\n" +
            "\n" +
            "A: Press the off button in the main screen."};


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_apps);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        appList = (ListView) findViewById(R.id.app_list);
        appList.setOnItemClickListener(this);
        PackageManager pm = this.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);
        for (ResolveInfo rInfo : list) {
            results.add(rInfo.activityInfo.applicationInfo.loadLabel(pm).toString());
            Log.w("Installed Applications", rInfo.activityInfo.applicationInfo.loadLabel(pm).toString());
        }
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, results);
        appList.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.bar_help:
                displayHelpList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (view.getTag() == "selected") {
            view.setBackgroundColor(Color.parseColor("#ffffff"));
            view.setTag("");
        } else {
            view.setBackgroundColor(Color.parseColor("#006699"));
            view.setTag("selected");
        }
    }

    void displayHelpList() {
        ListPopupWindow helpList;
        helpList = new ListPopupWindow(AppMenuActivity.this);
        helpList.setAdapter(new ArrayAdapter(
                AppMenuActivity.this,
                R.layout.help, helps));
        helpList.setAnchorView(appList);
        helpList.setWidth(800);
        helpList.setHeight(1200);

        helpList.setModal(true);
        helpList.show();
    }

}
