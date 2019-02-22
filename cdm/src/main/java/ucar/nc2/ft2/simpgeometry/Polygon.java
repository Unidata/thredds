package ucar.nc2.ft2.simpgeometry;


import java.util.List;
import ucar.ma2.Array;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * Generic interface for a Simple Geometry Polygon.
 * 
 * @author wchen@usgs.gov
 *
 */
public interface Polygon extends SimpleGeometry {

	/**
	 * Get the list of points which constitute the polygon.
	 * 
	 * @return points Points which constitute the polygon
	 */
	public List<Point> getPoints();

	/**
	 * Get the data associated with this Polygon
	 * 
	 * @return data Data series associated with the Polygon
	 */
	public Array getData();
	
	/**
	 * Get the next polygon in the sequence of multi-polygons
	 * 
	 * @return Next polygon in the same multipolygon if any, otherwise null
	 */
	public Polygon getNext();
	
	/**
	 * Get the previous polygon in the sequence of multi-polygons
	 * 
	 * @return Previous polygon in the same multipolygon if any, otherwise null
	 */
	public Polygon getPrev();
	
	/**
	 * Get whether or not this polygon is an interior ring
	 * 
	 * @return true if an interior ring, false if not
	 */
	public boolean getInteriorRing();
	
	/**
	 * Add a point to this polygon's points list
	 * 
	 */
	public void addPoint(double x, double y);
	
	/**
	 * Set the data associated with this Polygon
	 * 
	 * @param data Array of data to set to
	 */
	public void setData(Array data);
	
	/**
	 * Sets the next polygon which make up the multipolygon which this polygon is a part of.
	 * 
	 * @param next Polygon to set
	 */
	public void setNext(Polygon next);
	
	/**
	 * Sets the previous polygon which makes up the multipolygon which this polygon is a part of.
	 * 
	 * @param prev Polygon to set
	 */
	public void setPrev(Polygon prev);
	
	/**
	 *  Simply sets whether or not this polygon is an interior ring
	 * 
	 * @param interior ring or not
	 */
	public void setInteriorRing(boolean interior);
	
	/**
	 * Given a dataset, construct a polygon from the variable which holds polygons
	 * and the index as given.
	 * 
	 * @param dataset Where the polygon variable resides
	 * @param variable Which holds polygon information
	 * @param index for Indexing within the polygon variable
	 * 
	 * @return the constructed Polygon with associated data
	 */
	public Polygon setupPolygon(NetcdfDataset dataset, Variable variable, int index);

}
