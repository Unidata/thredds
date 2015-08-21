package ucar.nc2.ft.point.remote;

import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamProto;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.point.PointIteratorAbstract;

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

  private InputStream in;
  private FeatureMaker featureMaker;

  private PointFeature pf;
  private boolean finished = false;

  RemotePointFeatureIterator(InputStream in, FeatureMaker featureMaker) throws IOException {
    this.in = in;
    this.featureMaker = featureMaker;
  }

  public void close() {
    if (finished) return;
    if (in != null)
      try {
        in.close();
      } catch (IOException ioe) {
        //coverity[FB.DE_MIGHT_IGNORE]
      }
    in = null;
    finishCalcBounds();
    finished = true;
  }

  public boolean hasNext() {
    if (finished) return false;

    try {
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
        close();
        return false;

      } else if (mtype == PointStream.MessageType.Error) {
        int len = NcStream.readVInt(in);
        byte[] b = new byte[len];
        NcStream.readFully(in, b);
        NcStreamProto.Error proto = NcStreamProto.Error.parseFrom(b);
        String errMessage = NcStream.decodeErrorMessage(proto);

        pf = null;
        close();
        throw new IOException(errMessage);

      } else {
        pf = null;
        close();
        throw new IOException("Illegal pointstream message type= " + mtype); // maybe kill the socket ?
      }

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public PointFeature next() {
    if (null == pf) return null;
    calcBounds(pf);
    return pf;
  }

  public void setBufferSize(int bytes) {
  }

}

