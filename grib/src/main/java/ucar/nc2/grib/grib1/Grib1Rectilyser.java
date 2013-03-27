/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib1;

import ucar.nc2.grib.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

import java.io.IOException;
import java.util.*;

/**
 * Turn a collection of Grib1Records into a rectangular array.
 *
 * Use of Grib1Customizer:
 * <ul>
 *   <li>Needs to know if its a time interval, and if so, what the statistical type is</li>
 *   <li>Needs to extract the time value(s)</li>
 *   <li>Needs to know if its a level layer</li>
 *   <li>Needs to extract the level value(s)</li>
 * </ul>
 *
 * Note that the GribParameter Tables are <strong>not</strong> used. So they can be overrided and only the IOSP needs to
 * deal with that.
 *
 * @author caron
 * @since 3/30/11
 */
public class Grib1Rectilyser {
  private Grib1Customizer cust;
  private final List<Grib1Record> records;
  private final int gdsHash;

  private final boolean intvMerge;
  private final boolean useTableVersion;

  private List<VariableBag> gribvars;

  private final List<TimeCoord> timeCoords = new ArrayList<TimeCoord>();
  private final List<VertCoord> vertCoords = new ArrayList<VertCoord>();
  private final List<EnsCoord> ensCoords = new ArrayList<EnsCoord>();

  // records must be sorted - later ones override earlier ones with the same index
  public Grib1Rectilyser(Grib1Customizer cust, List<Grib1Record> records, int gdsHash, Map<String, Boolean> pdsHash) {
    this.cust = cust;
    this.records = records;
    this.gdsHash = gdsHash;

    intvMerge = assignValue(pdsHash, "intvMerge", true);
    useTableVersion = assignValue(pdsHash, "useTableVersion", true);
  }

  private boolean assignValue(Map<String, Boolean> pdsHash, String key, boolean value) {
    if (pdsHash != null) {
      Boolean b = pdsHash.get(key);
      if (b != null) value = b;
    }
    return value;
  }

  public List<Grib1Record> getRecords() {
    return records;
  }

  public List<VariableBag> getGribvars() {
    return gribvars;
  }

  public List<TimeCoord> getTimeCoords() {
    return timeCoords;
  }

  public List<VertCoord> getVertCoords() {
    return vertCoords;
  }

  public List<EnsCoord> getEnsCoords() {
    return ensCoords;
  }

  public void make( Counter counter) throws IOException {
    // unique variables using Grib1Record.cdmVariableHash()
    Map<Integer, VariableBag> vbHash = new HashMap<Integer, VariableBag>(100);
    for (Grib1Record gr : records) {
      int cdmHash = cdmVariableHash(gr, gdsHash, intvMerge, useTableVersion);
      VariableBag bag = vbHash.get(cdmHash);
      if (bag == null) {
        bag = new VariableBag(gr, cdmHash);
        vbHash.put(cdmHash, bag);
      }
      bag.atomList.add( new Record(gr));
    }
    gribvars = new ArrayList<VariableBag>(vbHash.values());
    Collections.sort(gribvars); // make it deterministic by sorting

    // create and assign time coordinate
    // uniform or not X isInterval or not
    for (VariableBag vb : gribvars) {
      TimeCoord use;
      boolean isUniform = checkTimeCoordsUniform(vb);
      Grib1SectionProductDefinition pds = vb.first.getPDSsection();
      Grib1ParamTime ptime = pds.getParamTime(cust);
      if (ptime.isInterval()) {
        use = makeTimeCoordsIntv(vb, isUniform);
      } else {
        use = makeTimeCoords(vb, isUniform);
      }
      vb.timeCoordIndex = TimeCoord.findCoord(timeCoords, use); // share coordinates when possible
    }

    // create and assign vert coordinate
    for (VariableBag vb : gribvars) {
      VertCoord vc = makeVertCoord(vb);
      if (vc.isVertDimensionUsed()) {
        vb.vertCoordIndex = VertCoord.findCoord(vertCoords, vc); // share coordinates when possible
      }
    }

    // create and assign ens coordinate
    for (VariableBag vb : gribvars) {
      EnsCoord ec = makeEnsCoord(vb);
      if (ec != null) {
        vb.ensCoordIndex = EnsCoord.findCoord(ensCoords, ec); // share coordinates when possible
      }
    }

    int tot_used = 0;
    int tot_dups = 0;

    // for each variable, create recordMap, which maps index (time, ens, vert) -> Grib1Record
    for (VariableBag vb : gribvars) {
      TimeCoord tc = timeCoords.get(vb.timeCoordIndex);
      VertCoord vc = (vb.vertCoordIndex < 0) ? null : vertCoords.get(vb.vertCoordIndex);
      EnsCoord ec = (vb.ensCoordIndex < 0) ? null :  ensCoords.get(vb.ensCoordIndex);

      int ntimes = tc.getSize();
      int nverts = (vc == null) ? 1 : vc.getSize();
      int nens = (ec == null) ? 1 : ec.getSize();
      vb.recordMap = new Record[ntimes * nverts * nens];

      for (Record r : vb.atomList) {
        int timeIdx =  (r.tcIntvCoord != null) ? tc.findInterval(r.tcIntvCoord) : tc.findIdx(r.tcCoord);
        if (timeIdx < 0) {
          timeIdx = (r.tcIntvCoord != null) ? tc.findInterval(r.tcIntvCoord) : tc.findIdx(r.tcCoord); // debug
          throw new IllegalStateException("Cant find time coord "+r.tcCoord);
        }

        int vertIdx =  (vb.vertCoordIndex < 0) ? 0 : vc.findIdx(r.vcCoord);
        if (vertIdx < 0) {
          vertIdx = vc.findIdx(r.vcCoord); // debug
          throw new IllegalStateException("Cant find vert coord "+r.vcCoord);
        }

        int ensIdx =  (vb.ensCoordIndex < 0) ? 0 : ec.findIdx(r.ecCoord);
        if (ensIdx < 0) {
          ensIdx =  ec.findIdx(r.ecCoord); // debug
          throw new IllegalStateException("Cant find ens coord "+r.ecCoord);
        }

        // later records overwrite earlier ones with same index. so atomList must be ordered
        int index = GribCollection.calcIndex(timeIdx, ensIdx, vertIdx, nens, nverts);
        if (vb.recordMap[index] != null) tot_dups++; else tot_used++;
        vb.recordMap[index] = r;
      }
      //System.out.printf("%d: recordMap %d = records %d - dups %d (%d)%n", vb.first.cdmVariableHash(),
      //        vb.recordMap.length, vb.atomList.size(), dups, vb.atomList.size() - vb.recordMap.length);
    }
    counter.recordsUnique += tot_used;
    counter.dups += tot_dups;
    counter.vars += gribvars.size();
  }

  // debugging
  public int cdmVariableHash(Grib1Record gr, int gdsHash) {
    return cdmVariableHash(gr, gdsHash, false, false);
  }

  /**
   * A hash code to group records into a CDM variable
   * Herein lies the semantics of a variable object identity.
   * Read it and weep.
   *
   * @param gdsHash can override the gdsHash
   * @return this records hash code, to group like records into a variable
   */
  private int cdmVariableHash(Grib1Record gr, int gdsHash, boolean intvMerge, boolean useTableVersion) {
    int result = 17;

    Grib1SectionGridDefinition gdss = gr.getGDSsection();
    if (gdsHash == 0)
      result += result * 37 + gdss.getGDS().hashCode(); // the horizontal grid
    else
      result += result * 37 + gdsHash;

    Grib1SectionProductDefinition pdss = gr.getPDSsection();
    result += result * 37 + pdss.getLevelType();
    if (cust.isLayer(pdss.getLevelType())) result += result * 37 + 1;

    result += result * 37 + pdss.getParameterNumber();
    if (useTableVersion)
      result += result * 37 + pdss.getTableVersion();

    Grib1ParamTime ptime = pdss.getParamTime(cust);
    if (ptime.isInterval()) {
      if (!intvMerge) result += result * 37 + ptime.getIntervalSize();  // create new variable for each interval size
      if (ptime.getStatType() != null) result += result * 37 + ptime.getStatType().ordinal(); // create new variable for each stat type
    }

    // LOOK maybe we should always add ??
    // if this uses any local tables, then we have to add the center id, and subcenter if present
    if (pdss.getParameterNumber() > 127) {
      result += result * 37 + pdss.getCenter();
      if (pdss.getSubCenter() > 0)
        result += result * 37 + pdss.getSubCenter();
    }
    return result;
  }

  public String getTimeIntervalName(int timeIdx) {
    TimeCoord tc = timeCoords.get(timeIdx);
    return tc.getTimeIntervalName();
  }

  static public class Counter {
    int recordsUnique;
    int recordsTotal;
    int filter;
    int dups;
    int vars;

    public String show () {
      Formatter f = new Formatter();
      float dupPercent = ((float) dups) / (recordsTotal - filter);
      f.format(" Rectilyser2: nvars=%d records total=%d filtered=%d unique=%d dups=%d (%f)%n",
              vars, recordsTotal, filter, recordsUnique, dups, dupPercent);
      return f.toString();
    }
  }

  public class Record {
    Grib1Record gr;
    int tcCoord;
    TimeCoord.Tinv tcIntvCoord;
    VertCoord.Level vcCoord;
    EnsCoord.Coord ecCoord;

    private Record(Grib1Record gr) {
      this.gr = gr;
    }
  }

  private VertCoord makeVertCoord(VariableBag vb) {
    Grib1SectionProductDefinition pdsFirst = vb.first.getPDSsection();
    VertCoord.VertUnit vertUnit = cust.getVertUnit(pdsFirst.getLevelType());
    boolean isLayer = cust.isLayer(pdsFirst.getLevelType());

    Set<VertCoord.Level> coords = new HashSet<VertCoord.Level>();
    for (Record r : vb.atomList) {
      Grib1SectionProductDefinition pds = r.gr.getPDSsection();
      Grib1ParamLevel plevel = cust.getParamLevel(pds);
      r.vcCoord = new VertCoord.Level(plevel.getValue1(), plevel.getValue2());
      coords.add(r.vcCoord);
    }

    List<VertCoord.Level> vlist = new ArrayList<VertCoord.Level>(coords);
    Collections.sort(vlist);
    if (vertUnit.isPositiveUp())
      Collections.reverse(vlist);

    return new VertCoord(vlist, vertUnit, isLayer);
  }

  private EnsCoord makeEnsCoord(VariableBag vb) {
    if (!vb.first.getPDSsection().isEnsemble()) return null;

    Set<EnsCoord.Coord> coords = new HashSet<EnsCoord.Coord>();
    for (Record r : vb.atomList) {
      Grib1SectionProductDefinition pds = r.gr.getPDSsection();
      r.ecCoord = new EnsCoord.Coord(pds.getPerturbationType(), pds.getPerturbationNumber());
      coords.add(r.ecCoord);
    }
    List<EnsCoord.Coord> elist = new ArrayList<EnsCoord.Coord>(coords);
    Collections.sort(elist);
    return new EnsCoord(elist);
  }

  private CalendarPeriod convertTimeDuration(int timeUnit) {
    return GribUtils.getCalendarPeriod( cust.convertTimeUnit(timeUnit));
  }

  /**
   * check if refDate and timeUnit is the same for all atoms.
   * set vb refDate, timeUnit fields as side effect. if not true, refDate is earliest
   *
   * @param vb check this collection
   * @return true if refDate, timeUnit are the same for all records
   */
  private boolean checkTimeCoordsUniform(VariableBag vb) {
    boolean isUniform = true;
    CalendarDate refDate = null;
    int timeUnit = -1;
    boolean timeUnitOk = true;

    for (Record r : vb.atomList) {
      Grib1SectionProductDefinition pds = r.gr.getPDSsection();

      int unit = cust.convertTimeUnit(pds.getTimeUnit());
      if (timeUnit < 0) { // first one
        timeUnit = unit;
        vb.timeUnit = GribUtils.getCalendarPeriod(timeUnit);

      } else if (unit != timeUnit) {
        isUniform = false;
      }

      CalendarDate cd = r.gr.getReferenceDate();
      if (refDate == null) {
        refDate = cd;

      } else if (!cd.equals(refDate)) {
        isUniform = false;
        if (cd.compareTo(refDate) < 0) // earliest one
          refDate = cd;
      }

      // LOOK - cant you just compare time units ??
      Grib1ParamTime ptime = pds.getParamTime(cust);
      int time = ptime.getForecastTime();
      CalendarDate date1 = cd.add( GribUtils.getCalendarPeriod(unit).multiply(time));  // actual forecast date
      int offset = TimeCoord.getOffset(refDate, date1, vb.timeUnit);
      CalendarDate date2 = refDate.add( vb.timeUnit.multiply(offset));  // forecast date using offset
      if (!date1.equals(date2)) {
        timeUnitOk = false;
      }
    }

    // drop down to minutes if the time unit in the grib record is not accurate
    if (!timeUnitOk)
      timeUnit = 0; // minutes

    vb.timeUnit = GribUtils.getCalendarPeriod(timeUnit);
    vb.refDate = refDate;
    return isUniform;
  }

  private TimeCoord makeTimeCoords(VariableBag vb, boolean uniform) {
    int timeType = -1;
    Set<Integer> times = new HashSet<Integer>();
    for (Record r : vb.atomList) {
      Grib1SectionProductDefinition pds = r.gr.getPDSsection();
      if (timeType < 0) timeType = pds.getTimeRangeIndicator();
      Grib1ParamTime ptime = pds.getParamTime(cust);
      int time = ptime.getForecastTime();
      CalendarPeriod duration = convertTimeDuration(pds.getTimeUnit());

      if (uniform) {
        r.tcCoord = time;
      } else {
        CalendarDate refDate = r.gr.getReferenceDate();
        CalendarDate date = refDate.add( duration.multiply(time));
        r.tcCoord = TimeCoord.getOffset(vb.refDate, date, vb.timeUnit);
      }
      times.add(r.tcCoord);
    }
    List<Integer> tlist = new ArrayList<Integer>(times);
    Collections.sort(tlist);
    return new TimeCoord(timeType, vb.refDate, vb.timeUnit, tlist);
  }

  private TimeCoord makeTimeCoordsIntv(VariableBag vb, boolean uniform) {
    int timeType = -1;
    Set<TimeCoord.Tinv> times = new HashSet<TimeCoord.Tinv>();
    for (Record r : vb.atomList) {
      Grib1SectionProductDefinition pds = r.gr.getPDSsection();
      if (timeType < 0) timeType = pds.getTimeRangeIndicator();
      Grib1ParamTime ptime = pds.getParamTime(cust);
      int[] timeb = ptime.getInterval();
      if (uniform) {
        r.tcIntvCoord = new TimeCoord.Tinv(timeb[0], timeb[1]);
        times.add(r.tcIntvCoord);
      } else {
        TimeCoord.Tinv org = new TimeCoord.Tinv(timeb[0], timeb[1]);
        CalendarPeriod fromUnit = convertTimeDuration(pds.getTimeUnit());
        r.tcIntvCoord =  org.convertReferenceDate(r.gr.getReferenceDate(), fromUnit, vb.refDate, vb.timeUnit);
        times.add(r.tcIntvCoord);
      }
    }
    List<TimeCoord.Tinv> tlist = new ArrayList<TimeCoord.Tinv>(times);
    Collections.sort(tlist);
    return new TimeCoord(timeType, vb.refDate, vb.timeUnit, tlist);
  }

  public void dump(Formatter f) {
    f.format("%nTime Coordinates%n");
    for (int i=0; i<timeCoords.size(); i++) {
      TimeCoord time = timeCoords.get(i);
      f.format("  %d: (%d) %s%n", i, time.getSize(), time);
    }

    f.format("%nVert Coordinates%n");
    for (int i=0; i<vertCoords.size(); i++) {
      VertCoord coord = vertCoords.get(i);
      f.format("  %d: (%d) %s%n", i, coord.getSize(), coord);
    }

    f.format("%nEns Coordinates%n");
    for (int i=0; i<ensCoords.size(); i++) {
      EnsCoord coord = ensCoords.get(i);
      f.format("  %d: (%d) %s%n", i, coord.getSize(), coord);
    }

    f.format("%nVariables%n");
    f.format("%n  %3s %3s %3s%n", "time", "vert", "ens");
    for (VariableBag vb : gribvars) {
      String vname = Grib1Utils.extractParameterCode(vb.first);
      f.format("  %3d %3d %3d %s records = %d density = %d/%d", vb.timeCoordIndex, vb.vertCoordIndex, vb.ensCoordIndex,
                vname, vb.atomList.size(), vb.countDensity(), vb.recordMap.length);
      if (vb.countDensity() != vb.recordMap.length) f.format(" HEY!!");
      f.format("%n");
    }
  }

  public class VariableBag implements Comparable<VariableBag> {
    Grib1Record first;
    int cdmHash;

    List<Record> atomList = new ArrayList<Record>(100);
    int timeCoordIndex = -1;
    int vertCoordIndex = -1;
    int ensCoordIndex = -1;
    CalendarDate refDate;
    CalendarPeriod timeUnit;
    Record[] recordMap;
    long pos;
    int length;

    private VariableBag(Grib1Record first, int cdmHash) {
      this.first = first;
      this.cdmHash = cdmHash;
    }

    @Override
    public int compareTo(VariableBag o) {
      return Grib1Utils.extractParameterCode(first).compareTo(Grib1Utils.extractParameterCode(o.first)); // LOOK
    }

    int countDensity() {
      int count = 0;
      for (Record r : recordMap)
        if (r != null) count++;
      return count;
    }
  }

}