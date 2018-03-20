/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point.remote;

import java.io.IOException;
import java.io.InputStream;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.point.CollectionInfo;
import ucar.nc2.ft.point.DsgCollectionImpl;
import ucar.nc2.ft.point.PointIteratorAbstract;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamProto;

/**
 * Iterate through a stream of PointStream.MessageType.PointFeature until PointStream.MessageType.End
 *
 * @author caron
 * @since May 14, 2009
 */
public class PointIteratorStream extends PointIteratorAbstract {

  private DsgFeatureCollection dsg;
  private InputStream in;
  private FeatureMaker featureMaker;

  private PointFeature pf;
  private boolean finished = false;

  PointIteratorStream(DsgCollectionImpl dsg, InputStream in, FeatureMaker featureMaker) throws IOException {
    this.dsg = dsg;
    this.in = in;
    this.featureMaker = featureMaker;
    CollectionInfo info = dsg.getInfo();
    if (!info.isComplete()) setCalculateBounds(info);
  }

  @Override
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

  @Override
  public boolean hasNext() {
    if (finished) return false;

    try {
      PointStream.MessageType mtype = PointStream.readMagic(in);
      if (mtype == PointStream.MessageType.PointFeature) {
        int len = NcStream.readVInt(in);

        byte[] b = new byte[len];
        NcStream.readFully(in, b);

        pf = featureMaker.make(dsg, b);
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

  @Override
  public PointFeature next() {
    if (null == pf) return null;
    calcBounds(pf);
    return pf;
  }
}

