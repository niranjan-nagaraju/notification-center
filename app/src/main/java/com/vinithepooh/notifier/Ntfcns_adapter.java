package com.vinithepooh.notifier;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;


/**
 * Created by vinithepooh on 14/02/19.
 */

public class Ntfcns_adapter extends RecyclerView.Adapter<Ntfcns_adapter.NViewHolder>{
    private ArrayList<NtfcnsDataModel> dataSet;

    public static class NViewHolder extends RecyclerView.ViewHolder {

        TextView textViewPlaceholder;
        View separator;

        ImageView imageViewAppIcon;
        TextView textViewApp;
        TextView textViewSubText;
        TextView textViewPostTime;

        TextView textViewNtfcnsTitle;
        TextView textViewNtfcns;
        TextView textViewNtfcnsBigText;
        ImageView imageViewLargeIcon;
        ImageView imageViewBigPicture;

        CardView card_view;

        public NViewHolder(View itemView) {
            super(itemView);
            this.textViewPlaceholder = (TextView) itemView.findViewById(R.id.textViewPlaceholder);
            this.separator = (View) itemView.findViewById(R.id.viewSeparator);

            this.imageViewAppIcon = (ImageView)  itemView.findViewById(R.id.imageViewAppIcon);
            this.textViewApp = (TextView) itemView.findViewById(R.id.textViewAppName);
            this.textViewSubText = (TextView) itemView.findViewById(R.id.textViewSubText);
            this.textViewPostTime = (TextView) itemView.findViewById(R.id.textViewPostTime);

            this.textViewNtfcnsTitle = (TextView) itemView.findViewById(R.id.textViewntfcnTitle);
            this.textViewNtfcns = (TextView) itemView.findViewById(R.id.textViewntfcn);
            this.textViewNtfcnsBigText = (TextView) itemView.findViewById(R.id.textViewntfcnBigText);
            this.imageViewLargeIcon = (ImageView) itemView.findViewById(R.id.imageViewntfcn_icon);
            this.imageViewBigPicture = (ImageView) itemView.findViewById(R.id.imageViewBigPicture);

            this.card_view = (CardView) itemView.findViewById(R.id.card_view);
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
        View separator = holder.separator;
        TextView textViewPlaceholder = holder.textViewPlaceholder;

        ImageView imageViewAppIcon = holder.imageViewAppIcon;
        TextView textViewApp = holder.textViewApp;
        TextView textViewSubText = holder.textViewSubText;
        TextView textViewPostTime = holder.textViewPostTime;

        TextView textViewNtfcnsTitle = holder.textViewNtfcnsTitle;
        final TextView textViewNtfcns = holder.textViewNtfcns;
        final TextView  textViewNtfcnsBigText= holder.textViewNtfcnsBigText;
        ImageView imageViewLargeIcon = holder.imageViewLargeIcon;
        final ImageView imageViewBigPicture = holder.imageViewBigPicture;


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

        //separator.setBackgroundColor(Color.RED);
        // show separator and date only for card #0
        // or when placeholder text has changed from the last card
        textViewPlaceholder.setText(dataSet.get(listPosition).getPlaceholder());
        if (listPosition == 0 ||
                (!dataSet.get(listPosition).getPlaceholder().equals(
                        dataSet.get(listPosition-1).getPlaceholder()))
                ) {
            separator.setVisibility(View.VISIBLE);
            textViewPlaceholder.setVisibility(View.VISIBLE);
        } else {
            separator.setVisibility(View.GONE);
            textViewPlaceholder.setVisibility(View.GONE);
        }

        if (dataSet.get(listPosition).getAppIcon() != null)
            imageViewAppIcon.setImageDrawable(dataSet.get(listPosition).getAppIcon());

        textViewApp.setText(dataSet.get(listPosition).getApp_name());

        if (dataSet.get(listPosition).getSubtext() != null)
            textViewSubText.setText(dataSet.get(listPosition).getSubtext());
        else
            textViewSubText.setVisibility(View.INVISIBLE);

        textViewPostTime.setText(
                DateUtils.getRelativeTimeSpanString(dataSet.get(listPosition).getPostTime()));

        textViewNtfcnsTitle.setText(dataSet.get(listPosition).getNtfcn_title());

        if (dataSet.get(listPosition).getNtfcn_contents() != null)
            textViewNtfcns.setText(dataSet.get(listPosition).getNtfcn_contents());
        else
            textViewNtfcns.setVisibility(View.INVISIBLE);


        if (dataSet.get(listPosition).getNtfcn_bigtext() != null)
            textViewNtfcnsBigText.setText(dataSet.get(listPosition).getNtfcn_bigtext());
        else
            textViewNtfcnsBigText.setVisibility(View.INVISIBLE);


        if (dataSet.get(listPosition).getLargeIcon() != null) {
            imageViewLargeIcon.setImageDrawable(dataSet.get(listPosition).getLargeIcon());
            imageViewLargeIcon.setVisibility(View.VISIBLE);
        }
        if (dataSet.get(listPosition).getNtfcn_bigpicture() != null) {
            imageViewBigPicture.setImageDrawable(dataSet.get(listPosition).getNtfcn_bigpicture());
        }

        textViewNtfcns.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * Toggle short text visibility to big text
                 * and expanded text on card click
                 */
                textViewNtfcnsBigText.setVisibility(View.VISIBLE);
                textViewNtfcns.setVisibility(View.GONE);
                if(imageViewBigPicture.getDrawable() != null) {
                    imageViewBigPicture.setVisibility(View.VISIBLE);
                }
            }
        });

        textViewNtfcnsBigText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * Toggle big text visibility to short text
                 * and un-expanded text on card click
                 */
                textViewNtfcnsBigText.setVisibility(View.GONE);
                textViewNtfcns.setVisibility(View.VISIBLE);
                imageViewBigPicture.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }
}
