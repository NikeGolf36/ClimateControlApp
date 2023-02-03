package com.example.seniordesignapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

class NodeViewHolder extends RecyclerView.ViewHolder{
    private final TextView nodeItemView;

    private NodeViewHolder(View itemView) {
        super(itemView);
        nodeItemView = itemView.findViewById(R.id.textView);
    }

    public void bind(String text, double temp, double hmd) {
        String node_out = text + " " + temp + " " + hmd;
        nodeItemView.setText(node_out);
    }

    static NodeViewHolder create(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_item, parent, false);
        return new NodeViewHolder(view);
    }
}