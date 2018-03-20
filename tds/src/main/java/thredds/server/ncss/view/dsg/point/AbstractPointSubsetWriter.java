/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
