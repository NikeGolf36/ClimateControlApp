package com.example.seniordesignapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class NodeInfoActivity extends AppCompatActivity {
    private char[] token;
    private String org;
    private String url;
    private static String bucket = "Sensor Data";
    public String node_name;
    public String id;
    private NodeRepository repo;
    private Button delete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_node_info);
        TextView node = findViewById(R.id.textViewNodeName);
        TextView t_view = findViewById(R.id.textViewTemp);
        TextView h_view = findViewById(R.id.textViewHmd);
        repo = new NodeRepository((Application) getApplicationContext());

        //delete button
        delete = findViewById(R.id.btn_delete);
        delete.setOnClickListener(deleteOnClickListener);

        //InfluxDB secrets
        token = getApplicationContext().getString(R.string.token).toCharArray();
        org = getApplicationContext().getString(R.string.org);
        url = getApplicationContext().getString(R.string.url);

        //Set Node Info
        node_name = "node name not set";
        double temp = 0.0;
        double hmd = 0.0;
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            id = extras.getString("id");
            node_name = extras.getString("node_name");
            temp = extras.getDouble("temp");
            hmd = extras.getDouble("hmd");
        }

        node.setText(node_name);
        t_view.setText("Current Temperature:  " + String.valueOf(Math.round(temp * 10.0)/10.0) + " \u2109");
        h_view.setText("Current Humidity:  " + String.valueOf(Math.round(hmd * 10.0)/10.0) + " %");

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                make_graphs();
            }
        });
        t.start();
    }

    private View.OnClickListener deleteOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Node node = new Node(id, "", 0, 0);
            repo.deleteNode(node);
            Toast.makeText(NodeInfoActivity.this, "To complete unpairing process, hold down reset button for 3 sec", Toast.LENGTH_LONG).show();
            finish();
        }
    };

    public void make_graphs() {
        InfluxDBClient influxDBClient = InfluxDBClientFactory.create(url, token, org, bucket);

        String flux = "from(bucket:\"Sensor Data\") " +
                "|> range(start: -1d) " +
                "|> filter(fn: (r) => r._measurement == \"climate\")"
                ;

        QueryApi queryApi = influxDBClient.getQueryApi();

        GraphView tempGraph = (GraphView) findViewById(R.id.tempGraph);
        LineGraphSeries<DataPoint> tempSeries = new LineGraphSeries<>();

        GraphView hmdGraph = (GraphView) findViewById(R.id.hmdGraph);
        LineGraphSeries<DataPoint> hmdSeries = new LineGraphSeries<>();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");

        List<FluxTable> tables = queryApi.query(flux);
        for (FluxTable fluxTable : tables) {
            List<FluxRecord> records = fluxTable.getRecords();
            for (FluxRecord fluxRecord : records) {
                if(fluxRecord.getValueByKey("id").equals(id)) {
                    if (fluxRecord.getValueByKey("_field").equals("temp")) {
                        Date date = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            date = Date.from(fluxRecord.getTime());
                        }
                        DataPoint point = new DataPoint(date, ((Number) fluxRecord.getValueByKey("_value")).doubleValue());
                        tempSeries.appendData(point, true, 1000);
                        //System.out.println(fluxRecord.getTime() + ": " + fluxRecord.getValueByKey("_value") + ' ' + fluxRecord.getValueByKey("_field")+ ' ' + fluxRecord.getValueByKey("location"));
                    }
                    if (fluxRecord.getValueByKey("_field").equals("hmd")) {
                        Date date = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            date = Date.from(fluxRecord.getTime());
                        }
                        DataPoint point = new DataPoint(date, ((Number) fluxRecord.getValueByKey("_value")).doubleValue());
                        hmdSeries.appendData(point, true, 1000);
                    }
                    System.out.println(fluxRecord.getTime() + ": " + fluxRecord.getValueByKey("_value") + ' ' + fluxRecord.getValueByKey("_field")+ ' ' + fluxRecord.getValueByKey("location"));
                }
            }
        }
        influxDBClient.close();

        //temp graph
        tempGraph.addSeries(tempSeries);
        tempGraph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        tempGraph.getGridLabelRenderer().setNumHorizontalLabels(3); // only 4 because of the space
        Date now = new Date();

        tempGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter()
        {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX){
                    return sdf.format(new Date((long) value));
                }
                else {
                    return super.formatLabel(value, isValueX);
                }
            }
        });

        tempGraph.getViewport().setScalable(true);

        //hmd graph
        hmdGraph.addSeries(hmdSeries);
        hmdGraph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        hmdGraph.getGridLabelRenderer().setNumHorizontalLabels(4); // only 4 because of the space
        hmdGraph.getViewport().setScalable(true);
        hmdGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter()
        {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX){
                    return sdf.format(new Date((long) value));
                }
                else {
                    return super.formatLabel(value, isValueX);
                }
            }
        });
    }
}

