package s4y.itag.ble;

import solutions.s4y.rasat.Channel;
import solutions.s4y.rasat.Observable;

class BLEPeripheralObservables implements BLEPeripheralObservablesInterface {

    final Channel<ConnectedEvent> channelConnected = new Channel<>();
    final Channel<BLEPeripheralObservablesInterface.ConnectionFailedEvent> channelConnectionFailed = new Channel<>();
    final Channel<BLEPeripheralObservablesInterface.DisconnectedEvent> channelDisconnected = new Channel<>();
    final Channel<BLEPeripheralObservablesInterface.DiscoveredServicesEvent> channelDiscoveredServices = new Channel<>();
    final Channel<BLEPeripheralObservablesInterface.CharacteristicEvent> channelWrite = new Channel<>();
    final Channel<BLECharacteristic> channelNotification = new Channel<>();
    final Channel<BLEPeripheralObservablesInterface.RSSIEvent> channelRSSI = new Channel<>();

    @Override
    public Observable<ConnectedEvent> observableConnected() {
        return channelConnected.observable;
    }

    @Override
    public Observable<BLEPeripheralObservablesInterface.ConnectionFailedEvent> observableConnectionFailed() {
        return channelConnectionFailed.observable;
    }

    @Override
    public Observable<BLEPeripheralObservablesInterface.DisconnectedEvent> observableDisconnected() {
        return channelDisconnected.observable;
    }

    @Override
    public Observable<BLEPeripheralObservablesInterface.DiscoveredServicesEvent> observableDiscoveredServices() {
        return channelDiscoveredServices.observable;
    }

    @Override
    public Observable<BLEPeripheralObservablesInterface.CharacteristicEvent> observableWrite() {
        return channelWrite.observable;
    }

    @Override
    public Observable<BLECharacteristic> observableNotification() {
        return channelNotification.observable;
    }

    @Override
    public Observable<BLEPeripheralObservablesInterface.RSSIEvent> observableRSSI() {
        return channelRSSI.observable;
    }
}
