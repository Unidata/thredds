// $Id:RadialDatasetSweepAdapter.java 51 2006-07-12 17:13:13Z caron $
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
package ucar.nc2.dt.radial;

import ucar.nc2.dataset.*;
import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.Dimension;
import ucar.nc2.Attribute;
import ucar.unidata.geoloc.LatLonRect;
import ucar.ma2.DataType;

import java.util.*;

/**
 * Make a NetcdfDataset into a RadialDatasetSweep.
 *
 * @author yuan
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */

public abstract class RadialDatasetSweepAdapter extends TypedDatasetImpl implements RadialDatasetSweep {
  protected EarthLocation origin;
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

  protected abstract void setTimeUnits(); // reminder for subclasses to set this

  public String getDetailInfo() {
    StringBuffer sbuff = new StringBuffer();

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

  public ucar.nc2.dt.EarthLocation getEarthLocation() { return origin; }

  // you must set EarthLocation before you call this.
  protected void setBoundingBox() {
    LatLonRect largestBB = null;
    // look through all the coord systems
    Iterator iter = csHash.values().iterator();
    while (iter.hasNext()) {
      RadialCoordSys sys = (RadialCoordSys) iter.next();
      sys.setOrigin( origin);
      LatLonRect bb = sys.getBoundingBox();
      if (largestBB == null)
        largestBB = bb;
      else
        largestBB.extend( bb);
    }
    boundingBox = largestBB;
  }

  public class MyRadialVariableAdapter implements VariableSimpleIF {

    private int rank;
    private int[] shape;
    private String name;
    private String desp;
    private List attributes;

    public MyRadialVariableAdapter( String vName, List atts )
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
}
