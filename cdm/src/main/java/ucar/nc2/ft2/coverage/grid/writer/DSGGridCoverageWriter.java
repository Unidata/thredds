/* Copyright */
package ucar.nc2.ft2.coverage.grid.writer;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.point.writer.CFPointWriterConfig;
import ucar.nc2.ft.point.writer.WriterCFPointCollection;
import ucar.nc2.ft2.coverage.grid.GridCoverage;
import ucar.nc2.ft2.coverage.grid.GridCoverageDataset;
import ucar.nc2.ft2.coverage.grid.GridSubset;
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

  private GridCoverageDataset gcd;
  private List<VarData> varData;
  private GridSubset subset;

  private class VarData {
    GridCoverage cov;
    Array data;

    public VarData(GridCoverage cov) throws IOException {
      this.cov = cov;
      this.data = cov.readData(subset);
      System.out.printf(" Coverage %s data shape = %s%n", cov.getName(), Misc.showInts(data.getShape()));
    }
  }

  public DSGGridCoverageWriter(GridCoverageDataset gcd, List<String> varNames, GridSubset subset) throws IOException {
    this.gcd = gcd;
    this.subset = subset;

    varData = new ArrayList<>(varNames.size());
    for (String varName : varNames) {
      GridCoverage cov = gcd.findCoverage(varName);
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
