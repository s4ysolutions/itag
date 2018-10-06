package solutions.s4y.itag.ble;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

public final class Device implements Serializable {
    private static final long serialVersionUID = 1345673754421L;

    final String addr;

    Device(BluetoothDevice device) {
        this.addr = device.getAddress();
    }
}
