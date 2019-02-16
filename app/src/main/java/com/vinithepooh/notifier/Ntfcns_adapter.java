package com.vinithepooh.notifier;

import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
/**
 * Created by vinithepooh on 14/02/19.
 */

public class Ntfcns_adapter extends RecyclerView.Adapter<Ntfcns_adapter.NViewHolder>{
    private ArrayList<NtfcnsDataModel> dataSet;

    public static class NViewHolder extends RecyclerView.ViewHolder {

        TextView textViewApps;
        TextView textViewPkgs;
        TextView textViewNtfcns;
        TextView textViewPlaceholder;
        View separator;
        CardView card_view;

        public NViewHolder(View itemView) {
            super(itemView);
            this.textViewApps = (TextView) itemView.findViewById(R.id.textViewAppName);
            this.textViewPkgs = (TextView) itemView.findViewById(R.id.textViewPkgName);
            this.textViewNtfcns = (TextView) itemView.findViewById(R.id.textViewntfcn);
            this.textViewPlaceholder = (TextView) itemView.findViewById(R.id.textViewPlaceholder);
            this.separator = (View) itemView.findViewById(R.id.viewseparator);
            this.card_view=(CardView) itemView.findViewById(R.id.card_view);
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
        TextView textViewApps = holder.textViewApps;
        TextView textViewPkgs = holder.textViewPkgs;
        TextView textViewNtfcns = holder.textViewNtfcns;
        TextView textViewPlaceholder = holder.textViewPlaceholder;
        View separator = holder.separator;

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

        textViewApps.setText(dataSet.get(listPosition).getApp_name());
        textViewPkgs.setText(dataSet.get(listPosition).getPkg_name());
        textViewNtfcns.setText(dataSet.get(listPosition).getNtfcn_contents());
        textViewPlaceholder.setText(dataSet.get(listPosition).getPlaceholder());
        //separator.setBackgroundColor(Color.RED);


        // show separator and date only for card #0
        if (listPosition == 0) {
            separator.setVisibility(View.VISIBLE);
            textViewPlaceholder.setVisibility(View.VISIBLE);
        } else {
            separator.setVisibility(View.GONE);
            textViewPlaceholder.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }
}
