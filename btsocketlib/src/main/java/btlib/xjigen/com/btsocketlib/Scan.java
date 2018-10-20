package btlib.xjigen.com.btsocketlib;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.UUID;

import static android.bluetooth.le.ScanSettings.*;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Scan extends ScanCallback {

    private ArrayList<BluetoothDevice> devices;
    private Context _context;
    private BLEClient client;
    BluetoothManager manager;
    BluetoothAdapter adapter;
    BluetoothLeScanner scanner;



    public Scan(){
        client = new BLEClient();
    }

    public void connect(int index) {
        scanner.stopScan(this);
        devices.get(index).connectGatt(_context, false, client);

    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void startScan(Context context) {
        _context = context;
        manager = (BluetoothManager) _context.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();
        scanner = adapter.getBluetoothLeScanner();
        ScanSettings settings = makeScanSettings();
        devices = new ArrayList<BluetoothDevice>();
        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUID.fromString(BtSocketLib.SERVICE_UUID_YOU_CAN_CHANGE))).build();
        ArrayList scanFilterList = new ArrayList();
        scanFilterList.add(scanFilter);
        scanner.startScan(scanFilterList,settings,this);

    }

    public BLEClient getBLEClient(){
        return client;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private ScanSettings makeScanSettings() {

        ScanSettings.Builder scanSettingBuiler = new ScanSettings.Builder();
        scanSettingBuiler.setScanMode(SCAN_MODE_LOW_LATENCY);
        return scanSettingBuiler.build();
    }


    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        super.onScanResult(callbackType,result);
        if(!devices.contains(result.getDevice())) {
            devices.add(result.getDevice());
        }
        client.connectInterface.callBackSearch(devices);
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
    }

}
