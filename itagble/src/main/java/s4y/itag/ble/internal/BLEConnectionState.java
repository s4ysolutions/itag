package s4y.itag.ble.internal;

public enum BLEConnectionState {
    unknown,
    disconnected,
    connecting,
    disconnecting,
    discovering,
    discoveringServices,
    discoveringCharacteristics,
    connected,
    writting,
    reading
}
