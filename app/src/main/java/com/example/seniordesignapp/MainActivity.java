package com.example.seniordesignapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    private NodeViewModel mNodeViewModel;
    public static final int NEW_NODE_ACTIVITY_REQUEST_CODE = 1;
    public static final String CHANNEL_ID = "climate_control_2";
    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView window_status;
    //private ConstraintLayout constraintLayout;
    private NotificationCompat.Builder open_notification;
    private NotificationCompat.Builder closed_notification;
    private SharedPreferences windowPref;
    private WorkManager workManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        window_status = findViewById(R.id.windowstatus);
        windowPref = getSharedPreferences("windowPref", MODE_PRIVATE);
        workManager = WorkManager.getInstance(this);
        //constraintLayout = findViewById(R.id.main_layout);

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
                        //int position = viewHolder.getAdapterPosition();
                        //Node node = adapter.getNodeAtPosition(position);
                        //Node node = new Node("clim_ctrl_01", "", 0, 0);
                        //mNodeViewModel.deleteNode(node);
                    }
                });
        helper.attachToRecyclerView(recyclerView);

        addNotifications();
        setWindowStatus();
        setSharedPreferences();

        //set periodic background work
        PeriodicWorkRequest backgroundWork = new PeriodicWorkRequest.Builder(BackgroundWorker.class, 16, TimeUnit.MINUTES).build();
        workManager.enqueueUniquePeriodicWork("periodic", ExistingPeriodicWorkPolicy.KEEP, backgroundWork);
    }


    @Override protected void onResume() {
        super.onResume();
        //one time worker request (query and calculations)
        WorkRequest backgroundWork = OneTimeWorkRequest.from(BackgroundWorker.class);
        workManager.enqueue(backgroundWork);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //create node
        if (requestCode == NEW_NODE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            Node node = new Node(data.getStringExtra(NewNodeActivity.Id_Reply), data.getStringExtra(NewNodeActivity.Name_Reply), 0, 0);
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
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentTitle("Window Status Update")
                        .setContentText("OPEN YOUR WINDOWS: Indoor Temperature too High")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentIntent(contentIntent);

        closed_notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentTitle("Window Status Update")
                        .setContentText("CLOSE YOUR WINDOWS: Indoor Temperature at Desired Temperature")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setContentIntent(contentIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
    }

    private void setWindowStatus(){
        //update window status View
        window_status.setText("Window Status: " + windowPref.getString("string", ""));

        //window info popup
        window_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                final View dialogView = inflater.inflate(R.layout.popup_window, null);
                EditText EditDesTemp = dialogView.findViewById(R.id.edit_destemp);
                EditText EditIndoor = dialogView.findViewById(R.id.edit_indoor);
                EditText EditOutdoor = dialogView.findViewById(R.id.edit_outdoor);

                SharedPreferences windowPref = getApplicationContext().getSharedPreferences("windowPref", MODE_PRIVATE);
                SharedPreferences.Editor updatePref = windowPref.edit();
                EditDesTemp.setText(String.valueOf(windowPref.getFloat("desTemp", 70)));
                EditIndoor.setText(String.valueOf(windowPref.getString("indoor", "Indoor")));
                EditOutdoor.setText(String.valueOf(windowPref.getString("outdoor", "Outdoor")));

                final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                        .setView(dialogView)
                        .setTitle("Window Information")
                        .setPositiveButton("Save", null)
                        .setNegativeButton("Cancel", null)
                        .create();

                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {

                        Button buttonPositive = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                        buttonPositive.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Float desTemp = Float.valueOf(EditDesTemp.getText().toString());
                                updatePref.putFloat("desTemp", desTemp);
                                updatePref.putString("indoor", EditIndoor.getText().toString());
                                updatePref.putString("outdoor", EditOutdoor.getText().toString());
                                updatePref.apply();
                                Log.d(TAG, "Saved: desTemp = "+windowPref.getFloat("desTemp", 70)
                                        +" indoor = "+windowPref.getString("indoor", "Indoor")
                                        +" outdoor = "+windowPref.getString("outdoor", "Outdoor"));
                                dialog.dismiss();
                            }
                        });
                        Button buttonNegative = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                        buttonNegative.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                            }
                        });
                    }
                });

                alertDialog.show();
            }
        });
    }

    private void setSharedPreferences(){
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
    }
}