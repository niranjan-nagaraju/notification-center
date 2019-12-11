package com.vinithepooh.notifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SubMenu;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static android.graphics.Typeface.BOLD;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final String TAG = "bulletin_board";
    private final String APP_NAME="Notifier";
    private final int NUM_CUSTOM_MENUS = 50;

    private long last_refresh_time = 0;


    private @CurrentNotificationsView.CurrentViewMode int currentNotificationsView =
            CurrentNotificationsView.TYPE_ACTIVE;
    private StringBuilder app_filter = new StringBuilder();

    // app names to menuitem references mapping
    //private ConcurrentHashMap<String, MenuItem> pkgs_to_menus = new ConcurrentHashMap<>();
    private ConcurrentHashMap<MenuItem, String> menus_to_pkgs = new ConcurrentHashMap<>();
    private ArrayList<MenuItem> custom_menus = new ArrayList<>();
    private MenuItem submenu_header;

    private static NLService mBoundService;
    private boolean mIsBound;

    private static RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    static View.OnClickListener cardsOnClickListener;
    private static ArrayList<Integer> removedItems;
    private TextView counterTv;
    private boolean in_search = false;

    private static EditText editSearchText;

    private SwipeRefreshLayout swipeLayout;
    private static CoordinatorLayout clayout;

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
        toolbar.setTitle("Active Notifications");
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        editSearchText = findViewById(R.id.editSearchText);

        /** Refresh cards on swipe to bottom */
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshCards();
            }
        });

        clayout = findViewById(R.id.clayout);


        /** floating search button */
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /** Set search hint to highlight current view and therefore the scope of the search */
                if (currentNotificationsView == CurrentNotificationsView.TYPE_ACTIVE)
                    editSearchText.setHint(" Search within active notifications");
                else if(currentNotificationsView == CurrentNotificationsView.TYPE_ALL)
                    editSearchText.setHint(" Search within all notifications");

                /** Clear previous search string if the searchbox wasnt visible
                 * to begin with.
                 */
               if (editSearchText.getVisibility() != View.VISIBLE)
                   editSearchText.setText("");

                /**
                 *  If the search box already has some content
                 *  treat this click as a 'post'/done button.
                 *  and do the actual search.
                 */
                if (!editSearchText.getText().toString().equals("")) {
                    String searchString = editSearchText.getText().toString();

                    editSearchText.clearFocus();

                    Toast.makeText(getApplicationContext(), "searching for: " + searchString,
                            Toast.LENGTH_LONG).show();

                    performSearch(searchString);
                } else {
                    /**
                     * Enable search input and open keyboard.
                     */
                    editSearchText.setVisibility(View.VISIBLE);
                    editSearchText.requestFocus();

                    /** Show keyboard when fab is clicked and input is empty */
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            }
        });

        editSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String searchString = editSearchText.getText().toString();

                    editSearchText.clearFocus();

                    Toast.makeText(getApplicationContext(), "searching for: " + searchString,
                            Toast.LENGTH_LONG).show();

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
                    // Do nothing on focus
                    //imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                } else {
                    // hide keyboard when searchbox is not in focus
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
                    editSearchText.setText("");


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

        editSearchText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                Log.i(TAG, "Edit text changed");
                String searchString = editSearchText.getText().toString();

                if (!searchString.isEmpty())
                    performSearch(searchString);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null); // remove tints for icon in nav menus

        final Menu menu = navigationView.getMenu();
        submenu_header = menu.findItem(R.id.nav_apps);
        SpannableString ss = new SpannableString("App-wise Notifications");
        ss.setSpan(new ForegroundColorSpan(Color.GRAY), 0, ss.length(), 0);
        ss.setSpan(new StyleSpan(BOLD), 0, ss.length(), 0);
        //ss.setSpan(new RelativeSizeSpan(1.2f), 0, ss.length(), 0);
        submenu_header.setTitle(ss);

        /** Create a bunch of menu items at start, and reuse as needed */

        for (int i=0; i<NUM_CUSTOM_MENUS; i++) {
            MenuItem mi = menu.add(R.id.nav_apps_grp,
                    Menu.FIRST + i, Menu.FIRST+i, "");
            mi.setCheckable(true);
            mi.setActionView(R.layout.menu_counter);
            mi.setVisible(false);

            custom_menus.add(mi);
            menus_to_pkgs.put(mi, "");
        }

        cardsOnClickListener = new CardsOnClickListener(this);

        recyclerView = (RecyclerView) findViewById(R.id.ntfcns_recycler_view);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        counterTv = (TextView) navigationView.getMenu().findItem(R.id.nav_ntfcns).getActionView();
        counterTv.setVisibility(View.GONE);


        enableSwipeToDeleteAndUndo();


        ((DrawerLayout) findViewById(R.id.drawer_layout)).addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                //Log.i(TAG, "Drawer slide!");
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                Log.i(TAG, "Drawer opened!");

                new UpdateMenusAsyncTask().execute();
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                Log.i(TAG, "Drawer closed!");
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                //Log.i(TAG, "Drawer state changed!");
            }
        });

    }




    /**
     * Refresh cards in current view
     */
    private void refreshCards() {
        try {
            if (!swipeLayout.isRefreshing())
                swipeLayout.setRefreshing(true);

            Log.i(TAG, "Refreshing cards!");

            /**
             * If another sync is already in progress,
             * wait till it completes and
             * use its results
             */
            if (mBoundService.isSync_in_progress()) {
                Log.i(TAG, "Another sync already in progress");
                while (mBoundService.isSync_in_progress() != true);
            } else {
                mBoundService.sync_notifications();
            }

            int num_active = mBoundService.get_active_count();
            Log.i(TAG, "Active notifications: " + String.valueOf(num_active));

            /** Update active count label */
            updateActiveCount(counterTv, num_active);

            /** Refreshing cards on a search view should refresh current search results */
            String searchString = editSearchText.getText().toString();
            if (editSearchText.isFocused() &&
                    !searchString.isEmpty()) {
                performSearch(searchString);

                if (swipeLayout.isRefreshing())
                    swipeLayout.setRefreshing(false);

                return;
            }


            /** Store last position in the current view */
            int lastFirstVisiblePosition =
                    ((LinearLayoutManager)recyclerView.getLayoutManager()).
                            findFirstCompletelyVisibleItemPosition();
            Toolbar toolbar = findViewById(R.id.toolbar);

            switch (currentNotificationsView) {
                case CurrentNotificationsView.TYPE_ACTIVE:
                    Log.i(TAG, "Refreshing active view cards!");
                    toolbar.setTitle("Active Notifications");
                    mBoundService.filter_active();
                    break;

                case CurrentNotificationsView.TYPE_ALL:
                    Log.i(TAG, "Refreshing all view cards!");
                    toolbar.setTitle("All Notifications");
                    mBoundService.filter_all();
                    break;

                case CurrentNotificationsView.TYPE_APP:
                    PackageManager pm = getPackageManager();
                    String app_name;
                    try {
                        app_name = pm.getApplicationLabel(
                                pm.getApplicationInfo(app_filter.toString(),
                                        PackageManager.GET_META_DATA)).toString();
                    } catch (PackageManager.NameNotFoundException e) {
                        app_name = "Unknown";
                    }

                    Log.i(TAG, "Refreshing app view cards: " + app_filter);
                    toolbar.setTitle(app_name + " Notifications");
                    mBoundService.filter_apps(app_filter.toString(), "");
                    break;

                default:
                    Log.i(TAG, "Refreshing: NOOP!");

                    // shouldn't be coming here at all
                    // raise an exception for now
                    // to signal something is wrong
                    throw new NullPointerException();
                    //break;
            }

            recyclerView.setAdapter(mBoundService.getAdapter());
            mBoundService.getAdapter().notifyDataSetChanged();

            /** restore last position in the current view */
            recyclerView.scrollToPosition(lastFirstVisiblePosition);

            if (swipeLayout.isRefreshing()) {
                swipeLayout.setRefreshing(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred while refreshing cards: " +
                    e.getMessage());
        }
    }


    private void performSearch(String searchKey) {
        try {
            Log.i(TAG, "Searching for: " + searchKey);

            Toolbar toolbar = findViewById(R.id.toolbar);

            toolbar.setTitle("Searching for: " + searchKey);

            switch (currentNotificationsView) {
                case CurrentNotificationsView.TYPE_ACTIVE:
                    mBoundService.filter_active(searchKey);
                    break;
                case CurrentNotificationsView.TYPE_ALL:
                    mBoundService.filter_all(searchKey);
                    break;
                case CurrentNotificationsView.TYPE_APP:
                    mBoundService.filter_apps(app_filter.toString(), searchKey);
                    break;
                default:
                    Log.i(TAG, "Unknown current view: NOOP!");
                    // shouldn't be coming here at all
                    // raise an exception for now
                    // to signal something is wrong
                    throw new NullPointerException();
                    //break;
            }

            recyclerView.setAdapter(mBoundService.getAdapter());
            mBoundService.getAdapter().notifyDataSetChanged();

            in_search = true;
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred during search: " +
                    e.getMessage());
        }
    }


    private static class CardsOnClickListener implements View.OnClickListener {

        private final Context context;
        private final String TAG = "bulletin_board";

        private CardsOnClickListener(Context context) {
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            try {
                ArrayList<NtfcnsDataModel> dataSet = ((Ntfcns_adapter)recyclerView.getAdapter()).getDataSet();
                Log.i(TAG, "Card clicked:  " + recyclerView.getChildAdapterPosition(v));
                //Log.(TAG, "pos: " + recyclerView.getChildAdapterPosition(v));
                Log.i(TAG, "Card app: " +
                        dataSet.get(recyclerView.getChildAdapterPosition(v)).getApp_name());

                handleCardClick(v);
            } catch (Exception e) {
                Log.e(TAG, "Error getting child position: " + e.getMessage());
            }
        }

        private void handleCardClick(View v) {
            LinearLayout group_card_layout = v.findViewById(R.id.group_card_layout);
            int listposition = recyclerView.getChildAdapterPosition(v);

            /** This is a group-heading card */
            if (group_card_layout.getVisibility() == View.VISIBLE) {
                TextView textViewPlaceholder = (TextView) v.findViewById(R.id.textViewPlaceholder);
                Log.i(TAG, "Clicked on group header: " +  textViewPlaceholder.getText().toString());

                try {
                    ArrayList<NtfcnsDataModel> dataSet = ((Ntfcns_adapter)recyclerView.getAdapter()).getDataSet();

                    boolean expanded = dataSet.get(recyclerView.getChildAdapterPosition(v)).isExpanded();
                    if (expanded) {
                        Log.i(TAG, "Collapse view: " + listposition);

                        int num_removed = mBoundService.collapse_group(listposition);
                        recyclerView.getAdapter().notifyItemRangeRemoved(listposition+1,
                                num_removed);

                        Log.i(TAG, "Collapsed view from: " + (listposition+1) + " entries: " +
                                (num_removed));
                        dataSet.get(recyclerView.getChildAdapterPosition(v)).setExpanded(false);

                        textViewPlaceholder.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.arrow_right_48px, 0, 0, 0);
                    } else {
                        Log.i(TAG, "Expand view: " + listposition);

                        String searchString = editSearchText.getText().toString();
                        if (! editSearchText.isFocused() ||
                                searchString.isEmpty()) {
                            searchString = "";
                        }

                        int num_added = mBoundService.expand_group(listposition, searchString);
                        recyclerView.getAdapter().notifyItemRangeInserted(listposition+1,
                                num_added);

                        Log.i(TAG, "Expanded view from: " + (listposition+1) + " entries: " +
                                (num_added));

                        dataSet.get(recyclerView.getChildAdapterPosition(v)).setExpanded(true);

                        textViewPlaceholder.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.arrow_down_48px, 0, 0, 0);
                    }
                } catch (IndexOutOfBoundsException e) {
                    Log.i(TAG, "Out of bounds: " + listposition);
                }

                return;
            }

            /** Regular card - open notification on click */
            TextView ntfcn_open_action = v.findViewById(R.id.ntfcn_open_action);
            ntfcn_open_action.performClick();
        }
    }



    @Override
    public void onStart() {
        super.onStart();

        if (Settings.Secure.getString(
                this.getContentResolver(),
                "enabled_notification_listeners").contains(getApplicationContext().getPackageName()))
        {
            Log.i(TAG,"hasNotificationAccess YES");

            /** refresh tasks on startup +
             * every time the activity is back to focus but only if its been a while
             */
            if (System.currentTimeMillis() > last_refresh_time + 60*1000) {
                new RefreshCardsAsyncTask().execute();
                last_refresh_time = System.currentTimeMillis();
            }
        } else {
            Log.i(TAG,"hasNotificationAccess NO");
            Snackbar snackbar = Snackbar.make(clayout,
                    APP_NAME + " does not have notification access, Enable from settings",
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("SETTINGS", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent=new Intent(
                            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    startActivity(intent);
                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();
            Toast.makeText(getApplicationContext(),
                    APP_NAME + " does not have notification access, Enable from settings",
                    Toast.LENGTH_LONG).show();
        }
    }



    @Override
    public void onStop() {
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

            /** If we were in an app view, return to active notifications view */
            if (currentNotificationsView == CurrentNotificationsView.TYPE_APP) {
                currentNotificationsView = CurrentNotificationsView.TYPE_ACTIVE;
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

        Toolbar toolbar = findViewById(R.id.toolbar);

        if (id == R.id.nav_ntfcns) {
            // Handle the active notifications action
            Log.d(TAG, "Listing notifications");
            try {
                currentNotificationsView = CurrentNotificationsView.TYPE_ACTIVE;
                toolbar.setTitle("Active Notifications");
                refreshCards();
            } catch (Exception e) {
                Log.e(TAG, "Exception occurred while refreshing active notifications: " +
                        e.getMessage());
            }
        } else if (id == R.id.nav_allntfcns) {
            // Handle the all notifications action
            Log.d(TAG, "Listing all notifications");
            try {
                currentNotificationsView = CurrentNotificationsView.TYPE_ALL;
                toolbar.setTitle("All Notifications");
                refreshCards();
            } catch (Exception e) {
                Log.e(TAG, "Exception occurred while refreshing all notifications: " +
                        e.getMessage());
            }
        } else if (submenu_header == item) {
            Log.i(TAG, "Found submenu header, collapse/expand");
            return true;
        } else {
            if (menus_to_pkgs.containsKey(item)) {
                Log.i(TAG, "Found custom menu: " + menus_to_pkgs.get(item) +
                        " ID: " + item.getItemId() +
                        " Title: " + item.getTitle());

                currentNotificationsView = CurrentNotificationsView.TYPE_APP;
                toolbar.setTitle(item.getTitle() + " Notifications");
                app_filter.setLength(0);
                app_filter.append(menus_to_pkgs.get(item));
                refreshCards();
            }
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

    private void updateActiveCount(TextView counter, int num_active) {
        if (num_active == 0) {
            counter.setVisibility(View.GONE);
            return;
        }

        counter.setVisibility(View.VISIBLE);
        if (num_active > 99)
            counter.setText(String.valueOf(99) + "+");
        else
            counter.setText(String.valueOf(num_active));
    }

    private class RefreshCardsAsyncTask extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Log.i(TAG, "Waiting for service...");
                while (mBoundService == null);

                // wait until our notification listener service is connected
                // to the notification manager
                Log.i(TAG, "Waiting for listener to be connected...");

                publishProgress("Waiting for listener to be connected...");
                while(!mBoundService.isListenerConnected());

                Log.i(TAG, "Service bound - updating cards");

                /**
                 * If another sync is already in progress,
                 * wait till it completes and
                 * use its results
                 */
                if (mBoundService.isSync_in_progress()) {
                    Log.i(TAG, "Another sync already in progress");
                    while (mBoundService.isSync_in_progress() != true);
                } else {
                    mBoundService.sync_notifications();
                }

                publishProgress("Resyncing notification data");

                /**
                 * if search box is up, and has some search string
                 * filter current view based on search results
                 */
                String searchString = "";
                if (editSearchText != null &&
                        editSearchText.isFocused()) {
                    searchString = editSearchText.getText().toString();
                    in_search = true;
                }

                if ( currentNotificationsView == CurrentNotificationsView.TYPE_ALL) {
                    mBoundService.filter_all(searchString);
                }
                else if (currentNotificationsView == CurrentNotificationsView.TYPE_ACTIVE) {
                    mBoundService.filter_active(searchString);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Exception in Refresh cards Asynctask - Error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(),
                    "Refreshing notifications",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            try {
                int num_active = mBoundService.get_active_count();
                Log.i(TAG, "Active notifications: " + String.valueOf(num_active));

                updateActiveCount(counterTv, num_active);

                /** Store last position in the current view */
                int lastFirstVisiblePosition =
                        ((LinearLayoutManager)recyclerView.getLayoutManager()).
                                findFirstCompletelyVisibleItemPosition();

                recyclerView.setAdapter(mBoundService.getAdapter());
                mBoundService.getAdapter().notifyDataSetChanged();

                /** restore last position in the current view */
                recyclerView.scrollToPosition(lastFirstVisiblePosition);


                Toast.makeText(getApplicationContext(),
                        "Refresh completed.",
                        Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Exception in postExecute - Error: " + e.getMessage());
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            Toast.makeText(getApplicationContext(),
                    values[0],
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onCancelled(Void aVoid) {
        }

        @Override
        protected void onCancelled() {
        }
    }



    /** Async task to update navigation menus in the background when drawer is opened */
    private class UpdateMenusAsyncTask extends AsyncTask<Void, String, Void> {
        TreeMap<String, Integer> apps_list = null;

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                /** Wait if a sync is currently in progress */
                while (mBoundService.isSync_in_progress());

                apps_list = mBoundService.build_apps_list();
            } catch (Exception e) {
                Log.e(TAG, "Exception in Update Menus Asynctask - Error: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "Updating Menus");
        }

        @Override
        protected void onPostExecute(Void aVoid){
            try {
                PackageManager pm = getPackageManager();
                NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
                Menu menu = navigationView.getMenu();

                /** We need more menus */
                if (apps_list.size() > custom_menus.size()) {
                    for (int i = custom_menus.size(); i < apps_list.size(); i++) {
                        MenuItem mi = menu.add(R.id.nav_apps_grp,
                                Menu.FIRST + i, Menu.FIRST+i, "");
                        mi.setCheckable(true);
                        mi.setActionView(R.layout.menu_counter);
                        mi.setVisible(true);

                        custom_menus.add(mi);
                        menus_to_pkgs.put(mi, "");
                    }
                } else {
                    /** We have more menus than apps, hide unneeded ones */
                    for (int i=custom_menus.size()-1; i>=apps_list.size(); i--) {
                        custom_menus.get(i).setVisible(false);
                    }
                }

                int i = 0;
                for (Map.Entry<String, Integer> entry : apps_list.entrySet()) {
                    String pkg = entry.getKey();
                    Integer active_count = entry.getValue();

                    Log.i(TAG, entry.getKey() + ": " + entry.getValue());

                    MenuItem mi = custom_menus.get(i);

                    /**
                     * if menu corresponds to the same app
                     * Just set active counter and return
                     * else add appropriate title, and icon
                     * and link menu to package name
                     *
                     * NOTE: we compare packaga names and not the app names
                     * because app names might not be unique
                     * e.g. Calendar
                     */
                    if (!menus_to_pkgs.get(mi).equals(pkg)) {
                        String app_name = null;
                        try {
                            app_name = pm.getApplicationLabel(
                                    pm.getApplicationInfo(pkg,
                                            PackageManager.GET_META_DATA)).toString();
                        } catch (PackageManager.NameNotFoundException e) {
                            app_name = "Unknown";
                        }

                        Drawable app_icon = null;
                        try {
                            app_icon = pm.getApplicationIcon(pkg);
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.e(TAG, "Error getting app icon");
                        }

                        mi.setIcon(app_icon);
                        mi.setTitle(app_name);
                        menus_to_pkgs.put(mi, pkg);
                    }

                    updateActiveCount((TextView)mi.getActionView(), active_count);
                    mi.setVisible(true);
                    i++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Exception in update menus async postExecute - Error: " +
                        e.getMessage());
            }
        }


        @Override
        protected void onProgressUpdate(String... values) {
            Toast.makeText(getApplicationContext(),
                    values[0],
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onCancelled(Void aVoid) {
        }

        @Override
        protected void onCancelled() {
        }

    }


        private void enableSwipeToDeleteAndUndo() {
        SwipeToDelete swipeToDeleteCallback = new SwipeToDelete(this) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

                final int position = viewHolder.getAdapterPosition();
                final Ntfcns_adapter adapter = (Ntfcns_adapter)recyclerView.getAdapter();
                final NtfcnsDataModel item = adapter.getDataSet().get(position);
                final boolean[] undo_clicked = new boolean[]{false};

                Log.i(TAG, "Swiped position: " + position);

                adapter.removeItem(position);

                StringBuilder snackbar_text = new StringBuilder(item.app_name);
                if (item.getNtfcn_active_status()) {
                    snackbar_text.append(" notification removed from active list.");
                } else {
                    snackbar_text.append(" notification removed.");
                }

                Snackbar snackbar = Snackbar
                        .make(clayout, snackbar_text.toString(),
                                Snackbar.LENGTH_LONG);

                snackbar.setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        undo_clicked[0] = true;
                        adapter.restoreItem(item, position);
                        //recyclerView.scrollToPosition(position);

                        Log.i(TAG, "Restoring item at position: " + position);
                    }
                });

                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();

                snackbar.addCallback(new Snackbar.Callback() {
                    /** Clear notification when snackbar is dismissed */
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        Log.i(TAG, "Position: " + position +
                                " Snackbar dismissed - Undo? " + undo_clicked[0]);

                        if (!undo_clicked[0]) {
                            Log.i(TAG, "Undo wasn't clicked; remove notification for real");

                            if (item.getNtfcn_active_status()) {
                                // Active notification

                                Log.i(TAG, "Active notification");

                                mBoundService.remove(item.getSbn(), true);

                                /** Clear all notifications from the status bar matching card content */
                                mBoundService.clearAll(item.getSbn());

                                item.ntfcn_active_status = false;

                                /**
                                 * If we were in an all-notifications/search view
                                 * Refresh cards so the cleared card appear
                                 * in the cached list
                                 * PS: This isnt needed in the active view because
                                 * the card is anyway dismissed, and no UI cues are needed.
                                 */
                                if (currentNotificationsView !=
                                        CurrentNotificationsView.TYPE_ACTIVE) {
                                    refreshCards();
                                }

                                Toast.makeText(getApplicationContext(),
                                        item.getApp_name() + " notification removed from active list.",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Log.i(TAG, "Cached notification");

                                mBoundService.remove(item.getSbn(), false);

                                Toast.makeText(getApplicationContext(),
                                        item.getApp_name() + " notification removed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onShown(Snackbar snackbar) {

                    }
                });


            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchhelper.attachToRecyclerView(recyclerView);
    }
}
