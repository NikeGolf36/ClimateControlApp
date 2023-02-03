package com.example.seniordesignapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

public class NewNodeActivity extends AppCompatActivity {

    public static final String EXTRA_REPLY = "com.example.android.wordlistsql.REPLY";
    public static final String EXTRA_REPLY_T = "com.example.android.wordlistsql.REPLY_T";
    public static final String EXTRA_REPLY_H = "com.example.android.wordlistsql.REPLY_H";


    private EditText EditNodeName;
    private EditText EditNodeTemp;
    private EditText EditNodeHmd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_node);
        EditNodeName = findViewById(R.id.edit_word);
        EditNodeTemp = findViewById(R.id.edit_temp);
        EditNodeHmd = findViewById(R.id.edit_hmd);

        final Button button = findViewById(R.id.button_save);
        button.setOnClickListener(view -> {
            Intent replyIntent = new Intent();
            if (TextUtils.isEmpty(EditNodeName.getText())) {
                setResult(RESULT_CANCELED, replyIntent);
            } else {
                String word = EditNodeName.getText().toString();
                String temp = EditNodeTemp.getText().toString();
                String hmd = EditNodeHmd.getText().toString();
                replyIntent.putExtra(EXTRA_REPLY, word);
                replyIntent.putExtra(EXTRA_REPLY_T, temp);
                replyIntent.putExtra(EXTRA_REPLY_H, hmd);
                setResult(RESULT_OK, replyIntent);
            }
            finish();
        });
    }
}