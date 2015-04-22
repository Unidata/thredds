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
package ucar.nc2.dt.radial;

import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.*;
import ucar.nc2.ft.FeatureDatasetFactory;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.units.DateRange;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.geoloc.LatLonRect;
import ucar.ma2.DataType;

import java.util.*;
import java.io.IOException;

/**
 * Make a NetcdfDataset into a RadialDatasetSweep.
 */
public abstract class AbstractRadialAdapter implements RadialDatasetSweep, FeatureDatasetFactory {
  protected NetcdfDataset netcdfDataset;
  protected String title, desc, location;
  protected Date startDate, endDate;
  protected LatLonRect boundingBox;
  protected List<VariableSimpleIF> dataVariables = new ArrayList<>();
  protected StringBuffer parseInfo = new StringBuffer();

  protected ucar.unidata.geoloc.EarthLocation origin;
  protected HashMap csHash = new HashMap();
  protected ucar.nc2.units.DateUnit dateUnits;
  protected ucar.nc2.time.CalendarDateUnit calDateUnits;
  protected FileCacheIF fileCache;

  public AbstractRadialAdapter() {
  }

  public AbstractRadialAdapter(NetcdfDataset ds) {
    this.netcdfDataset = ds;
    this.location = netcdfDataset.getLocation();

    this.title = netcdfDataset.getTitle();
    if (title == null)
      title = netcdfDataset.findAttValueIgnoreCase(null, "title", null);
    if (desc == null)
      desc = netcdfDataset.findAttValueIgnoreCase(null, "description", null);

    // look for radial data variables
    parseInfo.append("RadialDatasetAdapter look for RadialVariables\n");
    for (Variable var : ds.getVariables()) {
      addRadialVariable(ds, var);
    }
  }

  protected abstract void addRadialVariable(NetcdfDataset ds, Variable var);
  protected abstract RadialVariable makeRadialVariable(NetcdfDataset nds, VariableSimpleIF v, Variable v0);
  protected abstract void setTimeUnits() throws Exception; // reminder for subclasses to set this
  protected abstract void setEarthLocation(); // reminder for subclasses to set this
  protected abstract void setStartDate(); // reminder for subclasses to set this
  protected abstract void setEndDate(); // reminder for subclasses to set this

  public void setTitle(String title) {
    this.title = title;
  }

  public void setDescription(String desc) {
    this.desc = desc;
  }

  public void setLocationURI(String location) {
    this.location = location;
  }

  protected void removeDataVariable(String varName) {
    Iterator iter = dataVariables.iterator();
    while (iter.hasNext()) {
      VariableSimpleIF v = (VariableSimpleIF) iter.next();
      if (v.getShortName().equals(varName))
        iter.remove();
    }
  }

  // you must set EarthLocation before you call this.
  protected void setBoundingBox() {
    LatLonRect largestBB = null;
    // look through all the coord systems
    for (Object o : csHash.values()) {
      RadialCoordSys sys = (RadialCoordSys) o;
      sys.setOrigin(origin);
      LatLonRect bb = sys.getBoundingBox();
      if (largestBB == null)
        largestBB = bb;
      else if (bb != null)
        largestBB.extend(bb);
    }
    boundingBox = largestBB;
  }


  public void calcBounds() throws java.io.IOException {
    setBoundingBox();
    try {
      setTimeUnits();
    } catch (Exception e) {
      throw new IOException(e.getMessage());
    }
    setStartDate();
    setEndDate();
  }

  //////////////////////////////////////////////////////////////////////////

  public Date getStartDate() {
    return startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public LatLonRect getBoundingBox() {
    return boundingBox;
  }

  public List<VariableSimpleIF> getDataVariables() {
    return dataVariables;
  }

  public VariableSimpleIF getDataVariable(String shortName) {
    for (VariableSimpleIF s : dataVariables) {
      String ss = s.getShortName();
      if (shortName.equals(ss)) return s;
    }
    return null;
  }


  public RadialDatasetSweep.Type getCommonType() {
    return null;
  }

  public ucar.nc2.units.DateUnit getTimeUnits() {
    return dateUnits;
  }

  public ucar.nc2.time.CalendarDateUnit getCalendarDateUnit() {
    return calDateUnits;
  }

  public ucar.unidata.geoloc.EarthLocation getEarthLocation() {
    return origin;
  }


  /////////////////////////////////////////////
  // FeatureDatasetFactory

  public FeatureType[] getFeatureTypes() {
    return new FeatureType[]{FeatureType.RADIAL};
  }

  // FeatureDataset
  public FeatureType getFeatureType() {
    return FeatureType.RADIAL;
  }

  public DateRange getDateRange() {
    return new DateRange(getStartDate(), getEndDate());
  }

  public CalendarDateRange getCalendarDateRange() {
    return CalendarDateRange.of(getStartDate(), getEndDate());
  }

  public CalendarDate getCalendarDateStart() {
    return CalendarDate.of(getStartDate());
  }

  public CalendarDate getCalendarDateEnd() {
    return CalendarDate.of(getEndDate());
  }

  public void getDetailInfo(Formatter sf) {
    sf.format("%s", getDetailInfo());
  }

  public String getImplementationName() {
    return getClass().getName();
  }

  //////////////////////////////////////////////////
  //  FileCacheable

  @Override
  public synchronized void setFileCache(FileCacheIF fileCache) {
    this.fileCache = fileCache;
  }

  @Override
  public synchronized void close() throws java.io.IOException {
    if (fileCache != null) {
      if (fileCache.release(this)) return;
    }

    try {
      if (netcdfDataset != null) netcdfDataset.close();
    } finally {
      netcdfDataset = null;
    }
  }

  // release any resources like file handles
  public void release() throws IOException {
    if (netcdfDataset != null) netcdfDataset.release();
  }

  // reacquire any resources like file handles
  public void reacquire() throws IOException {
    if (netcdfDataset != null) netcdfDataset.reacquire();
  }

  @Override
  public long getLastModified() {
    return (netcdfDataset != null) ? netcdfDataset.getLastModified() : 0;
  }

  /////////////////////////////////////////////////

  public NetcdfFile getNetcdfFile() {
    return netcdfDataset;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return desc;
  }

  public String getLocationURI() {
    return location;
  }

  public String getLocation() {
    return location;
  }

  public List<Attribute> getGlobalAttributes() {
    if (netcdfDataset == null) return new ArrayList<>();
    return netcdfDataset.getGlobalAttributes();
  }

  public Attribute findGlobalAttributeIgnoreCase(String name) {
    if (netcdfDataset == null) return null;
    return netcdfDataset.findGlobalAttributeIgnoreCase(name);
  }

  public String getDetailInfo() {
    StringBuilder sbuff = new StringBuilder();

    sbuff.append(" Radar ID = " + getRadarID() + "\n");
    sbuff.append(" Radar Name = " + getRadarName() + "\n");
    sbuff.append(" Data Format Name= " + getDataFormat() + "\n");
    sbuff.append(" Common Type = " + getCommonType() + "\n");
    sbuff.append(" Common Origin = " + getCommonOrigin() + "\n");
    CalendarDateUnit dt = getCalendarDateUnit();
    if (dt != null)
      sbuff.append(" Date Unit = " + dt + "\n");
    sbuff.append(" isStationary = " + isStationary() + "\n");
    //sbuff.append(" isRadial = "+isRadial()+"\n");
    sbuff.append(" isVolume = " + isVolume() + "\n");
    sbuff.append("\n");

    sbuff.append("  location= ").append(getLocation()).append("\n");
    sbuff.append("  title= ").append(getTitle()).append("\n");
    sbuff.append("  desc= ").append(getDescription()).append("\n");
    sbuff.append("  start= ").append(CalendarDateFormatter.toDateTimeString(getStartDate())).append("\n");
    sbuff.append("  end  = ").append(CalendarDateFormatter.toDateTimeString(getEndDate())).append("\n");
    sbuff.append("  bb   = ").append(getBoundingBox()).append("\n");
    if (getBoundingBox() != null)
      sbuff.append("  bb   = ").append(getBoundingBox().toString2()).append("\n");

    sbuff.append("  has netcdf = ").append(getNetcdfFile() != null).append("\n");
    List<Attribute> ga = getGlobalAttributes();
    if (ga.size() > 0) {
      sbuff.append("  Attributes\n");
      for (Attribute a : ga) {
        sbuff.append("    ").append(a).append("\n");
      }
    }

    List<VariableSimpleIF> vars = getDataVariables();
    sbuff.append("  Variables (").append(vars.size()).append(")\n");
    for (VariableSimpleIF v : vars) {
      sbuff.append("    name='").append(v.getShortName()).append("' desc='").append(v.getDescription()).append("' units='").append(v.getUnitsString()).append("' type=").append(v.getDataType()).append("\n");
    }

    sbuff.append("\nparseInfo=\n");
    sbuff.append(parseInfo);
    sbuff.append("\n");

    return sbuff.toString();
  }

  ///////////////////////////////////////////////////////////////////////
  public class MyRadialVariableAdapter implements VariableSimpleIF {

    private int rank;
    private int[] shape;
    private String name;
    private String desp;
    private List<Attribute> attributes;

    public MyRadialVariableAdapter(String vName, List<Attribute> atts) {
      super();
      rank = 1;
      shape = new int[]{1};
      name = vName;
      desp = "A radial variable holding a list of radial sweeps";
      attributes = atts;
    }

    public String toString() {
      return name;
    }

    /**
     * Sort by name
     */
    public int compareTo(VariableSimpleIF o) {
      return getFullName().compareTo(o.getFullName());
    }

    public String getName() {
      return this.name;
    }

    public String getFullName() {
      return this.name;
    }

    public String getShortName() {
      return this.name;
    }

    public DataType getDataType() {
      return DataType.FLOAT;
    }

    public String getDescription() {
      return this.desp;
    }

    public String getInfo() {
      return this.desp;
    }

    public String getUnitsString() {
      return "N/A";
    }

    public int getRank() {
      return this.rank;
    }

    public int[] getShape() {
      return this.shape;
    }

    public List<Dimension> getDimensions() {
      return null;
    }

    public List<Attribute> getAttributes() {
      return attributes;
    }

    public ucar.nc2.Attribute findAttributeIgnoreCase(String attName) {
      Iterator it = attributes.iterator();
      Attribute at = null;
      while (it.hasNext()) {
        at = (Attribute) it.next();
        if (attName.equalsIgnoreCase(at.getShortName()))
          break;
      }
      return at;
    }
  }


}
