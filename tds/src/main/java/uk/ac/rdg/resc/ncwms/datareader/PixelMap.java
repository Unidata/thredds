/*
 * Copyright (c) 2007 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.datareader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionPoint;
import uk.ac.rdg.resc.ncwms.metadata.CoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.OneDCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.TwoDCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.projection.HorizontalProjection;

/**
 *<p>Maps pixels within the requested image to i and j indices of corresponding
 * points within the source data.  This is a very important class in ncWMS.  A
 * PixelMap is constructed using the constructor <code>new PixelMap(layer, grid)</code>,
 * which employs the following general algorithm:</p>
 *
 * <pre>
 * For each pixel in the image:
 *    1. Find the x-y coordinates of this point in the CRS of the image
 *    2. Transform these x-y coordinates into latitude and longitude
 *    3. Transform lat-lon into the coordinate system of the source data
 *    4. Transform the coordinate pair into index values (i and j)
 *    5. Add the mapping (pixel -> i,j) to the pixel map
 * </pre>
 *
 * <p>(A more efficient algorithm is used for the special case in which both the 
 * requested CRS and the CRS of the data are lat-lon.)</p>
 *
 * <p>The resulting PixelMap is then used by {@link DataReader}s to work out what
 * data to read from the source data files.  The grid below represents the source
 * data.  Black grid squares represent data points that must be read from the source
 * data and become part of the final image:</p>
 * <img src="doc-files/pixelmap_pbp.png">
 * <p>A variety of strategies are possible for reading these data points:</p>
 *
 * <h3>Strategy 1: read data points one at a time</h3> 
 * <p>Read each data point individually by iterating through {@link #getJIndices} 
 *    and {@link #getIIndices}.  This minimizes the memory footprint as the minimum
 *    amount of data is read from disk.  However, in general this method is inefficient
 *    as it maximizes the overhead of the low-level data extraction code by making 
 *    a large number of small data extractions.  This strategy is employed by
 *    {@link PixelByPixelDataReader} and is not recommended for general use.</p>
 *
 * <h3>Strategy 2: read all data points in one operation</h3>
 * <p>Read all data in one operation (potentially including lots of data points
 *       that are not needed) by finding the overall i-j bounding box with
 *       {@link #getMinIIndex}, {@link #getMaxIIndex}, {@link #getMinJIndex}
 *       and {@link #getMaxJIndex}.  This minimizes the number
 *       of calls to low-level data extraction code, but may result in a large memory
 *       footprint.  The {@link DataReader} would then subset this data array in-memory.
 *       This strategy is employed by {@link BoundingBoxDataReader}.  This approach is recommended for remote
 *       datasets (e.g. on an OPeNDAP server) as it minimizes the overhead associated
 *       with the data extraction operation.</p>
 * <p>This approach is illustrated in the diagram below.  Grey squares represent
 * data points that are read into memory but are discarded because they do not
 * form part of the final image:</p>
 * <img src="doc-files/pixelmap_bbox.png">
 *
 * <h3>Strategy 3: Read "scanlines" of data</h3>
 * <p>A compromise strategy, which balances memory considerations against the overhead 
 *       of the low-level data extraction code, works as follows:
 *       <ol>
 *          <li>Iterate through each row (i.e. each j index) that is represented in
 *              the PixelMap using {@link #getJIndices}.</li>
 *          <li>For each j index, extract data from the minimum to the maximum i index
 *              in this row (a "scanline") using {@link #getMinIIndexInRow} and
 *              {@link #getMaxIIndexInRow}.  (This assumes that the data are stored with the i
 *              dimension varying fastest, meaning that the scanline represents
 *              contiguous data in the source files.)</li>
 *       </ol>
 *       Therefore if there are 25 distinct j indices in the PixelMap there will be 25
 *       individual calls to the low-level data extraction code.  This algorithm has
 *       been found to work well in a variety of situations although it may not always
 *       be the most efficient.  This strategy is employed by the {@link DefaultDataReader}.</p>
 * <p>This approach is illustrated in the diagram below.  There is now a much smaller
 * amount of "wasted data" (i.e. grey squares) than in Strategy 2, and there are 
 * much fewer individual read operations than in Strategy 1.</p>
 * <img src="doc-files/pixelmap_scanline.png">
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class PixelMap
{
    private static final Logger logger = Logger.getLogger(PixelMap.class);
    
    // These define the bounding box (in terms of axis indices) of the data
    // to extract from the source files
    private int minIIndex = Integer.MAX_VALUE;
    private int minJIndex = Integer.MAX_VALUE;
    private int maxIIndex = -1;
    private int maxJIndex = -1;
    
    // Maps Y indices to row information
    private Map<Integer, Row> pixelMap = new HashMap<Integer, Row>();
    
    // Number of unique i-j pairs
    private int numUniqueIJPairs = 0;
    
    /**
     * Generates a PixelMap for the given Layer.  Data read from the Layer will
     * be projected onto the given HorizontalGrid
     * 
     * @throws Exception if the necessary transformations could not be performed
     */
    public PixelMap(Layer layer, HorizontalGrid grid) throws Exception
    {
        long start = System.currentTimeMillis();
        
        HorizontalProjection dataProj = layer.getHorizontalProjection();
        CoordAxis xAxis = layer.getXaxis();
        CoordAxis yAxis = layer.getYaxis();
        
        // Cycle through each pixel in the picture and work out which
        // x and y index in the source data it corresponds to
        int pixelIndex = 0;
        
        // We can gain efficiency if the target grid is a lat-lon grid and
        // the data exist on a lat-long grid by minimizing the number of
        // calls to axis.getIndex().
        if (dataProj.isLatLon() && grid.isLatLon() &&
            xAxis instanceof OneDCoordAxis && yAxis instanceof OneDCoordAxis)
        {
            logger.debug("Using optimized method for lat-lon coordinates with 1D axes");
            // These class casts should always be valid
            OneDCoordAxis xAxis1D = (OneDCoordAxis)xAxis;
            OneDCoordAxis yAxis1D = (OneDCoordAxis)yAxis;
            // Calculate the indices along the x axis.
            int[] xIndices = new int[grid.getXAxisValues().length];
            for (int i = 0; i < grid.getXAxisValues().length; i++)
            {
                xIndices[i] = xAxis1D.getIndex(grid.getXAxisValues()[i]);
            }
            for (double lat : grid.getYAxisValues())
            {
                if (lat >= -90.0 && lat <= 90.0)
                {
                    int yIndex = yAxis1D.getIndex(lat);
                    for (int xIndex : xIndices)
                    {
                        this.put(xIndex, yIndex, pixelIndex);
                        pixelIndex++;
                    }
                }
                else
                {
                    // We still need to increment the pixel index
                    pixelIndex += xIndices.length;
                }
            }
        }
        else
        {
            logger.debug("Using generic (but slower) method");
            for (double y : grid.getYAxisValues())
            {
                for (double x : grid.getXAxisValues())
                {
                    // Check that this point is valid in the target CRS
                    if (grid.isPointValidForCrs(x, y))
                    {
                        // Translate this point in the target grid to lat-lon
                        // TODO: the transformer can transform many points at once.
                        // Doing so might be more efficient than this method.
                        LatLonPoint latLon = grid.transformToLatLon(x, y);
                        // Translate this lat-lon point to a point in the data's projection coordinates
                        ProjectionPoint projPoint = dataProj.latLonToProj(latLon);
                        // Translate the projection point to grid point indices i, j
                        int i, j;
                        if (xAxis instanceof OneDCoordAxis && yAxis instanceof OneDCoordAxis)
                        {
                            OneDCoordAxis xAxis1D = (OneDCoordAxis)xAxis;
                            OneDCoordAxis yAxis1D = (OneDCoordAxis)yAxis;
                            i = xAxis1D.getIndex(projPoint.getX());
                            j = yAxis1D.getIndex(projPoint.getY());
                        }
                        else if (xAxis instanceof TwoDCoordAxis && yAxis instanceof TwoDCoordAxis)
                        {
                            TwoDCoordAxis xAxis2D = (TwoDCoordAxis)xAxis;
                            TwoDCoordAxis yAxis2D = (TwoDCoordAxis)yAxis;
                            i = xAxis2D.getIndex(projPoint);
                            j = yAxis2D.getIndex(projPoint);
                        }
                        else
                        {
                            // Shouldn't happen'
                            throw new IllegalStateException("x and y axes are of different types!");
                        }
                        this.put(i, j, pixelIndex); // Ignores negative indices
                    }
                    pixelIndex++;
                }
            }
        }
        logger.debug("Built pixel map in {} ms", System.currentTimeMillis() - start);
    }
    
    /**
     * Adds a new pixel index to this map.  Does nothing if either x or y is
     * negative.
     * @param i The i index of the point in the source data
     * @param j The j index of the point in the source data
     * @param pixel The index of the corresponding point in the picture
     */
    private void put(int i, int j, int pixel)
    {
        // If either of the indices are negative there is no data for this
        // pixel index
        if (i < 0 || j < 0) return;
        
        // Modify the bounding box if necessary
        if (i < this.minIIndex) this.minIIndex = i;
        if (i > this.maxIIndex) this.maxIIndex = i;
        if (j < this.minJIndex) this.minJIndex = j;
        if (j > this.maxJIndex) this.maxJIndex = j;
        
        // Get the information for this row (i.e. this y index),
        // creating a new row if necessary
        Row row = this.pixelMap.get(j);
        if (row == null)
        {
            row = new Row();
            this.pixelMap.put(j, row);
        }
        
        // Add the pixel to this row
        row.put(i, pixel);
    }
    
    /**
     * Returns true if this PixelMap does not contain any data: this will happen
     * if there is no intersection between the requested data and the data on disk.
     * @return true if this PixelMap does not contain any data: this will happen
     * if there is no intersection between the requested data and the data on disk
     */
    public boolean isEmpty()
    {
        return this.pixelMap.size() == 0;
    }
    
    /**
     * Gets the j indices of all rows in this pixel map
     * @return the Set of all j indices in this pixel map
     */
    public Set<Integer> getJIndices()
    {
        return this.pixelMap.keySet();
    }
    
    /**
     * Gets the i indices of all the data points in the given row that
     * are needed to make the final image.
     * @return the Set of all I indices in the given row
     * @throws IllegalArgumentException if there is no row with the given y index
     */
    public Set<Integer> getIIndices(int j)
    {
        return this.getRow(j).getIIndices().keySet();
    }
    
    /**
     * Gets the list of all pixel indices, representing individual points in the 
     * final image, that correspond with the given point in the source data.  A single
     * value from the source data might map to several pixels in the final image,
     * especially if we are "zoomed in".
     * @return a List of all pixel indices that correspond with the given i and
     * j index
     * @throws IllegalArgumentException if there is no row with the given j index
     * or if the given i index is not found in the row
     */
    public List<Integer> getPixelIndices(int i, int j)
    {
        Map<Integer, List<Integer>> row = this.getRow(j).getIIndices();
        if (!row.containsKey(i))
        {
            throw new IllegalArgumentException("The i index " + i +
                " was not found in the row with j index " + j);
        }
        return row.get(i);
    }
    
    /**
     * Gets the minimum i index in the row with the given j index
     * @return the minimum i index in the row with the given j index
     * @throws IllegalArgumentException if there is no row with the given y index
     */
    public int getMinIIndexInRow(int j)
    {
        return this.getRow(j).getMinIIndex();
    }
    
    /**
     * Gets the maximum i index in the row with the given j index
     * @return the maximum i index in the row with the given j index
     * @throws IllegalArgumentException if there is no row with the given y index
     */
    public int getMaxIIndexInRow(int j)
    {
        return this.getRow(j).getMaxIIndex();
    }
    
    /**
     * @return the row with the given j index
     * @throws IllegalArgumentException if there is no row with the given y index
     */
    private Row getRow(int j)
    {
        if (!this.pixelMap.containsKey(j))
        {
            throw new IllegalArgumentException("There is no row with j index " + j);
        }
        return this.pixelMap.get(j);
    }

    /**
     * Gets the minimum i index in the whole pixel map
     * @return the minimum i index in the whole pixel map
     */
    public int getMinIIndex()
    {
        return minIIndex;
    }

    /**
     * Gets the minimum j index in the whole pixel map
     * @return the minimum j index in the whole pixel map
     */
    public int getMinJIndex()
    {
        return minJIndex;
    }

    /**
     * Gets the maximum i index in the whole pixel map
     * @return the maximum i index in the whole pixel map
     */
    public int getMaxIIndex()
    {
        return maxIIndex;
    }

    /**
     * Gets the maximum j index in the whole pixel map
     * @return the maximum j index in the whole pixel map
     */
    public int getMaxJIndex()
    {
        return maxJIndex;
    }
    
    /**
     * Contains information about a particular row in the data
     */
    private class Row
    {
        // Maps i Indices to a list of pixel indices
        //             i        pixels
        private Map<Integer, List<Integer>> iIndices =
            new HashMap<Integer, List<Integer>>();
        // Min and max x Indices in this row
        private int minIIndex = Integer.MAX_VALUE;
        private int maxIIndex = -1;
        
        /**
         * Adds a mapping of an i index to a pixel index
         */
        public void put(int i, int pixel)
        {
            if (i < this.minIIndex) this.minIIndex = i;
            if (i > this.maxIIndex) this.maxIIndex = i;
            
            List<Integer> pixelIndices = this.iIndices.get(i);
            if (pixelIndices == null)
            {
                pixelIndices = new ArrayList<Integer>();
                this.iIndices.put(i, pixelIndices);
                // We have a new unique x-y pair
                numUniqueIJPairs++;
            }
            // Add the pixel index to the set
            pixelIndices.add(pixel);
        }

        public Map<Integer, List<Integer>> getIIndices()
        {
            return iIndices;
        }

        public int getMinIIndex()
        {
            return minIIndex;
        }

        public int getMaxIIndex()
        {
            return maxIIndex;
        }
    }

    /**
     * Gets the number of unique i-j pairs in this pixel map. When combined
     * with the size of the resulting image we can quantify the under- or
     * oversampling.  This is the number of data points that will be extracted
     * by the {@link PixelByPixelDataReader}.
     * @return the number of unique i-j pairs in this pixel map.
     */
    public int getNumUniqueIJPairs()
    {
        return numUniqueIJPairs;
    }
    
    /**
     * Gets the sum of the lengths of each row of data points,
     * i.e. sum(imax - imin + 1).  This is the number of data points that will
     * be extracted by the {@link DefaultDataReader}.
     * @return the sum of the lengths of each row of data points
     */
    public int getSumRowLengths()
    {
        int sumRowLengths = 0;
        for (Row row : this.pixelMap.values())
        {
            sumRowLengths += (row.getMaxIIndex() - row.getMinIIndex() + 1);
        }
        return sumRowLengths;
    }
    
    /**
     * Gets the size of the i-j bounding box that encompasses all data.  This is
     * the number of data points that will be extracted by the
     * {@link BoundingBoxDataReader}.
     * @return the size of the i-j bounding box that encompasses all data.
     */
    public int getBoundingBoxSize()
    {
        return (this.maxIIndex - this.minIIndex + 1) *
               (this.maxJIndex - this.minJIndex + 1);
    }
    
}
