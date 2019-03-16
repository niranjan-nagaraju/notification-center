package com.vinithepooh.notifier;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final String TAG = "bulletin_board";
    private final String APP_NAME="Notifications Center";


    private @CurrentNotificationsView.CurrentViewMode int currentNotificationsView =
            CurrentNotificationsView.TYPE_ACTIVE;

    // app names to menuitem references mapping
    private HashMap<String, MenuItem> app_menus;
    private NLService mBoundService;
    private boolean mIsBound;

    private static RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    static View.OnClickListener cardsOnClickListener;
    private static ArrayList<Integer> removedItems;
    private TextView counterTv;
    private boolean in_search = false;

    private SwipeRefreshLayout swipeLayout;

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
                    APP_NAME + ": Service connected",
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
                    APP_NAME + ": Service disconnected",
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

        // start the notification listener service
        Intent mServiceIntent = new Intent(this, NLService.class);
        startService(mServiceIntent);
        doBindService();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final EditText editSearchText = findViewById(R.id.editSearchText);
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);

        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeLayout.setRefreshing(true);
                refreshCards();
            }
        });


        /** floating search button */
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /**
                 *  If the search box already has some content
                 *  treat this click as a 'post'/done button.
                 *  and do the actual search.
                 */
                if (!editSearchText.getText().toString().equals("")) {
                    String searchString = editSearchText.getText().toString();

                    editSearchText.clearFocus();
                    Snackbar.make(findViewById(android.R.id.content), "searching for: " +
                                    searchString,
                            Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    /** clear text for future searches */
                    editSearchText.setText("");
                    performSearch(searchString);
                } else {
                    /**
                     * Enable search input and open keyboard.
                     */
                    editSearchText.setVisibility(View.VISIBLE);
                    editSearchText.requestFocus();
                }
            }
        });

        editSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String searchString = editSearchText.getText().toString();

                    editSearchText.clearFocus();
                    Snackbar.make(findViewById(android.R.id.content), "searching for: " +
                                    searchString,
                            Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    /** clear text for future searches */
                    editSearchText.setText("");

                    performSearch(searchString);
                    return true;
                }
                return false;
            }
        });

        editSearchText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (hasFocus) {
                    // show keyboard on focus
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                } else {
                    // hide searchbox
                    editSearchText.setVisibility(View.GONE);
                    // hide keyboard
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        });

        editSearchText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                /**
                 * back key pressed with search box in focus
                 * Hide search and return to previous view.
                 */
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    editSearchText.clearFocus();
                    editSearchText.setVisibility(View.GONE);


                    /**
                     * on back-press, just return to 'un-filtered' current view
                     * with all cards for current view regardless of search box string
                     */
                    refreshCards();
                    return true;
                }

                return false;
            }
        });


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
        } else {
            //service is not enabled try to enabled by calling...
            Log.i(TAG,"hasNotificationAccess NO");
            Snackbar.make(findViewById(android.R.id.content), APP_NAME + " does not have notification access, Enable from settings",
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            Toast.makeText(getApplicationContext(), APP_NAME + " does not have notification access, Enable from settings",
                    Toast.LENGTH_LONG).show();
        }

        cardsOnClickListener = new CardsOnClickListener(this);

        recyclerView = (RecyclerView) findViewById(R.id.ntfcns_recycler_view);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        removedItems = new ArrayList<Integer>();

        counterTv = (TextView) navigationView.getMenu().findItem(R.id.nav_ntfcns).getActionView();
        counterTv.setText("");


        // START runThread(); to be an UI update thread
        // checks active notifications, and updates current view.
        runThread();
    }


    /**
     * Refresh cards in current view
     */
    private void refreshCards() {
        swipeLayout.setRefreshing(true);
        Log.i(TAG, "Refreshing cards!");
        mBoundService.get_notifications();

        switch (currentNotificationsView) {
            case CurrentNotificationsView.TYPE_ACTIVE:
                Log.i(TAG, "Refreshing active view cards!");
                mBoundService.filter_active();

                int num_active = mBoundService.getAdapter().getItemCount();
                Log.i(TAG, "Active notifications: " + String.valueOf(num_active));

                /** Update active count label */
                if (num_active > 99)
                    counterTv.setText(String.valueOf(99) + "+");
                else
                    counterTv.setText(String.valueOf(num_active));
                break;

            case CurrentNotificationsView.TYPE_ALL:
                Log.i(TAG, "Refreshing all view cards!");
                mBoundService.filter_all();
                break;

            default:
                Log.i(TAG, "Refreshing: NOOP!");
                break;
        }

        recyclerView.setAdapter(mBoundService.getAdapter());
        mBoundService.getAdapter().notifyDataSetChanged();

        if (swipeLayout.isRefreshing()) {
            swipeLayout.setRefreshing(false);
        }
    }


    private void performSearch(String searchKey) {
        mBoundService.get_notifications();
        Log.i(TAG, "Searching for: " + searchKey);

        switch (currentNotificationsView) {
            case CurrentNotificationsView.TYPE_ACTIVE:
                mBoundService.filter_active(searchKey);
                break;
            case CurrentNotificationsView.TYPE_ALL:
                mBoundService.filter_all(searchKey);
                break;
            default:
                Log.i(TAG, "Unknown current view: NOOP!");
                break;
        }

        recyclerView.setAdapter(mBoundService.getAdapter());
        mBoundService.getAdapter().notifyDataSetChanged();

        in_search = true;
    }


    private static class CardsOnClickListener implements View.OnClickListener {

        private final Context context;
        private final String TAG = "bulletin_board";

        private CardsOnClickListener(Context context) {
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            Log.i(TAG, "Card clicked!");
            handleCardClick(v);
        }

        private void handleCardClick(View v) {
            TextView textViewApps = v.findViewById(R.id.textViewAppName);
            TextView textViewNtfcnsBigText = v.findViewById(R.id.textViewntfcnBigText);
            TextView textViewNtfcns = v.findViewById(R.id.textViewntfcn);
            ImageView imageViewBigPicture = v.findViewById(R.id.imageViewBigPicture);

            /** Toggle big text and un-expanded text on card click */
            if(textViewNtfcns.getVisibility() == View.GONE) {
                textViewNtfcnsBigText.setVisibility(View.GONE);
                textViewNtfcns.setVisibility(View.VISIBLE);
                imageViewBigPicture.setVisibility(View.GONE);
            } else {
                textViewNtfcnsBigText.setVisibility(View.VISIBLE);
                textViewNtfcns.setVisibility(View.GONE);
                if(imageViewBigPicture.getDrawable() != null) {
                    imageViewBigPicture.setVisibility(View.VISIBLE);
                }
            }

            Snackbar.make(v, "Clicked card with content: " + textViewApps.getText(),
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }

        /**
        private void removeItem(View v) {
            int selectedItemPosition = recyclerView.getChildAdapterPosition(v);
            RecyclerView.ViewHolder viewHolder
                    = recyclerView.findViewHolderForLayoutPosition(selectedItemPosition);
            TextView textViewApps
                    = (TextView) viewHolder.itemView.findViewById(R.id.textViewAppName);
            String selectedName = (String) textViewApps.getText();
            int selectedItemId = -1;
            for (int i = 0; i < SampleNotifications.pkg_names.length; i++) {
                if (selectedName.equals(SampleNotifications.pkg_names[i])) {
                    selectedItemId = SampleNotifications.id_[i];
                }
            }
            removedItems.add(selectedItemId);
            data.remove(selectedItemPosition);
            adapter.notifyItemRemoved(selectedItemPosition);
        }
         */
    }



    @Override
    public void onStart() {
        super.onStart();

        /** refresh tasks on startup */
        new RefreshCardsAsyncTask().execute();
    }



    @Override
    public void onStop() {
        //disconnect sqlite3 db etc
        super.onStop();
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            /**
             * Back key pressed while in search results page,
             * revert back to all cards for current view
             */
            if (in_search) {
                in_search = false;
                refreshCards();
                return;
            }

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
            Intent intent=new Intent(
                    "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
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
                currentNotificationsView = CurrentNotificationsView.TYPE_ACTIVE;
                refreshCards();
            } catch (Exception e) {
                Log.e(TAG, "Exception occurred while getting notifications from activity: " +
                        e.getMessage());
            }
        } else if (id == R.id.nav_allntfcns) {
            // Handle the all notifications action
            Log.d(TAG, "Listing all notifications");
            try {
                currentNotificationsView = CurrentNotificationsView.TYPE_ALL;
                refreshCards();
            } catch (Exception e) {
                Log.e(TAG, "Exception occurred while getting notifications from activity: " +
                        e.getMessage());
            }
        }

            /**
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

            Menu menu = navigationView.getMenu();
            menuItem = menu.add("My new menu");
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {
            */

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
                            int prune_counter = 0;
                            @Override
                            public void run() {
                                if (!mIsBound || mBoundService == null) {
                                    // UI is not in focus
                                } else {
                                    //prune_counter ++;
                                    /** prune entries every 1 minute */
                                    //if (prune_counter > 60) {
                                        Log.i(TAG, "Pruning entries");
                                      //  prune_counter = 0;
                                        mBoundService.prune();
                                    //}
                                }
                            }
                        });
                        Thread.sleep(60000);
                    } catch (Exception e) {
                        Log.i(TAG, "Exception in thread:" + e.getMessage());
                    }
                }
            }
        }.start();
    }


    private class RefreshCardsAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Log.i(TAG, "Waiting for service...");
                while (mBoundService == null);
                Log.i(TAG, "Service bound - updating cards");
                mBoundService.get_notifications();
                mBoundService.filter_active();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Exception in Asynctask - Error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            try {
                int num_active = mBoundService.getAdapter().getItemCount();
                Log.i(TAG, "Active notifications: " + String.valueOf(num_active));

                if (num_active > 99)
                    counterTv.setText(String.valueOf(99) + "+");
                else
                    counterTv.setText(String.valueOf(num_active));

                recyclerView.setAdapter(mBoundService.getAdapter());
                mBoundService.getAdapter().notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Exception in postExecute - Error: " + e.getMessage());
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }

        @Override
        protected void onCancelled(Void aVoid) {
        }

        @Override
        protected void onCancelled() {
        }
    }
}
