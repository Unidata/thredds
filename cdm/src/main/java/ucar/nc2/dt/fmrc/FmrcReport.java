// $Id:FmrcReport.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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

    StringBuffer sbuff = new StringBuffer();
    ArrayList errs = new ArrayList();

    List runSequences = fmrc.getRunSequences();
    for (int i = 0; i < runSequences.size(); i++) {
      FmrcInventory.RunSeq haveSeq = (FmrcInventory.RunSeq) runSequences.get(i);

      List vars = haveSeq.getVariables();
      for (int j = 0; j < vars.size(); j++) {
        FmrcInventory.UberGrid uv = (FmrcInventory.UberGrid) vars.get(j);
        String sname = uv.getName();
        FmrcDefinition.Grid g = def.findGridByName(sname);
        if (g == null) {
          errs.add( new ErrMessage(new Date(0), uv.name, "Extra Variable (not in definition)", ""));
          continue;
        }
        if (debug) System.out.println(uv.name);

        for (int k = 0; k < uv.runs.size(); k++) {
          FmrcInventory.RunExpected rune = (FmrcInventory.RunExpected) uv.runs.get(k);
          ForecastModelRunInventory.TimeCoord haveTc = rune.run.tc;
          ForecastModelRunInventory.TimeCoord wantTc = rune.expected;

          if (debug) System.out.println(" "+rune.run.runTime);

          if (rune.expected == null) {
            errs.add( new ErrMessage(rune.run.runTime, uv.name, "Extra Variable (not in definition)", ""));
            continue;
          }

          sbuff.setLength(0);
          if (showMissing && findMissing(haveTc.getOffsetHours(), wantTc.getOffsetHours(), sbuff))
            errs.add( new ErrMessage(rune.run.runTime, uv.name, "Missing All Grids at Offset hour:", sbuff.toString()));

          sbuff.setLength(0);
          if (findMissing(wantTc.getOffsetHours(), haveTc.getOffsetHours(), sbuff))
            errs.add( new ErrMessage(rune.run.runTime, uv.name, "Extra Grid at Offset hour:", sbuff.toString()));

          // ForecastModelRun.VertCoord haveVc =  rune.grid.vc;

          if (showMissing)  {
            sbuff.setLength(0);
            boolean haveErrs = false;
            double[] offsets = haveTc.getOffsetHours();
            for (int l = 0; l < offsets.length; l++) {
              double offset = offsets[l];
              double[] wantVc = normalize( rune.expectedGrid.getVertCoords(offset));
              double[] haveVc = normalize( rune.grid.getVertCoords(offset));
              if (findMissing(haveVc, wantVc, sbuff)) {
                haveErrs = true;
                sbuff.append("("+offset+") ");
              }
            }
            if (haveErrs)
              errs.add( new ErrMessage(rune.run.runTime, uv.name, "Missing Some Grids:", sbuff.toString()));
          }

          sbuff.setLength(0);
          boolean haveErrs = false;
          double[] offsets = haveTc.getOffsetHours();
          for (int l = 0; l < offsets.length; l++) {
            double offset = offsets[l];
            if (debug) System.out.println("  "+offset);

            double[] wantVc = normalize( rune.expectedGrid.getVertCoords(offset));
            double[] haveVc = normalize( rune.grid.getVertCoords(offset));
            if (findMissing(wantVc, haveVc, sbuff)) {
              haveErrs = true;
              sbuff.append("("+offset+") ");
            }
          }
          if (haveErrs)
            errs.add( new ErrMessage(rune.run.runTime, uv.name, "Extra Grids:", sbuff.toString()));

        }
      }
    }

    Collections.sort(errs);
    Date currentDate = null;
    String currentType = null;
    DateFormatter formatter = new  DateFormatter();

    for (int i = 0; i < errs.size(); i++) {
      ErrMessage err = (ErrMessage) errs.get(i);

      if (err.runDate != currentDate) {
        if (currentDate != null) out.println();
        out.println(" Run "+formatter.toDateTimeString(err.runDate));
        currentDate = err.runDate;
        currentType = null;
      }

      if (!err.type.equals(currentType)) {
        out.println("  "+err.type);
        currentType = err.type;
      }
      out.println("   "+err.varName+": "+err.message);
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
  boolean findMissing(double[] test, double[] standard, StringBuffer sbuff) {
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
        sbuff.append(" "+standard[countStandard]);
        errs = true;
        countStandard++;

      } else if (standard[countStandard] > test[countTest]) {
        countTest++;
      }
    }

    while (countStandard < standard.length) {
      sbuff.append(" "+standard[countStandard]);
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
    FmrcInventory fmrc = FmrcInventory.make("C:/data/grib/"+dir+"/", "test", null,
            "C:/data/grib/"+dir, "grib1", ForecastModelRunInventory.OPEN_NORMAL);
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