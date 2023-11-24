package s4y.itag.ble;

public enum BLEError {
    ok,
    timeout,
    noPeripheral,
    noAdapter,
    noGatt,
    notConnected,
    noImmediateAlertCharacteristic,
    noFindMeAlertCharacteristic,
    badStatus,
    noPermission,
}
