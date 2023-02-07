package com.example.seniordesignapp;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NodeDao {
    // allowing the insert of the same word multiple times by passing a
    // conflict resolution strategy
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Node node);

    @Query("DELETE FROM node_table")
    void deleteAll();

    @Query("SELECT * FROM node_table ORDER BY node_name ASC")
    LiveData<List<Node>> getAlphabetizedNodes();

    @Delete
    void deleteNode(Node node);

    @Update
    void updateNode(Node node);

    @Query("SELECT * FROM node_table")
    List<Node> getNodes();
}
