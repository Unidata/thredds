package ucar.nc2.ft.coverage;

import org.junit.Assert;
import ucar.nc2.ft2.coverage.CoordsSet;
import ucar.nc2.grib.*;
import ucar.nc2.grib.collection.GribDataValidator;
import ucar.nc2.grib.grib1.Grib1ParamLevel;
import ucar.nc2.grib.grib1.Grib1ParamTime;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib1.Grib1SectionProductDefinition;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2RecordScanner;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Misc;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.Map;

public class GribCoverageValidator implements GribDataValidator {

  @Override
  public void validate(GribTables cust, RandomAccessFile rafData, long dataPos, Map<String, Object> coords) throws IOException {
    if (cust instanceof Grib1Customizer)
      validateGrib1((Grib1Customizer) cust, rafData, dataPos, coords);
    else
      validateGrib2((Grib2Customizer) cust, rafData, dataPos, coords);
  }

  public void validateGrib1(Grib1Customizer cust, RandomAccessFile rafData, long dataPos, Map<String, Object> coords) throws IOException {
    rafData.seek(dataPos);
    Grib1Record gr = new Grib1Record(rafData);
    Grib1SectionProductDefinition pds = gr.getPDSsection();

    // runtime
    CalendarDate wantRuntime = (CalendarDate) coords.get(CoordsSet.runDate);
    CalendarDate refdate = gr.getReferenceDate();
    Assert.assertEquals("runtime", refdate, wantRuntime);

    // time offset
    Object coordOb = coords.get(CoordsSet.timeOffsetCoord);
    if (coordOb == null) {
      System.out.printf("HEY no timeOffsetCoord ");
      return;
    }

    double wantTimeOffset = (Double) coordOb;

    Grib1ParamTime ptime = gr.getParamTime(cust);
    if (ptime.isInterval()) {
      int tinv[] = ptime.getInterval();
      Assert.assertTrue("time coord lower", tinv[0] <= wantTimeOffset);          // lower <= time
      Assert.assertTrue("time coord lower", tinv[1] >= wantTimeOffset);          // upper >= time

    }else {
      Assert.assertEquals("offset coord", wantTimeOffset, ptime.getForecastTime(), Misc.maxReletiveError);
    }

    // vert
    Double wantVert = (Double) coords.get(CoordsSet.vertCoord);
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
        Assert.assertEquals("vert coord", lev1, wantVert, Misc.maxReletiveError);
      }
    }

    // ens
    Double wantEns = (Double) coords.get(CoordsSet.ensCoord);
    if (wantEns != null) {
      Assert.assertEquals("ens coord", pds.getPerturbationNumber(), wantEns, Misc.maxReletiveError);
    }

  }

  public void validateGrib2(Grib2Customizer cust, RandomAccessFile rafData, long dataPos, Map<String, Object> coords) throws IOException {
    Grib2Record gr = Grib2RecordScanner.findRecordByDrspos(rafData, dataPos);
    Grib2Pds pds = gr.getPDS();

    // runtime
    CalendarDate wantRuntime = (CalendarDate) coords.get(CoordsSet.runDate);
    CalendarDate refdate = gr.getReferenceDate();
    Assert.assertEquals("runtime", wantRuntime, refdate );

    // time offset
    CalendarDate wantTimeOffset = (CalendarDate) coords.get(CoordsSet.timeOffsetDate);
    if (gr.getPDS().isTimeInterval()) {
      TimeCoord.TinvDate tinv = cust.getForecastTimeInterval(gr);
      Assert.assertTrue("time coord lower", !tinv.getStart().isAfter(wantTimeOffset));          // lower <= time
      Assert.assertTrue("time coord upper", !tinv.getEnd().isBefore(wantTimeOffset));         // upper >= time

    }else {
      CalendarDate fdate = cust.getForecastDate(gr);
      if (!fdate.equals(wantTimeOffset))
        System.out.printf("HEY forecast date%n");
      Assert.assertEquals("time coord", wantTimeOffset, fdate);
    }

    // vert
    Object wantVert = coords.get(CoordsSet.vertCoord);
    if (wantVert != null) {
      double level1val = pds.getLevelValue1();
      if (Grib2Utils.isLayer(pds)) {
        double[] wantVertBounds = (double[]) wantVert;
        double level2val =  pds.getLevelValue2();
        //double lower = Math.min(level1val, level2val);
        //double upper = Math.max(level1val, level2val);
        //Assert.assertTrue("vert coord lower", lower <= wantVert);          // lower <= vert
        //Assert.assertTrue("vert coord upper", upper >= wantVert);          // upper >= vert
        Assert.assertEquals("vert coord 1", wantVertBounds[0], level1val, Misc.maxReletiveError);
        Assert.assertEquals("vert coord 2", wantVertBounds[1], level2val, Misc.maxReletiveError);

      } else {
        Assert.assertEquals("vert coord", (Double) wantVert, level1val, Misc.maxReletiveError);
      }
    }

    // ens
    Double wantEns = (Double) coords.get(CoordsSet.ensCoord);
    if (wantEns != null) {
      Grib2Pds.PdsEnsemble pdse = (Grib2Pds.PdsEnsemble) pds;
      Assert.assertEquals("ens coord", wantEns, pdse.getPerturbationNumber(), Misc.maxReletiveError);
    }

  }

}
