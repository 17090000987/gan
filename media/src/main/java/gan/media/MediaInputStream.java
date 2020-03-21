package gan.media;

import java.io.InputStream;

public interface MediaInputStream extends MediaListener {

    public String getUrl();

    public InputStream getInputStream();
}
