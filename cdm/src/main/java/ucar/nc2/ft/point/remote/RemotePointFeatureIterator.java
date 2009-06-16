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
  private boolean finished = false;

  RemotePointFeatureIterator(HttpMethod method, InputStream in, FeatureMaker featureMaker) throws IOException {
    this.method = method;
    this.in = in;
    this.featureMaker = featureMaker;
  }

  public void finish() {
    if (finished) return;
    if (method != null)
      method.releaseConnection();
    method = null;
    finishCalcBounds();
    finished = true;
  }

  public boolean hasNext() throws IOException {
    if (finished) return false;
    
    int len = NcStream.readVInt(in);
    //System.out.println(" RemotePointFeatureIterator len= " + len+ " count = "+count);
    if (len <= 0) {
      pf = null;
      finish();
      return false;
    }

    byte[] b = new byte[len];
    NcStream.readFully(in, b);

    pf = featureMaker.make(b);
    return true;
  }

  public PointFeature next() throws IOException {
    if (null == pf) return null;
    calcBounds(pf);
    return pf;
  }

  public void setBufferSize(int bytes) {
  }

}

