/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.cdmr;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.stream.CdmRemote;
import ucar.unidata.util.test.category.NeedsRdaData;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Describe
 *
 * @author caron
 * @since 12/24/2015.
 */
@Category(NeedsRdaData.class)
public class TestCdmrTiming {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  String local = TestOnLocalServer.withHttpPath("cdmremote/rdavmWork/yt.oper.an.sfc.regn400sc.10v_166.200805");
  String rdavm = "http://rdavm.ucar.edu:8080/thredds/cdmremote/files/e/ds629.1/yt.oper.an.sfc/2008/yt.oper.an.sfc.regn400sc.10v_166.200805";
  String cdmUrl = local;

  @Test
  public void readIndexSpace() throws IOException, InvalidRangeException {
    timeDataRead(cdmUrl, 1);
    timeDataRead(cdmUrl, 2);
    timeDataRead(cdmUrl, 10);
  }

  static int timeDataRead(String remote, int  stride) throws IOException, InvalidRangeException {
    logger.debug("--CdmRemote Read {} stride={}", remote, stride);

    try (NetcdfFile ncremote = new CdmRemote(remote)) {

      String gridName = "10_metre_V_wind_component_surface";
      Variable vs = ncremote.findVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      Section section = vs.getShapeAsSection();
      Assert.assertEquals(3, section.getRank());
      Section want = new Section().appendRange(1);
      want.appendRange(section.getRange(1).setStride(stride));
      want.appendRange(section.getRange(2).setStride(stride));

      long start = System.currentTimeMillis();

      Array data = vs.read(want);

      long took = System.currentTimeMillis() - start;
      logger.debug("took={} size={}", took, data.getSize());

    }
    return 1;
  }

  @Test
  public void readCoordSpace() throws IOException, InvalidRangeException {
    timeDataRead2(cdmUrl, 1);
    //timeDataRead2(cdmUrl, 2);
    //timeDataRead2(cdmUrl, 10);
  }

  static int timeDataRead2(String remote, int  stride) throws IOException, InvalidRangeException {
    logger.debug("--Coverage Read {} stride={} ", remote, stride);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(remote)) {
      CoverageCollection gcs = cc.getSingleCoverageCollection();

      String gridName = "10_metre_V_wind_component_surface";
      Coverage grid = gcs.findCoverage(gridName);
      Assert.assertNotNull(gridName, grid);

      SubsetParams params  = new SubsetParams().setTimePresent().setHorizStride(stride);

      long start = System.currentTimeMillis();
      GeoReferencedArray geo = grid.readData(params);

      long took = System.currentTimeMillis() - start;
      logger.debug("took={} size={}", took, geo.getData().getSize());

    }
    return 1;
  }
}
