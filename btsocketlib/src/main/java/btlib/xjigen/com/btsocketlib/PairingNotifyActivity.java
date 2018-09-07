package btlib.xjigen.com.btsocketlib;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;

import com.unity3d.player.UnityPlayer;

public class PairingNotifyActivity extends Activity {
    private String pairingCallbackGameObject;
    private String pairingDelegateMethod;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent i = getIntent();
        pairingCallbackGameObject = i.getStringExtra("gameObjectName");
        pairingDelegateMethod = i.getStringExtra("delegateMethod");
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BtSocketLib.WAIT_TIME);
        startActivity(intent);
        startActivityForResult(intent, BtSocketLib.REQUEST_DISCOVERABLE_BT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BtSocketLib.REQUEST_DISCOVERABLE_BT) {
            if (resultCode == BtSocketLib.WAIT_TIME) {
                callBackPairing();
            }
        }
    }

    private void callBackPairing(){
        UnityPlayer.UnitySendMessage(pairingCallbackGameObject,pairingDelegateMethod,"");
    }
}
