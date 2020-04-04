package gan.media.parser;

import java.nio.ByteBuffer;

public interface PacketParser {
    public void parse(ByteBuffer packet, int offset, int length);
    public void stop();
}
