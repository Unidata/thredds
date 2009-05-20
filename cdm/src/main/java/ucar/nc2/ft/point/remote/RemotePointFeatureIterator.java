package ucar.nc2.ft.point.remote;

import ucar.nc2.stream.NcStream;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.point.PointIteratorAbstract;
import org.apache.commons.httpclient.HttpMethod;

import java.io.InputStream;
import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since May 14, 2009
 */
public class RemotePointFeatureIterator extends PointIteratorAbstract {
  private HttpMethod method;
  private InputStream in;
  private FeatureMaker featureMaker;

  private PointFeature pf;
  int count = 0;

  RemotePointFeatureIterator(HttpMethod method, InputStream in, FeatureMaker featureMaker) throws IOException {
    this.method = method;
    this.in = in;
    this.featureMaker = featureMaker;
  }

  public void finish() {
    if (method != null)
      method.releaseConnection();
    method = null;
  }

  public boolean hasNext() throws IOException {
    int len = NcStream.readVInt(in);
    if (len <= 0) {
      System.out.println(" total read= " + count);
      finish();
      pf = null;
      return false;
    }

    byte[] b = new byte[len];
    NcStream.readFully(in, b);

    pf = featureMaker.make(b);
    //System.out.println(" count= " + count + " pf=" + pf);
    count++;
    return true;
  }

  public PointFeature next() throws IOException {
    if (pf != null)
      calcBounds(pf);
    return pf;
  }

  public void setBufferSize(int bytes) {
  }

}

