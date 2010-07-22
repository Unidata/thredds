package ucar.nc2.ft.point.remote;

import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamProto;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.point.PointIteratorAbstract;
import org.apache.commons.httpclient.HttpMethod;

import java.io.InputStream;
import java.io.IOException;

/**
 * Iterate through a stream of PointStream.MessageType.PointFeature until PointStream.MessageType.End
 *
 * @author caron
 * @since May 14, 2009
 */
public class RemotePointFeatureIterator extends PointIteratorAbstract {
  private static final boolean debug = false;

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

    PointStream.MessageType mtype = PointStream.readMagic(in);
    if (mtype == PointStream.MessageType.PointFeature) {
      int len = NcStream.readVInt(in);
      if (debug && (getCount() % 100 == 0))
        System.out.println(" RemotePointFeatureIterator len= " + len + " count = " + getCount());

      byte[] b = new byte[len];
      NcStream.readFully(in, b);

      pf = featureMaker.make(b);
      return true;

    } else if (mtype == PointStream.MessageType.End) {
      pf = null;
      finish();
      return false;
      
    } else if (mtype == PointStream.MessageType.Error) {
      int len = NcStream.readVInt(in);
      byte[] b = new byte[len];
      NcStream.readFully(in, b);
      NcStreamProto.Error proto = NcStreamProto.Error.parseFrom(b);
      String errMessage = NcStream.decodeErrorMessage(proto);

      pf = null;
      finish();
      throw new IOException(errMessage);

    } else {
      pf = null;
      finish();
      throw new IOException("Illegal pointstream message type= "+mtype); // maybe kill the socket ?
    }
  }

  public PointFeature next() throws IOException {
    if (null == pf) return null;
    calcBounds(pf);
    return pf;
  }

  public void setBufferSize(int bytes) {
  }

}

