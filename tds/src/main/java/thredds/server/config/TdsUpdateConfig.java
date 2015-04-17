package thredds.server.config;

import org.springframework.stereotype.Component;

/**
 * Created by sarms on 02/13/2015.
 */

@Component
public class TdsUpdateConfig {
    // fetch tds version info from Unidata and log it.
    private boolean logVersionInfo;

    public boolean isLogVersionInfo() {
        return logVersionInfo;
    }

    public void setLogVersionInfo(boolean logVersionInfo) {
        this.logVersionInfo = logVersionInfo;
    }

}
