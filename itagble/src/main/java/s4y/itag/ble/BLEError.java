package s4y.itag.ble;

enum BLEError {
    ok,
    timeout,
    noPeripheral,
    noAdapter,
    noGatt,
    notConnected,
    noImmediateAlertCharacteristic,
    noFindMeAlertCharacteristic,
    badStatus
}
