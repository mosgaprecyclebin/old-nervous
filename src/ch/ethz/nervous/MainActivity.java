package ch.ethz.nervous;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class MainActivity extends Activity {
    // Debugging
    private static final String TAG = "MainActivity";
    private static final boolean D = true;

    // Member fields
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> devices;
    private FileOutputStream log;
    private WifiManager wifiManager;
    private boolean bluetoothFinished, wifiFinished;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        devices = new ArrayAdapter<String>(this, R.layout.device_name);

        ListView newDevicesListView = (ListView) findViewById(R.id.devices);
        newDevicesListView.setAdapter(devices);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        wifiManager = (WifiManager) getBaseContext().getSystemService(Context.WIFI_SERVICE);
        registerReceiver(mReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onStart() {
    	super.onStart();
        try {
        	File dir = new File(Environment.getExternalStorageDirectory(), "nervous");
        	dir.mkdir();
        	File file = new File(dir, "log.txt");
        	log = new FileOutputStream(file, true);
        } catch (IOException ex) {
        	ex.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
    	super.onStop();
    	try {
    		log.close();
    	} catch (IOException ex) {
    		ex.printStackTrace();
    	}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.title_scanning);

        if (bluetoothAdapter.isEnabled()) {
        	bluetoothAdapter.startDiscovery();
        } else {
        	bluetoothFinished = true;
        }

        if (wifiManager.isWifiEnabled()) {
        	wifiManager.startScan();
        } else {
        	wifiFinished = true;
        }

        checkIfFinished();
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    devices.add("BLUETOOTH: " + device.getName() + "\n" + device.getAddress());
                    log("bluetooth", device.getAddress(), device.getName());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (devices.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.nothing_found).toString();
                    devices.add(noDevices);
                }
            	bluetoothFinished = true;
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                List<ScanResult> wifis = wifiManager.getScanResults();
                for (ScanResult wifi : wifis) {
                    devices.add("WIFI: " + wifi.SSID + "\n" + wifi.BSSID);
                    log("wifi", wifi.BSSID, wifi.SSID);
                }
            	wifiFinished = true;
            }
            checkIfFinished();
        }
    };

    private void checkIfFinished() {
        if (bluetoothFinished && wifiFinished) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName("ch.ethz.iamscience", "ch.ethz.iamscience.IncreaseScoreActivity"));
            startActivity(intent);
        	setProgressBarIndeterminateVisibility(false);
        	setTitle(R.string.title_done);
        }
    }

    private void log(String type, String mac, String name) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        Calendar cal = Calendar.getInstance();
        String timestamp = dateFormat.format(cal.getTime());
        String line = type + "," + mac + "," + name + "," + timestamp + "\n";
        try {
        	log.write(line.getBytes());
        } catch (IOException ex) {
        	Log.println(Log.ERROR, "nervous", ex.getMessage());
        }
    }

}
