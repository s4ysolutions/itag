package solutions.s4y.itag.ble;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

public final class Device implements Serializable {
    private static final long serialVersionUID = 1345673754421L;
    public enum Color {
        BLACK,
        WHITE,
        RED,
        GREEN,
        BLUE
    }

    public final String addr;
    public Color color;
    public String name;

    Device(BluetoothDevice device) {
        this.addr = device.getAddress();
        this.color=Color.WHITE;
        this.name="";
    }
}
