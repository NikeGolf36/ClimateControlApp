package com.example.seniordesignapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private NodeViewModel mNodeViewModel;
    public static final int NEW_NODE_ACTIVITY_REQUEST_CODE = 1;
    public static final String CHANNEL_ID = "climate_control_2";
    private TextView window_status;
    private NotificationCompat.Builder open_notification;
    private NotificationCompat.Builder closed_notification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        window_status = findViewById(R.id.windowstatus);

        RecyclerView recyclerView = findViewById(R.id.recyclerview);

        final NodeListAdapter adapter = new NodeListAdapter(new NodeListAdapter.NodeDiff());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mNodeViewModel = new ViewModelProvider(this).get(NodeViewModel.class);

        mNodeViewModel.getAllNodes().observe(this, nodes -> {
            // Update the cached copy of the words in the adapter.
            adapter.submitList(nodes);
        });

        //button for adding nodes
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener( view -> {
            Intent intent = new Intent(MainActivity.this, NewNodeActivity.class);
            startActivityForResult(intent, NEW_NODE_ACTIVITY_REQUEST_CODE);
        });


        //Open nodeInfo page
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

        //update window status View
        SharedPreferences windowPref = getSharedPreferences("windowPref", MODE_PRIVATE);
        window_status.setText("Window Status: " + windowPref.getString("string", ""));

        //NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                //.setSmallIcon(R.drawable.notification_icon)


        addNotifications();

        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if(key.equals("string")){
                    window_status.setText("Window Status: " + prefs.getString(key, ""));
                }
                if(key.equals("notif")){
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                    if(prefs.getInt(key, 0) == 1){
                        notificationManager.notify(1234, open_notification.build());
                    }
                    else if(prefs.getInt(key, 0) == 2){
                        notificationManager.notify(5678, closed_notification.build());
                    }
                }
            }
        };

        windowPref.registerOnSharedPreferenceChangeListener(listener);

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
        //one time worker request (query and calculations)
        WorkRequest backgroundWork = OneTimeWorkRequest.from(BackgroundWorker.class);
        WorkManager workManager = WorkManager.getInstance(this);
        workManager.enqueue(backgroundWork);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //create node
        if (requestCode == NEW_NODE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            double temp = new Double(data.getStringExtra(NewNodeActivity.EXTRA_REPLY_T)).doubleValue();
            double hmd = new Double(data.getStringExtra(NewNodeActivity.EXTRA_REPLY_H)).doubleValue();
            Node node = new Node(data.getStringExtra(NewNodeActivity.EXTRA_REPLY), temp, hmd);
            mNodeViewModel.insert(node);
        }
    }

    private void addNotifications() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            //notificationManager.deleteNotificationChannel(CHANNEL_ID);
            notificationManager.createNotificationChannel(channel);
        }

        open_notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.color.window_color)
                        .setContentTitle("Window Status Update")
                        .setContentText("OPEN YOUR WINDOWS: Indoor Temperature too High")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentIntent(contentIntent);

        closed_notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.color.window_color)
                        .setContentTitle("Window Status Update")
                        .setContentText("CLOSE YOUR WINDOWS: Indoor Temperature at Desired Temperature")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentIntent(contentIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

    }
}