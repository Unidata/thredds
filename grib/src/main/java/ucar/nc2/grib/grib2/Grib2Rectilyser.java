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

package ucar.nc2.grib.grib2;

import thredds.inventory.MFile;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

import java.io.IOException;
import java.util.*;

/**
 * Turn a collection of Grib2Records into a rectangular array
 *
 * @author caron
 * @since 3/30/11
 */
public class Grib2Rectilyser {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2CollectionBuilder.class);
  //static private final boolean useGenType = false; // LOOK dummy for now

  private final Grib2Customizer cust;
  private final int gdsHash;
  private final boolean intvMerge;
  private final boolean useGenType;
  private final boolean useTableVersion;

  private final List<Grib2Record> records;
  private List<VariableBag> gribvars;

  private final List<TimeCoord> timeCoords = new ArrayList<TimeCoord>();
  private final List<VertCoord> vertCoords = new ArrayList<VertCoord>();
  private final List<EnsCoord> ensCoords = new ArrayList<EnsCoord>();

  // records must be sorted - later ones override earlier ones with the same index
  public Grib2Rectilyser(Grib2Customizer cust, List<Grib2Record> records, int gdsHash, Map<String, Boolean> pdsHash) {
    this.cust = cust;
    this.records = records;
    this.gdsHash = gdsHash;

    intvMerge = assignValue(pdsHash, "intvMerge", true);
    useTableVersion = assignValue(pdsHash, "useTableVersion", true);
    useGenType = assignValue(pdsHash, "useGenType", false);
  }

  private boolean assignValue(Map<String, Boolean> pdsHash, String key, boolean value) {
    if (pdsHash != null) {
      Boolean b = pdsHash.get(key);
      if (b != null) value = b;
    }
    return value;
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

  List<MFile> files = null; // temp debug
  public void make(Counter counter, List<MFile> files) throws IOException {
    this.files = files;

    // unique variables using Grib2Record.cdmVariableHash()
    Map<Integer, VariableBag> vbHash = new HashMap<Integer, VariableBag>(100);
    for (Grib2Record gr : records) {
      int cdmHash = cdmVariableHash(gr, gdsHash);
      VariableBag bag = vbHash.get(cdmHash);
      if (bag == null) {
        bag = new VariableBag(gr, cdmHash);
        vbHash.put(cdmHash, bag);
        //bag.useGenType = useGenType;
      }
      bag.atomList.add(new Record(gr));
    }
    gribvars = new ArrayList<VariableBag>(vbHash.values());
    Collections.sort(gribvars); // make it deterministic by sorting

    // create and assign time coordinate
    // uniform or not X isInterval or not
    for (VariableBag vb : gribvars) {
      setTimeUnit(vb);
      TimeCoord use = null;
      if (vb.first.getPDS().isTimeInterval()) {
        use = makeTimeCoordsIntv(vb);
      } else {
        boolean isUniform = checkTimeCoordsUniform(vb);
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

    // for each variable, create recordMap, which maps index (time, ens, vert) -> Grib2Record
    for (VariableBag vb : gribvars) {
      TimeCoord tc = timeCoords.get(vb.timeCoordIndex);
      VertCoord vc = (vb.vertCoordIndex < 0) ? null : vertCoords.get(vb.vertCoordIndex);
      EnsCoord ec = (vb.ensCoordIndex < 0) ? null : ensCoords.get(vb.ensCoordIndex);

      int ntimes = tc.getSize();
      int nverts = (vc == null) ? 1 : vc.getSize();
      int nens = (ec == null) ? 1 : ec.getSize();
      vb.recordMap = new Record[ntimes * nverts * nens];

      for (Record r : vb.atomList) {
        int timeIdx = (r.tcIntvCoord != null) ? r.tcIntvCoord.index : tc.findIdx(r.tcCoord);
        if (timeIdx < 0) {
          timeIdx = (r.tcIntvCoord != null) ? r.tcIntvCoord.index : tc.findIdx(r.tcCoord); // debug
          throw new IllegalStateException("Cant find time coord " + r.tcCoord);
        }

        int vertIdx = (vb.vertCoordIndex < 0) ? 0 : vc.findIdx(r.vcCoord);
        if (vertIdx < 0) {
          vertIdx = vc.findIdx(r.vcCoord); // debug
          throw new IllegalStateException("Cant find vert coord " + r.vcCoord);
        }

        int ensIdx = (vb.ensCoordIndex < 0) ? 0 : ec.findIdx(r.ecCoord);
        if (ensIdx < 0) {
          ensIdx = ec.findIdx(r.ecCoord); // debug
          throw new IllegalStateException("Cant find ens coord " + r.ecCoord);
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

  static public class Counter {
    public int recordsTotal;
    public int recordsUnique;
    public int dups;
    public int filter;
    public int vars;

    public String show () {
      Formatter f = new Formatter();
      float dupPercent = ((float) dups) / (recordsTotal - filter);
      f.format(" Rectilyser2: nvars=%d records total=%d filtered=%d unique=%d dups=%d (%f)%n",
              vars, recordsTotal, filter, recordsUnique, dups, dupPercent);
      return f.toString();
    }
  }

  public class Record {
    Grib2Record gr;
    int tcCoord;
    TimeCoord.TinvDate tcIntvCoord;
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
    if (!vertUnit.isPositiveUp()) {
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

  private CalendarPeriod convertTimeDuration(int timeUnit) {
    return Grib2Utils.getCalendarPeriod( cust.convertTimeUnit(timeUnit));
  }

  private void setTimeUnit(VariableBag vb) {
    Record first = vb.atomList.get(0);
    Grib2Pds pds = first.gr.getPDS();
    int unit = cust.convertTimeUnit(pds.getTimeUnit());
    vb.timeUnit = Grib2Utils.getCalendarPeriod(unit);
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
      int unit = cust.convertTimeUnit(pds.getTimeUnit());
      if (timeUnit < 0) { // first one
        timeUnit = unit;
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
      int time = pds.getForecastTime();
      CalendarDate date1 = cd.add(Grib2Utils.getCalendarPeriod(unit).multiply(time));  // actual forecast date
      int offset = TimeCoord.getOffset(refDate, date1, vb.timeUnit);
      CalendarDate date2 = refDate.add(vb.timeUnit.multiply(offset));  // forecast date using offset
      if (!date1.equals(date2)) {
        timeUnitOk = false;
      }
    }

    // drop down to minutes if the time unit in the grib record is not accurate
    if (!timeUnitOk)
      timeUnit = 0; // minutes

    vb.timeUnit = Grib2Utils.getCalendarPeriod(timeUnit);
    vb.refDate = refDate;
    return isUniform;
  }

  private TimeCoord makeTimeCoords(VariableBag vb, boolean uniform) {
    Set<Integer> times = new HashSet<Integer>();
    for (Record r : vb.atomList) {
      Grib2Pds pds = r.gr.getPDS();
      int time = pds.getForecastTime();
      CalendarPeriod duration = convertTimeDuration(pds.getTimeUnit());

      if (uniform) {
        r.tcCoord = time;
      } else {
        CalendarDate refDate = r.gr.getReferenceDate();
        CalendarDate date = refDate.add(duration.multiply(time));
        r.tcCoord = TimeCoord.getOffset(vb.refDate, date, vb.timeUnit);
      }
      times.add(r.tcCoord);
    }
    List<Integer> tlist = new ArrayList<Integer>(times);
    Collections.sort(tlist);
    return new TimeCoord(0, vb.refDate, vb.timeUnit, tlist);
  }

  private TimeCoord makeTimeCoordsIntv(VariableBag vb) {
    int timeIntvCode = 999; // just for documentation in the time coord attribute
    Map<Integer, TimeCoord.TinvDate> times = new HashMap<Integer, TimeCoord.TinvDate>();
    for (Record r : vb.atomList) {
      Grib2Pds pds = r.gr.getPDS();
      if (timeIntvCode == 999) timeIntvCode = pds.getStatisticalProcessType();
      TimeCoord.TinvDate mine = cust.getForecastTimeInterval(r.gr);
      TimeCoord.TinvDate org = times.get(mine.hashCode());
      if (org == null) times.put(mine.hashCode(), mine);
      r.tcIntvCoord = (org == null) ? mine : org; // always point to the one thats in the HashMap
    }

     /* if (uniform) {
        r.tcIntvCoord = org;
        times.add(r.tcIntvCoord);

      } else {
        int timeUnit = cust.convertTimeUnit(pds.getTimeUnit());
        CalendarPeriod fromUnit = Grib2Utils.getCalendarPeriod(timeUnit);
        r.tcIntvCoord = org.convertReferenceDate(r.gr.getReferenceDate(), fromUnit, vb.refDate, vb.timeUnit); // LOOK heres the magic
        times.add(r.tcIntvCoord);
      }
    } */
    List<TimeCoord.TinvDate> tlist = new ArrayList<TimeCoord.TinvDate>(times.values());
    Collections.sort(tlist);
    return new TimeCoord(timeIntvCode, vb.refDate, vb.timeUnit, tlist);
  }

  public void dump(Formatter f, Grib2Customizer tables) {
    f.format("%nTime Coordinates%n");
    for (int i = 0; i < timeCoords.size(); i++) {
      TimeCoord time = timeCoords.get(i);
      f.format("  %d: (%d) %s%n", i, time.getSize(), time);
    }

    f.format("%nVert Coordinates%n");
    for (int i = 0; i < vertCoords.size(); i++) {
      VertCoord coord = vertCoords.get(i);
      f.format("  %d: (%d) %s%n", i, coord.getSize(), coord);
    }

    f.format("%nEns Coordinates%n");
    for (int i = 0; i < ensCoords.size(); i++) {
      EnsCoord coord = ensCoords.get(i);
      f.format("  %d: (%d) %s%n", i, coord.getSize(), coord);
    }

    f.format("%nVariables%n");
    f.format("%n  %3s %3s %3s%n", "time", "vert", "ens");
    for (VariableBag vb : gribvars) {
      String vname = tables.getVariableName(vb.first);
      f.format("  %3d %3d %3d %s records = %d density = %d/%d hash=%d", vb.timeCoordIndex, vb.vertCoordIndex, vb.ensCoordIndex,
              vname, vb.atomList.size(), vb.countDensity(), vb.recordMap.length, vb.cdmHash);
      if (vb.countDensity() != vb.recordMap.length) f.format(" HEY!!");
      f.format("%n");
    }
  }

  public class VariableBag implements Comparable<VariableBag> {
    Grib2Record first;
    int cdmHash;
    //boolean useGenType;

    List<Record> atomList = new ArrayList<Record>(100);
    int timeCoordIndex = -1;
    int vertCoordIndex = -1;
    int ensCoordIndex = -1;
    CalendarDate refDate;
    CalendarPeriod timeUnit;
    Record[] recordMap;
    long pos;
    int length;

    private VariableBag(Grib2Record first, int cdmHash) {
      this.first = first;
      this.cdmHash = cdmHash;
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

  /**
   * A hash code to group records into a CDM variable
   * Herein lies the semantics of a variable object identity.
   * Read it and weep.
   *
   * @param gr the Grib record
   * @param gdsHash can override the gdsHash
   * @return this record's hash code, identical hash means belongs to the same variable
   */
  public int cdmVariableHash(Grib2Record gr, int gdsHash) {
    Grib2SectionGridDefinition gdss = gr.getGDSsection();
    Grib2Pds pds2 = gr.getPDS();

    int result = 17;

    if (gdsHash == 0)
      result += result * 37 + gdss.getGDS().hashCode(); // the horizontal grid
    else
      result += result * 37 + gdsHash;

    result += result * 37 + gr.getDiscipline();
    result += result * 37 + pds2.getLevelType1();
    if (Grib2Utils.isLayer(gr)) result += result * 37 + 1;

    result += result * 37 + pds2.getParameterCategory();
    result += result * 37 + pds2.getTemplateNumber();

    if (pds2.isTimeInterval()) {
      if (!intvMerge) {
        double size = 0;
        try {
          size = cust.getForecastTimeIntervalSizeInHours(gr); // LOOK using an Hour here, but will need to make this configurable
        } catch (Throwable t) {
          logger.error("bad", t);
          if (files != null)
            logger.error("Failed on file = "+files.get(gr.getFile()));
        }
        result += result * (int) (37 + (1000 * size)); // create new variable for each interval size - default not
      }
      result += result * 37 + pds2.getStatisticalProcessType(); // create new variable for each stat type
    }

    if (pds2.isSpatialInterval()) {
       result += result * 37 + pds2.getStatisticalProcessType(); // template 15
     }

     result += result * 37 + pds2.getParameterNumber();

    int ensDerivedType = -1;
    if (pds2.isEnsembleDerived()) {  // a derived ensemble must have a derivedForecastType
      Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds2;
      ensDerivedType = pdsDerived.getDerivedForecastType(); // derived type (table 4.7)
      result += result * 37 + ensDerivedType;

    } else if (pds2.isEnsemble()) {
      result += result * 37 + 1;
    }

    // each probability interval generates a separate variable; could be a dimension instead
    int probType = -1;
    if (pds2.isProbability()) {
      Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds2;
      probType = pdsProb.getProbabilityType();
      result += result * 37 + pdsProb.getProbabilityHashcode();
    }

    // if this uses any local tables, then we have to add the center id, and subcenter if present
    if ((pds2.getParameterCategory() > 191) || (pds2.getParameterNumber() > 191) || (pds2.getLevelType1() > 191)
            || (pds2.isTimeInterval() && pds2.getStatisticalProcessType() > 191)
            || (ensDerivedType > 191) || (probType > 191)) {
      Grib2SectionIdentification id = gr.getId();
      result += result * 37 + id.getCenter_id();
      if (id.getSubcenter_id() > 0)
        result += result * 37 + id.getSubcenter_id();
    }

    // only use the GenProcessType when "error" 2/8/2012 LOOK WTF ??
    int genType = pds2.getGenProcessType();
    if (useGenType || (genType == 6 || genType == 7)) {
      result += result * 37 + genType;
    }

    return result;
  }

  public String getTimeIntervalName(int timeIdx) {
    TimeCoord tc = timeCoords.get(timeIdx);
    return tc.getTimeIntervalName();
  }

}