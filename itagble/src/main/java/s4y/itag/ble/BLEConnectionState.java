package s4y.itag.ble;

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
