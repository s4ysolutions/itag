package solutions.s4y.itag.ble;

import android.bluetooth.BluetoothDevice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public final class ITagDevice implements Serializable {
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
    public boolean linked;

    ITagDevice(final @NotNull BluetoothDevice device, @Nullable  final ITagDevice oldDevice) {
        this.addr = device.getAddress();
        this.color=oldDevice==null?Color.WHITE:oldDevice.color;
        this.name=oldDevice==null?"":oldDevice.name;
        this.linked= oldDevice != null && oldDevice.linked;
    }
}
