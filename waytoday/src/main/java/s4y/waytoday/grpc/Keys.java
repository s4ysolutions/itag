package s4y.waytoday.grpc;

import io.grpc.Metadata;

public class Keys {
    public static final Metadata.Key<String> wsseKey = Metadata.Key.of("wsse", Metadata.ASCII_STRING_MARSHALLER);
}
