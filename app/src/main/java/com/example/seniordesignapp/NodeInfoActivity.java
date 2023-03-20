package com.example.seniordesignapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

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
    private static char[] token = "1LP6zDF9s5Jku-kiOEBiDkEX_uTUh61Ig97BslDlLI-nc2bZavpQGbhmW_4gc4YgCjDbPs94pS2aKji_fpJy1A==".toCharArray();
    private static String org = "e39c345d7ab3212f";
    private static String bucket = "Sensor Data";
    public String node_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_node_info);
        TextView node = findViewById(R.id.textViewNodeName);
        TextView t_view = findViewById(R.id.textViewTemp);
        TextView h_view = findViewById(R.id.textViewHmd);

        node_name = "node name not set";
        double temp = 0.0;
        double hmd = 0.0;
        Bundle extras = getIntent().getExtras();
        if(extras != null){
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

    public void make_graphs() {
        InfluxDBClient influxDBClient = InfluxDBClientFactory.create("https://europe-west1-1.gcp.cloud2.influxdata.com", token, org, bucket);

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

        double loc = 0.0;
        List<FluxTable> tables = queryApi.query(flux);
        for (FluxTable fluxTable : tables) {
            List<FluxRecord> records = fluxTable.getRecords();
            for (FluxRecord fluxRecord : records) {
                if(fluxRecord.getValueByKey("location").equals(node_name)) {
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

        //tempGraph.getViewport().setMinX(now.getTime() - 24 * 3600 * 1000);
        //tempGraph.getViewport().setMaxX(now.getTime());
        //tempGraph.getViewport().setXAxisBoundsManual(true);
        tempGraph.getViewport().setScalable(true);
        //tempGraph.getViewport().setScrollable(true);
        //tempGraph.getGridLabelRenderer().setPadding(32);
        //tempGraph.getGridLabelRenderer().setLabelsSpace(5);
        //tempGraph.getGridLabelRenderer().setLabelsSpace(6*3600*1000);
        //tempGraph.getGridLabelRenderer().setHumanRounding(false);

        //tempGraph.getGridLabelRenderer().setHorizontalLabelsAngle(135);
        //tempGraph.getGridLabelRenderer().setLabelHorizontalHeight(5);


        hmdGraph.addSeries(hmdSeries);
        hmdGraph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        hmdGraph.getGridLabelRenderer().setNumHorizontalLabels(4); // only 4 because of the space
        hmdGraph.getViewport().setScalable(true);
        //hmdGraph.getGridLabelRenderer().setHorizontalLabelsAngle(45);
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
        //hmdGraph.getGridLabelRenderer().setHumanRounding(true);
    }
}

