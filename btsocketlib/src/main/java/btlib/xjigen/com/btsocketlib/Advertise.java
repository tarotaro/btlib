package btlib.xjigen.com.btsocketlib;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;
import android.os.ParcelUuid;

import java.util.UUID;

import android.content.Context;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Advertise extends AdvertiseCallback {


    //アドバタイズの設定
    private static final boolean CONNECTABLE = true;
    private static final int TIMEOUT = 0;

    //BLE
    private BluetoothLeAdvertiser advertiser;
    private BluetoothGattServer gattServer;
    private BLEServer server;
    private ConnectInterface connectInterface;

    //アドバタイズを開始
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void startAdvertise(Context context,ConnectInterface _connectInterface) {

        //BLE各種を取得
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        advertiser = getAdvertiser(adapter);
        connectInterface = _connectInterface;
        server = new BLEServer(connectInterface);
        gattServer = getGattServer(context, manager);
        server.setGattServer(gattServer);



        //UUIDを設定
        setUuid();

        //アドバタイズを開始
        advertiser.startAdvertising(makeAdvertiseSetting(),makeAdvertiseData(),this);
    }

    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        super.onStartSuccess(settingsInEffect);
    }

    @Override
    public void onStartFailure(int errorCode) {
        super.onStartFailure(errorCode);
    }

    public BLEServer getBLEServer(){
        return server;
    }

    //アドバタイズを停止
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void stopAdvertise() {
        //アドバタイズを停止
        if (advertiser != null) {
            advertiser.stopAdvertising(this);
            advertiser = null;

        }
    }

    //Advertiserを取得
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private BluetoothLeAdvertiser getAdvertiser(BluetoothAdapter adapter) {
        return adapter.getBluetoothLeAdvertiser();
    }

    //GattServerを取得
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private BluetoothGattServer getGattServer(Context context, BluetoothManager manager) {
        return manager.openGattServer(context,server);
    }

    //UUIDを設定
    private void setUuid() {

        //serviceUUIDを設定
        BluetoothGattService service = new BluetoothGattService(
                UUID.fromString(BtSocketLib.SERVICE_UUID_YOU_CAN_CHANGE),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        //characteristicUUIDを設定
        BluetoothGattCharacteristic characteristicR = new BluetoothGattCharacteristic(
                UUID.fromString(BtSocketLib.CHAR_READ_UUID_YOU_CAN_CHANGE),
                /*BluetoothGattCharacteristic.PROPERTY_NOTIFY |*/
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
        );

        BluetoothGattCharacteristic characteristicW = new BluetoothGattCharacteristic(
                UUID.fromString(BtSocketLib.CHAR_WRITE_UUID_YOU_CAN_CHANGE),
                /*BluetoothGattCharacteristic.PROPERTY_NOTIFY |*/
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                /*BluetoothGattDescriptor.PERMISSION_WRITE |*/
                BluetoothGattCharacteristic.PERMISSION_WRITE);


        /*BluetoothGattDescriptor dataDescriptor = new BluetoothGattDescriptor(
                UUID.fromString(BtSocketLib.CHAR_DESK_CONFIG_UUID_YOU_CAN_CHANGE)
                ,BluetoothGattDescriptor.PERMISSION_WRITE);

        characteristicW.addDescriptor(dataDescriptor);*/

        //characteristicUUIDをserviceUUIDにのせる
        service.addCharacteristic(characteristicR);
        service.addCharacteristic(characteristicW);

        //serviceUUIDをサーバーにのせる
        gattServer.addService(service);
    }

    //アドバタイズを設定
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private AdvertiseSettings makeAdvertiseSetting() {

        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();

        //アドバタイズモード
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        //アドバタイズパワー
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        //ペリフェラルへの接続を許可する
        builder.setConnectable(CONNECTABLE);

        //調査中。。
        builder.setTimeout(TIMEOUT);

        return builder.build();
    }

    //アドバタイズデータを作成
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private AdvertiseData makeAdvertiseData() {

        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.setIncludeTxPowerLevel(false);
        builder.addServiceUuid(new ParcelUuid(UUID.fromString(BtSocketLib.SERVICE_UUID_YOU_CAN_CHANGE)));

        return builder.build();
    }
}