package ucar.nc2.ft2.simpgeometry;

import java.io.IOException;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.simpgeometry.Point;
import ucar.nc2.ft2.simpgeometry.exception.InvalidDataseriesException;

/**
 * A CF 1.8 compliant Point
 * for use with Simple Geometries.
 * Can also represent multipoints.
 * 
 * @author wchen@usgs.gov
 *
 */
public class CFPoint implements Point{

	private double x;	// x coordinate
	private double y;	// y coordinate
	private Point next;	// next element in a multipoint
	private Point prev;	// previous element in a multipoint
	private Array data;	// data of the point
	
	/**
	 * Get the geometry the data is associated with.
	 * 
	 */
	
	public GeometryType getGeometryType() {
		return GeometryType.POINT;
	}
	
	/**
	 * Get the data associated with this point
	 * 
	 * @return data
	 */
	public Array getData() {
		return data;
	}
	
	/**
	 * Return the x coordinate for the point.
	 * 
	 * @return x of the point
	 */
	public double getX() {
		return x;
	}
	
	/**
	 * Return the y coordinate for the point
	 * 
	 * @ return y of the point
	 */
	public double getY() {
		return y;
	}
	
	/**
	 * Retrieves the next point within a multipoint if any
	 * 
	 * @return next point if it exists, null if not
	 */
	public Point getNext() {
		return next;
	}
	
	/**
	 * Retrieves the previous point within a multipoint if any
	 * 
	 * @return previous point if it exists null if not
	 */
	public Point getPrev() {
		return prev;
	}
	
	
	/**
	 * Sets the data array of the point.
	 * 
	 * @param arr the array which will be the points new data array
	 */
	public void setData(Array arr) {
		this.data = arr;
	}
	
	/**
	 * Sets the x coordinate of the point.
	 * 
	 * @param x coordinate of the point
	 */
	public void setX(double x) {
		this.x = x;
	}
	
	/**
	 * Set the y coordinate of the point.
	 * 
	 * @param y coordinate of the point
	 */
	public void setY(double y) {
		this.y = y;
	}
	
	/**
	 *  Sets the next point in a multipoint
	 * 
	 */

	public void setNext(Point next) {
		this.next = next;
	}
	
	/**
	 * 
	 *  Set the previous point in a multipoint
	 * 
	 */
	public void setPrev(Point prev) {
		this.prev = prev;
	}
	
	/**
	 * Given a dataset, variable, and index, automatically populates this Point and
	 * returns it. If not found, returns null.
	 * 
	 * @param dataset which the variable is a part of
	 * @param vari the variable which has a geometry attribute
	 * @param index of the point within the variable
	 * @return return a point
	 */
	public Point setupPoint(NetcdfDataset set, Variable vari, int index)
	{
		// Points are much simpler, node_count is used multigeometries so it's a bit different
		// No need for the index finder here, unless there is a multipoint
		Array xPts = null;
		Array yPts = null;
		Integer ind = (int)index;
		Variable nodeCounts = null;
		boolean multi = false;
		SimpleGeometryIndexFinder indexFinder = null;

		List<CoordinateAxis> axes = set.getCoordinateAxes();
		CoordinateAxis x = null; CoordinateAxis y = null;
		
		String[] nodeCoords = vari.findAttributeIgnoreCase(CF.NODE_COORDINATES).getStringValue().split(" ");
		
		// Look for x and y
		
		for(CoordinateAxis ax : axes){
			
			if(ax.getFullName().equals(nodeCoords[0])) x = ax;
			if(ax.getFullName().equals(nodeCoords[1])) y = ax;
		}
		
		// Node count is used very differently in points
		// Similar use to part_node_count in other geometries
		String node_c_str = vari.findAttValueIgnoreCase(CF.NODE_COUNT, "");
		
		if(!node_c_str.equals("")) {
			nodeCounts = set.findVariable(node_c_str);
			indexFinder = new SimpleGeometryIndexFinder(nodeCounts);
			multi = true;
		}
		
		try {
			
			//
			if(multi)
			{
				xPts = x.read( indexFinder.getBeginning(index) + ":" + indexFinder.getEnd(index) ).reduce();
				yPts = y.read( indexFinder.getBeginning(index) + ":" + indexFinder.getEnd(index) ).reduce();
			}
			
			else
			{
				xPts = x.read( ind.toString() ).reduce();
				yPts = y.read( ind.toString() ).reduce();
				this.x = xPts.getDouble(0);
				this.y = yPts.getDouble(0);
			}
		
			// Set points
			if(!multi) {
				this.x = xPts.getDouble(0);
				this.y = yPts.getDouble(0);
				
				// Set data of each
				switch(vari.getRank()) {
				
				case 2:
					this.setData(vari.read(CFSimpleGeometryHelper.getSubsetString(vari, index)).reduce());
					break;
					
				case 1:
					this.setData(vari.read("" + index));
					break;
					
				default:
					throw new InvalidDataseriesException(InvalidDataseriesException.RANK_MISMATCH);	// currently do not support anything but dataseries and scalar associations
				
				}
				
			}
		
			else {
				IndexIterator itrX = xPts.getIndexIterator();
				IndexIterator itrY = yPts.getIndexIterator();
				this.next = null;
				this.prev = null;
				
				Point point = this;
		
				// x and y should have the same shape (size), will add some handling on this
				while(itrX.hasNext()) {
					point.setX(itrX.getDoubleNext());
					point.setY(itrY.getDoubleNext());
					
					// Set data of each
					switch(vari.getRank()) {
					
					case 2:
						point.setData(vari.read(CFSimpleGeometryHelper.getSubsetString(vari, index)).reduce());
						break;
						
					case 1:
						point.setData(vari.read("" + index));
						break;
						
					default:
						throw new InvalidDataseriesException(InvalidDataseriesException.RANK_MISMATCH);	// currently do not support anything but dataseries and scalar associations
					
					}

					point.setNext(new CFPoint()); // -1 is a default value, it gets assigned eventually
					point = point.getNext();
				}
				
				// Clean up the last point since it will be invalid
				point = point.getPrev();
				point.setNext(null);
			}
		
		} catch (IOException e) {

			e.printStackTrace();
			return null;
		
		} catch (InvalidRangeException e) {
			
			e.printStackTrace();
			return null;
		} catch (InvalidDataseriesException e) {

			e.printStackTrace();
			return null;
		}
		
		return this;
	}
	
	/**
	 * Gets the upper bounding box coordinate on the point.
	 * @return double array = (x, y)
	 */
	public double[] getBBUpper() {
		double[] bbUpper = { this.getX() + 10, this.getY() + 10 };
		return bbUpper;
	}
	
	/**
	 * Gets the lower bounding box coordinate on the polygon.
	 * @return double array = (x, y)
	 */
	public double[] getBBLower() {
		double[] bbLower = { this.getX() - 10, this.getY() - 10 };
		return bbLower;
	}
	
	/**
	 * Construct a new point from specified parameters
	 * The construction will automatically connect in related parts of a Multipoint - just specify any constituents
	 * of a multipoint as next or prev.
	 * 
	 * @param x - the x coordinate of the point
	 * @param y - the y coordinate of the point
	 * @param prev - previous point if part of a multipoint
	 * @param next - next point if part of a multipoint
	 * @param data - data associated with the point
	 */
	public CFPoint(double x, double y, Point prev, Point next, Array data) {
		this.next = next;
		this.prev = prev;
		
		// Create links automatically
		if(next != null) {
			next.setPrev(this);
		}
		
		if(prev != null) {
			prev.setNext(this);
		}
		
		this.x = x;
		this.y = y;
		this.data = data;
	}
	
	/**
	 * Constructs a new empty point at (0,0) with no connections.
	 * 
	 */
	public CFPoint() {
		this(0, 0, null, null, null);
	}
}
