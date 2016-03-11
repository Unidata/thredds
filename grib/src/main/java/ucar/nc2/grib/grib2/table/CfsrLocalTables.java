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

package ucar.nc2.grib.grib2.table;

import ucar.nc2.constants.CDM;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.grib2.Grib2Parameter;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.unidata.util.StringUtil2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Formatter;

/**
 * Read screen scraped DSS tables.
 * CFSR 093 not needed - screen scraped NCEP ok 2/2/2012
 *
 * @author caron
 * @since 11/3/11
 */
public class CfsrLocalTables extends NcepLocalTables {
  private static final String tableName = "resources/grib2/local/cfsr.txt";
  private static boolean debug = false;
  private static CfsrLocalTables single;

  public static CfsrLocalTables getCust(Grib2Table table) {
    if (single == null) single = new CfsrLocalTables(table);
    return single;
  }

  private CfsrLocalTables(Grib2Table grib2Table) {
    super(grib2Table);
    if (grib2Table.getPath() == null)
      grib2Table.setPath(tableName);
    initLocalTable();
  }

  @Override
  public String getTablePath(int discipline, int category, int number) {
    if ((category <= 191) && (number <= 191)) return super.getTablePath(discipline, category, number);
    return tableName;
  }

  @Override
  public String getGeneratingProcessName(int genProcess) {
    switch (genProcess) {
      case 197:
        return "CFSR";
      default:
        return super.getGeneratingProcessName(genProcess);
    }
  }

  // 193
  // Average of N forecasts (or initialized analyses); each product has forecast period of P1 (P1=0 for initialized analyses);
  // products have reference times at intervals of P2, beginning at the given reference time.
  // 194
  // Average of N uninitialized analyses, starting at reference time, at intervals of P2.
  // 195:
  // Average of forecast accumulations. P1 = start of accumulation period. P2 = end of accumulation period.
  // Reference time is the start time of the first forecast, other forecasts at 24-hour intervals.
  // 204
  // Average of forecast accumulations. P1 = start of accumulation period. P2 = end of accumulation period.
  // Reference time is the start time of the first forecast, other forecasts at 6-hour intervals.
  // Number in Ave = number of forecast used
  // 205
  // Average of forecast averages. P1 = start of averaging period. P2 = end of averaging period.
  // Reference time is the start time of the first forecast, other forecasts at 6-hour intervals. Number in Ave = number of forecast used

  @Override
  public int[] getForecastTimeIntervalOffset(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    if (!pds.isTimeInterval()) return null;

    // LOOK this is hack for CFSR monthly combobulation
    // see http://rda.ucar.edu/datasets/ds093.2/#docs/time_ranges.html

    int statType = pds.getOctet(47);
    int n = pds.getInt4StartingAtOctet(50);
    int p2 = pds.getInt4StartingAtOctet(55);
    int p2mp1 = pds.getInt4StartingAtOctet(62);

    int p1 = p2 - p2mp1;
    int start, end;

    switch (statType) {
      case 193:
        start = p1;
        end = p1 + n * p2;
        break;
      case 194:
        start = 0;
        end = n * p2;
        break;
      case 195:
      case 204:
      case 205:
        start = p1;
        end = p2;
        break;
      default:
        throw new IllegalArgumentException("unknown statType "+statType);
    }

    return new int[]{start, end};
  }

  @Override
  public TimeCoord.TinvDate getForecastTimeInterval(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    if (!pds.isTimeInterval()) return null;

    int[] intv = getForecastTimeIntervalOffset(gr);
    assert intv != null;
    int intvLen = intv[1]-intv[0];

    int timeUnitOrg = pds.getTimeUnit();
    int timeUnitConvert = convertTimeUnit(timeUnitOrg);
    CalendarPeriod unitPeriod = Grib2Utils.getCalendarPeriod(timeUnitConvert);
    if (unitPeriod == null)
      throw new IllegalArgumentException("unknown CalendarPeriod "+timeUnitConvert+ " org="+timeUnitOrg);

    CalendarPeriod.Field fld = unitPeriod.getField();

    CalendarDate start = gr.getReferenceDate().add(intv[0], fld);
    CalendarPeriod period = CalendarPeriod.of(intvLen, fld);

    return new TimeCoord.TinvDate(start, period);
  }

  /**
   * Only use in GribVariable to decide on variable identity when intvMerge = false.
   * By returning a constant, we dont support intvMerge = false.
   * Problem is we cant reconstruct interval length without reference time, which is not in the pds.
   */
  @Override
  public double getForecastTimeIntervalSizeInHours(Grib2Pds pds) {
    return 6.0;
  }

  private boolean isCfsr2(Grib2Pds pds) {
    int genType = pds.getGenProcessId();
    if ((genType != 82) && (genType != 89)) return false;

    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) pds;
    Grib2Pds.TimeInterval[] ti = pdsIntv.getTimeIntervals();
    return ti.length != 1;
  }


  @Override
  public void showSpecialPdsInfo(Grib2Record gr, Formatter f) {
    Grib2Pds pds = gr.getPDS();
    if (!pds.isTimeInterval()) return;
    if (pds.getRawLength() < 65) return;

    /*     Octet(s)	Description
        47	From NCEP Code Table 4.10
        48	Should be ignored
        49	Should be ignored
        50-53	Number of grids used in the average
        54	Should be ignored
        55-58	This is "P2" from the GRIB1 format
        59	From NCEP Code Table 4.10
        60	Should be ignored
        61	Should be ignored
        62-65	This is "P2 minus P1"; P1 and P2 are fields from the GRIB1 format
        66	Should be ignored
        67-70	Should be ignored */

    int statType = pds.getOctet(47);
    int statType2 = pds.getOctet(59);
    int ngrids = pds.getInt4StartingAtOctet(50);
    int p2 = pds.getInt4StartingAtOctet(55);
    int p2mp1 = pds.getInt4StartingAtOctet(62);

    f.format("%nCFSR MM special encoding (NCAR)%n");
    f.format("  (47) Code Table 4.10 = %d%n", statType);
    f.format("  (50-53) N in avg     = %d%n", ngrids);
    f.format("  (55-58) Grib1 P2     = %d%n", p2);
    f.format("  (59) Code Table 4.10 = %d%n", statType2);
    f.format("  (62-65) P2 minus P1  = %d%n", p2mp1);
    f.format("                   P1  = %d%n", p2 - p2mp1);

    int[] intv = getForecastTimeIntervalOffset(gr);
    if (intv == null) return;
    f.format("ForecastTimeIntervalOffset  = (%d,%d)%n", intv[0],intv[1]);
    f.format("      ForecastTimeInterval  = %s%n", getForecastTimeInterval(gr));

    /* Section 4 Octet 58 (possibly 32 bits: 55-58) is the length of the averaging period per unit.
       For cycle fractions, this is 24, for complete monthly averages, it is 6.
       The product of this and the num_in_avg {above} should always equal the total number of hours in a respective month.
     - Section 4 Octet 65 is the hours skipped between each calculation component.
    f.format("%nCFSR MM special encoding (Swank)%n");
    f.format("  (55-58) length of avg period per unit                     = %d%n", p2);
    f.format("  (62-65) hours skipped between each calculation component  = %d%n", p2mp1);
    f.format("  nhours in month %d should be  = %d%n", ngrids * p2, 24 * 31); */
  }

  //////////////////////////////////////////////////////////////////////

  // see http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_doc.shtml
  protected void initLocalTable() {
    ClassLoader cl = this.getClass().getClassLoader();
    try (InputStream is = cl.getResourceAsStream(tableName)) {
      if (is == null) throw new IllegalStateException("Cant find " + tableName);
      BufferedReader br = new BufferedReader(new InputStreamReader(is, CDM.utf8Charset));

      while (true) {
        String line = br.readLine();
        if (line == null) break;
        if ((line.length() == 0) || line.startsWith("#")) continue;
        String[] flds = StringUtil2.splitString(line);

        int p1 = Integer.parseInt(flds[0].trim()); // must have a number
        int p2 = Integer.parseInt(flds[1].trim()); // must have a number
        int p3 = Integer.parseInt(flds[2].trim()); // must have a number
        StringBuilder b = new StringBuilder();
        int count = 3;

        while (count < flds.length && !flds[count].equals("."))
          b.append(flds[count++]).append(' ');
        String abbrev = b.toString().trim();
        b.setLength(0);
        count++;

        while (count < flds.length && !flds[count].equals("."))
          b.append(flds[count++]).append(' ');
        String name = b.toString().trim();
        b.setLength(0);
        count++;

        while (count < flds.length && !flds[count].equals("."))
          b.append(flds[count++]).append(' ');
        String unit = b.toString().trim();

        Grib2Parameter s = new Grib2Parameter(p1, p2, p3, name, unit, abbrev, null);
        local.put(makeParamId(p1, p2, p3), s);
        if (debug) System.out.printf(" %s%n", s);
      }

    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

/*
0	0	0	TMP	. Temperature	. K
0	0	2	POT	. Potential temperature	. K
0	0	4	T MAX	. Maximum temperature	. K
0	0	5	T MIN	. Minimum temperature	. K
0	0	6	DPT	. Dewpoint temperature	. K
*/

  public static void main(String[] args) {
    CfsrLocalTables t = new CfsrLocalTables(new Grib2Table("DSS", 7, 0, 0, 0, -1, null, Grib2Table.Type.cfsr));
    Formatter f = new Formatter();
    Grib2Parameter.compareTables("DSS-093", "Standard WMO version 8", t.getParameters(), Grib2Customizer.factory(0, 0, 0, 0, 0), f);
    System.out.printf("%s%n", f);

    Formatter f2 = new Formatter();
    Grib2Parameter.compareTables("DSS-093", "NCEP Table", t.getParameters(), Grib2Customizer.factory(7, 0, 0, 0, 0), f2);
    System.out.printf("%s%n", f2);

  }
}
