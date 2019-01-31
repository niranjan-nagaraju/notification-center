package com.vinithepooh.notifier;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
    private final String TAG = "bulletin_board";
    private HashMap<String, MenuItem> app_menus;
    private NLService mBoundService;
    private boolean mIsBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has
            // been established, giving us the service object we can use
            // to interact with the service.  Because we have bound to a
            // explicit service that we know is running in our own
            // process, we can cast its IBinder to a concrete class and
            // directly access it.
            mBoundService = ((NLService.NLBinder)service).getService();

            Log.i(TAG,"Service connected");


            // Tell the user about this for our demo.
            Toast.makeText(getApplicationContext(),
                    "Service connected",
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has
            // been unexpectedly disconnected -- that is, its process
            // crashed. Because it is running in our same process, we
            // should never see this happen.
            mBoundService = null;

            Log.i(TAG,"Service disconnected");

            Toast.makeText(getApplicationContext(),
                    "Service disconnected",
                    Toast.LENGTH_SHORT).show();
        }
    };


    void doBindService() {
        bindService(new Intent(this, NLService.class),
                mConnection,
                Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

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

        // start the notification listener service
        Intent mServiceIntent = new Intent(this, NLService.class);
        startService(mServiceIntent);
        doBindService();

        //TODO: runThread();
    }

    @Override
    public void onStart() {
        super.onStart();


        Log.i(TAG,"**********  Service registered onstart");

    }


    @Override
    public void onStop() {
        //disconnect sqlite3 db etc
        Log.i(TAG,"**********  Activity onstop");
        super.onStop();

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
        } else if (id == R.id.exitapp) {
            // Stop background notification listener and exit.

            Log.d(TAG, "Exiting application");

            Toast.makeText(getApplicationContext(), "Exiting application",
                    Toast.LENGTH_LONG).show();


            // stop service
            Intent mServiceIntent = new Intent(this, NLService.class);
            stopService(mServiceIntent);
            doUnbindService();

            // Exit activity
            finish();
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
            try {
                String notifications_str = mBoundService.get_notifications();
                Log.i(TAG, "getNofifcations() returned:\n" + notifications_str);
                txtView.setText(notifications_str);
            } catch (Exception e) {
                Log.e(TAG, "Exception occurred while getting notifications from activity: " + e.getMessage());

            }

            /**

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
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
        doUnbindService();
    }

    private void runThread() {
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(TAG, "Inside thread - updating notifications");
                                if (!mIsBound || mBoundService == null) {
                                    Log.i(TAG, "Not bound!");
                                } else {
                                    txtView.setText("Inside thread!");
                                }
                            }
                        });
                        Thread.sleep(500);
                    } catch (Exception e) {
                        Log.i(TAG, "Exception in thread:" + e.getMessage());
                    }
                }
            }
        }.start();
    }
}
