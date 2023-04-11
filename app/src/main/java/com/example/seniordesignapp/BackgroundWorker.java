package com.example.seniordesignapp;

import static android.content.Context.MODE_PRIVATE;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import java.lang.Math;

import java.util.List;

public class BackgroundWorker extends Worker {
    private static char[] token = "1LP6zDF9s5Jku-kiOEBiDkEX_uTUh61Ig97BslDlLI-nc2bZavpQGbhmW_4gc4YgCjDbPs94pS2aKji_fpJy1A==".toCharArray();
    private static String org = "e39c345d7ab3212f";
    private static String bucket = "Sensor Data";
    private NodeRepository repo;

    public BackgroundWorker(@NonNull Context appContext,
                            @NonNull WorkerParameters workerParams){
        super(appContext, workerParams);
        this.repo = new NodeRepository((Application) appContext);
    }

    @NonNull
    @Override
    public Result doWork() {
        InfluxDBClient influxDBClient = InfluxDBClientFactory.create("https://europe-west1-1.gcp.cloud2.influxdata.com", token, org, bucket);

        String flux = "from(bucket:\"Sensor Data\") " +
                "|> range(start: -10m) " +
                "|> filter(fn: (r) => r._measurement == \"climate\")" +
                "|> last()"
                ;

        QueryApi queryApi = influxDBClient.getQueryApi();
        SharedPreferences windowPref = getApplicationContext().getSharedPreferences("windowPref", MODE_PRIVATE);
        SharedPreferences.Editor updatePref = windowPref.edit();

        double current_temp = 0.0;
        double current_hmd = 0.0;
        double indoor_temp = 0.0;
        double indoor_hmd = 0.0;
        double outdoor_temp = 0.0;
        double outdoor_hmd = 0.0;

        List<Node> nodes = repo.getNodes();

        List<FluxTable> tables = queryApi.query(flux);
        for(Node node : nodes) {
            for (FluxTable fluxTable : tables) {
                List<FluxRecord> records = fluxTable.getRecords();
                for (FluxRecord fluxRecord : records) {
                    if(fluxRecord.getValueByKey("id").equals(node.getId())) {
                        if (fluxRecord.getValueByKey("_field").equals("temp")) {
                            current_temp = ((Number) fluxRecord.getValueByKey("_value")).doubleValue();
                        }
                        if (fluxRecord.getValueByKey("_field").equals("hmd")) {
                            current_hmd = ((Number) fluxRecord.getValueByKey("_value")).doubleValue();
                        }
                        System.out.println(fluxRecord.getTime() + ": " + fluxRecord.getValueByKey("_field") + ' ' + fluxRecord.getValueByKey("_value") + ' ' + fluxRecord.getValueByKey("location"));
                    }
                }

            }
            System.out.println(node.getId() + " " + current_temp + " " + current_hmd);
            Node updated_node = new Node(node.getId(), node.getNode_name(), current_temp, current_hmd);
            repo.updateNode(updated_node);
            if(node.getNode_name().equals(windowPref.getString("indoor", "Indoor"))){
                indoor_temp = current_temp;
                indoor_hmd = current_hmd;
            }
            else if(node.getNode_name().equals(windowPref.getString("outdoor", "Outdoor"))){
                outdoor_temp = current_temp;
                outdoor_hmd = current_hmd;
            }
            current_hmd = current_temp = 0.0;
        }

        influxDBClient.close();

        //windowCalculation(75.0, 45.5, 68, 50);
        if(indoor_temp != 0 && indoor_hmd != 0 && outdoor_temp != 0 && outdoor_hmd != 0){
            windowCalculation(indoor_temp, indoor_hmd, outdoor_temp, outdoor_hmd);
        }
        else{
            System.out.println("Cannot calculate window status, check connections and sensor names");
            updatePref.putBoolean("status", false);
            updatePref.putString("string", "Closed");
            updatePref.apply();
        }
        return Result.success();
    }

    public void windowCalculation(double i_temp, double i_hmd, double o_temp, double o_hmd){
        SharedPreferences windowPref = getApplicationContext().getSharedPreferences("windowPref", MODE_PRIVATE);
        SharedPreferences.Editor updatePref = windowPref.edit();
        boolean windowStatus = false;
        String string = "Closed";
        int notif = 0;
        float des_temp = windowPref.getFloat("desTemp", 71);
        System.out.println(des_temp);
        //convert to C
        des_temp = (des_temp - 32) * 5/9;
        i_temp = (i_temp - 32) * 5/9;
        o_temp = (o_temp - 32) * 5/9;

        //get indoor and outdoor dewpoint
        double i_dew = (i_temp * 17.625)/(i_temp + 243.04) + Math.log(i_hmd/100.0);
        i_dew = (i_dew * 243.04)/(17.625 - i_dew);
        //System.out.println(i_dew);


        double o_dew = (o_temp * 17.625)/(o_temp + 243.04) + Math.log(o_hmd/100.0);
        o_dew = (o_dew * 243.04)/(17.625 - o_dew);
        //System.out.println(o_dew);


        if(i_temp > des_temp + 1.5 && i_temp > o_temp){ //indoor 1.5 degrees (C) above desired and outdoor is less
            if(i_dew + 5 > o_dew){ //outdoor moisture is about the same or lower that indoor moisture
                windowStatus = true;
                string = "Opened";
            }
        }

        if(windowStatus == true && windowPref.getBoolean("status", false) == false) {
            notif = 1; //notify user to open window
        }
        else if(windowStatus == false && windowPref.getBoolean("status", false) == true){
            notif = 2; //notify user to close window
        }
        //System.out.println(windowStatus);
        //System.out.println(string);

        updatePref.putBoolean("status", windowStatus);
        updatePref.putString("string", string);
        updatePref.putInt("notif", notif);
        updatePref.apply();

        System.out.println(windowPref.getBoolean("status", false));
        System.out.println(windowPref.getString("string", ""));
        System.out.println(windowPref.getInt("notif", 0));
    }
}
