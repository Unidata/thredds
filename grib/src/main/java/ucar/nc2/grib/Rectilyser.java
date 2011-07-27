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

package ucar.nc2.grib;

import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.table.GribTables;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDuration;

import java.io.IOException;
import java.util.*;

/**
 * Turn a collection of Grib2Records into a rectangular array
 *
 * @author caron
 * @since 3/30/11
 */
public class Rectilyser {
  private final GribTables tables;
  private final int gdsHash;

  private final List<Grib2Record> records;
  private List<VariableBag> gribvars;

  private final List<TimeCoord> timeCoords = new ArrayList<TimeCoord>();
  private final List<VertCoord> vertCoords = new ArrayList<VertCoord>();
  private final List<EnsCoord> ensCoords = new ArrayList<EnsCoord>();

  // records must be sorted - later ones override earlier ones with the same index
  public Rectilyser(List<Grib2Record> records, int gdsHash) {
    this.records = records;
    this.gdsHash = gdsHash;

    Grib2Record first = records.get(0);
    Grib2SectionIdentification ids = first.getId();
    this.tables = GribTables.factory(ids.getCenter_id(), ids.getSubcenter_id(), ids.getMaster_table_version(), ids.getLocal_table_version());
  }

  public List<Grib2Record> getRecords() {
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

  public void make( Formatter f, Counter counter) throws IOException {
    // unique variables using Grib2Record.cdmVariableHash()
    Map<Integer, VariableBag> vbHash = new HashMap<Integer, VariableBag>(100);
    for (Grib2Record gr : records) {
      int cdmHash = gr.cdmVariableHash(gdsHash);
      VariableBag bag = vbHash.get(cdmHash);
      if (bag == null) {
        bag = new VariableBag(gr);
        vbHash.put(cdmHash, bag);
      }
      bag.atomList.add( new Record(gr));
    }
    gribvars = new ArrayList<VariableBag>(vbHash.values());
    Collections.sort(gribvars); // make it deterministic by sorting

    // create and assign time coordinate
    // uniform or not X isInterval or not
    for (VariableBag vb : gribvars) {
      TimeCoord use = null;
      boolean isUniform = checkTimeCoordsUniform(vb);
      if (vb.first.getPDS().isInterval()) {
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

    int tot_recordMap = 0;
    int tot_records = 0;
    int tot_dups = 0;

    // for each variable, create recordMap, which maps index (time, ens, vert) -> Grib2Record
    for (VariableBag vb : gribvars) {
      TimeCoord tc = timeCoords.get(vb.timeCoordIndex);
      VertCoord vc = (vb.vertCoordIndex < 0) ? null : vertCoords.get(vb.vertCoordIndex);
      EnsCoord ec = (vb.ensCoordIndex < 0) ? null :  ensCoords.get(vb.ensCoordIndex);

      int ntimes = tc.getSize();
      int nverts = (vc == null) ? 1 : vc.getSize();
      int nens = (ec == null) ? 1 : ec.getSize();
      vb.recordMap = new Record[ntimes * nverts * nens];
      int dups = 0;

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
        if (vb.recordMap[index] != null) dups++;
        vb.recordMap[index] = r;
      }
      //System.out.printf("%d: recordMap %d = records %d - dups %d (%d)%n", vb.first.cdmVariableHash(),
      //        vb.recordMap.length, vb.atomList.size(), dups, vb.atomList.size() - vb.recordMap.length);
      tot_recordMap += vb.recordMap.length;
      tot_records += vb.atomList.size();
      tot_dups += dups;
    }
    f.format("records unique=%d total=%d dups=%d (%f) %n", tot_recordMap, tot_records, tot_dups, ((float)tot_dups)/tot_records);
    counter.recordsUnique += tot_recordMap;
    counter.records += tot_records;
    counter.dups += tot_dups;
    counter.vars += gribvars.size();
  }

  static public class Counter {
    int recordsUnique;
    int records;
    int dups;
    int vars;
  }

  public class Record {
    Grib2Record gr;
    int tcCoord;
    TimeCoord.Tinv tcIntvCoord;
    VertCoord.Level vcCoord;
    EnsCoord.Coord ecCoord;

    private Record(Grib2Record gr) {
      this.gr = gr;
    }
  }

  private VertCoord makeVertCoord(VariableBag vb) {
    Grib2Pds pdsFirst = vb.first.getPDS();
    VertCoord.VertUnit vertUnit = Grib2Utils.getLevelUnit(pdsFirst.getLevelType1());
    boolean isLayer = Grib2Utils.isLayer(vb.first);

    Set<VertCoord.Level> coords = new HashSet<VertCoord.Level>();

    for (Record r : vb.atomList) {
      Grib2Pds pds = r.gr.getPDS();
      r.vcCoord = new VertCoord.Level(pds.getLevelValue1(), pds.getLevelValue2());
      coords.add(r.vcCoord);
    }

    List<VertCoord.Level> vlist = new ArrayList<VertCoord.Level>(coords);
    Collections.sort(vlist);
    if (!vertUnit.isPositiveUp) {
      Collections.reverse(vlist);
    }

    return new VertCoord(vlist, vertUnit, isLayer);
  }


  private EnsCoord makeEnsCoord(VariableBag vb) {
    if (!vb.first.getPDS().isEnsemble()) return null;

    Set<EnsCoord.Coord> coords = new HashSet<EnsCoord.Coord>();
    for (Record r : vb.atomList) {
      Grib2Pds pds = r.gr.getPDS();
      r.ecCoord = new EnsCoord.Coord(pds.getPerturbationType(), pds.getPerturbationNumber());
      coords.add(r.ecCoord);
    }
    List<EnsCoord.Coord> elist = new ArrayList<EnsCoord.Coord>(coords);
    Collections.sort(elist);
    return new EnsCoord(elist);
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
      Grib2Pds pds = r.gr.getPDS();
      int unit = pds.getTimeUnit();
      if (timeUnit < 0) { // first one
        timeUnit = unit;
        vb.timeUnit = Grib2Utils.getCalendarDuration(timeUnit);

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

      int time = pds.getForecastTime();
      CalendarDate date1 = cd.add(time, Grib2Utils.getCalendarDuration(unit));  // actual forecast date
      int offset = TimeCoord.getOffset(refDate, date1, vb.timeUnit);
      CalendarDate date2 = refDate.add(offset, vb.timeUnit);  // forecast date using offset
      if (!date1.equals(date2)) {
        timeUnitOk = false;
      }
    }

    // drop down to minutes if the time unit in the grib record is not accurate
    if (!timeUnitOk)
      timeUnit = 0; // minutes

    vb.timeUnit = Grib2Utils.getCalendarDuration(timeUnit);
    vb.refDate = refDate;
    return isUniform;
  }

  private TimeCoord makeTimeCoords(VariableBag vb, boolean uniform) {
    Set<Integer> times = new HashSet<Integer>();
    for (Record r : vb.atomList) {
      Grib2Pds pds = r.gr.getPDS();
      int time = pds.getForecastTime();
      int unit = pds.getTimeUnit();
      CalendarDuration duration = Grib2Utils.getCalendarDuration(unit);

      if (uniform) {
        r.tcCoord = time;
      } else {
        CalendarDate refDate = r.gr.getReferenceDate();
        CalendarDate date = refDate.add(time, duration);
        r.tcCoord = TimeCoord.getOffset(vb.refDate, date, vb.timeUnit);
      }
      times.add(r.tcCoord);
    }
    List<Integer> tlist = new ArrayList<Integer>(times);
    Collections.sort(tlist);
    return new TimeCoord(vb.refDate, vb.timeUnit, tlist);
  }

  private TimeCoord makeTimeCoordsIntv(VariableBag vb, boolean uniform) {
    Set<TimeCoord.Tinv> times = new HashSet<TimeCoord.Tinv>();
    for (Record r : vb.atomList) {
      Grib2Pds pds = r.gr.getPDS();
      int[] timeb = tables.getForecastTimeInterval(r.gr);
      if (uniform) {
        r.tcIntvCoord = new TimeCoord.Tinv(timeb[0], timeb[1]);
        times.add(r.tcIntvCoord);
      } else {
        TimeCoord.Tinv org = new TimeCoord.Tinv(timeb[0], timeb[1]);
        CalendarDuration fromUnit = Grib2Utils.getCalendarDuration(pds.getTimeUnit());
        r.tcIntvCoord =  org.convertReferenceDate(r.gr.getReferenceDate(), fromUnit, vb.refDate, vb.timeUnit);
        times.add(r.tcIntvCoord);
      }
    }
    List<TimeCoord.Tinv> tlist = new ArrayList<TimeCoord.Tinv>(times);
    Collections.sort(tlist);
    return new TimeCoord(vb.refDate, vb.timeUnit, tlist);
  }

  public void dump(Formatter f, GribTables tables) {
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
      String vname = tables.getVariableName(vb.first);
      f.format("  %3d %3d %3d %s records = %d density = %d/%d", vb.timeCoordIndex, vb.vertCoordIndex, vb.ensCoordIndex,
                vname, vb.atomList.size(), vb.countDensity(), vb.recordMap.length);
      if (vb.countDensity() != vb.recordMap.length) f.format(" HEY!!");
      f.format("%n");
    }
  }

  public class VariableBag implements Comparable<VariableBag> {
    Grib2Record first;
    List<Record> atomList = new ArrayList<Record>(100);
    int timeCoordIndex = -1;
    int vertCoordIndex = -1;
    int ensCoordIndex = -1;
    CalendarDate refDate;
    CalendarDuration timeUnit;
    Record[] recordMap;
    long pos;
    int length;

    private VariableBag(Grib2Record first) {
      this.first = first;
    }

    @Override
    public int compareTo(VariableBag o) {
      return Grib2Utils.getVariableName(first).compareTo(Grib2Utils.getVariableName(o.first));
    }

    int countDensity() {
      int count = 0;
      for (Record r : recordMap)
        if (r != null) count++;
      return count;
    }
  }

}