package com.example.seniordesignapp;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class NodeViewModel extends AndroidViewModel {

    private NodeRepository mRepository;

    private final LiveData<List<Node>> mAllNodes;

    public NodeViewModel (Application application) {
        super(application);
        mRepository = new NodeRepository(application);
        mAllNodes = mRepository.getAllNodes();
    }

    LiveData<List<Node>> getAllNodes() { return mAllNodes; }

    public void insert(Node node) { mRepository.insert(node); }

    public void deleteAll() {mRepository.deleteAll();}
    public void deleteNode(Node node) {mRepository.deleteNode(node);}
    public void updateNode(Node node) {mRepository.updateNode(node);}
}