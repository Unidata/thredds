package ucar.unidata.test.util;

import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * An enumeration of external THREDDS servers used during testing. Tests that call {@link #assumeIsAvailable} will be
 * ignored if the associated server is unavailable.
 *
 * @author cwardgar
 * @since 2015/04/18
 */
public enum ThreddsServer {
    LIVE("http://thredds.ucar.edu/thredds/"),
    TEST("http://thredds-test.unidata.ucar.edu/thredds/"),
    DEV("http://thredds-dev.unidata.ucar.edu/thredds/"),
    REMOTETEST("http://remotetest.unidata.ucar.edu/thredds/");

    private static final Logger logger = LoggerFactory.getLogger(ThreddsServer.class);

    private final String host;

    private Boolean available;

    ThreddsServer(String host) {
        this.host = host;
    }

    public boolean isAvailable() {
        if (available == null) {
            available = ping(5000);
        }

        return available;
    }

    /**
     * Checks if the host is available.
     *
     * @throws AssumptionViolatedException  if the server is not available, causing any JUnit test that this method is
     *      called in to be ignored. The testing report will list it as such.
     */
    public void assumeIsAvailable() throws AssumptionViolatedException {
        Assume.assumeTrue("Host isn't available: " + host, isAvailable());
    }

    /**
     * Pings a HTTP URL. This effectively sends a HEAD request and returns <code>true</code> if the response code is in
     * the 200-399 range.
     *
     * @param timeout The timeout in millis for both the connection timeout and the response read timeout. Note that
     *                the total timeout is effectively two times the given timeout.
     * @return <code>true</code> if the given HTTP URL has returned response code 200-399 on a HEAD request within the
     *         given timeout, otherwise <code>false</code>.
     * @see <a href="http://stackoverflow.com/questions/3584210/preferred-java-way-to-ping-a-http-url-for-availability">
     *      Stack Overflow question</a>.
     */
    public boolean ping(int timeout) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(host).openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("HEAD");

            int responseCode = connection.getResponseCode();
            if (200 <= responseCode && responseCode <= 399) {
                return true;
            } else {
                String message = String.format("Received unexpected response (%s: %s) from host: %s",
                        responseCode, connection.getResponseMessage(), host);
                logger.warn(message);
                return false;
            }
        } catch (Exception e) {
            logger.warn(String.format("Couldn't reach host within %dms: %s", timeout, host), e);
            return false;
        }
    }
}
