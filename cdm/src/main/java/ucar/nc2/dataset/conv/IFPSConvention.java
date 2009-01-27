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

package ucar.nc2.dataset.conv;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.*;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.Projection;

import java.io.IOException;
import java.util.*;

/**
 * IFPS Convention Allows Local NWS forecast office generated forecast datasets to be brought into IDV.
 * @author Burks
 */

public class IFPSConvention extends CoordSysBuilder {

  /**
   * @param ncfile the NetcdfFile to test
   * @return true if we think this is a IFPSConvention file.
   */
  public static boolean isMine( NetcdfFile ncfile) {
    Variable v = ncfile.findVariable("latitude");

    return (null != ncfile.findDimension("DIM_0")) && (null != ncfile.findVariable("longitude"))
            && (null != v) && (null != ncfile.findAttValueIgnoreCase(v, "projectionType", null));
  }

  private Variable projVar = null; // use this to get projection info

  public IFPSConvention() {
    this.conventionName = "IFPS";
  }

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    if (null != ds.findVariable("xCoord")) return; // check if its already been done - aggregating enhanced datasets.

    parseInfo.format("IFPS augmentDataset \n");

   // Figure out projection info. Assume the same for all variables
    Variable lonVar = ds.findVariable("longitude");
    lonVar.addAttribute( new Attribute("units", "degrees_east"));
    lonVar.addAttribute( new Attribute(_Coordinate.AxisType, "Lon"));
    Variable latVar = ds.findVariable("latitude");
    latVar.addAttribute( new Attribute(_Coordinate.AxisType, "Lat"));
    latVar.addAttribute( new Attribute("units", "degrees_north"));

    projVar = latVar;
    String projName = ds.findAttValueIgnoreCase(projVar, "projectionType", null);
    if (projName.equals("LAMBERT_CONFORMAL")) {
      Projection proj = makeLCProjection( ds);
      makeXYcoords( ds, proj, latVar, lonVar);
    }

    // figure out the time coordinate for each data variable
    // LOOK : always seperate; could try to discover if they are the same
    List<Variable> vars = ds.getVariables();
    for (Variable ncvar : vars) {
        //variables that are used but not displayable or have no data have DIM_0, also don't want history, since those are just how the person edited the grids
        if ((!ncvar.getDimension(0).getName().equals("DIM_0")) && !ncvar.getName().endsWith("History")
              && (ncvar.getRank() > 2) && !ncvar.getName().startsWith("Tool")) {
            createTimeCoordinate(ds, ncvar);
        } else if (ncvar.getName().equals("Topo")){
            //Deal with Topography variable
            ncvar.addAttribute(new Attribute("long_name", "Topography"));
            ncvar.addAttribute(new Attribute("units", "ft"));
        }
    }

    ds.finish();
  }

  private void createTimeCoordinate(NetcdfDataset ds,Variable ncVar){
    //Time coordinate is stored in the attribute validTimes
    //One caveat is that the times have two bounds an upper and a lower

    // get the times values
    Attribute timesAtt = ncVar.findAttribute("validTimes");
    if (timesAtt == null) return;
    Array timesArray = timesAtt.getValues();

    // get every other one LOOK this is awkward
    try {
      int n = (int) timesArray.getSize();
      List<Range> list = new ArrayList<Range>();
      list.add(new Range(0, n-1, 2));
      timesArray = timesArray.section(list);
    } catch (InvalidRangeException e) {
      throw new IllegalStateException(e);
    }

    // make sure it matches the dimension
    DataType dtype = DataType.getType( timesArray.getElementType());
    int nTimesAtt = (int) timesArray.getSize();

    // create a special dimension and coordinate variable
    Dimension dimTime = ncVar.getDimension(0);
    int nTimesDim = dimTime.getLength();
    if (nTimesDim != nTimesAtt) {
      parseInfo.format(" **error ntimes in attribute (%d) doesnt match dimension length (%d) for variable %s\n",nTimesAtt, nTimesDim, ncVar.getName());
      return;
    }

    // add the dimension
    String dimName = ncVar.getName()+"_timeCoord";
    Dimension newDim = new Dimension(dimName, nTimesDim);
    ds.addDimension( null, newDim);

    // add the coordinate variable
    String units = "seconds since 1970-1-1 00:00:00";
    String desc = "time coordinate for "+ncVar.getName();

    CoordinateAxis1D timeCoord = new CoordinateAxis1D( ds, null, dimName, dtype, dimName, units, desc);
    timeCoord.setCachedData(timesArray, true);
    timeCoord.addAttribute(new Attribute("long_name",  desc));
    timeCoord.addAttribute(new Attribute("units",  units));
    timeCoord.addAttribute(new Attribute(_Coordinate.AxisType, "Time"));
    ds.addCoordinateAxis(timeCoord);

    parseInfo.format(" added coordinate variable %s\n", dimName);

    // now make the original variable use the new dimension
    List<Dimension> dimsList = ncVar.getDimensions();
    dimsList.set(0, newDim);
    ncVar.setDimensions( dimsList);

    // better to explicitly set the coordinate system
    ncVar.addAttribute(new Attribute(_Coordinate.Axes, dimName+" yCoord xCoord"));

    // fix the attributes
    Attribute att = ncVar.findAttribute("fillValue");
    if (att != null)
      ncVar.addAttribute(new Attribute("_FillValue", att.getNumericValue()));
    att = ncVar.findAttribute("descriptiveName");
    if (null != att)
      ncVar.addAttribute(new Attribute("long_name", att.getStringValue()));

    // ncVar.enhance();
  }

  protected String getZisPositive( NetcdfDataset ds, CoordinateAxis v) {
    return "up";
  }

  private Projection makeLCProjection(NetcdfDataset ds) {
    Attribute latLonOrigin = projVar.findAttributeIgnoreCase("latLonOrigin");
    double centralLon = latLonOrigin.getNumericValue(0).doubleValue();
    double centralLat = latLonOrigin.getNumericValue(1).doubleValue();

    double par1 = findAttributeDouble( "stdParallelOne");
    double par2 = findAttributeDouble( "stdParallelTwo");
    LambertConformal lc = new LambertConformal(centralLat, centralLon, par1, par2);

    // make Coordinate Transform Variable
    ProjectionCT ct = new ProjectionCT("lambertConformalProjection", "FGDC", lc);
    VariableDS ctVar = makeCoordinateTransformVariable(ds, ct);
    ctVar.addAttribute( new Attribute(_Coordinate.Axes, "xCoord yCoord"));
    ds.addVariable(null, ctVar);

    return lc;
  }

  private void makeXYcoords(NetcdfDataset ds, Projection proj, Variable latVar, Variable lonVar) throws IOException {
    // brute force
    Array latData = latVar.read();
    Array lonData = lonVar.read();

    Dimension y_dim = latVar.getDimension(0);
    Dimension x_dim = latVar.getDimension(1);

    Array xData = Array.factory( float.class, new int[] {x_dim.getLength()});
    Array yData = Array.factory( float.class, new int[] {y_dim.getLength()});

    LatLonPointImpl latlon = new LatLonPointImpl();
    ProjectionPointImpl pp = new ProjectionPointImpl();

    Index latlonIndex = latData.getIndex();
    Index xIndex = xData.getIndex();
    Index yIndex = yData.getIndex();

    // construct x coord
    for (int i=0; i<x_dim.getLength(); i++) {
      double lat = latData.getDouble( latlonIndex.set1(i));
      double lon = lonData.getDouble( latlonIndex);
      latlon.set( lat, lon);
      proj.latLonToProj( latlon, pp);
      xData.setDouble( xIndex.set(i), pp.getX());
    }

    // construct y coord
    for (int i=0; i<y_dim.getLength(); i++) {
      double lat = latData.getDouble( latlonIndex.set0(i));
      double lon = lonData.getDouble( latlonIndex);
      latlon.set( lat, lon);
      proj.latLonToProj( latlon, pp);
      yData.setDouble( yIndex.set(i), pp.getY());
    }

    VariableDS xaxis = new VariableDS(ds, null, null, "xCoord", DataType.FLOAT, x_dim.getName(), "km", "x on projection");
    xaxis.addAttribute(new Attribute("units", "km"));
    xaxis.addAttribute(new Attribute("long_name", "x on projection"));
    xaxis.addAttribute(new Attribute(_Coordinate.AxisType, "GeoX"));

    VariableDS yaxis = new VariableDS(ds, null, null, "yCoord", DataType.FLOAT, y_dim.getName(), "km", "y on projection");
    yaxis.addAttribute(new Attribute("units", "km"));
    yaxis.addAttribute(new Attribute("long_name", "y on projection"));
    yaxis.addAttribute(new Attribute(_Coordinate.AxisType, "GeoY"));

    xaxis.setCachedData( xData, true);
    yaxis.setCachedData( yData, true);

    ds.addVariable( null, xaxis);
    ds.addVariable( null, yaxis);
  }

   private double findAttributeDouble(String attname) {
    Attribute att = projVar.findAttributeIgnoreCase(attname);
    return att.getNumericValue().doubleValue();
  }

}