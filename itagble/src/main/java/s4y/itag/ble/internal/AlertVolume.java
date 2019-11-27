package s4y.itag.ble.internal;

enum AlertVolume {
    NO_ALERT((byte)0x00),
    MEDIUM_ALERT((byte)0x01),
    HIGH_ALERT((byte)0x02);

    public final byte value;
    AlertVolume(byte value) {
        this.value = value;
    }
}
