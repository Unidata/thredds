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

import ucar.nc2.dataset.*;
import ucar.nc2.dt.*;
import ucar.nc2.units.DateRange;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.Dimension;
import ucar.nc2.Attribute;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonRect;
import ucar.ma2.DataType;

import java.util.*;
import java.io.IOException;

/**
 * Make a NetcdfDataset into a RadialDatasetSweep.
 *
 * @author yuan
 */

public abstract class RadialDatasetSweepAdapter extends TypedDatasetImpl implements RadialDatasetSweep {
  protected ucar.unidata.geoloc.EarthLocation origin;
  protected HashMap csHash = new HashMap();
  protected ucar.nc2.units.DateUnit dateUnits;

  public RadialDatasetSweepAdapter() {}
  
  public RadialDatasetSweepAdapter( NetcdfDataset ds) {
    super(ds);

    // look for radial data variables
    parseInfo.append("RadialDatasetAdapter look for RadialVariables\n");
    List vars  = ds.getVariables();
    for (int i=0; i< vars.size(); i++) {
     // VariableEnhanced varDS = (VariableEnhanced) vars.get(i);
      addRadialVariable( ds, (Variable) vars.get(i));
    }
  }

  protected abstract void addRadialVariable(NetcdfDataset ds, Variable var);

  protected abstract RadialVariable makeRadialVariable(NetcdfDataset nds, VariableSimpleIF v, Variable v0);

  protected abstract void setTimeUnits() throws Exception; // reminder for subclasses to set this

  public String getDetailInfo() {
    StringBuilder sbuff = new StringBuilder();

    sbuff.append(" Radar ID = "+getRadarID()+"\n");
    sbuff.append(" Radar Name = "+getRadarName()+"\n");
    sbuff.append(" Data Format Name= "+getDataFormat()+"\n");
    sbuff.append(" Common Type = "+getCommonType()+"\n");
    sbuff.append(" Common Origin = "+getCommonOrigin()+"\n");
    sbuff.append(" Date Unit = "+getTimeUnits().getUnitsString()+"\n");
    sbuff.append(" isStationary = "+isStationary()+"\n");
    //sbuff.append(" isRadial = "+isRadial()+"\n");
    sbuff.append(" isVolume = "+isVolume()+"\n");
    sbuff.append("\n");
    sbuff.append(super.getDetailInfo());

    return sbuff.toString();
  }

  protected abstract void setEarthLocation(); // reminder for subclasses to set this

  public RadialDatasetSweep.Type getCommonType() {
      return null;
  }

  public ucar.nc2.units.DateUnit getTimeUnits() { return dateUnits; }

  public ucar.unidata.geoloc.EarthLocation getEarthLocation() { return origin; }

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
      else
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


  public class MyRadialVariableAdapter implements VariableSimpleIF {

    private int rank;
    private int[] shape;
    private String name;
    private String desp;
    private List<Attribute> attributes;

    public MyRadialVariableAdapter( String vName, List<Attribute> atts )
    {
      super();
      rank = 1;
      shape= new int[] {1};
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
     return getName().compareTo(o.getName());
    }
    public String getName() { return this.name; }
    public String getShortName() { return this.name; }
    public DataType getDataType() { return DataType.FLOAT; }
    public String getDescription() { return this.desp; }
    public String getInfo() { return this.desp; }
    public String getUnitsString() { return "N/A"; }

    public int getRank() {  return this.rank; }
    public int[] getShape() { return this.shape; }
    public List<Dimension> getDimensions() { return null; }
    public List<Attribute> getAttributes() { return attributes; }
    public ucar.nc2.Attribute findAttributeIgnoreCase(String attName){
        Iterator it = attributes.iterator();
        Attribute at = null;
        while(it.hasNext()){
           at = (Attribute)it.next();
           if(attName.equalsIgnoreCase(at.getName()))
              break;
        }
        return at;
    }
  }

  /////////////////////////////////////////////
  // FeatureDataset

  public FeatureType getFeatureType() {
    return FeatureType.RADIAL;
  }

  public DateRange getDateRange() {
    return new DateRange(getStartDate(), getEndDate());
  }

  public void getDetailInfo(Formatter sf) {
    sf.format("%s", getDetailInfo());
  }

  public String getImplementationName() {
    return getClass().getName();
  }

  //////////////////////////////////////////////////
  //  FileCacheable

  public synchronized void close() throws java.io.IOException {
    if (fileCache != null) {
      fileCache.release(this);
    } else {
      try {
        if (ncfile != null) ncfile.close();
      } finally {
        ncfile = null;
      }
    }
  }

  public boolean sync() throws IOException {
    return false;
  }

  protected FileCache fileCache;
  public void setFileCache(FileCache fileCache) {
    this.fileCache = fileCache;
  }

}
