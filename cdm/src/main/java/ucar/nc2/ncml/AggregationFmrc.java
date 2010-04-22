/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ncml;

import thredds.inventory.DateExtractor;
import thredds.inventory.DateExtractorFromName;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.ft.fmrc.Fmrc;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dataset.*;
import ucar.nc2.util.CancelTask;

import java.util.*;
import java.io.*;

/**
 * Implement NcML Forecast Model Run Collection Aggregation
 * with files that are complete runs (have all forecast times in the same file)
 *
 * @author caron
 */
public class AggregationFmrc extends AggregationOuterDimension {
  static protected Set<NetcdfDataset.Enhance> fmrcEnhanceMode = NetcdfDataset.getDefaultEnhanceMode();

  private boolean debug = false;
  private Fmrc fmrc;
  private String runMatcher; // , forecastMatcher, offsetMatcher; // scanFmrc

  public AggregationFmrc(NetcdfDataset ncd, String dimName, String recheckS) {
    super(ncd, dimName, Type.forecastModelRunCollection, recheckS);
  }

  public void addDirectoryScanFmrc(String dirName, String suffix, String regexpPatternString, String subdirs, String olderThan,
                                   String runMatcher, String forecastMatcher, String offsetMatcher) throws IOException {

    // only one
    this.runMatcher = runMatcher;
    //this.forecastMatcher = forecastMatcher;
    //this.offsetMatcher = offsetMatcher;

    // this.enhance = NetcdfDataset.getDefaultEnhanceMode();
    isDate = true;

    //DatasetScanner d = new DatasetScanner(null, dirName, suffix, regexpPatternString, subdirs, olderThan);
    //datasetManager.addDirectoryScan(d);
    datasetManager.addDirectoryScan(dirName, suffix, regexpPatternString, subdirs, olderThan, null);
    if (runMatcher != null) {
      DateExtractor dateExtractor = new DateExtractorFromName(runMatcher, false);
      datasetManager.setDateExtractor(dateExtractor);
    }
  }

  @Override
  protected void makeDatasets(CancelTask cancelTask) throws IOException {
    super.makeDatasets(cancelTask);
    for (Dataset ds : datasets)
      ds.enhance = fmrcEnhanceMode;
  }

  @Override
  public void getDetailInfo(Formatter f) {
    super.getDetailInfo(f);

    if (runMatcher != null)
      f.format("  runMatcher=%s%n", runMatcher);
   /*  if (forecastMatcher != null)
      f.format("  forecastMatcher=%s%n", forecastMatcher);
    if (offsetMatcher != null)
      f.format("  offsetMatcher=%s%n", offsetMatcher); */
  }

  @Override
  protected void buildNetcdfDataset(CancelTask cancelTask) throws IOException {
    DateExtractor dateExtractor = null;
    if (runMatcher != null)
      dateExtractor = new DateExtractorFromName(runMatcher, false); // uses path
    if (dateExtractor == null && dateFormatMark != null)
      dateExtractor = new DateExtractorFromName(dateFormatMark, true);
    fmrc = new Fmrc(datasetManager);

    // fill in the ncDataset
    fmrc.getDataset2D( ncDataset);
  }

  // we assume the variables are complete, but the time dimensions and values have to be recomputed
  @Override
  protected void rebuildDataset() throws IOException {
    throw new UnsupportedOperationException();
    // ncDataset.empty();
    // fmrc.getDataset2D(false, true, ncDataset);
  }

  /**
   * testing
   */
  public static void main(String arg[]) throws IOException {
    String defaultFilename = "C:/data/rap/fmrc.xml";
    String filename = (arg.length > 0) ? arg[0] : defaultFilename;

    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(filename);
    GridDatatype gg = gds.findGridDatatype("T");
    GridCoordSystem gsys = gg.getCoordinateSystem();

    // gsys.getTimeAxisForRun(1);  // generate error

    CoordinateAxis1DTime rtaxis = gsys.getRunTimeAxis();
    CoordinateAxis taxis2D = gsys.getTimeAxis();
    Array data = taxis2D.read();
    NCdump.printArray(data, "2D time array", System.out, null);

    System.out.println("Run Time, Valid Times");
    Date[] runtimes = rtaxis.getTimeDates();
    for (int i = 0; i < runtimes.length; i++) {
      System.out.println("\n" + runtimes[i]);

      CoordinateAxis1DTime taxis = gsys.getTimeAxisForRun(i);
      Date[] times = taxis.getTimeDates();
      for (int j = 0; j < times.length; j++) {
        System.out.println("   " + times[j]);
      }
    }

  }


}