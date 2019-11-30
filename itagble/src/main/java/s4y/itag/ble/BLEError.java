package s4y.itag.ble;

public enum BLEError {
    ok,
    timeout,
    noPeripheral,
    noManager,
    noGatt,
    notConnected,
    noImmediateAlertCharacteristic,
    noFindMeAlertCharacteristic,
    badStatus
}
