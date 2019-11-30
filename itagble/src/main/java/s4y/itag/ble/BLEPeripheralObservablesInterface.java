package s4y.itag.ble;

import s4y.rasat.Observable;

interface BLEPeripheralObservablesInterface {
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
        final BLEService[] services;
        final int status;

        public DiscoveredServicesEvent(BLEService[] services, int status) {
            this.services = services;
            this.status = status;
        }
    }

    class CharacteristicEvent {
        final BLECharacteristic characteristic;
        final int status;

        public CharacteristicEvent(BLECharacteristic characteristic, int status) {
            this.characteristic = characteristic;
            this.status = status;
        }
    }

    Observable<ConnectedEvent> observableConnected();
    Observable<ConnectionFailedEvent> observableConnectionFailed();
    Observable<DisconnectedEvent> observableDisconnected();
    Observable<DiscoveredServicesEvent> observableDiscoveredServices();
    Observable<CharacteristicEvent> observableWrite();
    Observable<CharacteristicEvent> observableNotification();
}
