package com.vinithepooh.notifier;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;


/**
 * Created by vinithepooh on 14/02/19.
 */

public class Ntfcns_adapter extends RecyclerView.Adapter<Ntfcns_adapter.NViewHolder>{
    private ArrayList<NtfcnsDataModel> dataSet;
    final static int max_actions = 5;
    private final String TAG = "bulletin_board_adapter";

    public static class NViewHolder extends RecyclerView.ViewHolder {

        TextView textViewPlaceholder;
        LinearLayout top_card_layout;
        LinearLayout group_card_layout;

        ImageView imageViewAppIcon;
        TextView textViewApp;
        TextView textViewSubText;
        TextView textViewPostTime;
        TextView textViewActiveStatus;

        TextView textViewNtfcnsTitle;
        TextView textViewNtfcns;
        TextView textViewNtfcnsBigText;
        ImageView imageViewLargeIcon;
        ImageView imageViewBigPicture;

        CardView card_view;

        LinearLayout ntfcns_actions_layout;
        LinearLayout ntfcn_header_layout;
        TextView[] ntfcn_action = new TextView[max_actions];
        EditText editTextRemoteInput;

        /**
        TextView ntfcn_action2;
        TextView ntfcn_action3;
        TextView ntfcn_action4;
        TextView ntfcn_action5;
         */
        TextView ntfcn_open_action;

        public NViewHolder(View itemView) {
            super(itemView);
            this.textViewPlaceholder = (TextView) itemView.findViewById(R.id.textViewPlaceholder);
            this.top_card_layout = itemView.findViewById(R.id.top_card_layout);
            this.group_card_layout = itemView.findViewById(R.id.group_card_layout);
            this.ntfcn_header_layout = itemView.findViewById(R.id.ntfcn_header_layout);

            this.imageViewAppIcon = (ImageView)  itemView.findViewById(R.id.imageViewAppIcon);
            this.textViewApp = (TextView) itemView.findViewById(R.id.textViewAppName);
            this.textViewSubText = (TextView) itemView.findViewById(R.id.textViewSubText);
            this.textViewPostTime = (TextView) itemView.findViewById(R.id.textViewPostTime);
            this.textViewActiveStatus = (TextView) itemView.findViewById(R.id.textViewActiveStatus);

            this.textViewNtfcnsTitle = (TextView) itemView.findViewById(R.id.textViewntfcnTitle);
            this.textViewNtfcns = (TextView) itemView.findViewById(R.id.textViewntfcn);
            this.textViewNtfcnsBigText = (TextView) itemView.findViewById(R.id.textViewntfcnBigText);
            this.imageViewLargeIcon = (ImageView) itemView.findViewById(R.id.imageViewntfcn_icon);
            this.imageViewBigPicture = (ImageView) itemView.findViewById(R.id.imageViewBigPicture);

            this.card_view = (CardView) itemView.findViewById(R.id.card_view);

            this.ntfcns_actions_layout = itemView.findViewById(R.id.linear_layout_actions);
            this.ntfcn_open_action = itemView.findViewById(R.id.ntfcn_open_action);

            this.ntfcn_action[0] = itemView.findViewById(R.id.ntfcn_action1);
            this.ntfcn_action[1] = itemView.findViewById(R.id.ntfcn_action2);
            this.ntfcn_action[2] = itemView.findViewById(R.id.ntfcn_action3);
            this.ntfcn_action[3] = itemView.findViewById(R.id.ntfcn_action4);
            this.ntfcn_action[4] = itemView.findViewById(R.id.ntfcn_action5);

            this.editTextRemoteInput = itemView.findViewById(R.id.editTextRemoteInput);
        }
    }

    public Ntfcns_adapter(ArrayList<NtfcnsDataModel> data) {
        this.dataSet = data;
    }

    @Override
    public NViewHolder onCreateViewHolder(ViewGroup parent,
                                           int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ntfcns_card_layout, parent, false);


        NViewHolder myViewHolder = new NViewHolder(view);
        return myViewHolder;
    }


    @Override
    public void onBindViewHolder(NViewHolder holder, final int listPosition) {
        TextView textViewPlaceholder = holder.textViewPlaceholder;
        LinearLayout top_card_layout = holder.top_card_layout;
        LinearLayout group_card_layout = holder.group_card_layout;
        LinearLayout ntfcn_header_layout = holder.ntfcn_header_layout;

        ImageView imageViewAppIcon = holder.imageViewAppIcon;
        TextView textViewApp = holder.textViewApp;
        TextView textViewSubText = holder.textViewSubText;
        TextView textViewPostTime = holder.textViewPostTime;
        TextView textViewActiveStatus = holder.textViewActiveStatus;

        TextView textViewNtfcnsTitle = holder.textViewNtfcnsTitle;
        final TextView textViewNtfcns = holder.textViewNtfcns;
        final TextView  textViewNtfcnsBigText= holder.textViewNtfcnsBigText;
        ImageView imageViewLargeIcon = holder.imageViewLargeIcon;
        final ImageView imageViewBigPicture = holder.imageViewBigPicture;


        final LinearLayout ntfcns_action_lyt = holder.ntfcns_actions_layout;
        final TextView[] ntfcn_action = holder.ntfcn_action;
        final TextView ntfcn_open_action = holder.ntfcn_open_action;
        final EditText editTextRemoteInput = holder.editTextRemoteInput;

        holder.card_view.setOnClickListener(MainActivity.cardsOnClickListener);

        /**
         * NOTE: onclick listener has been set from main activity
         * This is now redundant
         *
        holder.card_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(v, "Clicked card with content: " + dataSet.get(listPosition).getApp_name(),
                        Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
         */

        ntfcn_header_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Clicked header");


                /** Toggle big text and un-expanded text on card click */
                if(textViewNtfcns.getVisibility() == View.GONE) {
                    textViewNtfcnsBigText.setVisibility(View.GONE);
                    textViewNtfcns.setVisibility(View.VISIBLE);
                    imageViewBigPicture.setVisibility(View.GONE);

                    /** Hide actions bar */
                    ntfcns_action_lyt.setVisibility(View.GONE);

                    /** Hide remote text input */
                    editTextRemoteInput.setVisibility(View.GONE);
                } else {
                    textViewNtfcnsBigText.setVisibility(View.VISIBLE);
                    textViewNtfcns.setVisibility(View.GONE);
                    if(imageViewBigPicture.getDrawable() != null) {
                        imageViewBigPicture.setVisibility(View.VISIBLE);
                    }

                    /** Show actions bar but only if there's atleast one action for the notification */
                    if (ntfcn_action[0].getText().length() != 0)
                        ntfcns_action_lyt.setVisibility(View.VISIBLE);
                    else
                        ntfcns_action_lyt.setVisibility(View.GONE);

                    /**
                     * Expanded view is exactly the same as un-expanded
                     * Treat it as a regular card click and open the notification
                     */
                    if (textViewNtfcns.getText().equals(textViewNtfcnsBigText.getText()) &&
                            imageViewBigPicture.getDrawable() == null &&
                            ntfcns_action_lyt.getVisibility() != View.VISIBLE) {

                        ntfcn_open_action.performClick();
                    }
                }
            }
        });


        // or when placeholder text has changed from the last card
        textViewPlaceholder.setText(dataSet.get(listPosition).getPlaceholder());
        if (listPosition == 0 ||
                (!dataSet.get(listPosition).getPlaceholder().equals(
                        dataSet.get(listPosition-1).getPlaceholder()))
                ) {
            group_card_layout.setVisibility(View.VISIBLE);
            top_card_layout.setVisibility(View.GONE);

            /** No shadows and padding for group header */
            holder.card_view.setCardElevation(0);
            holder.card_view.setContentPadding(0,0,0,0);
            return;
        } else {
            group_card_layout.setVisibility(View.GONE);
            top_card_layout.setVisibility(View.VISIBLE);
            float density =
                    holder.card_view.getContext().getResources().getDisplayMetrics().density;

            /** restore normal card parameters for regular notification cards */
            holder.card_view.setCardElevation( 5 * density);
            holder.card_view.setContentPadding((int)(10*density), (int)(10*density),
                    (int)(10*density), (int)(10*density));
        }

        if (dataSet.get(listPosition).getAppIcon() != null) {
            imageViewAppIcon.setImageDrawable(dataSet.get(listPosition).getAppIcon());
        } else {
            imageViewAppIcon.setImageResource(0);
        }

        textViewApp.setText(dataSet.get(listPosition).getApp_name());

        if (!dataSet.get(listPosition).getSubtext().isEmpty()) {
            textViewSubText.setText(dataSet.get(listPosition).getSubtext());
        } else {
            /** Reserve space at the center for alignment sake even if subtext is empty */
            textViewSubText.setText("");
        }

        textViewPostTime.setText(
                DateUtils.getRelativeTimeSpanString(dataSet.get(listPosition).getPostTime()));

        /** set active status if notification is still in the status bar */
        if (dataSet.get(listPosition).getNtfcn_active_status() == false)
            textViewActiveStatus.setVisibility(View.GONE);
        else
            textViewActiveStatus.setVisibility(View.VISIBLE);

        textViewNtfcnsTitle.setText(dataSet.get(listPosition).getNtfcn_title());

        if (!dataSet.get(listPosition).getNtfcn_contents().isEmpty()) {
            textViewNtfcns.setText(dataSet.get(listPosition).getNtfcn_contents());
        } else {
            textViewNtfcns.setText("");
        }


        if (!dataSet.get(listPosition).getNtfcn_bigtext().isEmpty()) {
            textViewNtfcnsBigText.setText(dataSet.get(listPosition).getNtfcn_bigtext());
        } else {
            /** Set 'big text' to same as text if this notification doesnt have big text content */
            textViewNtfcnsBigText.setText(dataSet.get(listPosition).getNtfcn_contents());
        }

        if (dataSet.get(listPosition).getLargeIcon() != null) {
            //imageViewLargeIcon.setImageDrawable(dataSet.get(listPosition).getLargeIcon());
            imageViewLargeIcon.setImageBitmap(dataSet.get(listPosition).getLargeIcon());
            imageViewLargeIcon.setVisibility(View.VISIBLE);
        } else {
            imageViewLargeIcon.setImageResource(0);
            imageViewLargeIcon.setVisibility(View.GONE);
        }

        if (dataSet.get(listPosition).getNtfcn_bigpicture() != null) {
            imageViewBigPicture.setImageBitmap(dataSet.get(listPosition).getNtfcn_bigpicture());
        } else {
            imageViewBigPicture.setImageResource(0);
            imageViewBigPicture.setVisibility(View.GONE);
        }

        /** Fill notification actions */
        StatusBarNotification sbn = dataSet.get(listPosition).getSbn();

        for (int i=0; i<max_actions; i++) {
            ntfcn_action[i].setText("");
            ntfcn_action[i].setVisibility(View.GONE);
        }

        try {
            final Notification ntfcn = sbn.getNotification();

            if (NotificationCompat.getActionCount(ntfcn) == 0)
                ntfcns_action_lyt.setVisibility(View.GONE);

            for (int i =0; i < NotificationCompat.getActionCount(ntfcn); i++) {
                final NotificationCompat.Action action = NotificationCompat.getAction(ntfcn, i);

                ntfcn_action[i].setText(action.title);
                ntfcn_action[i].setVisibility(View.VISIBLE);

                final RemoteInput[] ris = action.getRemoteInputs();
                boolean remote_found = false;
                if (ris != null) {
                    for (RemoteInput ri : ris) {
                        Log.i(TAG,
                                dataSet.get(listPosition).getApp_name() +
                                        ": Remote input found for action: " + action.title + " key: " +
                                        ri.getResultKey());

                        remote_found = true;
                    }
                }

                if (remote_found) {
                    /** Enable remote input textbox on click */
                    ntfcn_action[i].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // show remote input keyboard
                            editTextRemoteInput.setVisibility(View.VISIBLE);

                            editTextRemoteInput.requestFocus();
                            // show keyboard
                            InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                        }
                    });


                    /** Get remote input textbox contents and submit contents to
                     *  a remote input handler
                     */
                    editTextRemoteInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_GO) {
                                try {
                                    String inputString = editTextRemoteInput.getText().toString();

                                    editTextRemoteInput.clearFocus();
                                    editTextRemoteInput.setVisibility(View.GONE);
                                    Log.i(TAG, "Remote input: " +
                                            editTextRemoteInput.getText());

                                    /** clear text for future  */
                                    editTextRemoteInput.setText("");

                                    // hide keyboard
                                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                                    if (inputString.toString().isEmpty())
                                        return true;

                                    Intent intent = new Intent();
                                    Bundle bundle = new Bundle();

                                    ArrayList<RemoteInput> actualInputs = new ArrayList<>();

                                    for (RemoteInput ri : ris) {
                                        Log.i(TAG, "RemoteInput: " + ri.getLabel());
                                        bundle.putCharSequence(ri.getResultKey(), inputString);
                                        RemoteInput.Builder builder = new RemoteInput.Builder(ri.getResultKey());
                                        builder.setLabel(ri.getLabel());
                                        builder.setChoices(ri.getChoices());
                                        builder.setAllowFreeFormInput(ri.getAllowFreeFormInput());
                                        builder.addExtras(ri.getExtras());
                                        actualInputs.add(builder.build());
                                    }

                                    RemoteInput[] inputs = actualInputs.toArray(new RemoteInput[actualInputs.size()]);
                                    RemoteInput.addResultsToIntent(inputs, intent, bundle);
                                    action.actionIntent.send(v.getContext().getApplicationContext(), 0, intent);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error in remote input: " +
                                            e.getMessage());
                                }
                                Toast.makeText(v.getContext().getApplicationContext(),
                                                "remote input sent",
                                        Toast.LENGTH_LONG).show();
                                return true;
                            }
                            return false;
                        }
                    });

                    editTextRemoteInput.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            final int DRAWABLE_RIGHT = 2;

                            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                                if(event.getRawX() >=
                                        (editTextRemoteInput.getRight() - editTextRemoteInput.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                                    try {
                                        String inputString = editTextRemoteInput.getText().toString();

                                        editTextRemoteInput.clearFocus();
                                        editTextRemoteInput.setVisibility(View.GONE);
                                        Log.i(TAG, "Remote input: " +
                                                editTextRemoteInput.getText());

                                        /** clear text for future  */
                                        editTextRemoteInput.setText("");

                                        // hide keyboard
                                        InputMethodManager imm =
                                                (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                                        if (inputString.toString().isEmpty())
                                            return true;

                                        Intent intent = new Intent();
                                        Bundle bundle = new Bundle();

                                        ArrayList<RemoteInput> actualInputs = new ArrayList<>();

                                        for (RemoteInput ri : ris) {
                                            bundle.putCharSequence(ri.getResultKey(), inputString);
                                            RemoteInput.Builder builder = new RemoteInput.Builder(ri.getResultKey());
                                            builder.setLabel(ri.getLabel());
                                            builder.setChoices(ri.getChoices());
                                            builder.setAllowFreeFormInput(ri.getAllowFreeFormInput());
                                            builder.addExtras(ri.getExtras());
                                            actualInputs.add(builder.build());
                                        }

                                        RemoteInput[] inputs = actualInputs.toArray(new RemoteInput[actualInputs.size()]);
                                        RemoteInput.addResultsToIntent(inputs, intent, bundle);
                                        action.actionIntent.send(v.getContext().getApplicationContext(), 0, intent);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error in remote input: " +
                                                e.getMessage());
                                    }
                                    Toast.makeText(v.getContext().getApplicationContext(),
                                            "remote input sent",
                                            Toast.LENGTH_LONG).show();
                                    return true;
                                }
                            }
                            return false;
                        }
                    });

                    editTextRemoteInput.addTextChangedListener(new TextWatcher() {

                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                            if (!editTextRemoteInput.getText().toString().isEmpty())
                                editTextRemoteInput.setCompoundDrawableTintMode(null);
                            else
                                editTextRemoteInput.setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);
                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            if (!editTextRemoteInput.getText().toString().isEmpty())
                                editTextRemoteInput.setCompoundDrawableTintMode(null);
                            else
                                editTextRemoteInput.setCompoundDrawableTintMode(PorterDuff.Mode.SRC_IN);
                        }
                    });


                    editTextRemoteInput.setOnKeyListener(new View.OnKeyListener() {
                        @Override
                        public boolean onKey(View v, int keyCode, KeyEvent event) {
                            /**
                             * back key pressed with remote input box in focus
                             * just makes sure the app isn't closed
                             * on back press.
                             */
                            if (keyCode == KeyEvent.KEYCODE_BACK) {
                                if (!editTextRemoteInput.getText().toString().isEmpty()) {
                                    editTextRemoteInput.clearFocus();
                                    editTextRemoteInput.setText("");

                                    return true;
                                }
                            }

                            return false;
                        }
                    });
                } else {

                    ntfcn_action[i].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setAction(Long.toString(System.currentTimeMillis()));

                            try {
                                action.actionIntent.send(v.getContext().getApplicationContext(), new Random().nextInt(),
                                        intent);
                            } catch (PendingIntent.CanceledException e) {
                                Log.e(TAG, "Exception executing action: " +
                                        e.getMessage());
                            }
                        }
                    });
                }
            }

            ntfcn_open_action.setVisibility(View.GONE);

            ntfcn_open_action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setAction(Long.toString(System.currentTimeMillis()));

                    try {
                        ntfcn.contentIntent.send(v.getContext().getApplicationContext(), new Random().nextInt(),
                                intent);
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "Exception executing open action: " +
                                e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Exception occurred while getting actions" +
                    e.getMessage());
        }

        /** Autolink prevents card view onclick() from working, add an explicit listener */
        textViewNtfcnsBigText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ntfcn_open_action.performClick();
            }
        });


        /** Autolink prevents card view onclick() from working, add an explicit listener */
        textViewNtfcns.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ntfcn_open_action.performClick();
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public ArrayList<NtfcnsDataModel> getDataSet() {
        return this.dataSet;
    }
}
