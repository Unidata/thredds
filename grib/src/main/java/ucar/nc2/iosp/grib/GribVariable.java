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

package ucar.nc2.iosp.grib;

import ucar.grib.GribGridRecord;
import ucar.grib.grib1.Grib1Tables;
import ucar.ma2.Array;

import ucar.nc2.*;
import ucar.nc2.constants.CF;
import ucar.nc2.iosp.grid.*;
import ucar.unidata.io.RandomAccessFile;
import ucar.grib.grib1.Grib1Data;
import ucar.grib.grib2.Grib2Data;
import ucar.grib.grib2.Grib2Pds;
import ucar.grib.grib1.Grib1GridTableLookup;
import ucar.grib.grib2.Grib2GridTableLookup;
import ucar.grib.grib2.Grib2Tables;

import java.io.IOException;
import java.util.Date;
import java.util.Formatter;


/**
 * A Variable for a Grid dataset.
 *
 * @author caron
 */
public class GribVariable extends GridVariable {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GribVariable.class);

  static private boolean compareData = false;
  static private boolean warnOk = true;
  static private boolean sendAll = false; // if false, just send once per variable

  GribVariable(String filename, String name, GridHorizCoordSys hcs, GridTableLookup lookup) {
    super(filename, name, hcs, lookup);
  }

  @Override
  protected boolean isEnsemble() {
    if (firstRecord instanceof GribGridRecord) {
      GribGridRecord ggr = (GribGridRecord) firstRecord;
      return ggr.getPds().isEnsemble();
    }
    return false;
  }

  @Override
  protected boolean trackRecords(int time, int level, GridRecord p, RandomAccessFile raf, boolean oneSent) {
    boolean sentMessage = false;
    int recno = calcRecno(time, level, p, raf.getLocation());
    GribGridRecord ggp = (GribGridRecord) p;

    if (ggp.getBelongs() != null) {
      log.warn("GribGridRecord " + ggp.cdmVariableName(lookup, true, true) + " recno = " + recno + " already belongs to = " + ggp.getBelongs());
    }
    ggp.setBelongs(new Belongs(recno, this));

    if (recordTracker[recno] != null) {
      GribGridRecord ggq = (GribGridRecord) recordTracker[recno];
      if (compareData) {
        if (!compareData(ggq, ggp, raf)) {
          log.warn("GridVariable " + vname + " recno = " + recno + " already has in slot = " + ggq.toString() +
                  " with different data for " + raf.getLocation());
          sentMessage = true;
        }
      }
      }

    if (recordTracker[recno] == null) {
      recordTracker[recno] = p;
      if (log.isDebugEnabled())
        log.debug(" " + vc.getVariableName() + " (type=" + p.getLevelType1() + "," + p.getLevelType2() + ")  value="
                + p.getLevel1() + "," + p.getLevel2());

    } else { // already one in that slot
      if (!sentMessage && warnOk && !oneSent) {
        GribGridRecord qp = (GribGridRecord) recordTracker[recno];
        log.warn("Duplicate record for " + raf.getLocation() + "\n " + ggp.toString() + "\n " + qp.toString());

        /* GribGridRecord p2 = (GribGridRecord) recordTracker[recno];
        Date validTime1 = p.getValidTime();
        Date validTime2 = p2.getValidTime();

        int r2 = calcRecno(tcs.findIndex(p), getVertIndex(p), recordTracker[recno], raf.getLocation());
        int r1 = calcRecno(time, level, p, raf.getLocation());
        System.out.printf("HEY%n");*/
      }
      if ((!sendAll)) oneSent = true;
      recordTracker[recno] = p;  // replace it with latest one
    }

    return oneSent;
  }

  private int calcRecno(int time, int level, GridRecord p, String filename) {
    int recno = time * nlevels + level;

    if (ecs != null) {
      GribEnsembleCoord ecsb = (GribEnsembleCoord) ecs;

      GribGridRecord ggr = (GribGridRecord) p;
      int ens = ecsb.getIndex(ggr);
      if (ens < 0) {
        int ensNumber = ggr.getPds().getPerturbationNumber();
        int ensType = ggr.getPds().getPerturbationType();

        log.warn("ENS NOT FOUND record; level=" + level + " time= " + time +
                " for " + getName() + " file=" + filename +
                "\n ensNumber= " + ensNumber + " ensType= " + ensType + "\n");

        ecsb.getIndex(ggr); // allow breakpoint
        return recno; // LOOK
        // continue; //  used to skip it ??!!
      }
      recno = ens * (ntimes * nlevels) + (time * nlevels) + level;  // order is ens, time, level
      if (recno < 0) {
        System.out.println("HEY bad recno");
        ecsb.getIndex(ggr);
      }
    }

    return recno;
  }

  public class Belongs {
    public int recnum;
    public GridVariable gv;

    private Belongs(int recnum, GridVariable gv) {
      this.recnum = recnum;
      this.gv = gv;
    }

    @Override
    public String toString() {
      return "Belongs{" +
              "recnum=" + recnum +
              ", gv=" + gv.vname +
              '}';
    }
  }


  private boolean compareData(GribGridRecord ggr1, GribGridRecord ggr2, RandomAccessFile raf) {
    if (raf == null) return false;

    float[] data1 = null, data2 = null;
    try {
      if (ggr1.getEdition() == 2) {
        Grib2Data g2read = new Grib2Data(raf);
        data1 = g2read.getData(ggr1.getGdsOffset(), ggr1.getPdsOffset(), ggr1.getReferenceTimeInMsecs());
        data2 = g2read.getData(ggr2.getGdsOffset(), ggr2.getPdsOffset(), ggr2.getReferenceTimeInMsecs());
      } else {
        Grib1Data g1read = new Grib1Data(raf);
        data1 = g1read.getData(ggr1.getGdsOffset(), ggr1.getPdsOffset(), ggr1.getDecimalScale(), ggr1.isBmsExists());
        data2 = g1read.getData(ggr2.getGdsOffset(), ggr2.getPdsOffset(), ggr2.getDecimalScale(), ggr2.isBmsExists());
      }
    } catch (IOException e) {
      log.error("Failed to read data", e);
      return false;
    }

    if (data1.length != data2.length)
      return false;

    for (int i = 0; i < data1.length; i++) {
      if (data1[i] != data2[i] && !Double.isNaN(data1[i]) && !Double.isNaN(data2[i]))
        return false;
    }
    return true;
  }


  @Override
  protected void addExtraAttributes(GridParameter param, Variable v) {
    super.addExtraAttributes(param, v);

    if (firstRecord instanceof GribGridRecord) {
      GribGridRecord ggr = (GribGridRecord) firstRecord;
      if (ggr.isInterval()) {
        CF.CellMethods cm = CF.CellMethods.convertGribCodeTable4_10(ggr.getStatisticalProcessType());
        if (cm != null)
          v.addAttribute(new Attribute("cell_methods", tcs.getName() + ": " + cm.toString()));
      }
    }

    if (lookup instanceof Grib2GridTableLookup) {
      Grib2GridTableLookup g2lookup = (Grib2GridTableLookup) lookup;
      GribGridRecord ggr = (GribGridRecord) firstRecord;
      Grib2Pds pds2 = (Grib2Pds) ggr.getPds();

      int[] paramId = g2lookup.getParameterId(firstRecord);
      v.addAttribute(new Attribute("GRIB_param_discipline", lookup.getDisciplineName(firstRecord)));
      v.addAttribute(new Attribute("GRIB_param_category", lookup.getCategoryName(firstRecord)));
      v.addAttribute(new Attribute("GRIB_param_name", param.getName()));
      v.addAttribute(new Attribute("GRIB_generating_process_type", g2lookup.getGenProcessTypeName(firstRecord)));
      v.addAttribute(new Attribute("GRIB_param_id", Array.factory(int.class, new int[]{paramId.length}, paramId)));
      v.addAttribute(new Attribute("GRIB_product_definition_template", pds2.getProductDefinitionTemplate()));
      v.addAttribute(new Attribute("GRIB_product_definition_template_desc", Grib2Tables.codeTable4_0(pds2.getProductDefinitionTemplate())));
      v.addAttribute(new Attribute("GRIB_level_type", new Integer(pds2.getLevelType1())));
      v.addAttribute(new Attribute("GRIB_level_type_name", lookup.getLevelName(firstRecord)));
      if (pds2.isInterval())
        v.addAttribute(new Attribute("GRIB_interval_stat_type", ggr.getStatisticalProcessTypeName()));
      if (pds2.isEnsembleDerived()) {
        Grib2Pds.PdsEnsembleDerived pdsDerived = (Grib2Pds.PdsEnsembleDerived) pds2;
        v.addAttribute(new Attribute("GRIB_ensemble_derived_type", new Integer(pdsDerived.getDerivedForecastType())));
      }
      if (pds2.isEnsemble())
        v.addAttribute(new Attribute("GRIB_ensemble", "true"));
      if (pds2.isProbability()) {
        Grib2Pds.PdsProbability pdsProb = (Grib2Pds.PdsProbability) pds2;
        v.addAttribute(new Attribute("GRIB_probability_type", new Integer(pdsProb.getProbabilityType())));
        v.addAttribute(new Attribute("GRIB_probability_lower_limit", new Double(pdsProb.getProbabilityLowerLimit())));
        v.addAttribute(new Attribute("GRIB_probability_upper_limit", new Double(pdsProb.getProbabilityUpperLimit())));
      }

    } else if (lookup instanceof Grib1GridTableLookup) {
      //Grib1GridTableLookup g1lookup = (Grib1GridTableLookup) lookup;
      //int[] paramId = g1lookup.getParameterId(firstRecord);
      GribGridRecord ggr = (GribGridRecord) firstRecord;

      v.addAttribute(new Attribute("GRIB_param_name", param.getDescription()));
      if (param.getName() != null) v.addAttribute(new Attribute("GRIB_param_short_name", param.getName()));
      v.addAttribute(new Attribute("GRIB_center_id", ggr.getCenter()));
      v.addAttribute(new Attribute("GRIB_table_version", ggr.getTableVersion()));
      v.addAttribute(new Attribute("GRIB_param_number", ggr.getParameterNumber()));
      v.addAttribute(new Attribute("GRIB_level_type", firstRecord.getLevelType1()));
      //v.addAttribute(new Attribute("GRIB_param_id", Array.factory(int.class, new int[]{paramId.length}, paramId)));
      v.addAttribute(new Attribute("GRIB_time_range_indicator", Grib1Tables.getTimeRangeIndicatorName(ggr.getTimeUnit())));

    }
  }

  @Override
  protected String makeLongName() {

    Formatter f = new Formatter();
    GridParameter param = lookup.getParameter(firstRecord);
    f.format("%s", param.getDescription());

    if (firstRecord instanceof GribGridRecord) {
      GribGridRecord ggr = (GribGridRecord) firstRecord;

      if (ggr.getEdition() == 2) {
        Grib2Pds pds2 = (Grib2Pds) ggr.getPds();
        String useGenType = pds2.getUseGenProcessType();
        if (useGenType != null)
          f.format("_%s", useGenType);
      }

      String suffixName = ggr.makeSuffix();
      if (suffixName != null && suffixName.length() != 0)
        f.format("%s", suffixName);

      if (ggr.isInterval()) {
        String intervalName = makeIntervalName();
        if (intervalName.length() != 0) {
          String stat = ggr.getStatisticalProcessTypeNameShort();
          if (stat != null)
            f.format(" (%s for %s)", ggr.getStatisticalProcessTypeName(), intervalName);
          else
            f.format(" (%s)", intervalName);
        }
      }
    }

    String levelName = makeLevelName(firstRecord, lookup);
    if (levelName.length() != 0)
      f.format(" @ %s", levelName);

    return f.toString();
  }

  @Override
  public String makeLevelName(GridRecord gr, GridTableLookup lookup) {
    // for grib2, we need to add the layer to disambiguate
    if ( lookup instanceof Grib2GridTableLookup ) {
      String vname = lookup.getLevelName(gr);
      return lookup.isLayer(gr) ? vname + "_layer" : vname;
    } else {
      return super.makeLevelName(gr, lookup);
    }
  }
}

