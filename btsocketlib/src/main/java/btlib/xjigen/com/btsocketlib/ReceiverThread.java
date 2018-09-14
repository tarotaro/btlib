package btlib.xjigen.com.btsocketlib;


import android.bluetooth.BluetoothSocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

public abstract class ReceiverThread extends Thread {
    protected BluetoothSocket mSocket;
}
