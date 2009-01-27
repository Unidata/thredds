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

package ucar.nc2.dt.fmrc;

import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.Misc;

import java.io.PrintStream;
import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Helper class to generate a report for ForecastModelRunServlet.
 *
 * @author caron
 */

public class FmrcReport {
  private boolean debug = false;

  public void report( FmrcInventory fmrc, PrintStream out, boolean showMissing) {
    out.println("ForecastModelRunCollection "+fmrc.getDefinitionPath());

    FmrcDefinition def = fmrc.getDefinition();
    if (null == def) {
      out.println(" No Definition");
      return;
    }

    StringBuilder sbuff = new StringBuilder();
    List<ErrMessage> errs = new ArrayList<ErrMessage>();

    for (FmrcInventory.RunSeq haveSeq : fmrc.getRunSequences()) {
      List<FmrcInventory.UberGrid> vars = haveSeq.getVariables();
      for (FmrcInventory.UberGrid uv : vars) {
        String sname = uv.getName();
        FmrcDefinition.Grid g = def.findGridByName(sname);
        if (g == null) {
          errs.add(new ErrMessage(new Date(0), uv.name, "Extra Variable (not in definition)", ""));
          continue;
        }
        if (debug) System.out.println(uv.name);

        for (FmrcInventory.RunExpected rune : uv.runs) {
          ForecastModelRunInventory.TimeCoord haveTc = rune.run.tc;
          ForecastModelRunInventory.TimeCoord wantTc = rune.expected;

          if (debug) System.out.println(" " + rune.run.runTime);

          if (rune.expected == null) {
            errs.add(new ErrMessage(rune.run.runTime, uv.name, "Extra Variable (not in definition)", ""));
            continue;
          }

          sbuff.setLength(0);
          if (showMissing && findMissing(haveTc.getOffsetHours(), wantTc.getOffsetHours(), sbuff))
            errs.add(new ErrMessage(rune.run.runTime, uv.name, "Missing All Grids at Offset hour:", sbuff.toString()));

          sbuff.setLength(0);
          if (findMissing(wantTc.getOffsetHours(), haveTc.getOffsetHours(), sbuff))
            errs.add(new ErrMessage(rune.run.runTime, uv.name, "Extra Grid at Offset hour:", sbuff.toString()));

          // ForecastModelRun.VertCoord haveVc =  rune.grid.vc;

          if (showMissing) {
            sbuff.setLength(0);
            boolean haveErrs = false;
            for (double offset : haveTc.getOffsetHours()) {
              double[] wantVc = normalize(rune.expectedGrid.getVertCoords(offset));
              double[] haveVc = normalize(rune.grid.getVertCoords(offset));
              if (findMissing(haveVc, wantVc, sbuff)) {
                haveErrs = true;
                sbuff.append("(").append(offset).append(") ");
              }
            }
            if (haveErrs)
              errs.add(new ErrMessage(rune.run.runTime, uv.name, "Missing Some Grids:", sbuff.toString()));
          }

          sbuff.setLength(0);
          boolean haveErrs = false;
          for (double offset : haveTc.getOffsetHours()) {
            if (debug) System.out.println("  " + offset);

            double[] wantVc = normalize(rune.expectedGrid.getVertCoords(offset));
            double[] haveVc = normalize(rune.grid.getVertCoords(offset));
            if (findMissing(wantVc, haveVc, sbuff)) {
              haveErrs = true;
              sbuff.append("(").append(offset).append(") ");
            }
          }
          if (haveErrs)
            errs.add(new ErrMessage(rune.run.runTime, uv.name, "Extra Grids:", sbuff.toString()));

        }
      }
    }

    Collections.sort(errs);
    Date currentDate = null;
    String currentType = null;
    DateFormatter formatter = new  DateFormatter();

    for (ErrMessage err : errs) {
      if (err.runDate != currentDate) {
        if (currentDate != null) out.println();
        out.println(" Run " + formatter.toDateTimeString(err.runDate));
        currentDate = err.runDate;
        currentType = null;
      }

      if (!err.type.equals(currentType)) {
        out.println("  " + err.type);
        currentType = err.type;
      }
      out.println("   " + err.varName + ": " + err.message);
    }
    out.println();
  }

  double[] normalize( double[] v) {
    int countNans = 0;
    for (int i = 0; i < v.length; i++)
      if (Double.isNaN(v[i])) countNans++;

    if (countNans > 0) {
      double[] vnew = new double[v.length-countNans];
      int count = 0;
      for (int i = 0; i < v.length; i++)
        if (!Double.isNaN(v[i]))
          vnew[count++] = v[i];
      v = vnew;
    }

    // assume its sorted or reversed
    if (v.length < 2) return v;
    if (v[0] < v[1]) return v;
    double[] v2 = new double[v.length];
    for (int i = 0; i < v.length; i++)
      v2[v.length-i-1] = v[i];
    return v2;
  }

  // make sure that everything in "standard" also exists in "test"
  boolean findMissing(double[] test, double[] standard, StringBuilder sbuff) {
    int countTest = 0;
    int countStandard = 0;
    boolean errs = false;

    while ((countTest < test.length) && (countStandard < standard.length)) {
      if (Double.isNaN(standard[countStandard])) {
        countStandard++;

      } else if (Double.isNaN(test[countTest])) {
        countTest++;

      } else if ( Misc.closeEnough(standard[countStandard], test[countTest])) {
        countTest++;
        countStandard++;

      } else if (standard[countStandard] < test[countTest]) {
        sbuff.append(" ").append(standard[countStandard]);
        errs = true;
        countStandard++;

      } else if (standard[countStandard] > test[countTest]) {
        countTest++;
      }
    }

    while (countStandard < standard.length) {
      sbuff.append(" ").append(standard[countStandard]);
      errs = true;
      countStandard++;
    }

    return errs;
  }

  private class ErrMessage implements Comparable {
    Date runDate;
    String varName, type, message;

    ErrMessage( Date runDate, String varName, String type, String message) {
      this.runDate = runDate;
      this.varName = varName;
      this.type = type;
      this.message = message;
    }

    public int compareTo(Object o) {
      ErrMessage om = (ErrMessage) o;
      int result = runDate.compareTo( om.runDate);
      if (result != 0) return result;
      result = type.compareTo( om.type);
      if (result != 0) return result;
      result = varName.compareTo( om.varName);
      return result;
    }
  }

  static void doit(String dir) throws Exception {
    ///local/robb/data/NAM_CONUS_12km
    //FmrcInventory fmrc = FmrcInventory.makeFromDirectory("C:/data/grib/"+dir+"/", "test", null,
    //        "C:/data/grib/"+dir, "grib1", ForecastModelRunInventory.OPEN_NORMAL);
    FmrcInventory fmrc = FmrcInventory.makeFromDirectory("/local/robb/data/NAM_CONUS_12km/", "test", null,
            "/local/robb/data/NAM_CONUS_12km", "grib2", ForecastModelRunInventory.OPEN_NORMAL);
    FmrcReport report = new FmrcReport();
    report.report( fmrc, System.out, true);
  }

  public static void main(String args[]) throws Exception {
    doit("ruc/c20p");
    /* doit("ruc/conus40");
    doit("ruc/c20p");
    doit("ruc/c20s");
    doit("nam/c20s");
    doit("gfs/alaska191");
    doit("gfs/conus80");  */
  }

}


/*

  public void report( ForecastModelRunCollection fmrc, PrintStream out, boolean showMissing) {
    FmrcDefinition def = fmrc.getDefinition();
    if (null == def) {
      out.println("No Definition");
      return;
    }

    List runSequences = fmrc.getRunSequences();
    for (int i = 0; i < runSequences.size(); i++) {
      ForecastModelRunCollection.RunSeq have = (ForecastModelRunCollection.RunSeq) runSequences.get(i);

      List vars = have.getVariables();
      for (int j = 0; j < vars.size(); j++) {
        ForecastModelRunCollection.UberGrid uv = (ForecastModelRunCollection.UberGrid) vars.get(j);
        FmrcDefinition.RunSeq want = def.findSeqForVariable( uv.getName());
        if (want == null)
          out.println("Cant find variable "+uv.getName());
        else {
          StringBuffer sbuff = new StringBuffer();
          if (want.compare( have, sbuff, showMissing)) {
            out.println("Checking variable= "+uv.getName());
            out.println(sbuff);
          }
        }
      }
    }
  }


  boolean compareOffsets(double[] haveOffset, double[] wantOffset, StringBuffer sbuff, boolean showMissing) {
    int countHave = 0;
    int countWant = 0;
    boolean errs = false;

    while ((countHave < haveOffset.length) && (countWant < wantOffset.length)) {
      if (wantOffset[countWant] == haveOffset[countHave]) {
        countHave++;
        countWant++;
      } else if (wantOffset[countWant] < haveOffset[countHave]) {
        if (showMissing) {
          sbuff.append("  missing " + wantOffset[countWant] + "\n");
          errs = true;
        }
        countWant++;
      } else if (wantOffset[countWant] > haveOffset[countHave]) {
        sbuff.append("  extra " + haveOffset[countWant] + "\n");
        errs = true;
        countHave++;
      }
    }

    while (countHave < haveOffset.length) {
      sbuff.append("  extra " + haveOffset[countWant] + "\n");
      errs = true;
      countHave++;
    }

    while (countWant < wantOffset.length) {
      if (showMissing) {
        sbuff.append("  missing " + wantOffset[countWant] + "\n");
        errs = true;
      }
      countWant++;
    }

    return errs;
  }
*/