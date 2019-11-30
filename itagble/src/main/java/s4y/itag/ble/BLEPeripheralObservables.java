package s4y.itag.ble;

import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import s4y.rasat.Channel;
import s4y.rasat.Observable;

class BLEPeripheralObservables implements BLEPeripheralObservablesInterface {

    class ConnectedEvent {
    }

    class ConnectionFailedEvent {
        final int status;

        public ConnectionFailedEvent(int status) {
            this.status = status;
        }
    }

    class DisconnectedEvent {
        final int status;

        public DisconnectedEvent(int status) {
            this.status = status;
        }
    }

    class DiscoveredServicesEvent {
        final CBService[] services;
        final int status;

        public DiscoveredServicesEvent(CBService[] services, int status) {
            this.services = services;
            this.status = status;
        }
    }

    class CharacteristicEvent {
        final CBCharacteristic characteristic;
        final int status;

        public CharacteristicEvent(CBCharacteristic characteristic, int status) {
            this.characteristic = characteristic;
            this.status = status;
        }
    }

    private final Channel<BLEPeripheralObservablesInterface.ConnectedEvent> channelConnected = new Channel<>();
    private final Channel<BLEPeripheralObservablesInterface.ConnectionFailedEvent> channelConnectionFailed = new Channel<>();
    private final Channel<BLEPeripheralObservablesInterface.DisconnectedEvent> channelDisconnected = new Channel<>();
    private final Channel<BLEPeripheralObservablesInterface.DiscoveredServicesEvent> channelDiscoveredServices = new Channel<>();
    private final Channel<BLEPeripheralObservablesInterface.CharacteristicEvent> channelWrite = new Channel<>();
    private final Channel<BLEPeripheralObservablesInterface.CharacteristicEvent> channelNotification = new Channel<>();

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
