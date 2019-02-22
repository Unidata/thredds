package ucar.nc2.ft2.simpgeometry;

import ucar.ma2.Array;

/**
 * An interface to interact with
 * Simple Geometry Feature Types.
 * 
 *
 * @author Katie
 * @author wchen@usgs.gov
 *
 */
public interface SimpleGeometry {

    /**
     * Sets the data associated with this geometry
     * 
     * @param data
     */
    public void setData(Array data);
    
    /**
     * Fetches the data associated with this geometry.
     * 
     * @return data
     */
    public Array getData();

    /**
     * Gets the lower bounding box of this geometry.
     * 
     * @return lower bounding box, a one dimensional array of length two
     */
    public double[] getBBLower();
    
    /**
     * Gets the upper bounding box of this geometry.
     * 
     * @return upper bounding box, a one dimenstional array of length two
     */
    public double[] getBBUpper();
}
