package com.example.seniordesignapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

class NodeViewHolder extends RecyclerView.ViewHolder{
    private final TextView nodeView;
    private final TextView tempView;
    private final TextView hmdView;

    private NodeViewHolder(View itemView) {
        super(itemView);
        nodeView = itemView.findViewById(R.id.nodeView);
        tempView = itemView.findViewById(R.id.tempView);
        hmdView = itemView.findViewById(R.id.hmdView);

    }

    public void bind(String text, double temp, double hmd) {
        String node_out = text + " " + temp + " " + hmd;
        nodeView.setText(text);
        tempView.setText(String.valueOf(temp) + " \u2109");
        hmdView.setText(String.valueOf(hmd) + " %");
    }

    static NodeViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_item, parent, false);
        return new NodeViewHolder(view);
    }
}