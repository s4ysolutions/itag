package s4y.itag.ble;

enum BLEConnectionState {
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
