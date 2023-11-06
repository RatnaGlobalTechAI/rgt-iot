package com.example.myapplication;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Button on;
    private Button off;
    private Button paired;
    private BluetoothAdapter bluetoothAdapter;
    BroadcastReceiver receiver;
    private ListView mDevicesListView;
    ArrayAdapter<String> arrayAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ActivityResultLauncher<Intent> someActivityResultLauncher;
    private HashMap<String, String> scannedDevices = new HashMap<>();
    List<String> arrayList = new ArrayList<>();
    private boolean pairedCheck = false;

    private RequestQueue mRequestQueue;
    private StringRequest mStringRequest;
    private String url = "https://tinyurl.com/vpzncd7x";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        on = (Button) findViewById(R.id.on);
        off = (Button) findViewById(R.id.off);
        paired = (Button) findViewById(R.id.paired);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mDevicesListView = (ListView) findViewById(R.id.devices_list);

        arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, arrayList);
        mDevicesListView.setAdapter(arrayAdapter);
//        mDevicesListView.setBackgroundColor(Color.WHITE);
        mDevicesListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    String itemAtPosition = (String) mDevicesListView.getItemAtPosition(position);
                    Toast.makeText(getApplicationContext(), itemAtPosition, Toast.LENGTH_SHORT).show();
                    BluetoothDevice dev = null;
                    if(mPairedDevices!=null){
                        for (BluetoothDevice device : mPairedDevices) {
                            String name = device.getName();
                            if (name.equals(itemAtPosition)){
                                dev = device;
                            }
                        }
                    }

                    if(dev!=null) {
                        if(dev.getName().equals(itemAtPosition)){
                            try {
                                removeBond(dev);
                                arrayAdapter.remove(dev.getName());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else if (scannedDevices.containsKey(itemAtPosition)) {
                        String scannedAddress = scannedDevices.get(itemAtPosition);
                        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                        BluetoothDevice mBluetoothDevice = bluetoothManager.getAdapter().getRemoteDevice(scannedAddress);
                        connect(mBluetoothDevice);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please Enable bluetooth", Toast.LENGTH_SHORT).show();
                }

            }
        });

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
        }

        boolean locationStatus = enableLocation(getApplicationContext());

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (ContextCompat.checkSelfPermission(MainActivity.this, BLUETOOTH_SCAN) == PERMISSION_GRANTED) {
                        BluetoothDevice prev_device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if ((device.getName() != null) && (device.getName().length() > 0)) {
                            if (arrayAdapter.getPosition(device.getName()) == -1) {
                                boolean b = arrayAdapter.getPosition(device.getName()) == -1;
                                Log.d("duplication", b + " " + device.getAddress());
                                String abc = device.getName();
                                arrayAdapter.add(abc);
                                scannedDevices.put(abc, device.getAddress());
                                arrayAdapter.notifyDataSetChanged();
                            } else {
                                Log.d("duplication", device.getAddress());
                            }
                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{BLUETOOTH_SCAN}, 1);
                        }
                    }
                }
            }
        };

        off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!locationStatus) {
                    onL();
                }
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), BLUETOOTH_SCAN) == PERMISSION_GRANTED) {
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                        Toast.makeText(getApplicationContext(), "Bluetooth Scan is Stopped", Toast.LENGTH_SHORT).show();
                    } else {
                        if (pairedCheck) {
                            arrayAdapter.clear();
                            pairedCheck = false;
                        }
                        bluetoothAdapter.startDiscovery();
                    }
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{BLUETOOTH_SCAN}, 1);
                }
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arrayAdapter.clear();
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    enableBT(enableBtIntent);
                    String BT = "Bluetooth enabled";
                    Toast.makeText(getApplicationContext(), "Bluetooth turned On", Toast.LENGTH_SHORT).show();
                    if (locationStatus) {

                    } else {
                        Toast.makeText(getApplicationContext(), "Please Enable Location", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth turned Off", Toast.LENGTH_SHORT).show();
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        bluetoothAdapter.disable();
                        return;
                    }
                }
            }
        });

        paired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                listPairedDevices();
            }
        });
    }

    public void enableBT(Intent enableBtIntent) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{BLUETOOTH_CONNECT}, 2);
                return;
            }
        }
        startActivityForResult(enableBtIntent, 1);
    }

    public boolean enableLocation(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    public void onL() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // Build the alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Your GPS seems to be disabled, do you want to enable it?");
            builder.setMessage("TO Scan devices please enable Location Services and GPS");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user acknowledges the alert dialog
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
    }

    private Boolean connect(BluetoothDevice bdDevice) {
        Boolean bool = false;
        try {
            Log.i("Log", "service method is called ");
            Class cl = Class.forName("android.bluetooth.BluetoothDevice");
            Class[] par = {};
            Method method = cl.getMethod("createBond", par);
            Object[] args = {};
            bool = (Boolean) method.invoke(bdDevice);//, args);// this invoke creates the detected devices paired.
            Log.i("Log", "This is: "+bool.booleanValue());
            //Log.i("Log", "devicesss: "+bdDevice.getName());
        } catch (Exception e) {
            Log.i("Log", "Inside catch of serviceFromDevice Method");
            e.printStackTrace();
        }
        return bool.booleanValue();
    };

    public boolean createBond(BluetoothDevice btDevice)
            throws Exception
    {
        Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = class1.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    public boolean removeBond(BluetoothDevice btDevice)
            throws Exception
    {
        Class btClass = Class.forName("android.bluetooth.BluetoothDevice");
        Method removeBondMethod = btClass.getMethod("removeBond");
        Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    private void listPairedDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{BLUETOOTH_CONNECT}, 2);
            return;
        }else{
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                Toast.makeText(getApplicationContext(), "Bluetooth Scan is Stopped", Toast.LENGTH_SHORT).show();
            }
            arrayAdapter.clear();
            pairedCheck= true;
            mPairedDevices = bluetoothAdapter.getBondedDevices();
            if(bluetoothAdapter.isEnabled()) {
                // put it's one to the adapter
                for (BluetoothDevice device : mPairedDevices)
                    arrayAdapter.add(device.getName());

                Toast.makeText(getApplicationContext(), "paired devices", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getApplicationContext(), "Please Enable blutooth", Toast.LENGTH_SHORT).show();
            }
        }
    }

}

