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
package thredds.server.ncss.view.dsg.point;

import thredds.server.ncss.exception.NcssException;
import thredds.server.ncss.view.dsg.DsgSubsetWriter;
import ucar.nc2.ft.*;
import ucar.nc2.ft2.coverage.SubsetParams;

import java.io.IOException;
import java.util.List;

/**
 * Created by cwardgar on 2014/06/02.
 */
public abstract class AbstractPointSubsetWriter extends DsgSubsetWriter {
  protected final PointFeatureCollection pointFeatureCollection;

  public AbstractPointSubsetWriter(FeatureDatasetPoint fdPoint, SubsetParams ncssParams)
          throws NcssException, IOException {
    super(fdPoint, ncssParams);

    List<DsgFeatureCollection> featColList = fdPoint.getPointFeatureCollectionList();
    assert featColList.size() == 1 : "Is there ever a case when this is NOT 1?";
    assert featColList.get(0) instanceof PointFeatureCollection :
            "This class only deals with PointFeatureCollections.";

    this.pointFeatureCollection = (PointFeatureCollection) featColList.get(0);
  }

  public abstract void writeHeader(PointFeature pf) throws Exception;

  public abstract void writePoint(PointFeature pointFeat) throws Exception;

  public abstract void writeFooter() throws Exception;

  @Override
  public void write() throws Exception {

    // Perform spatial and temporal subset.
    PointFeatureCollection subsettedPointFeatColl =
            pointFeatureCollection.subset(ncssParams.getLatLonBoundingBox(), wantedRange);
    if (subsettedPointFeatColl == null) // means theres nothing in the subset
      return;

    int count = 0;
    boolean headerDone = false;
    for (PointFeature pointFeat : subsettedPointFeatColl) {
      if (!headerDone) {
        writeHeader(pointFeat);
        headerDone = true;
      }
      writePoint(pointFeat);
      count++;
    }

    if (count == 0)
      throw new NcssException("No features are in the requested subset");

    writeFooter();
  }
}
