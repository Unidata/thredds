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
package thredds.server.wms.util;

import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.VectorLayer;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.datareader.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;

import java.util.List;
import java.util.ArrayList;

import ucar.nc2.dt.GridDataset;

/**
 * Created by IntelliJ IDEA.
 * User: pmak
 * Date: May 13, 2008
 * Time: 10:56:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class LayerOps
{
    private static org.slf4j.Logger log =
              org.slf4j.LoggerFactory.getLogger( LayerOps.class );


    public static int getZIndex(String zValue, Layer layer)
        throws InvalidDimensionValueException
    {
        // Get the z value.  The default value is the first value in the array
        // of z values
        if (zValue == null)
        {
            // No value has been specified
            return layer.isZaxisPresent() ? layer.getDefaultZIndex() : -1;
        }
        // The user has specified a z value.  Check that we have a z axis
        if (!layer.isZaxisPresent())
        {
            return -1; // We ignore the given value
        }
        // Check to see if this is a single value (the
        // user hasn't requested anything of the form z1,z2 or start/stop/step)
        if (zValue.split(",").length > 1 || zValue.split("/").length > 1)
        {
            throw new InvalidDimensionValueException("elevation", zValue);
        }
        return layer.findZIndex(zValue);
    }

    /**
     * @return a List of indices along the time axis corresponding with the
     * requested TIME parameter.  If there is no time axis, this will return
     * a List with a single value of -1.
     */
    public static List<Integer> getTIndices(String timeString, Layer layer)
        throws WmsException
    {
        List<Integer> tIndices = new ArrayList<Integer>();
        if (layer.isTaxisPresent())
        {
            if (timeString == null)
            {
                // The default time is the last value along the axis
                // TODO: this should be the time closest to now
                tIndices.add(layer.getDefaultTIndex());
            }
            else
            {
                // Interpret the time specification
                for (String t : timeString.split(","))
                {
                    String[] startStop = t.split("/");
                    if (startStop.length == 1)
                    {
                        // This is a single time value
                        tIndices.add(layer.findTIndex(startStop[0]));
                    }
                    else if (startStop.length == 2)
                    {
                        // Use all time values from start to stop inclusive
                        tIndices.addAll(layer.findTIndices(startStop[0], startStop[1]));
                    }
                    else
                    {
                        throw new InvalidDimensionValueException("time", t);
                    }
                }
            }
        }
        else
        {
            // The variable has no time axis.  We ignore any provided TIME value.
            tIndices.add(-1); // Signifies a single frame with no particular time value
        }
        return tIndices;
    }


    public static List<float[]> readData(GridDataset gd, Layer layer, int tIndex, int zIndex,
        HorizontalGrid grid, /*TileCache tileCache, */UsageLogEntry usageLogEntry, DataReader reader)
        throws Exception
    {
        List<float[]> picData = new ArrayList<float[]>();
        if (layer instanceof VectorLayer)
        {
            VectorLayer vecLayer = (VectorLayer)layer;
            picData.add(readDataArray(gd, vecLayer.getEastwardComponent(), tIndex,
                zIndex, grid,/*, tileCache, */usageLogEntry, reader));
            picData.add(readDataArray(gd, vecLayer.getNorthwardComponent(), tIndex,
                zIndex, grid,/*, tileCache, */ usageLogEntry, reader));
        }
        else
        {
            picData.add(readDataArray(gd, layer, tIndex, zIndex, grid,/*, tileCache,*/
                usageLogEntry, reader));
        }
        return picData;
    }

    /**
     * Reads an array of data from a Layer that is <b>not</b> a VectorLayer.
     * @param usageLogEntry a UsageLogEntry that is used to collect information
     * about the usage of this WMS (may be null, e.g. if this is being called
     * from the MetadataLoader).
     */
    public static float[] readDataArray(GridDataset gd, Layer layer, int tIndex, int zIndex,
        HorizontalGrid grid,/*, TileCache tileCache, */UsageLogEntry usageLogEntry, DataReader reader)
        throws Exception
    {
        // Get a DataReader object for reading the data
        log.debug("Got data reader of type {}", reader.getClass().getName());

        // See exactly which file we're reading from, and which time index in
        // the file (handles datasets with glob aggregation)
        String filename;
        int tIndexInFile;
        if (tIndex >= 0)
        {
            TimestepInfo tInfo = layer.getTimesteps().get(tIndex);
            filename = tInfo.getFilename();
            tIndexInFile = tInfo.getIndexInFile();
        }
        else
        {
            // There is no time axis
            // TODO: this fails if there is a layer in the dataset which
            // has no time axis but the dataset is still a glob aggregation
            // (e.g. a bathymetry layer that is present in every file in the glob
            // aggregation, but has no time dependence).
            filename = layer.getDataset().getLocationURI().toString();
            tIndexInFile = tIndex;
        }
        float[] data = null;
        /*TileCacheKey key = null;
        if (tileCache != null)
        {
            // Check the cache to see if we've already extracted this data
            // The TileCacheKey class stores different information depending
            // on whether the file in question is a local file, an OPeNDAP
            // endpoint or an NcML aggregation (see the Javadoc for the TileCache
            // class)
            key = new TileCacheKey(filename, layer, grid, tIndexInFile, zIndex);
            // Try to get the data from cache
            data = tileCache.get(key);
        }            */
        if (data == null)
        {
            // Data not found in cache or cache not present
            data = reader.read(gd, layer, tIndexInFile, zIndex, grid);
            // Put the data into the cache
            //if (tileCache != null) tileCache.put(key, data);
            if (usageLogEntry != null) usageLogEntry.setUsedCache(false);
        }
        else
        {
            // Data were found in the cache
            if (usageLogEntry != null) usageLogEntry.setUsedCache(true);
        }

        return data;
    }


    public static float[] findMinMax(GridDataset gd, Layer layer, int tIndex, int zIndex,
            HorizontalGrid grid, UsageLogEntry usageLogEntry, DataReader reader)
            throws Exception
        {
            // Now read the data
            // TODO: should we use the tile cache here?
            List<float[]> picData = LayerOps.readData(gd, layer, tIndex, zIndex,
                grid, usageLogEntry, reader);

            // Now find the minimum and maximum values: for a vector this is the magnitude
            float min = Float.NaN;
            float max = Float.NaN;
            
            for (int i = 0; i < picData.get(0).length; i++)
            {

                float val = picData.get(0)[i];


                if (!Float.isNaN(val))
                {
                    if (picData.size() == 2)
                    {
                        // This is a vector quantity: calculate the magnitude
                        val = (float)Math.sqrt(val * val + picData.get(1)[i] * picData.get(1)[i]);
                    }

                    if (Float.isNaN(min) || val < min) min = val;
                    if (Float.isNaN(max) || val > max) max = val;
                }
            }
            
            return new float[]{min, max};
        }


}
