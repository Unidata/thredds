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

package ucar.nc2.grib.collection;

import thredds.inventory.MFile;
import ucar.sparr.*;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.nc2.time.CalendarPeriod;

import java.io.IOException;
import java.util.*;

/**
 * Turn a collection of Grib2Records into a rectangular array.
 * Do seperately for each Group
 * Helper class for Grib2CollectionBuilder.
 *
 * @author caron
 * @since 11/26/2013
 */
public class Grib2Rectilyser {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2CollectionBuilder.class);

  private final Grib2Customizer cust;
  private final int gdsHash;

  private final boolean intvMerge;
  private final boolean useGenType;

  private final List<Grib2Record> records;
  private List<VariableBag> gribvars;
  private List<Coordinate> coords;

  public Grib2Rectilyser(Grib2Customizer cust, List<Grib2Record> records, int gdsHash, Map<String, Boolean> pdsConfig) {
    this.cust = cust;
    this.records = records;
    this.gdsHash = gdsHash;

    intvMerge = assignValue(pdsConfig, "intvMerge", true);
    // useTableVersion = assignValue(pdsConfig, "useTableVersion", true);
    useGenType = assignValue(pdsConfig, "useGenType", false);
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

  public List<Coordinate> getCoordinates() {
    return coords;
  }

  List<MFile> files = null; // temp debug
  public void make(Counter counter, List<MFile> files, Formatter info) throws IOException {
    this.files = files;

    // assign each record to unique variable using cdmVariableHash()
    Map<Integer, VariableBag> vbHash = new HashMap<>(100);
    for (Grib2Record gr : records) {
      int cdmHash = cdmVariableHash(gr, gdsHash);
      VariableBag bag = vbHash.get(cdmHash);
      if (bag == null) {
        bag = new VariableBag(gr, cdmHash);
        vbHash.put(cdmHash, bag);
      }
      bag.atomList.add(gr);
    }
    gribvars = new ArrayList<>(vbHash.values());
    Collections.sort(gribvars); // make it deterministic by sorting

    // create coordinates for each variable
    for (VariableBag vb : gribvars) {
      Grib2Pds pdsFirst = vb.first.getPDS();
      int unit = cust.convertTimeUnit(pdsFirst.getTimeUnit());
      vb.timeUnit = Grib2Utils.getCalendarPeriod(unit);
      vb.coordND = new CoordinateND<>();
      vb.coordND.addBuilder(new CoordinateRuntime.Builder());

      if (vb.first.getPDS().isTimeInterval())
        vb.coordND.addBuilder(new CoordinateTimeIntv.Builder(cust, vb.timeUnit, unit));
      else
        vb.coordND.addBuilder(new CoordinateTime.Builder(pdsFirst.getTimeUnit()));

      VertCoord.VertUnit vertUnit = Grib2Utils.getLevelUnit(pdsFirst.getLevelType1());
      if (vertUnit.isVerticalCoordinate())
        vb.coordND.addBuilder(new CoordinateVert.Builder(pdsFirst.getLevelType1()));

      for (Grib2Record gr : vb.atomList)
        vb.coordND.addRecord(gr);

      vb.coordND.finish(vb.atomList, info);
    }

    // make shared coordinates
    CoordinateUniquify uniquify = new CoordinateUniquify();
    for (VariableBag vb : gribvars) {
      uniquify.addCoords(vb.coordND.getCoordinates());
    }
    uniquify.finish();
    this.coords = uniquify.getUnionCoords();

    int tot_used = 0;
    int tot_dups = 0;

    // redo the variables against the shared coordinates (at the moment this is just possibly runtime)
    for (VariableBag vb : gribvars) {
      vb.coordIndex = new ArrayList<>();
      vb.coordND = uniquify.reindex(vb.coordND, vb.coordIndex);
      tot_used += vb.coordND.getSparseArray().countNotMissing();
      tot_dups += vb.coordND.getSparseArray().getNduplicates();
     }

    counter.recordsUnique += tot_used;
    counter.dups += tot_dups;
    counter.vars += gribvars.size();
  }

  public void showInfo(Formatter f, Grib2Customizer tables) {
    //f.format("%nVariables%n");
    //f.format("%n  %3s %3s %3s%n", "time", "vert", "ens");
    Counter all = new Counter();

    for (VariableBag vb : gribvars) {
      f.format("Variable %s (%d)%n", tables.getVariableName(vb.first), vb.cdmHash);
      vb.coordND.showInfo(f, all);
      //f.format("  %3d %3d %3d %s records = %d density = %f hash=%d", vb.timeCoordIndex, vb.vertCoordIndex, vb.ensCoordIndex,
      //        vname, vb.atomList.size(), vb.recordMap.density(), vb.cdmHash);
      f.format("%n");
    }
    f.format("%n all= %s", all.show());
  }

  public class VariableBag implements Comparable<VariableBag> {
    public Grib2Record first;
    public int cdmHash;

    public List<Grib2Record> atomList = new ArrayList<>(100); // not sorted
    public CoordinateND<Grib2Record> coordND;
    CalendarPeriod timeUnit;

    public List<Integer> coordIndex;
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
    if (Grib2Utils.isLayer(pds2)) result += result * 37 + 1;

    result += result * 37 + pds2.getParameterCategory();
    result += result * 37 + pds2.getTemplateNumber();

    if (pds2.isTimeInterval()) {
      if (!intvMerge) {
        double size = 0;
        try {
          size = cust.getForecastTimeIntervalSizeInHours(pds2); // LOOK using an Hour here, but will need to make this configurable
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

  /* public String getTimeIntervalName(int timeIdx) {
    TimeCoord tc = timeCoords.get(timeIdx);
    return tc.getTimeIntervalName();
  } */

}