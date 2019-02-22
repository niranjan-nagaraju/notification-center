package com.vinithepooh.notifier;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
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
        TextView textViewNtfcns = holder.textViewNtfcns;
        TextView  textViewNtfcnsBigText= holder.textViewNtfcnsBigText;
        ImageView imageViewLargeIcon = holder.imageViewLargeIcon;
        ImageView imageViewBigPicture = holder.imageViewBigPicture;


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
        textViewPlaceholder.setText(dataSet.get(listPosition).getPlaceholder());
        if (listPosition == 0) {
            separator.setVisibility(View.VISIBLE);
            textViewPlaceholder.setVisibility(View.VISIBLE);
        } else {
            separator.setVisibility(View.GONE);
            textViewPlaceholder.setVisibility(View.GONE);
        }

        if (dataSet.get(listPosition).getAppIcon() != null)
            imageViewAppIcon.setImageDrawable(dataSet.get(listPosition).getAppIcon());

        textViewApp.setText(dataSet.get(listPosition).getApp_name());
        textViewSubText.setText(dataSet.get(listPosition).getSubtext());
        textViewPostTime.setText(dataSet.get(listPosition).getPostTime());

        textViewNtfcnsTitle.setText(dataSet.get(listPosition).getNtfcn_title());
        textViewNtfcns.setText(dataSet.get(listPosition).getNtfcn_contents());
        textViewNtfcnsBigText.setText(dataSet.get(listPosition).getNtfcn_bigtext());

        if (dataSet.get(listPosition).getLargeIcon() != null) {
            imageViewLargeIcon.setImageDrawable(dataSet.get(listPosition).getLargeIcon());
            imageViewLargeIcon.setVisibility(View.VISIBLE);
        }
        if (dataSet.get(listPosition).getNtfcn_bigpicture() != null) {
            imageViewBigPicture.setImageDrawable(dataSet.get(listPosition).getNtfcn_bigpicture());
        }
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }
}
