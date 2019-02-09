package solutions.s4y.waytoday.grpc;

import androidx.annotation.NonNull;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GRPCChannelProvider {
    private final boolean tls;
    private final String host;
    private final int port;

    GRPCChannelProvider(@NonNull String host, int port) {
        this.host = host;
        this.port = port;
        tls = (port % 1000) > 100;
    }

    public ManagedChannel channel() {
        ManagedChannelBuilder channelBuilder = ManagedChannelBuilder
                .forAddress(host, port);
        if (!tls)
            channelBuilder.usePlaintext();
        return channelBuilder.build();
    }
}
