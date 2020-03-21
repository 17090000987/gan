package gan.media.mp4;

import gan.media.MediaOutputInfo;
import gan.media.MediaOutputStream;
import gan.media.MediaOutputStreamRunnable1;

public class MediaOutputStreamRunnableMp4 extends MediaOutputStreamRunnable1 {

    public boolean headWrited = false;

    public MediaOutputStreamRunnableMp4(MediaOutputStream out, MediaOutputInfo mediaSession) {
        super(out, mediaSession,32768, MediaOutputStreamRunnable1.PacketType_Mp4);
    }

    public void writeHead(byte[] head1, byte[] head2){
        if(headWrited){
           return;
        }
        putPacket((byte)0,head1,0, head1.length, 0);
        putPacket((byte)0,head2,0, head2.length, 0);
        headWrited = true;
    }

}
