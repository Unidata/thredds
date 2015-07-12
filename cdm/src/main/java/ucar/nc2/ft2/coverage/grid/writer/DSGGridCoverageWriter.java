/* Copyright */
package ucar.nc2.ft2.coverage.grid.writer;

import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.ft2.coverage.ArrayWithCoordinates;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageDataset;
import ucar.nc2.ft2.coverage.grid.GridCoverage;
import ucar.nc2.ft2.coverage.grid.GridCoverageDataset;
import ucar.nc2.ft2.coverage.CoverageSubset;
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
  private CoverageSubset subset;

  private class VarData {
    Coverage cov;
    ArrayWithCoordinates array;

    public VarData(Coverage cov) throws IOException {
      this.cov = cov;
      this.array = cov.readData(subset);
      System.out.printf(" Coverage %s data shape = %s%n", cov.getName(), Misc.showInts(array.getData().getShape()));
    }
  }

  public DSGGridCoverageWriter(CoverageDataset gcd, List<String> varNames, CoverageSubset subset) throws IOException {
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
