package btlib.xjigen.com.btsocketlib;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;

import static android.bluetooth.le.ScanSettings.*;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Scan extends ScanCallback {

    ArrayList<BluetoothDevice> devices;
    private Context _context;
    private BLEClient client;
    public void connect(int index) {
        client = new BLEClient();
        devices.get(index).connectGatt(_context, false, client);
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void startScan(Context context) {
        _context = context;
        BluetoothManager manager = (BluetoothManager) _context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        ScanSettings settings = makeScanSettings();
        devices = new ArrayList<BluetoothDevice>();
        scanner.startScan(null,settings,this);

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
        devices.add(result.getDevice());
    }

    @Override
    public void onScanFailed(int errorCode) {

    }

}
