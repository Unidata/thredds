package ucar.nc2.ft.point.writer;

import org.junit.Test;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.unidata.test.util.TestDir;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * test WriterCFStationCollection
 *
 * @author caron
 * @since 4/11/12
 */
public class TestStationWriter {

  @Test
  public void testWrite() throws IOException {
    writeStationDataset(TestDir.cdmUnitTestDir + "ft/station/200501q3h-gr.nc");
  }

  void writeStationDataset(String location) throws IOException {
    File fileIn = new File(location);
    System.out.printf("================ TestStationWriter read %s size=%d %n", location, fileIn.length());

    // open point dataset
    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(FeatureType.STATION, location, null, out);
    if (fdataset == null) {
      System.out.printf("**failed on %s %n --> %s %n", location, out);
      assert false;
    }

    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    StationTimeSeriesFeatureCollection fds = null;
    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      if (fc instanceof StationTimeSeriesFeatureCollection)
        fds = (StationTimeSeriesFeatureCollection) fc;
    }
    assert fds != null;

    // write CF equivilent
    int pos = location.lastIndexOf("/");
    String name = location.substring(pos+1);
    File fileOut = new File(TestDir.temporaryLocalDataDir, name);
    System.out.printf("write to file %s%n ", fileOut.getAbsolutePath());

    WriterCFStationCollection writer = new WriterCFStationCollection(fileOut.getPath(), "Rewrite as CF: "+ name);

    ucar.nc2.ft.PointFeatureCollection pfc = fds.flatten(null, (CalendarDateRange) null);

    int count = 0;
    boolean first = true;
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      if (first) {
        writer.writeHeader(fds.getStations(), fdataset.getDataVariables(), pf.getTimeUnit(), null);
        first = false;
      }
      StationPointFeature spf = (StationPointFeature) pf;
      writer.writeRecord(spf.getStation(), pf, pf.getData());
      count++;
      if (count % 100 == 0) System.out.printf("%d ", count);
      if (count % 1000 == 0) System.out.printf("%n ");
    }

    writer.finish();

    ////////////////////////////////
    // open result

    System.out.printf("%s size = %d (%f) %n", fileOut.getPath(), fileOut.length(), ((double)fileOut.length() / fileIn.length()) );
    FeatureDataset result = FeatureDatasetFactoryManager.open(FeatureType.STATION, fileOut.getPath(), null, out);
    System.out.printf("----------- TestStationWriter getDetailInfo -----------------%n");
    result.getDetailInfo(out);
    System.out.printf("%s %n", out);
  }


}
