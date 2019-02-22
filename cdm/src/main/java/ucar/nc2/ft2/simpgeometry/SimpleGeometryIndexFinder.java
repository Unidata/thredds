package ucar.nc2.ft2.simpgeometry;

import java.io.IOException;

import ucar.ma2.Array;
import ucar.nc2.Variable;

/**
 * A Simple Geometry Index Finder can go find the beginning and end indicies of
 * a simple geometry X and Y within a variable. But first the Index Finder needs a few variables to find it in.
 * If the indexer is tasked to find a Simple Geometry close to one before, it will find it faster.
 * 
 * @author wchen@usgs.gov
 *
 */
public class SimpleGeometryIndexFinder {
	
	private final int INVALID_INDEX = -10;	// default invalid index before any lookups have been done
	private Array nodeCount = null;
	private int pastIndex;
	private int previousEnd;
	private int previousBegin;
	
	private int getNodeCount(int index) {
		return nodeCount.getInt(index);
	}
	
	/**
	 * Gets the beginning index of a geometry's points given the index of the geometry within the array. 
	 * 
	 * @param index
	 * @return beginning of the range
	 */
	public int getBeginning(int index) {
		
		//Test if the last end is the new beginning
		if(index == (pastIndex + 1 ))
		{
			return previousEnd + 1;
		}
		
		// Otherwise, find it!
		int newBeginning = 0;
		for(int i = 0; i < index; i++) {
			newBeginning += getNodeCount(i);
		}
		
		pastIndex = index;
		previousBegin = newBeginning;
		return newBeginning;
	}
	
	/**
	 * Gets the ending index of a geometry's points given the index of the geometry within the array. 
	 * 
	 * @param index of the geometry within the array
	 * @return end of the range
	 */
	public int getEnd(int index) {

		// Test if the last beginning is the new end
		if(index == (pastIndex - 1))
		{
			return previousBegin - 1;
		}
		
		// Otherwise find it!
		int new_end = 0;
		for(int i = 0; i < index + 1; i++) {
			new_end += getNodeCount(i);
		}
		
		pastIndex = index;
		previousEnd = new_end;
		return new_end - 1;
	}
	
	/**
	 * Create a new indexer, the indexer must be given a variable to look through.
	 * 
	 * @param node_count Amount of nodes per geometry
	 */
	public SimpleGeometryIndexFinder(Variable node_count) {
		
		try {
			this.nodeCount = node_count.read();
		} catch (IOException e) {

			this.nodeCount = null;
			e.printStackTrace();
		}
		
		pastIndex = INVALID_INDEX;
		previousEnd = INVALID_INDEX;
		previousBegin = INVALID_INDEX;
	}
}
