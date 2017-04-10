package thredds.server.config;


import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by rmay on 12/2/14.
 *  @author rmay
 * @since 12/2/14.
 */

@Component
public class JupyterConfigBean {
    private int timeout;
    private Path pythonPath;

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Path getPythonPath() {
        return pythonPath;
    }

    public void setPythonPath(String pythonPath) {
        this.pythonPath = Paths.get(pythonPath);
    }
}
