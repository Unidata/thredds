/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft2.simpgeometry.adapter;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.ft2.simpgeometry.*;

/**
 * Simple Geometry Coordinate System / Enhanced Dataset Implementation
 * Forked from ucar.nc2.ft2.coverage.adapter.GridCS
 *
 * @author John
 * @author wchen@usgs.gov
 * @since 8/9/2018
 */
public class SimpleGeometryCS {

  private List<CoordinateAxis> simpleGeometryX, simpleGeometryY, simpleGeometryZ, simpleGeometryID;
  SimpleGeometryCSBuilder builder;
  
  public SimpleGeometryCS(SimpleGeometryCSBuilder builder) {
    this.builder = builder;
    simpleGeometryX = new ArrayList<CoordinateAxis>(); simpleGeometryY = new ArrayList<CoordinateAxis>();
    simpleGeometryZ = new ArrayList<CoordinateAxis>(); simpleGeometryID = new ArrayList<CoordinateAxis>();
    
    for(CoordinateAxis axis : builder.getSgAxes()) {
    	
    	// Look for simple geometry axes and add them
    	if(axis.getAxisType().equals(AxisType.SimpleGeometryX)) simpleGeometryX.add(axis);
    	else if (axis.getAxisType().equals(AxisType.SimpleGeometryY)) simpleGeometryY.add(axis);
    	else if(axis.getAxisType().equals(AxisType.SimpleGeometryZ)) simpleGeometryZ.add(axis);
    	else if(axis.getAxisType().equals(AxisType.SimpleGeometryID)) simpleGeometryID.add(axis);

    }
  }
  
  /**
   * Given a variable name, returns the type of geometry which that variable is holding
   * 
   * @param name name of the variable
   * @return geometry type associated with that variable
   */
  public GeometryType getGeometryType(String name) {
	return this.builder.getGeometryType(name);
  }
  
  /**
   * Get a list of all simple geometry X axes.
   * 
   * @return list of simple geometry X axes
   */
  public List<CoordinateAxis> getSimpleGeometryX() {
	return this.simpleGeometryX;
  }
  
  /**
   * Get a list of all simple geometry Y axes
   * 
   * @return list of simple geometry Y axes
   */
  public List<CoordinateAxis> getSimpleGeometryY() {
	return this.simpleGeometryY;
  }
  
  /**
   * Get a list of all simple geometry Z axes.
   * 
   * @return list of simple geometry Z axes.
   */
  public List<CoordinateAxis> getSimpleGeometryZ() {
	return this.simpleGeometryZ;
  }
  
  /**
   * Get a list of all simple geometry ID axes.
   * Simple Geometry ID axes are used for indexing into simple geometry variables.
   * 
   * @return list of simple geometry ID axes
   */
  public List<CoordinateAxis> getSimpleGeometryID() {
	return this.simpleGeometryID;
  }
  
  /**
   * Get a list of all dimensions in this dataset.
   * 
   * @return list of dimensions.
   */
  public List<Dimension> getDimensions(){
	  return builder.getDimensions();
  }
  
  /**
   * Given a Variable name and a geometry index
   * returns a Polygon 
   * 
   * @param name of the data variable
   * @param index within the variable
   * 
   * @return polygon with all associated data, null if not found
   */
  public Polygon getPolygon(String name, int index) {
	 return builder.getPolygon(name, index);
  }
  
  /**
   * Given a Variable name and a beginning index and end index, returns a list of
   * polygon (inclusive on both sides)
   * 
   * @param name
   * @param indexBegin
   * @param indexEnd
   * @return
   */
  public List<Polygon> getPolygons(String name, int indexBegin, int indexEnd) {
	  return builder.getPolygons(name, indexBegin, indexEnd);
  }
  
  /**
   * Given a Variable name and a geometry index
   * returns a Line 
   * 
   * @param name of the data variable
   * @param index within the variable
   * 
   * @return line with all associated data, null if not found
   */
  public Line getLine(String name, int index) {
	 return builder.getLine(name, index);
  }
  
  /**
   * Given a Variable name and a beginning index and end index, returns a list of
   * lines (inclusive on both sides)
   * 
   * @param name
   * @param indexBegin
   * @param indexEnd
   * @return
   */
  public List<Line> getLines(String name, int indexBegin, int indexEnd) {
	  return builder.getLines(name, indexBegin, indexEnd);
  }
  
  /**
   * Given a Variable name and a geometry index
   * returns a Point
   * 
   * @param name of the data variable
   * @param index within the variable
   * @return point with all associated data, null if not found
   */
  public Point getPoint(String name, int index) {
	 return builder.getPoint(name,index);
  }
  
  /**
   * Given a Variable name and a beginning index and end index
   * returns a list of points (inclusive on both sides)
   * 
   * @param name of the data variable
   * @param indexBegin within the variable
   * @param indexEnd within the variable
   * @return a list of points with associated data
   */
  public List<Point> getPoints(String name, int indexBegin, int indexEnd) {
	  return builder.getPoints(name, indexBegin, indexEnd);
  }

    public static SimpleGeometryCS makeSGCoordSys(Formatter sbuff, CoordinateSystem cs, VariableEnhanced v) {
        if (sbuff != null) {
            sbuff.format(" ");
            v.getNameAndDimensions(sbuff, false, true);
            sbuff.format(" check CS %s: ", cs.getName());
        }

        SimpleGeometryCS gcs = new SimpleGeometryCS(new SimpleGeometryCSBuilder(null, cs, null));
        return gcs;
    }


}
