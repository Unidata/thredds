/*
 * Copyright (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */
package ucar.nc2.ncml;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.fmrc.Fmrc;
import ucar.nc2.time.CalendarDate;

import static java.lang.Math.toIntExact;


/** Test FMRC NcML aggregations using a directory scan and explicitly listed datasets. */

public class TestAggFmrc {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  String FILENAME_SCAN = "testAggFmrcScan.ncml";
  String FILENAME_EXPLICIT = "testAggFmrcExplicit.ncml";

  String FILENAME_A = "GFS_Puerto_Rico_191km_20090729_0000.nc";
  String FILENAME_B = "GFS_Puerto_Rico_191km_20090730_0000.nc";
  String FILENAME_C = "GFS_Puerto_Rico_191km_20090731_0000.nc";

  String AGG_VAR_NAME = "Temperature_isobaric";

  private NetcdfFile ncfileScan, ncfileExplicit, ncfileA, ncfileB, ncfileC;
  private Variable varScan, varExplicit, varA, varB, varC;
  private Array valuesScan, valuesExplicit, valuesA, valuesB, valuesC, valuesBestScanVar, valuesBestExplicitVar;
  private Fmrc fmrcScan, fmrcExplicit;

  /**
   * Read aggregation of unsigned variable for test
   */
  @Before
  public void prepAggDataset() {
    String filenameScan = "file:./" + TestNcML.topDir + "fmrc/" + FILENAME_SCAN;
    String filenameExplicit = "file:./" + TestNcML.topDir + "fmrc/" + FILENAME_EXPLICIT;
    String filenameA = "file:./" + TestNcML.topDir + "fmrc/" + FILENAME_A;
    String filenameB = "file:./" + TestNcML.topDir + "fmrc/" + FILENAME_B;
    String filenameC = "file:./" + TestNcML.topDir + "fmrc/" + FILENAME_C;
    try {

      fmrcScan = Fmrc.open(filenameScan, null);
      Assert.assertNotNull(fmrcScan);
      GridDataset bestScan = fmrcScan.getDatasetBest();
      Variable bestScanVar = bestScan.findGridByName(AGG_VAR_NAME).getVariable();
      valuesBestScanVar = bestScanVar.read();

      fmrcExplicit = Fmrc.open(filenameExplicit, null);
      Assert.assertNotNull(fmrcExplicit);
      GridDataset bestExplicit = fmrcExplicit.getDatasetBest();
      Variable bestExplicitVar = bestExplicit.findGridByName(AGG_VAR_NAME).getVariable();
      valuesBestExplicitVar = bestExplicitVar.read();

      ncfileScan = NcMLReader.readNcML(filenameScan, null);
      varScan = ncfileScan.findVariable(AGG_VAR_NAME);
      valuesScan = varScan.read();

      ncfileExplicit = NcMLReader.readNcML(filenameExplicit, null);
      varExplicit = ncfileScan.findVariable(AGG_VAR_NAME);
      valuesExplicit = varExplicit.read();

      ncfileA = NetcdfDataset.openDataset(filenameA);
      varA = ncfileA.findVariable(AGG_VAR_NAME);
      valuesA = varA.read();

      ncfileB = NetcdfDataset.openDataset(filenameB);
      varB = ncfileB.findVariable(AGG_VAR_NAME);
      valuesB = varB.read();

      ncfileC = NetcdfDataset.openDataset(filenameC);
      varC = ncfileC.findVariable(AGG_VAR_NAME);
      valuesC = varC.read();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testAggVarShape() {
    for (Variable var : new Variable[]{varScan, varExplicit}) {
      Assert.assertEquals(3, var.getShape(0));
      Assert.assertEquals(21, var.getShape(1));
      Assert.assertEquals(6, var.getShape(2));
      Assert.assertEquals(39, var.getShape(3));
      Assert.assertEquals(45, var.getShape(4));
    }
  }

  @Test
  public void testAggVarShapes() {
    Assert.assertArrayEquals(varScan.getShape(), varExplicit.getShape());
  }

  @Test
  public void testAggVarRank() {
    Assert.assertEquals(varScan.getRank(), varExplicit.getRank());
  }

  @Test
  public void testDataValues() throws IOException {
    Array valuesScan = varScan.read();
    Array valuesExplicit = varExplicit.read();
    List<int[]> idxs = new ArrayList<>();
    idxs.add(new int[]{0, 0, 0, 0, 0});
    idxs.add(new int[]{2, 20, 5, 38, 44});
    idxs.add(new int[]{1, 10, 3, 19, 22});

    Index idx = Index.factory(valuesScan.getShape());

    for (int[] loc : idxs) {
      idx.set(loc);
      float a = valuesScan.getFloat(idx);
      float b = valuesExplicit.getFloat(idx);
      Assert.assertEquals(a, b, 0);
    }
  }

  @Test
  public void testAggDataValuesWithOrig() throws IOException {
    // test values in aggregation against values from files that were aggregated.
    // Agg Shapes
    // run, time, isobaric,  y,  x
    //   3,   21,        6, 39, 45

    // individual file shapes time, isobaric,  y,  x
    //                 valuesA  20,        6, 39, 45
    //                 valuesB  21,        6, 39, 45
    //                 valuesC  20,        6, 39, 45

    int[] startAgg = new int[]{0, 0, 0, 0, 0};
    int[] endAgg = new int[]{2, 19, 5, 38, 44};
    int[] startFileBInAgg = new int[]{1, 0, 0, 0, 0};


    Index idxAgg = Index.factory(valuesScan.getShape());

    // test first value in valA and agg vals
    idxAgg.set(startAgg);
    float a = valuesScan.getFloat(idxAgg);
    float b = valuesExplicit.getFloat(idxAgg);
    float orig = valuesA.getFloat(0);
    Assert.assertEquals(a, orig, 0);
    Assert.assertEquals(b, orig, 0);

    // test last value in valC and agg vales
    idxAgg.set(endAgg);
    a = valuesScan.getFloat(idxAgg);
    b = valuesExplicit.getFloat(idxAgg);
    orig = valuesC.getFloat(toIntExact(valuesC.getSize()) - 1);
    Assert.assertEquals(a, orig, 0);
    Assert.assertEquals(b, orig, 0);

    // test first value in valB against agg vals
    idxAgg.set(startFileBInAgg);
    a = valuesScan.getFloat(idxAgg);
    b = valuesExplicit.getFloat(idxAgg);
    orig = valuesB.getFloat(0);
    Assert.assertEquals(a, orig, 0);
    Assert.assertEquals(b, orig, 0);
  }

  @Test
  public void FmrcRunDates() {

    try {
      List<CalendarDate> scanRunTimes = fmrcScan.getRunDates();
      List<CalendarDate> explicitRunTimes = fmrcExplicit.getRunDates();

      Assert.assertEquals(scanRunTimes.size(), 3);
      Assert.assertEquals(explicitRunTimes.size(), scanRunTimes.size());

      for (int i = 0; i < scanRunTimes.size(); i++) {
        Assert.assertTrue(explicitRunTimes.get(i).equals(scanRunTimes.get(i)));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void FmrcForecastDates() {

    try {
      List<CalendarDate> scanForecastTimes = fmrcScan.getForecastDates();
      List<CalendarDate> explicitForecastTimes = fmrcExplicit.getForecastDates();

      Assert.assertEquals(scanForecastTimes.size(), scanForecastTimes.size());
      Assert.assertEquals(explicitForecastTimes.size(), 24);

      for (int i = 0; i < scanForecastTimes.size(); i++) {
        Assert.assertTrue(scanForecastTimes.get(i).equals(explicitForecastTimes.get(i)));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void FmrcForecastOffsets() {
    try {
      Assert.assertArrayEquals(fmrcExplicit.getForecastOffsets(), fmrcScan.getForecastOffsets(), 0);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testBestDataset() {
    // delta_t is 12 hours, first time in dataset is 2007-07-29T1200
    // best ds shapes time, isobaric,  y,  x
    //                  24,        6, 39, 45
    //
    // individual file shapes time, isobaric,  y,  x
    //                 valuesA  20,        6, 39, 45
    //                 valuesB  21,        6, 39, 45
    //                 valuesC  20,        6, 39, 45
    int[] startFmrc = new int[]{0, 0, 0, 0};
    int[] endFmrc = new int[]{23, 5, 38, 44};
    int[] startFileBInFmrc = new int[]{1, 0, 0, 0};


    Index idxAgg = Index.factory(valuesBestScanVar.getShape());

    // test first value in valA and agg vals
    idxAgg.set(startFmrc);
    float a = valuesBestScanVar.getFloat(idxAgg);
    float b = valuesBestExplicitVar.getFloat(idxAgg);
    float orig = valuesA.getFloat(0);
    Assert.assertEquals(a, orig, 0);
    Assert.assertEquals(b, orig, 0);

    // test last value in valC and agg vales
    idxAgg.set(endFmrc);
    a = valuesBestScanVar.getFloat(idxAgg);
    b = valuesBestExplicitVar.getFloat(idxAgg);
    orig = valuesC.getFloat(toIntExact(valuesC.getSize()) - 1);
    Assert.assertEquals(a, orig, 0);
    Assert.assertEquals(b, orig, 0);

    // test first value in valB against agg vals
    idxAgg.set(startFileBInFmrc);
    a = valuesBestScanVar.getFloat(idxAgg);
    b = valuesBestExplicitVar.getFloat(idxAgg);
    orig = valuesB.getFloat(0);
    Assert.assertEquals(a, orig, 0);
    Assert.assertEquals(b, orig, 0);

  }
  /**
   * close out datasets when tests are finished
   */
  @After
  public void closeAggDataset() {
    try {
      ncfileScan.close();
      ncfileExplicit.close();
      ncfileA.close();
      ncfileB.close();
      ncfileC.close();
      fmrcExplicit.close();
      fmrcScan.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
