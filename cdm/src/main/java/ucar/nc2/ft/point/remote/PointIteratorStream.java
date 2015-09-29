/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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

  public PointFeature next() {
    if (null == pf) return null;
    calcBounds(pf);
    return pf;
  }

  public void setBufferSize(int bytes) {
  }

}

