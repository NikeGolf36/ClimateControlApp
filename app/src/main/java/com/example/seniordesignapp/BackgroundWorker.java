package com.example.seniordesignapp;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

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
                "|> range(start: -1d) " +
                "|> filter(fn: (r) => r._measurement == \"values\")" +
                "|> last()"
                ;

        QueryApi queryApi = influxDBClient.getQueryApi();

        double current_temp = 0.0;
        double current_hmd = 0.0;

        List<Node> nodes = repo.getNodes();

        List<FluxTable> tables = queryApi.query(flux);
        for(Node node : nodes) {
            for (FluxTable fluxTable : tables) {
                List<FluxRecord> records = fluxTable.getRecords();
                for (FluxRecord fluxRecord : records) {
                    if(fluxRecord.getValueByKey("location").equals(node.getNode_name())) {
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
            System.out.println(node.getNode_name() + " " + current_temp + " " + current_hmd);
            Node updated_node = new Node(node.getNode_name(), current_temp, current_hmd);
            repo.updateNode(updated_node);
            current_hmd = current_temp = 0.0;
        }

        influxDBClient.close();
        return Result.success();
    }
}
