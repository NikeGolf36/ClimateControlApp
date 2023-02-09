package com.example.seniordesignapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private NodeViewModel mNodeViewModel;
    public static final int NEW_NODE_ACTIVITY_REQUEST_CODE = 1;
    //private NodeClickListener listener;
    //public NodeListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView window_status = findViewById(R.id.windowstatus);

        RecyclerView recyclerView = findViewById(R.id.recyclerview);

        final NodeListAdapter adapter = new NodeListAdapter(new NodeListAdapter.NodeDiff());
        //adapter = new NodeListAdapter(new NodeListAdapter.NodeDiff(), listener);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mNodeViewModel = new ViewModelProvider(this).get(NodeViewModel.class);

        mNodeViewModel.getAllNodes().observe(this, nodes -> {
            // Update the cached copy of the words in the adapter.
            adapter.submitList(nodes);
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener( view -> {
            Intent intent = new Intent(MainActivity.this, NewNodeActivity.class);
            startActivityForResult(intent, NEW_NODE_ACTIVITY_REQUEST_CODE);
        });

        //setOnClickListener();

        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        Node node = adapter.getNodeAtPosition(position);
                        mNodeViewModel.deleteNode(node);
                    }
                });
        helper.attachToRecyclerView(recyclerView);

        //PeriodicWorkRequest backgroundWork = new PeriodicWorkRequest.Builder(BackgroundWorker.class, 16, TimeUnit.MINUTES).build();
        //WorkRequest backgroundWork = OneTimeWorkRequest.from(BackgroundWorker.class);
        //WorkManager workManager = WorkManager.getInstance(this);
        //workManager.enqueue(backgroundWork);
        //workManager.enqueueUniquePeriodicWork("q", ExistingPeriodicWorkPolicy.KEEP, backgroundWork);

        //workManager.getWorkInfosForUniqueWork("query");
        //workManager.cancelUniqueWork("query");

        //Node node = adapter.getNodeAtPosition(1);
       // node.setNode_name("Hello");
    }

    @Override protected void onResume() {
        super.onResume();
        WorkRequest backgroundWork = OneTimeWorkRequest.from(BackgroundWorker.class);
        WorkManager workManager = WorkManager.getInstance(this);
        workManager.enqueue(backgroundWork);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NEW_NODE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            double temp = new Double(data.getStringExtra(NewNodeActivity.EXTRA_REPLY_T)).doubleValue();
            double hmd = new Double(data.getStringExtra(NewNodeActivity.EXTRA_REPLY_H)).doubleValue();
            Node node = new Node(data.getStringExtra(NewNodeActivity.EXTRA_REPLY), temp, hmd);
            mNodeViewModel.insert(node);
            //Node node1 = new Node(data.getStringExtra(NewNodeActivity.EXTRA_REPLY), 0, 0);
            //mNodeViewModel.updateNode(node1);
        }
    }
}