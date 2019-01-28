package ucar.nc2.ft2.simpgeometry;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * A class which given a dataset, will read from the dataset one of the simple geometry types. Those being polygon, line, or point.
 * 
 * @author wchen@usgs.gov
 *
 */
public class SimpleGeometryReader {
	
	private NetcdfDataset ds;
	
	/**
	 * Returns a Polygon given a variable name and the geometric index. If the Polygon is not found it will return null. If the Polygon is a part of the Multi-Polygon, it will return the head
	 * (the first Polygon in the series which constitutes the Multi-Polygon).
	 * 
	 * @param name of the variable which holds the Polygon
	 * @param index of the Polygon within the variable
	 * @return a new polygon with all associated information
	 */
	public Polygon readPolygon(String name, int index) {
		
		Variable polyvar = ds.findVariable(name);
		if(polyvar == null) return null;
		
		Polygon poly = null;
		
		// CFConvention
		if(ds.findGlobalAttribute(CF.CONVENTIONS) != null)
			if(ucar.nc2.dataset.conv.CF1Convention.getVersion(ds.findGlobalAttribute(CF.CONVENTIONS).getStringValue()) >= 8)
				poly = new CFPolygon();
		
		if(poly == null) return null;
		else return poly.setupPolygon(ds, polyvar, index);
	}
	
	/**
	 * Returns a Line given a variable name and the geometric index. If the Line is not found it will return null. If the Line is a part of the Multi-Line, it will return the head
	 * (the first Line in the series which constitutes the Multi-Line).
	 * 
	 * @param name of the variable which holds the Line
	 * @param index of the Line within the variable
	 * @return a new line with all associated information
	 */
	public Line readLine(String name, int index) {
	
		Variable linevar = ds.findVariable(name);
		if(linevar == null) return null;
		Line line = null;
		
		// CFConvention
		if(ds.findGlobalAttribute(CF.CONVENTIONS) != null)
			if(ucar.nc2.dataset.conv.CF1Convention.getVersion(ds.findGlobalAttribute(CF.CONVENTIONS).getStringValue()) >= 8)
				line = new CFLine();
		
		if(line == null) return null;
		else return line.setupLine(ds, linevar, index);
	}
	
	/**
	 * Returns a Point given a variable name and the geometric index. If the Point is not found it will return null. If the Point is a part of the Multi-Point, it will return the head
	 * (the first Point in the series which constitutes the Multi-Point).
	 * 
	 * @param name of the variable which holds the Point
	 * @param index of the Point within the variable
	 * @return a new Point with all associated information
	 */
	public Point readPoint(String name, int index) {

		Variable pointvar = ds.findVariable(name);
		if(pointvar == null) return null;
		Point pt = null;
		
		// CFConvention
		if(ds.findGlobalAttribute(CF.CONVENTIONS) != null)
			if(ucar.nc2.dataset.conv.CF1Convention.getVersion(ds.findGlobalAttribute(CF.CONVENTIONS).getStringValue()) >= 8)
				pt = new CFPoint();
		
		if(pt == null) return pt;
		else return pt.setupPoint(ds, pointvar, index);
	}
	
	/**
	 * Given a variable name, returns the geometry type which that variable is associated with.
	 * If the variable has no simple geometry information, null will be returned.
	 * 
	 * @param name variable name which will have geometry type be checked
	 * @return Geometry Type if holds geometry information, null if not
	 */
	public GeometryType getGeometryType(String name) {
		Variable geometryVar = ds.findVariable(name);
		if(geometryVar == null) return null;
		
		// CFConvention
		if(ds.findGlobalAttribute(CF.CONVENTIONS) != null)
			if(ucar.nc2.dataset.conv.CF1Convention.getVersion(ds.findGlobalAttribute(CF.CONVENTIONS).getStringValue()) >= 8)
			{
				Attribute geometryTypeAttr = null;
				String geometry_type = null;
				
				geometryTypeAttr = geometryVar.findAttribute(CF.GEOMETRY_TYPE);
				if(geometryTypeAttr == null) return null;
				geometry_type = geometryTypeAttr.getStringValue();
				
				switch(geometry_type)
				{
					case CF.POLYGON:
						return GeometryType.POLYGON;
					case CF.LINE:
						return GeometryType.LINE;
					case CF.POINT:
						return GeometryType.POINT;
					default:
						return null;
				}
			}
		
		return null;
	}
	
	/**
	 * Constructs a new Simple Geometry Reader over the specified dataset.
	 * 
	 * @param ds - the specified dataset
	 */
	public SimpleGeometryReader(NetcdfDataset ds) {
		this.ds = ds;
	}
	
}
