package com.vinithepooh.notifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private TextView txtView;
    private NotificationReceiver nReceiver;
    private final String TAG = "bulletin_board";
    private HashMap<String, MenuItem> app_menus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Search", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        txtView = findViewById(R.id.textView);

        /**
        txtView.setText("Notify center\n" +
                "a\nb\nc\nd\ne\nf\ng\nh\ni\n"+
                "j\nk\nl\nm\nn\no\np\nq\nr\n"+
                "s\nt\nu\nv\nw\nx\ny\nz\n" +
                "a\nb\nc\nd\ne\nf\ng\nh\ni\n"+
                "j\nk\nl\nm\nn\no\np\nq\nr\n"+
                "s\nt\nu\nv\nw\nx\ny\nz\n"
        );*/


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        if (Settings.Secure.getString(
                this.getContentResolver(),
                "enabled_notification_listeners").contains(getApplicationContext().getPackageName()))
        {
            Log.i(TAG,"hasNotificationAccess YES");
            Snackbar.make(findViewById(android.R.id.content), "Have notification access.",
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            //service is not enabled try to enabled by calling...
            Log.i(TAG,"hasNotificationAccess NO");
            Snackbar.make(findViewById(android.R.id.content), "Does not have notification access, Enable from settings",
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            Toast.makeText(getApplicationContext(), "Does not have notification access, Enable from settings",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.vinithepooh.notifier.NOTIFICATION_LISTENER");
        registerReceiver(nReceiver,filter);

        Log.i(TAG,"**********  Service registered onstart");

    }


    @Override
    public void onStop() {
        //disconnect sqlite3 db etc
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Log.d(TAG, "Opening settings");
            Intent intent=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        MenuItem menuItem = null;


        Log.i(TAG, "Menu clicked: " + id);

        try {
            if (menuItem == item) {
                Log.i(TAG, "Custom menu: ");
            } else {
                Log.i(TAG, "*** NOT custom menu: ");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred while comparing menus: " + e.getMessage());
        }

        if (id == R.id.nav_ntfcns) {
            // Handle the active notifications action
            Log.d(TAG, "Listing notifications");

            Intent i = new Intent("com.vinithepooh.notifier.NOTIFICATION_LISTENER_SERVICE");
            i.putExtra("command","list");
            sendBroadcast(i);

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

            Menu menu = navigationView.getMenu();
            menuItem = menu.add("My new menu");

            /**
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {
            */
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nReceiver);
    }


    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String temp = intent.getStringExtra("notification_event") + "\n" + txtView.getText();
            txtView.setText(temp);
        }
    }

}
