package com.example.seniordesignapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Node.class}, version = 3, exportSchema = false)
public abstract class NodeRoomDatabase extends RoomDatabase {

    public abstract NodeDao nodeDao();

    private static volatile NodeRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    static final ExecutorService databaseDeleteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    static final ExecutorService databaseUpdateExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static NodeRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (NodeRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    NodeRoomDatabase.class, "node_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}