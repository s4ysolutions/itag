package s4y.itag.ble;

enum BLEError {
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
