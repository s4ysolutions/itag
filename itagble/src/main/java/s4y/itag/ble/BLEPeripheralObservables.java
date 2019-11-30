package s4y.itag.ble;

import s4y.rasat.Channel;
import s4y.rasat.Observable;

class BLEPeripheralObservables implements BLEPeripheralObservablesInterface {

    final Channel<BLEPeripheralObservablesInterface.ConnectedEvent> channelConnected = new Channel<>();
    final Channel<BLEPeripheralObservablesInterface.ConnectionFailedEvent> channelConnectionFailed = new Channel<>();
    final Channel<BLEPeripheralObservablesInterface.DisconnectedEvent> channelDisconnected = new Channel<>();
    final Channel<BLEPeripheralObservablesInterface.DiscoveredServicesEvent> channelDiscoveredServices = new Channel<>();
    final Channel<BLEPeripheralObservablesInterface.CharacteristicEvent> channelWrite = new Channel<>();
    final Channel<BLEPeripheralObservablesInterface.CharacteristicEvent> channelNotification = new Channel<>();

    @Override
    public Observable<BLEPeripheralObservablesInterface.ConnectedEvent> observableConnected() {
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
    public Observable<BLEPeripheralObservablesInterface.CharacteristicEvent> observableNotification() {
        return channelNotification.observable;
    }
}
