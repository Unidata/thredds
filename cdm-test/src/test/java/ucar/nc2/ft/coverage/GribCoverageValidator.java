package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.grib.*;
import ucar.nc2.grib.collection.GribDataValidator;
import ucar.nc2.grib.coord.TimeCoordIntvDateValue;
import ucar.nc2.grib.grib1.Grib1ParamLevel;
import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.Assert2;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class GribCoverageValidator implements GribDataValidator {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void validate(GribTables cust, RandomAccessFile rafData, long dataPos, SubsetParams coords) throws IOException {
    if (cust instanceof Grib1Customizer)
      validateGrib1((Grib1Customizer) cust, rafData, dataPos, coords);
    else
      validateGrib2((Grib2Tables) cust, rafData, dataPos, coords);
  }

  public void validateGrib1(Grib1Customizer cust, RandomAccessFile rafData, long dataPos, SubsetParams coords) throws IOException {
    rafData.seek(dataPos);
    Grib1Record gr = new Grib1Record(rafData);
    Grib1SectionProductDefinition pds = gr.getPDSsection();

    // runtime
    CalendarDate wantRuntime = coords.getRunTime();
    CalendarDate refdate = gr.getReferenceDate();
    Assert.assertEquals("runtime", refdate, wantRuntime);

    // time offset
    Double timeOffset = coords.getTimeOffset();
    double[] timeOffsetIntv = coords.getTimeOffsetIntv(); // LOOK ??
    if (timeOffset == null) {
      logger.debug("HEY no timeOffsetCoord ");
      return;
    }

    Grib1ParamTime ptime = gr.getParamTime(cust);
    if (ptime.isInterval()) {
      int tinv[] = ptime.getInterval();
      Assert.assertTrue("time coord lower", tinv[0] <= timeOffset);          // lower <= time
      Assert.assertTrue("time coord lower", tinv[1] >= timeOffset);          // upper >= time
    } else {
      Assert2.assertNearlyEquals(timeOffset, ptime.getForecastTime());
    }

    // vert
    Double wantVert = coords.getVertCoord();
    if (wantVert != null) {
      Grib1ParamLevel plevel = cust.getParamLevel(pds);
      float lev1 = plevel.getValue1();
      if (cust.isLayer(pds.getLevelType())) {
        float lev2 = plevel.getValue2();
        double lower = Math.min(lev1, lev2);
        double upper = Math.max(lev1, lev2);
        Assert.assertTrue("vert coord lower", lower <= wantVert);          // lower <= vert
        Assert.assertTrue("vert coord upper", upper >= wantVert);          // upper >= vert

      } else {
        Assert2.assertNearlyEquals(lev1, wantVert);
      }
    }

    // ens
    Double wantEns = coords.getEnsCoord();
    if (wantEns != null) {
      Assert2.assertNearlyEquals(pds.getPerturbationNumber(), wantEns);
    }

  }

  public void validateGrib2(Grib2Tables cust, RandomAccessFile rafData, long dataPos, SubsetParams coords) throws IOException {
    Grib2Record gr = Grib2RecordScanner.findRecordByDrspos(rafData, dataPos);
    Grib2Pds pds = gr.getPDS();

    // runtime
    CalendarDate wantRuntime = coords.getRunTime();
    CalendarDate refdate = gr.getReferenceDate();
    Assert.assertEquals("runtime", wantRuntime, refdate);

    // time offset
    CalendarDate wantTimeOffset = (CalendarDate) coords.get(SubsetParams.timeOffsetDate);
    if (gr.getPDS().isTimeInterval()) {
      TimeCoordIntvDateValue tinv = cust.getForecastTimeInterval(gr);
      double[] wantTimeOffsetIntv = coords.getTimeOffsetIntv();
      if (wantTimeOffset != null) {
        Assert.assertTrue("time coord lower", !tinv.getStart().isAfter(wantTimeOffset));          // lower <= time
        Assert.assertTrue("time coord upper", !tinv.getEnd().isBefore(wantTimeOffset));// upper >= time

      } else if (wantTimeOffsetIntv != null) {
        int[] gribIntv = cust.getForecastTimeIntervalOffset(gr);

        Assert.assertTrue("time coord lower", wantTimeOffsetIntv[0] == gribIntv[0]);
        Assert.assertTrue("time coord upper", wantTimeOffsetIntv[1] == gribIntv[1]);
      }

    } else {
      CalendarDate fdate = cust.getForecastDate(gr);
      if (!fdate.equals(wantTimeOffset))
        logger.debug("HEY forecast date");
      Assert.assertEquals("time coord", wantTimeOffset, fdate);
    }

    // vert
    Double vertCoord = coords.getVertCoord();
    double[] vertCoordIntv = coords.getVertCoordIntv();
    double level1val = pds.getLevelValue1();

    if (vertCoordIntv != null) {
      Assert.assertTrue(Grib2Utils.isLayer(pds));
      double level2val = pds.getLevelValue2();
      //double lower = Math.min(level1val, level2val);
      //double upper = Math.max(level1val, level2val);
      //Assert.assertTrue("vert coord lower", lower <= wantVert);          // lower <= vert
      //Assert.assertTrue("vert coord upper", upper >= wantVert);          // upper >= vert
      Assert2.assertNearlyEquals(vertCoordIntv[0], level1val, 1e-6);
      Assert2.assertNearlyEquals(vertCoordIntv[1], level2val, 1e-6);

    } else if (vertCoord != null) {
      Assert2.assertNearlyEquals(vertCoord, level1val, 1e-6);
    }

    // ens
    Double wantEns = coords.getEnsCoord();
    if (wantEns != null) {
      Grib2Pds.PdsEnsemble pdse = (Grib2Pds.PdsEnsemble) pds;
      Assert2.assertNearlyEquals(wantEns, pdse.getPerturbationNumber());
    }
  }
}
