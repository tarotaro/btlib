package btlib.xjigen.com.btsocketlib;


import android.bluetooth.BluetoothSocket;
import android.support.v4.util.CircularArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

public abstract class ReceiverThread extends Thread {
    protected BluetoothSocket mSocket;
    protected CircularArray buffer = new CircularArray<Integer>(512);
    protected void sendData(byte data[]) throws IOException {
        OutputStream os = mSocket.getOutputStream();
        os.write(data);
    }

    protected CircularArray getBuffer(){
        return buffer;
    }

    protected void loop() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        int value;
        while((value = br.read())!= -1){
            Integer i = Integer.valueOf(value);
            buffer.addFirst(i);
        }
    }

}
