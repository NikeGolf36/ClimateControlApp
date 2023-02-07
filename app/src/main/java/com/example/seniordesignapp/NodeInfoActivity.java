package com.example.seniordesignapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class NodeInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_node_info);
        TextView node = findViewById(R.id.textViewNodeName);
        TextView t_view = findViewById(R.id.textViewTemp);
        TextView h_view = findViewById(R.id.textViewHmd);

        String node_name = "node name not set";
        double temp = 0.0;
        double hmd = 0.0;
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            node_name = extras.getString("node_name");
            temp = extras.getDouble("temp");
            hmd = extras.getDouble("hmd");
        }

        node.setText(node_name);
        t_view.setText("Current Temperature:  " + String.valueOf(temp) + " \u2109");
        h_view.setText("Current Humidity:  " + String.valueOf(hmd) + " %");
    }
}