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
package ucar.nc2.ft2.coverage.writer;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.Misc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Write DSG CF-1.6 file from a GridCoverage
 *
 * @author caron
 * @since 7/8/2015
 */
public class DSGGridCoverageWriter {
  private static final boolean debug = true;

  private CoverageDataset gcd;
  private List<VarData> varData;
  private SubsetParams subset;

  private class VarData {
    Coverage cov;
    GeoReferencedArray array;

    public VarData(Coverage cov) throws IOException {
      this.cov = cov;
      try {
        this.array = cov.readData(subset);
        if (debug) System.out.printf(" Coverage %s data shape = %s%n", cov.getName(), Misc.showInts(array.getData().getShape()));
      } catch (InvalidRangeException e) {
        e.printStackTrace();
      }
    }
  }

  public DSGGridCoverageWriter(CoverageDataset gcd, List<String> varNames, SubsetParams subset) throws IOException {
    this.gcd = gcd;
    this.subset = subset;

    varData = new ArrayList<>(varNames.size());
    for (String varName : varNames) {
      Coverage cov = gcd.findCoverage(varName);
      if (cov != null) {
        varData.add(new VarData(cov));
      }
    }
  }

  public void writePointFeatureCollection(NetcdfFileWriter writer) throws IOException {
    // CFPointWriterConfig config = new CFPointWriterConfig(version);

    /* Array data = gcd.readData(subset);

    try (WriterCFPointCollection pointWriter = new WriterCFPointCollection(responseFilename, gcd.getGlobalAttributes(),
            fdpoint.getDataVariables(), pfc.getExtraVariables(),
            pfc.getTimeUnit(), pfc.getAltUnits(), config)) {

      int count = 0;
      while (pfc.hasNext()) {
        PointFeature pf = pfc.next();
        if (count == 0)
          pointWriter.writeHeader(pf);

        pointWriter.writeRecord(pf, pf.getFeatureData());
        count++;
        if (debug && count % 100 == 0) System.out.printf("%d ", count);
        if (debug && count % 1000 == 0) System.out.printf("%n ");
      }

      pointWriter.finish();
      return count;
    } */
  }

  public void streamResponse(OutputStream out) throws IOException {



  }



}
