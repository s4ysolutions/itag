package s4y.itag.ble;

public enum BLEError {
    ok,
    timeout,
    noPeripheral,
    noGatt,
    noImmediateAlertCharacteristic,
    noFindMeAlertCharacteristic,
    badStatus
}
