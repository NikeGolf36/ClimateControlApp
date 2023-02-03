package com.example.seniordesignapp;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

class NodeRepository {

    private NodeDao mNodeDao;
    private LiveData<List<Node>> mAllNodes;

    // Note that in order to unit test the WordRepository, you have to remove the Application
    // dependency. This adds complexity and much more code, and this sample is not about testing.
    // See the BasicSample in the android-architecture-components repository at
    // https://github.com/googlesamples
    NodeRepository(Application application) {
        NodeRoomDatabase db = NodeRoomDatabase.getDatabase(application);
        mNodeDao = db.nodeDao();
        mAllNodes = mNodeDao.getAlphabetizedNodes();
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    LiveData<List<Node>> getAllNodes() {
        return mAllNodes;
    }

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    void insert(Node node) {
        NodeRoomDatabase.databaseWriteExecutor.execute(() -> {
            mNodeDao.insert(node);
        });
    }

    void deleteAll(){
        NodeRoomDatabase.databaseDeleteExecutor.execute(() -> {
            mNodeDao.deleteAll();
        });
    }

    void deleteNode(Node node){
        NodeRoomDatabase.databaseDeleteExecutor.execute(() -> {
            mNodeDao.deleteNode(node);
        });
    }
}