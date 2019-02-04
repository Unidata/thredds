package ucar.nc2.ft2.simpgeometry;

import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.Dimension;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.ft2.simpgeometry.adapter.SimpleGeometryCS;
import ucar.nc2.util.Indent;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * SimpleGeometry - forked from Coverage.java
 * Immutable after setCoordSys() is called.
 *
 * @author Katie
 * @since 8/13/2018
 */
// @Immutable
public class SimpleGeometryFeature implements VariableSimpleIF{
  private final String name;
  private final DataType dataType;
  private final AttributeContainerHelper atts;
  private final String units, description;
  private final String coordSysName;
  protected final Object user;
  
  private int[] shapes;
  private CoordinateAxis xAxis, yAxis, zAxis, IDAxis;
  
  private final GeometryType geometryType; // use enum

  private SimpleGeometryCS coordSys; // almost immutable use coordsys

    /**
     * @param name name of the feature
     * @param dataType data type
     * @param atts list of attriburtes
     * @param coordSysName name of the coordinate system
     * @param units units to be used
     * @param description description of the feature
     * @param user user responsible for feature
     * @param geometryType type of geometry
     */
  public SimpleGeometryFeature(String name, DataType dataType, List<Attribute> atts, String coordSysName, String units, String description, Object user, GeometryType geometryType) {
    this.name = name;
    this.dataType = dataType;
    this.atts = new AttributeContainerHelper(name, atts);
    this.coordSysName = coordSysName;
    this.units = units;
    this.description = description;
    this.user = user;
    this.geometryType = geometryType;
    this.xAxis = null;
    this.yAxis = null;
    this.zAxis = null;
    this.IDAxis = null;
    shapes = null;
  }


  public void setCoordSys (SimpleGeometryCS coordSys) {
    if (this.coordSys != null) throw new RuntimeException("Can't change coordSys once set");
    this.coordSys = coordSys;
    
    String axesStrList[] = null;
    
    // Find the name of the axes specific to this geometry
    axesStrList = coordSysName.split(" ");

    List<String> axesStrActualList = new ArrayList<String>();
    int shapeLength = 0;
    
    if(axesStrList != null) {
    
    	for(int i = 0; i < axesStrList.length; i++) {
    		axesStrActualList.add(axesStrList[i]);
    	}
    
    	// Set up x Axis
    	for(CoordinateAxis xAx : coordSys.getSimpleGeometryX()) {
    		if(axesStrActualList.contains(xAx.getFullNameEscaped())) {
    			xAxis = xAx;
    			shapeLength++;
    		}
    	}
    
    	// Set up y Axis
    	for(CoordinateAxis yAx : coordSys.getSimpleGeometryY()) {
    		if(axesStrActualList.contains(yAx.getFullNameEscaped())) {
    			yAxis = yAx;
    			shapeLength++;
    		}
    	}
    
    	// Set up z Axis
    	for(CoordinateAxis zAx : coordSys.getSimpleGeometryZ()) {
    		if(axesStrActualList.contains(zAx.getFullNameEscaped())) {
    			zAxis = zAx;
    			shapeLength++;
    		}
    	}
    
    	// Set up ID axis
    	for(CoordinateAxis idAx : coordSys.getSimpleGeometryID()) {
    		if(axesStrActualList.contains(idAx.getFullNameEscaped())) {
    			IDAxis = idAx;
    			shapeLength++;
    		}
    	}
    
    	shapes = new int[shapeLength];
    
    	int shapeIndex = 0;
    	if(xAxis != null) { shapes[shapeIndex] = (int) xAxis.getSize(); shapeIndex++;}
    	if(yAxis != null) { shapes[shapeIndex] = (int) yAxis.getSize(); shapeIndex++; }
    	if(zAxis != null) { shapes[shapeIndex] = (int) zAxis.getSize(); shapeIndex++; }
    	if(IDAxis != null) { shapes[shapeIndex] = (int) IDAxis.getSize(); shapeIndex++; }
    }
  }

  public String getName() {
    return name;
  }


  @Override
  public DataType getDataType() {
    return dataType;
  }

  @Override
  public List<Attribute> getAttributes() {
    return atts.getAttributes();
  }

  @Override
  public Attribute findAttributeIgnoreCase(String name) {
    return atts.findAttributeIgnoreCase(name);
  }

  @Override
  public String getUnitsString() {
    return units;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public String getCoordSysName() {
    return coordSysName;
  }

  public Object getUserObject() {
    return user;
  }
  
  public GeometryType getGeometryType() {
	  return geometryType; 
  }
  
  public String getGeometryDescription() {
	  return this.geometryType.getDescription();
  }
  
  @Override
  public String toString() {
    Formatter f = new Formatter();
    Indent indent = new Indent(2);
    toString(f, indent);
    return f.toString();
  }

  public void toString(Formatter f, Indent indent) {
    indent.incr();
    f.format("%n%s  %s %s(%s) desc='%s' units='%s' geometry='%s'%n", indent, dataType, name, coordSysName, description, units, this.getGeometryDescription());
    f.format("%s    attributes:%n", indent);
    for (Attribute att : atts.getAttributes())
      f.format("%s     %s%n", indent, att);
    indent.decr();
  }

  @Nonnull
  public SimpleGeometryCS getCoordSys() {
    return coordSys;
  }
  
  /**
   * Retrieves the x Axis that corresponds to this geometry.
   * 
   * @return x axis
   */
  public CoordinateAxis getXAxis() {
	return xAxis;  
  }
  
  /**
   * Retrieves the y Axis that corresponds to this geometry.
   * 
   * @return y axis
   */
  public CoordinateAxis getYAxis() {
	return yAxis;  
  }
  
  /**
   * Retrieves the z Axis that corresponds to this geometry.
   * 
   * @return z axis
   */
  public CoordinateAxis getZAxis() {
	return zAxis;  
  }
  
  /**
   * Retrieves the ID Axis that corresponds to this geometry.
   * 
   * @return id axis
   */
  public CoordinateAxis getIDAxis() {
	return IDAxis;  
  }

  /**
   * Get the data associated with the index
   * @param  index  number associated with the geometry 
   */
  public SimpleGeometry readGeometry(int index) throws IOException, InvalidRangeException {

	  SimpleGeometry geom = null;
	  switch (geometryType) {
		  
		  case POINT:
			  Point point = coordSys.getPoint(name, index);
			  geom = point;
			  break;
		  case LINE:
			  Line line = coordSys.getLine(name, index);
			  geom = line;
			  break;
		  case POLYGON:
			  Polygon poly = coordSys.getPolygon(name, index);
			  geom = poly;
			  break;
          default:
              break;

		  }
	  return geom;
  }

  @Override
  public String getFullName() {
    return getName();
  }

  @Override
  public String getShortName() {
    return getName();
  }

  @Override
  public int getRank() {
    return getShape().length;
  }

  @Override
  public int[] getShape() {
    return shapes;
  }

  @Override
  public List<Dimension> getDimensions() {
    return coordSys.getDimensions();
  }

  @Override
  public int compareTo(@Nonnull VariableSimpleIF o) {
    return getFullName().compareTo(o.getFullName());
  }
}
