package gan.media.parser;

import java.io.IOException;
import java.io.InputStream;

public interface StreamParser {
    public void start();
    public void inputStream(InputStream is) throws IOException;
    public void stop();
}
