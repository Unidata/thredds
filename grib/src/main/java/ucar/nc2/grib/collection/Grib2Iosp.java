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

package ucar.nc2.grib.collection;

import ucar.nc2.constants.DataFormatType;
import ucar.nc2.grib.grib2.*;
import ucar.nc2.grib.*;
import ucar.nc2.grib.grib2.table.Grib2Customizer;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.http.HTTPRandomAccessFile;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.Formatter;

/**
 * Grib-2 Collection IOSP, ver2.
 * Handles both collections and single GRIB files.
 *
 * @author caron
 * @since 4/6/11
 */
public class Grib2Iosp extends GribIosp {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2Iosp.class);

  static public String makeVariableNameFromTable(Grib2Customizer cust, GribCollectionImmutable gribCollection,
                                                 GribCollectionImmutable.VariableIndex vindex, boolean useGenType) {
    Formatter f = new Formatter();

    GribTables.Parameter param = cust.getParameter(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());

    if (param == null) {
      f.format("VAR%d-%d-%d_FROM_%d-%d-%d", vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter(), gribCollection.getCenter(), 
              gribCollection.getSubcenter(), vindex.getTableVersion());
    } else {
      f.format("%s", GribUtils.makeNameFromDescription(param.getName()));
    }

    if (vindex.getGenProcessType() == 6 || vindex.getGenProcessType() == 7) {
      f.format("_error");  // its an "error" type variable - add to name

    } else if (useGenType && vindex.getGenProcessType() >= 0) {
        String genType = cust.getGeneratingProcessTypeName(vindex.getGenProcessType());
        String s = StringUtil2.substitute(genType, " ", "_");
        f.format("_%s", s);
    }

    if (vindex.getLevelType() != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format("_%s", cust.getLevelNameShort(vindex.getLevelType())); // vindex.getLevelType()); // code table 4.5
      if (vindex.isLayer()) f.format("_layer");
    }

    String intvName = vindex.getIntvName();
    if (intvName != null && !intvName.isEmpty()) {
      f.format("_%s", intvName);
    }

    if (vindex.getIntvType() >= 0) {
      String statName = cust.getStatisticNameShort(vindex.getIntvType());
      if (statName != null) f.format("_%s", statName);
    }

    if (vindex.getSpatialStatisticalProcessType() >= 0) {
      String statName = cust.getTableValue("4.10", vindex.getSpatialStatisticalProcessType());
      if (statName != null) f.format("_%s", statName);
    }

    if (vindex.getEnsDerivedType() >= 0) {
      f.format("_%s", cust.getProbabilityNameShort(vindex.getEnsDerivedType()));
    } else if (vindex.getProbabilityName() != null && vindex.getProbabilityName().length() > 0) {
      String s = StringUtil2.substitute(vindex.getProbabilityName(), ".", "p");
      f.format("_probability_%s", s);
    } else if (vindex.isEnsemble()) {
      f.format("_ens");
    }

    return f.toString();
  }

  static public String makeVariableLongName(Grib2Customizer cust, GribCollectionImmutable.VariableIndex vindex, boolean useGenType) {
    Formatter f = new Formatter();

    boolean isProb = (vindex.getProbabilityName() != null && vindex.getProbabilityName().length() > 0);
    if (isProb)
      f.format("Probability ");

    GribTables.Parameter gp = cust.getParameter(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
    if (gp == null)
      f.format("Unknown Parameter %d-%d-%d", vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
    else
      f.format("%s", gp.getName());

    if (vindex.getIntvType() >= 0 && vindex.getIntvName() != null && !vindex.getIntvName().isEmpty()) {
      String intvName = cust.getStatisticNameShort(vindex.getIntvType());
      if (intvName == null || intvName.equalsIgnoreCase("Missing")) intvName = cust.getStatisticNameShort(vindex.getIntvType());
      if (intvName == null) f.format(" (%s)", vindex.getIntvName());
      else f.format(" (%s %s)", vindex.getIntvName(), intvName);

    } else if (vindex.getIntvType() >= 0) {
      String intvName = cust.getStatisticNameShort(vindex.getIntvType());
      f.format(" (%s)", intvName);
    }

    if (vindex.getSpatialStatisticalProcessType() >= 0) {
      String statName = cust.getTableValue("4.10", vindex.getSpatialStatisticalProcessType());
      if (statName != null) f.format("_%s", statName);
    }

    if (vindex.getEnsDerivedType() >= 0)
      f.format(" (%s)", cust.getTableValue("4.10", vindex.getEnsDerivedType()));

    else if (isProb)
      f.format(" %s %s", vindex.getProbabilityName(), getVindexUnits(cust, vindex)); // add data units here

    if (vindex.getGenProcessType() == 6 || vindex.getGenProcessType() == 7) {
      f.format(" error");  // its an "error" type variable - add to name

    } else if (useGenType && vindex.getGenProcessType() >= 0) {
      f.format(" %s", cust.getGeneratingProcessTypeName(vindex.getGenProcessType()));
    }

    if (vindex.getLevelType() != GribNumbers.UNDEFINED) { // satellite data doesnt have a level
      f.format(" @ %s", cust.getTableValue("4.5", vindex.getLevelType()));
      if (vindex.isLayer()) f.format(" layer");
    }

    return f.toString();
  }

  @Override
  protected String makeVariableName(GribCollectionImmutable.VariableIndex vindex) {
    return makeVariableNameFromTable(cust, gribCollection, vindex, gribCollection.config.gribConfig.useGenType);
  }

  @Override
  protected String makeVariableLongName(GribCollectionImmutable.VariableIndex vindex) {
    return makeVariableLongName(cust, vindex, gribCollection.config.gribConfig.useGenType);
  }

  @Override
  protected String makeVariableUnits(GribCollectionImmutable.VariableIndex vindex) {
    return makeVariableUnits(cust, vindex);
  }

  static public String makeVariableUnits(Grib2Customizer tables, GribCollectionImmutable.VariableIndex vindex) {
    if (vindex.getProbabilityName() != null && vindex.getProbabilityName().length() > 0) return "%";
    return getVindexUnits(tables, vindex);
  }

  static private String getVindexUnits(Grib2Customizer tables, GribCollectionImmutable.VariableIndex vindex) {
    GribTables.Parameter gp = tables.getParameter(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
    String val = (gp == null) ? "" : gp.getUnit();
    return (val == null) ? "" : val;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private Grib2Customizer cust;

  // accept grib2 or ncx files
  @Override
  public boolean isValidFile(RandomAccessFile raf) throws IOException {
    if (raf instanceof HTTPRandomAccessFile) { // only do remote if memory resident
      if (raf.length() > raf.getBufferSize())
        return false;

    } else {                                  // wont accept remote index
      GribCdmIndex.GribCollectionType type = GribCdmIndex.getType(raf);
      if (type == GribCdmIndex.GribCollectionType.GRIB2) return true;
      if (type == GribCdmIndex.GribCollectionType.Partition2) return true;
    }

    // check for GRIB2 data file
    return Grib2RecordScanner.isValidFile(raf);
  }

  @Override
  public String getFileTypeId() {
    return DataFormatType.GRIB2.getDescription();
  }

  @Override
  public String getFileTypeDescription() {
    return "GRIB2 Collection";
  }

  // public no-arg constructor for reflection
  public Grib2Iosp() {
    super(false, logger);
  }

  public Grib2Iosp(GribCollectionImmutable.GroupGC gHcs, GribCollectionImmutable.Type gtype) {
    super(false, logger);
    this.gHcs = gHcs;
    this.owned = true;
    this.gtype = gtype;
  }

  // LOOK more likely we will set an individual dataset
  public Grib2Iosp(GribCollectionImmutable gc) {
    super(false, logger);
    this.gribCollection = gc;
    this.owned = true;
  }

  @Override
  protected ucar.nc2.grib.GribTables createCustomizer() {
    cust = Grib2Customizer.factory(gribCollection.getCenter(), gribCollection.getSubcenter(), gribCollection.getMaster(), gribCollection.getLocal(),
            gribCollection.getGenProcessId());
    return cust;
  }

  @Override
  protected String getVerticalCoordDesc(int vc_code) {
    return cust.getTableValue("4.5", vc_code);
  }

  @Override
  protected GribTables.Parameter getParameter(GribCollectionImmutable.VariableIndex vindex) {
    return cust.getParameter(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
  }

  public Object getLastRecordRead() {
    return Grib2Record.lastRecordRead;
  }

  public void clearLastRecordRead() {
    Grib2Record.lastRecordRead = null;
  }

  public Object getGribCustomizer() {
    return cust;
  }
}
