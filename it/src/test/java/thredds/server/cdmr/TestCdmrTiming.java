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
 *
 */
package thredds.server.cdmr;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.TestWithLocalServer;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.stream.CdmRemote;
import ucar.unidata.util.test.category.NeedsRdaData;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 12/24/2015.
 */
@Category(NeedsRdaData.class)
public class TestCdmrTiming {
  String local = /* CdmRemote.SCHEME + */ TestWithLocalServer.server + "cdmremote/rdavmWork/yt.oper.an.sfc.regn400sc.10v_166.200805";
  String rdavm = "http://rdavm.ucar.edu:8080/thredds/cdmremote/files/e/ds629.1/yt.oper.an.sfc/2008/yt.oper.an.sfc.regn400sc.10v_166.200805";
  String cdmUrl = local;

  @Test
  public void readIndexSpace() throws IOException, InvalidRangeException {
    timeDataRead(cdmUrl, 1);
    timeDataRead(cdmUrl, 2);
    timeDataRead(cdmUrl, 10);
  }

  static int timeDataRead(String remote, int  stride) throws IOException, InvalidRangeException {
    System.out.printf("--CdmRemote Read %s stride=%d ", remote, stride);

    try ( NetcdfFile ncremote = new CdmRemote(remote)) {

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
      System.out.printf(" took=%d size=%d %n", took, data.getSize());

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
    System.out.printf("--Coverage Read %s stride=%d ", remote, stride);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(remote)) {
      CoverageCollection gcs = cc.getSingleCoverageCollection();

      String gridName = "10_metre_V_wind_component_surface";
      Coverage grid = gcs.findCoverage(gridName);
      Assert.assertNotNull(gridName, grid);

      SubsetParams params  = new SubsetParams().setTimePresent().setHorizStride(stride);

      long start = System.currentTimeMillis();
      GeoReferencedArray geo = grid.readData(params);

      long took = System.currentTimeMillis() - start;
      System.out.printf(" took=%d size=%d %n", took, geo.getData().getSize());

    }
    return 1;
  }

}
