package s4y.itag.ble;

public enum BLEError {
    ok,
    timeout,
    noPeripheral,
    noManager,
    noGatt,
    noImmediateAlertCharacteristic,
    noFindMeAlertCharacteristic,
    badStatus
}
