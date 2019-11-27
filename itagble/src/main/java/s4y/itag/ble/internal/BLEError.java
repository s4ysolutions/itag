package s4y.itag.ble.internal;

public enum BLEError {
    ok,
    timeout,
    noPeripheral,
    noGatt,
    badUUID,
    noImmediateAlertService,
    noFindMeAlertService,
    noImmediateAlertCharacteristic,
    noFindMeAlertCharacteristic
}
