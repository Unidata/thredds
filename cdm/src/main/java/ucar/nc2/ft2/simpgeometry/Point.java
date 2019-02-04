package ucar.nc2.ft2.simpgeometry;

import ucar.ma2.Array;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * Generic interface for a Simple Geometry Point.
 * 
 * @author wchen@usgs.gov
 *
 */
public interface Point extends SimpleGeometry {

	/**
	 * Get the data associated with this point
	 * 
	 * @return data
	 */
	public Array getData();
	
	/**
	 * Return the x coordinate for the point.
	 * 
	 * @return x of the point
	 */
	public double getX();
	
	/**
	 * Return the y coordinate for the point
	 * 
	 * @ return y of the point
	 */
	public double getY();
	/**
	 * Retrieves the next point within a multipoint if any
	 * 
	 * @return next point if it exists, null if not
	 */
	public Point getNext();
	
	/**
	 * Retrieves the previous point within a multipoint if any
	 * 
	 * @return previous point if it exists null if not
	 */
	public Point getPrev();

	/**
	 * Sets the data array of the point.
	 * 
	 * @param arr the array which will be the points new data array
	 */
	public void setData(Array arr);
	
	/**
	 * Sets the x coordinate of the point.
	 * 
	 * @param x coordinate of the point
	 */
	public void setX(double x);
	
	/**
	 * Set the y coordinate of the point.
	 * 
	 * @param y coordinate of the point
	 */
	public void setY(double y);
	
	/**
	 *  Sets the next point in a multipoint
	 */
	public void setNext(Point next);
	
	/**
	 *  Set the previous point in a multipoint
	 */
	public void setPrev(Point prev);
	
	/**
	 * Given a dataset, construct a point from the variable which holds points
	 * and the index as given.
	 * 
	 * @param dataset Where the point variable resides
	 * @param variable Which holds point information
	 * @param index for Indexing within the polygon variable
	 * 
	 * @return the constructed Point with associated data
	 */
	public Point setupPoint(NetcdfDataset dataset, Variable variable, int index);
}
