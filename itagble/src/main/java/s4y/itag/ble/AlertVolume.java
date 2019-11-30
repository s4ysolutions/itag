package s4y.itag.ble;

enum AlertVolume {
    NO_ALERT((byte) 0x00),
    MEDIUM_ALERT((byte) 0x01),
    HIGH_ALERT((byte) 0x02);

    public final byte value;

    AlertVolume(byte value) {
        this.value = value;
    }

    static AlertVolume fromCharacteristic(BLECharacteristic characteristic) {
        int value = characteristic.int8();
        switch (value) {
            case 0:
                return NO_ALERT;
            case 1:
                return MEDIUM_ALERT;
            default:
                return HIGH_ALERT;
        }
    }
}
