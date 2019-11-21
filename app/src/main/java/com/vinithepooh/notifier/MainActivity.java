package com.vinithepooh.notifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.util.Linkify;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final String TAG = "bulletin_board";
    private final String APP_NAME="Notifications Center";
    private boolean first_run = true;


    private @CurrentNotificationsView.CurrentViewMode int currentNotificationsView =
            CurrentNotificationsView.TYPE_ACTIVE;

    // app names to menuitem references mapping
    private HashMap<String, MenuItem> app_menus;
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
                swipeLayout.setRefreshing(true);
                if(in_search) {
                    performSearch(editSearchText.getText().toString());
                    swipeLayout.setRefreshing(false);
                } else {
                    refreshCards();
                }
            }
        });


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


        // TODO: START runThread(); to be an UI update thread
        // checks active notifications, and updates current view every minute
    }




    /**
     * Refresh cards in current view
     */
    private void refreshCards() {
        try {
            if (!swipeLayout.isRefreshing())
                swipeLayout.setRefreshing(true);

            Log.i(TAG, "Refreshing cards!");

            /** Refreshing cards on a search view should refresh current search results */
            String searchString = editSearchText.getText().toString();
            if (editSearchText.isFocused() &&
                    !searchString.isEmpty()) {
                performSearch(searchString);

                if (swipeLayout.isRefreshing())
                    swipeLayout.setRefreshing(false);

                return;
            }

            mBoundService.sync_notifications();

            int num_active = mBoundService.get_active_count();
            Log.i(TAG, "Active notifications: " + String.valueOf(num_active));

            /** Update active count label */
            if (num_active > 99)
                counterTv.setText(String.valueOf(99) + "+");
            else
                counterTv.setText(String.valueOf(num_active));

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

                default:
                    Log.i(TAG, "Refreshing: NOOP!");
                    break;
            }

            recyclerView.setAdapter(mBoundService.getAdapter());
            mBoundService.getAdapter().notifyDataSetChanged();

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
            //mBoundService.sync_notifications();
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
                default:
                    Log.i(TAG, "Unknown current view: NOOP!");
                    break;
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
                Log.i(TAG, "Card clicked:  " + recyclerView.getChildAdapterPosition(v));
                //Log.(TAG, "pos: " + recyclerView.getChildAdapterPosition(v));
                handleCardClick(v);
            } catch (Exception e) {
                Log.e(TAG, "Error getting child position: " + e.getMessage());
            }
        }

        private void handleCardClick(View v) {
            TextView textViewApps = v.findViewById(R.id.textViewAppName);
            TextView textViewNtfcnsBigText = v.findViewById(R.id.textViewntfcnBigText);
            TextView textViewNtfcns = v.findViewById(R.id.textViewntfcn);
            ImageView imageViewBigPicture = v.findViewById(R.id.imageViewBigPicture);

            LinearLayout top_card_layout = v.findViewById(R.id.top_card_layout);
            LinearLayout group_card_layout = v.findViewById(R.id.group_card_layout);

            /** This is a group-heading card */
            if (group_card_layout.getVisibility() == View.VISIBLE) {
                final int expanded = 0;
                final int collapsed = 1;
                TextView textViewPlaceholder = (TextView) v.findViewById(R.id.textViewPlaceholder);
                Log.i(TAG, "Clicked on group header: " +  textViewPlaceholder.getText().toString());

                if (textViewPlaceholder.getTag() == null)
                    textViewPlaceholder.setTag(expanded);

                int listposition = recyclerView.getChildAdapterPosition(v);

                try {
                    if ((int)textViewPlaceholder.getTag() == expanded) {
                        Log.i(TAG, "Collapse view: " + listposition);

                        int num_removed = mBoundService.collapse_group(listposition);
                        recyclerView.getAdapter().notifyItemRangeRemoved(listposition+1,
                                num_removed);

                        Log.i(TAG, "Collapsed view from: " + (listposition+1) + " entries: " +
                                (num_removed));

                        textViewPlaceholder.setTag(collapsed);
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

                        textViewPlaceholder.setTag(expanded);
                        textViewPlaceholder.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.arrow_down_48px, 0, 0, 0);
                    }
                } catch (IndexOutOfBoundsException e) {
                    Log.i(TAG, "Out of bounds: " + listposition);
                }

                return;
            }

            LinearLayout ntfcns_actions_layout = v.findViewById(R.id.linear_layout_actions);

            /** Toggle big text and un-expanded text on card click */
            if(textViewNtfcns.getVisibility() == View.GONE) {
                textViewNtfcnsBigText.setVisibility(View.GONE);
                textViewNtfcns.setVisibility(View.VISIBLE);
                imageViewBigPicture.setVisibility(View.GONE);

                /** Hide actions bar */
                ntfcns_actions_layout.setVisibility(View.GONE);

                EditText editTextRemoteInput = v.findViewById(R.id.editTextRemoteInput);

                /** Hide remote text input */
                editTextRemoteInput.setVisibility(View.GONE);
            } else {
                textViewNtfcnsBigText.setVisibility(View.VISIBLE);
                textViewNtfcns.setVisibility(View.GONE);
                if(imageViewBigPicture.getDrawable() != null) {
                    imageViewBigPicture.setVisibility(View.VISIBLE);
                }

                /** Show actions bar */
                ntfcns_actions_layout.setVisibility(View.VISIBLE);
            }

            /**
            Snackbar.make(v, "Clicked card with content: " + textViewApps.getText(),
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
             */
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
        if (first_run) {
            new RefreshCardsAsyncTask().execute();
            first_run = false;
        }
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


    private class RefreshCardsAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Log.i(TAG, "Waiting for service...");
                while (mBoundService == null);

                // wait until our notification listener service is connected
                // to the notification manager
                Log.i(TAG, "Waiting for listener to be connected...");

                while(!mBoundService.isListenerConnected());

                Log.i(TAG, "Service bound - updating cards");
                mBoundService.sync_notifications();

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
                int num_active = mBoundService.get_active_count();
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
