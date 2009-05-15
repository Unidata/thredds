package ucar.nc2.ft.point.remote;

import ucar.nc2.stream.NcStream;
import org.apache.commons.httpclient.HttpMethod;

import java.io.InputStream;
import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since May 14, 2009
 */
public class RemoteIterator {
  private boolean debug = true;

  private HttpMethod method;
  private InputStream in;

  int count = 0;

  RemoteIterator(HttpMethod method, InputStream in) throws IOException {
    this.method = method;
    this.in = in;
  }

  public byte[] getNext() throws IOException {
    int len = NcStream.readVInt(in);
    if (len <= 0) {
      if (debug) System.out.println(" total read= " + count);
      finish();
      return null;
    }

    byte[] b = new byte[len];
    NcStream.readFully(in, b);
    count++;
    return b;
  }

  public void finish() {
    if (method != null)
      method.releaseConnection();
    method = null;
  }

}

