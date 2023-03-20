package com.example.seniordesignapp;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.espressif.provisioning.DeviceConnectionEvent;
import com.espressif.provisioning.ESPConstants;
import com.espressif.provisioning.ESPProvisionManager;
import com.espressif.provisioning.listeners.BleScanListener;
import com.espressif.provisioning.listeners.ProvisionListener;
import com.espressif.provisioning.listeners.ResponseListener;

import java.util.ArrayList;
import java.util.HashMap;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

public class NewNodeActivity extends AppCompatActivity {

    public static final String Name_Reply = "REPLY";
    private static final String TAG = NewNodeActivity.class.getSimpleName();

    // Request codes
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;

    // Time out
    private static final long DEVICE_CONNECT_TIMEOUT = 20000;

//    public static boolean isBleWorkDone = false;

    private Button btnScan;
    private ListView listView;
    private ProgressBar progressBar;

    private BleDeviceListAdapter adapter;
    private BluetoothAdapter bleAdapter;
    private ArrayList<BleDevice> deviceList;
    private HashMap<BluetoothDevice, String> bluetoothDevices;
    private Handler handler;

    private int position = -1;
    private String deviceNamePrefix = "clim_ctrl";
    private String pop = "abcd1234";
    private boolean isDeviceConnected = false, isConnecting = false;
    private boolean isScanning = false;

    private EditText EditNodeName;
    private ESPProvisionManager provisionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_node);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bleAdapter == null) {
            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        isConnecting = false;
        isDeviceConnected = false;
        handler = new Handler();
        bluetoothDevices = new HashMap<>();
        deviceList = new ArrayList<>();
        provisionManager = ESPProvisionManager.getInstance(getApplicationContext());
        provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_1);
        EventBus.getDefault().register(this);


        final Button button = findViewById(R.id.button_save);
        button.setOnClickListener(view -> {
            Intent replyIntent = new Intent();
            if (TextUtils.isEmpty(EditNodeName.getText())) {
                setResult(RESULT_CANCELED, replyIntent);
            } else {
                String word = EditNodeName.getText().toString();
                replyIntent.putExtra(Name_Reply, word);
                setResult(RESULT_OK, replyIntent);
            }
            finish();
        });
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bleAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(NewNodeActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission is not granted.");
                    return;
                }
            } else {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

        } else {

            if (!isDeviceConnected && !isConnecting) {
                startScan();
            }
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if (isScanning) {
            stopScan();
        }

        if (provisionManager.getEspDevice() != null) {
            provisionManager.getEspDevice().disconnectDevice();
        }

        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult, requestCode : " + requestCode + ", resultCode : " + resultCode);

        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            startScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {

            case REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startScan();
                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    finish();
                }
            }
            break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DeviceConnectionEvent event) {

        Log.d(TAG, "ON Device Prov Event RECEIVED : " + event.getEventType());
        handler.removeCallbacks(disconnectDeviceTask);

        switch (event.getEventType()) {

            case ESPConstants.EVENT_DEVICE_CONNECTED:
                Log.d(TAG, "Device Connected Event Received");
                progressBar.setVisibility(View.GONE);
                isConnecting = false;
                isDeviceConnected = true;
                Toast.makeText(NewNodeActivity.this, "Connected", Toast.LENGTH_LONG).show();
                Log.d(TAG, "POP : " + pop);
                provisionManager.getEspDevice().setProofOfPossession(pop);

                provisionManager.getEspDevice().initSession(new ResponseListener() {
                    @Override
                    public void onSuccess(byte[] returnData) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                Log.d(TAG, "POP Success");
                                WiFiConfig();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                Log.e(TAG, "POP Failure");
                            }
                        });
                    }
                });
                break;

            case ESPConstants.EVENT_DEVICE_DISCONNECTED:

                progressBar.setVisibility(View.GONE);
                isConnecting = false;
                isDeviceConnected = false;
                Log.e(TAG, "Device Disconnected");
                break;

            case ESPConstants.EVENT_DEVICE_CONNECTION_FAILED:
                progressBar.setVisibility(View.GONE);
                isConnecting = false;
                isDeviceConnected = false;
                Toast.makeText(NewNodeActivity.this, "Device connection failed", Toast.LENGTH_LONG).show();
                //Utils.displayDeviceConnectionError(this, getString(R.string.error_device_connect_failed));
                break;
        }
    }

    private void initViews() {

        btnScan = findViewById(R.id.btn_scan);
        listView = findViewById(R.id.ble_devices_list);
        progressBar = findViewById(R.id.ble_landing_progress_indicator);
        EditNodeName = findViewById(R.id.edit_word);

        adapter = new BleDeviceListAdapter(this, R.layout.item_ble_scan, deviceList);

        // Assign adapter to ListView
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(onDeviceCLickListener);
        btnScan.setOnClickListener(btnScanClickListener);
    }

    private boolean hasPermissions() {

        if (bleAdapter == null || !bleAdapter.isEnabled()) {

            requestBluetoothEnable();
            return false;

        } else if (!hasLocationAndBtPermissions()) {

            requestLocationAndBtPermission();
            return false;
        }
        return true;
    }

    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                requestLocationAndBtPermission();
            }
        } else {
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private boolean hasLocationAndBtPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean permissionsGranted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            return permissionsGranted;
        } else {
            return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestLocationAndBtPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_FINE_LOCATION);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        }
    }

    private void startScan() {

        if (!hasPermissions() || isScanning) {
            return;
        }

        isScanning = true;
        deviceList.clear();
        bluetoothDevices.clear();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            provisionManager.searchBleEspDevices(deviceNamePrefix, bleScanListener);
            updateProgressAndScanBtn();
        } else {
            Log.e(TAG, "Not able to start scan as Location permission is not granted.");
            Toast.makeText(NewNodeActivity.this, "Please give location permission to start BLE scan", Toast.LENGTH_LONG).show();
        }
    }

    private void stopScan() {

        isScanning = false;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            provisionManager.stopBleScan();
            updateProgressAndScanBtn();
        } else {
            Log.e(TAG, "Not able to stop scan as Location permission is not granted.");
            Toast.makeText(NewNodeActivity.this, "Please give location permission to stop BLE scan", Toast.LENGTH_LONG).show();
        }

        if (deviceList.size() <= 0) {
            Toast.makeText(NewNodeActivity.this, "No BLE Devices", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProgressAndScanBtn() {

        if (isScanning) {

            btnScan.setEnabled(false);
            btnScan.setAlpha(0.5f);
            btnScan.setTextColor(Color.WHITE);
            progressBar.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);

        } else {

            btnScan.setEnabled(true);
            btnScan.setAlpha(1f);
            progressBar.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    }

    private View.OnClickListener btnScanClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            bluetoothDevices.clear();
            adapter.clear();
            startScan();
        }
    };

    private BleScanListener bleScanListener = new BleScanListener() {

        @Override
        public void scanStartFailed() {
            Toast.makeText(NewNodeActivity.this, "Please turn on Bluetooth to connect BLE device", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPeripheralFound(BluetoothDevice device, ScanResult scanResult) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(NewNodeActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "====== onPeripheralFound ===== " + device.getName());
                }
            } else {
                Log.d(TAG, "====== onPeripheralFound ===== " + device.getName());
            }

            boolean deviceExists = false;
            String serviceUuid = "";

            if (scanResult.getScanRecord().getServiceUuids() != null && scanResult.getScanRecord().getServiceUuids().size() > 0) {
                serviceUuid = scanResult.getScanRecord().getServiceUuids().get(0).toString();
            }
            Log.d(TAG, "Add service UUID : " + serviceUuid);

            if (bluetoothDevices.containsKey(device)) {
                deviceExists = true;
            }

            if (!deviceExists) {
                BleDevice bleDevice = new BleDevice();
                bleDevice.setName(scanResult.getScanRecord().getDeviceName());
                bleDevice.setBluetoothDevice(device);

                listView.setVisibility(View.VISIBLE);
                bluetoothDevices.put(device, serviceUuid);
                deviceList.add(bleDevice);
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void scanCompleted() {
            isScanning = false;
            updateProgressAndScanBtn();
        }

        @Override
        public void onFailure(Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    };

    private AdapterView.OnItemClickListener onDeviceCLickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

            stopScan();
            if (TextUtils.isEmpty(EditNodeName.getText())) {
                Toast.makeText(NewNodeActivity.this, "Enter Sensor Name", Toast.LENGTH_LONG).show();
            } else {
                isConnecting = true;
                isDeviceConnected = false;
                btnScan.setVisibility(View.GONE);
                listView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                NewNodeActivity.this.position = position;
                BleDevice bleDevice = adapter.getItem(position);
                String uuid = bluetoothDevices.get(bleDevice.getBluetoothDevice());
                Log.d(TAG, "=================== Connect to device : " + bleDevice.getName() + " UUID : " + uuid);

                if (ActivityCompat.checkSelfPermission(NewNodeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    provisionManager.getEspDevice().connectBLEDevice(bleDevice.getBluetoothDevice(), uuid);
                    handler.postDelayed(disconnectDeviceTask, DEVICE_CONNECT_TIMEOUT);
                } else {
                    Log.e(TAG, "Not able to connect device as Location permission is not granted.");
                    Toast.makeText(NewNodeActivity.this, "Please give location permission to connect device", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    private Runnable disconnectDeviceTask = new Runnable() {

        @Override
        public void run() {

            Log.e(TAG, "Disconnect device");
            progressBar.setVisibility(View.GONE);
            btnScan.setVisibility(View.VISIBLE);
            Toast.makeText(NewNodeActivity.this, "Device Not Supported", Toast.LENGTH_LONG).show();
            //Utils.displayDeviceConnectionError(BLEProvisionLanding.this, getString(R.string.error_device_not_supported));
        }
    };

    private void WiFiConfig() {
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_wifi_network, null);
        final EditText etSsid = dialogView.findViewById(R.id.ssid);
        final EditText etPassword = dialogView.findViewById(R.id.password);

        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle("WiFi Network")
                .setPositiveButton("Provision", null)
                .setNegativeButton("Cancel", null)
                .create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button buttonPositive = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                buttonPositive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String password = etPassword.getText().toString();
                        String networkName = etSsid.getText().toString();

                        if (TextUtils.isEmpty(networkName)) {
                            etSsid.setError("SSID Empty");

                        }
                        if(TextUtils.isEmpty(password)){
                            etPassword.setError("PASSWORD Empty");
                        }
                        if(TextUtils.isEmpty(networkName) == false && TextUtils.isEmpty(password) == false) {
                            dialog.dismiss();
                            goForProvisioning(networkName, password);
                        }


                    }
                });
                Button buttonNegative = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE);
                buttonNegative.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
        });

        alertDialog.show();
    }


    private void goForProvisioning(String ssid, String password) {
        Log.d(TAG, "SSID: " + ssid);
        Log.d(TAG, "PASSWORD: " + password);
        progressBar.setVisibility(View.VISIBLE);

        provisionManager.getEspDevice().sendDataToCustomEndPoint("name", EditNodeName.getText().toString().getBytes(), new ResponseListener() {
            @Override
            public void onSuccess(byte[] returnData) {
                Log.d(TAG, "Success");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, e.getMessage());
            }
        });

        provisionManager.getEspDevice().provision(ssid, password, new ProvisionListener() {

            @Override
            public void createSessionFailed(Exception e) {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(NewNodeActivity.this, "Session Failed", Toast.LENGTH_LONG).show();
                        if (provisionManager.getEspDevice() != null) {
                            provisionManager.getEspDevice().disconnectDevice();
                        }
                        finish();
                    }
                });
            }

            @Override
            public void wifiConfigSent() {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Log.d(TAG, "WiFi Credentials Sent");
                    }
                });
            }

            @Override
            public void wifiConfigFailed(Exception e) {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(NewNodeActivity.this, "WiFi Credentials Send Failed", Toast.LENGTH_LONG).show();
                        if (provisionManager.getEspDevice() != null) {
                            provisionManager.getEspDevice().disconnectDevice();
                        }
                    }
                });
            }

            @Override
            public void wifiConfigApplied() {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Log.d(TAG, "WiFi Credentials Applied");
                    }
                });
            }

            @Override
            public void wifiConfigApplyFailed(Exception e) {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(NewNodeActivity.this, "WiFi Credentials Incorrect", Toast.LENGTH_LONG).show();
                        if (provisionManager.getEspDevice() != null) {
                            provisionManager.getEspDevice().disconnectDevice();
                        }
                    }
                });
            }

            @Override
            public void provisioningFailedFromDevice(final ESPConstants.ProvisionFailureReason failureReason) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        switch (failureReason) {
                            case AUTH_FAILED:
                                Log.e(TAG, "Authentication Failed");
                                Toast.makeText(NewNodeActivity.this, "Authentication Failed", Toast.LENGTH_LONG).show();
                                break;
                            case NETWORK_NOT_FOUND:
                                Log.e(TAG, "Network Not Found");
                                Toast.makeText(NewNodeActivity.this, "Network Not Found", Toast.LENGTH_LONG).show();
                                break;
                            case DEVICE_DISCONNECTED:
                            case UNKNOWN:
                                Log.e(TAG, "Error Provisioning Step 3");;
                                Toast.makeText(NewNodeActivity.this, "Error: Please Try Again", Toast.LENGTH_LONG).show();
                                break;
                        }
                        progressBar.setVisibility(View.GONE);
                        if (provisionManager.getEspDevice() != null) {
                            provisionManager.getEspDevice().disconnectDevice();
                        }
                    }
                });
            }

            @Override
            public void deviceProvisioningSuccess() {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(NewNodeActivity.this, "Connection Success", Toast.LENGTH_LONG).show();
                        if (provisionManager.getEspDevice() != null) {
                            provisionManager.getEspDevice().disconnectDevice();
                        }
                        String word = EditNodeName.getText().toString();
                        Intent replyIntent = new Intent();
                        replyIntent.putExtra(Name_Reply, word);
                        setResult(RESULT_OK, replyIntent);
                        finish();
                    }
                });
            }

            @Override
            public void onProvisioningFailed(Exception e) {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(NewNodeActivity.this, "Connection Failed", Toast.LENGTH_LONG).show();
                        if (provisionManager.getEspDevice() != null) {
                            provisionManager.getEspDevice().disconnectDevice();
                        }
                    }
                });
            }
        });
    }

}