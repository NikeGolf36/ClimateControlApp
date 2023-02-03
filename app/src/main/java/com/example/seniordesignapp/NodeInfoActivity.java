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
        t_view.setText(String.valueOf(temp));
        h_view.setText(String.valueOf(hmd));
    }
}