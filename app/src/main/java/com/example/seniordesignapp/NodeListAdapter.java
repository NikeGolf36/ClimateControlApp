package com.example.seniordesignapp;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class NodeListAdapter extends ListAdapter<Node, NodeViewHolder> {


    public NodeListAdapter(@NonNull DiffUtil.ItemCallback<Node> diffCallback) {
        super(diffCallback);
    }

    @Override
    public NodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return NodeViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(NodeViewHolder holder, int position) {
        Node current = getItem(position);
        holder.bind(current.getNode_name(), current.getTemp(), current.getHmd());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // call activity.
                Intent intent = new Intent(v.getContext(), NodeInfoActivity.class);
                // For passing values
                intent.putExtra("id", current.getId());
                intent.putExtra("node_name", current.getNode_name());
                intent.putExtra("temp", current.getTemp());
                intent.putExtra("hmd", current.getHmd());
                v.getContext().startActivity(intent);
            }
        });
    }

    static class NodeDiff extends DiffUtil.ItemCallback<Node> {

        @Override
        public boolean areItemsTheSame(@NonNull Node oldItem, @NonNull Node newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Node oldItem, @NonNull Node newItem) {
            return oldItem.getNode_name().equals(newItem.getNode_name());
        }
    }

    public Node getNodeAtPosition (int position) {
        return getItem(position);
    }
}
