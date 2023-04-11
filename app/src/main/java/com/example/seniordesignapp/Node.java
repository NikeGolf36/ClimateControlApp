package com.example.seniordesignapp;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "node_table")
public class Node {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "node_name")
    private String node_name;

    @ColumnInfo(name = "temp")
    private double temp;

    @ColumnInfo(name = "hmd")
    private double hmd;

    public Node(String id, String node_name, double temp, double hmd) {
        this.id = id;
        this.node_name = node_name;
        this.temp = temp;
        this.hmd = hmd;
    }

    public String getId() {
        return this.id;
    }

    public String getNode_name() {
        return this.node_name;
    }

    public void setNode_name(String node_name){
        this.node_name = node_name;
    }

    public double getTemp() {
        return this.temp;
    }

    public void set_temp(double temp){
        this.temp = temp;
    }

    public double getHmd() {
        return this.hmd;
    }

    public void set_hmd(double hmd){
        this.hmd = hmd;
    }
}
