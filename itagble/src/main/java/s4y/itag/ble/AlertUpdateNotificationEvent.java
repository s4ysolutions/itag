package s4y.itag.ble;

class AlertUpdateNotificationEvent {
    final String id;
    final AlertVolume volume;

    public AlertUpdateNotificationEvent(String id, AlertVolume volume) {
        this.id = id;
        this.volume = volume;
    }
}
