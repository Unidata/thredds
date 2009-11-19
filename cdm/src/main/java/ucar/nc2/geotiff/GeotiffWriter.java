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

package ucar.nc2.geotiff;


import ucar.ma2.*;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.Attribute;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.geoloc.projection.proj4.AlbersEqualAreaEllipse;

import java.io.IOException;


/**
 * Write GeoTIFF files.
 *
 * @author caron, yuan
 */
public class GeotiffWriter {

    /** _more_          */
    private String fileOut;

    /** _more_          */
    private GeoTiff geotiff;

    /** _more_          */
    private short pageNumber = 1;

    /**
     * Geotiff writer.
     *
     * @param fileOut name of output file.
     */
    public GeotiffWriter(String fileOut) {
        this.fileOut = fileOut;
        geotiff      = new GeoTiff(fileOut);
    }

    /**
     * Write Grid data to the geotiff file.
     *
     * @param dataset grid in contained in this dataset
     * @param grid data is in this grid
     * @param data      2D array in YX order
     * @param greyScale if true, write greyScale image, else dataSample.
     * @throws IOException on i/o error
     */
    public void writeGrid(GridDataset dataset, GridDatatype grid, Array data,
                          boolean greyScale) throws IOException {
        GridCoordSystem gcs = grid.getCoordinateSystem();

        if ( !gcs.isRegularSpatial()) {
            throw new IllegalArgumentException(
                "Must have 1D x and y axes for " + grid.getName());
        }

        CoordinateAxis1D xaxis = (CoordinateAxis1D) gcs.getXHorizAxis();
        CoordinateAxis1D yaxis = (CoordinateAxis1D) gcs.getYHorizAxis();

        //latlon coord does not need to be scaled
        double scaler = (gcs.isLatLon())
                        ? 1.0
                        : 1000.0;

        // data must go from top to bottom LOOK IS THIS REALLY NEEDED ?
        double xStart = xaxis.getCoordValue(0) * scaler;
        double yStart = yaxis.getCoordValue(0) * scaler;
        double xInc   = xaxis.getIncrement() * scaler;
        double yInc   = Math.abs(yaxis.getIncrement()) * scaler;

        if (yaxis.getCoordValue(0) < yaxis.getCoordValue(1)) {
            data   = data.flip(0);
            yStart = yaxis.getCoordValue((int) yaxis.getSize() - 1) * scaler;
        }

        if (gcs.isLatLon()) {
            Array lon = xaxis.read();
            data   = geoShiftDataAtLon(data, lon);
            xStart = geoShiftGetXstart(lon, xInc);
            //xStart = -180.0;
        }

        if ( !xaxis.isRegular() || !yaxis.isRegular()) {
            throw new IllegalArgumentException(
                "Must be evenly spaced grid = " + grid.getName());
        }

        if (pageNumber > 1) {
            geotiff.initTags();
        }

        // write it out
        writeGrid(grid, data, greyScale, xStart, yStart, xInc, yInc,
                  pageNumber);
        pageNumber++;
    }

    /**
     * Write Grid data to the geotiff file.
     *
     * @param fileName _more_
     * @param gridName _more_
     * @param time _more_
     * @param level _more_
     * @param greyScale _more_
     * @param pt _more_
     *
     * @throws IOException _more_
     */
    public void writeGrid(String fileName, String gridName, int time,
                          int level, boolean greyScale,
                          LatLonRect pt) throws IOException {
        double          scaler;
        GridDataset     dataset = ucar.nc2.dt.grid.GridDataset.open(fileName);
        GridDatatype    grid    = dataset.findGridDatatype(gridName);
        GridCoordSystem gcs     = grid.getCoordinateSystem();
        ProjectionImpl  proj    = grid.getProjection();

        if (grid == null) {
            throw new IllegalArgumentException("No grid named " + gridName
                    + " in fileName");
        }
        if ( !gcs.isRegularSpatial()) {
            Attribute att = dataset.findGlobalAttributeIgnoreCase("datasetId");
            if(att != null && att.getStringValue().contains("DMSP")){
                writeSwathGrid(fileName, gridName,time,level, greyScale, pt);
                return;
            } else {
                throw new IllegalArgumentException(
                    "Must have 1D x and y axes for " + grid.getName());
            }

        }

        CoordinateAxis1D xaxis = (CoordinateAxis1D) gcs.getXHorizAxis();
        CoordinateAxis1D yaxis = (CoordinateAxis1D) gcs.getYHorizAxis();
        if ( !xaxis.isRegular() || !yaxis.isRegular()) {
            throw new IllegalArgumentException(
                "Must be evenly spaced grid = " + grid.getName());
        }

        // read in data
        Array data = grid.readDataSlice(time, level, -1, -1);
        Array lon  = xaxis.read();
        Array lat  = yaxis.read();

        //latlon coord does not need to time 1000.0
        if (gcs.isLatLon()) {
            scaler = 1.0;
        } else {
            scaler = 1000.0;
        }

        if (yaxis.getCoordValue(0) < yaxis.getCoordValue(1)) {
            data = data.flip(0);
            lat  = lat.flip(0);
        }

        if (gcs.isLatLon()) {
            data = geoShiftDataAtLon(data, lon);
            lon  = geoShiftLon(lon);
        }
        // now it is time to subset the data out of latlonrect
        // it is assumed that latlonrect pt is in +-180
        LatLonPointImpl llp0   = pt.getLowerLeftPoint();
        LatLonPointImpl llpn   = pt.getUpperRightPoint();
        double          minLon = llp0.getLongitude();
        double          minLat = llp0.getLatitude();
        double          maxLon = llpn.getLongitude();
        double          maxLat = llpn.getLatitude();

        // (x1, y1) is upper left point and (x2, y2) is lower right point
        int    x1;
        int    x2;
        int    y1;
        int    y2;
        double xStart;
        double yStart;
        if ( !gcs.isLatLon()) {

            ProjectionPoint pjp0     = proj.latLonToProj(maxLat, minLon);
            double[]        lonArray = (double[]) lon.copyTo1DJavaArray();
            double[]        latArray = (double[]) lat.copyTo1DJavaArray();
            x1     = getXIndex(lon, pjp0.getX(), 0);
            y1     = getYIndex(lat, pjp0.getY(), 0);
            yStart = pjp0.getY() * 1000.0;  //latArray[y1];
            xStart = pjp0.getX() * 1000.0;  //lonArray[x1];
            ProjectionPoint pjpn = proj.latLonToProj(minLat, maxLon);
            x2 = getXIndex(lon, pjpn.getX(), 1);
            y2 = getYIndex(lat, pjpn.getY(), 1);

        } else {
            xStart = minLon;
            yStart = maxLat;
            x1     = getLonIndex(lon, minLon, 0);
            y1     = getLatIndex(lat, maxLat, 0);
            x2     = getLonIndex(lon, maxLon, 1);
            y2     = getLatIndex(lat, minLat, 1);
        }
        // data must go from top to bottom LOOK IS THIS REALLY NEEDED ?

        double xInc = xaxis.getIncrement() * scaler;
        double yInc = Math.abs(yaxis.getIncrement()) * scaler;

        // subseting data inside the box
        Array data1 = getYXDataInBox(data, x1, x2, y1, y2);

        if (pageNumber > 1) {
            geotiff.initTags();
        }

        // write it out
        writeGrid(grid, data1, greyScale, xStart, yStart, xInc, yInc,
                  pageNumber);
        pageNumber++;

    }

    /**
     * Write Swath Grid data to the geotiff file.
     *
     * @param fileName _more_
     * @param gridName _more_
     * @param time _more_
     * @param level _more_
     * @param greyScale _more_
     * @param llr _more_
     *
     * @throws IOException _more_
     */
    public void writeSwathGrid(String fileName, String gridName, int time,
                               int level, boolean greyScale,
                               LatLonRect llr) throws IOException {

        double           scaler;
        GridDataset      dataset =
            ucar.nc2.dt.grid.GridDataset.open(fileName);
        GridDatatype     grid    = dataset.findGridDatatype(gridName);
        GridCoordSystem  gcs     = grid.getCoordinateSystem();
        ProjectionImpl   proj    = grid.getProjection();

        CoordinateAxis2D xaxis   = (CoordinateAxis2D) gcs.getXHorizAxis();
        CoordinateAxis2D yaxis   = (CoordinateAxis2D) gcs.getYHorizAxis();

        // read in data
        Array    data      = grid.readDataSlice(time, level, -1, -1);
        Array    lon       = xaxis.read();
        Array    lat       = yaxis.read();

        double[] swathInfo = getSwathLatLonInformation(lat, lon);

        //latlon coord does not need to time 1000.0
        if (gcs.isLatLon()) {
            scaler = 1.0;
        } else {
            scaler = 1000.0;
        }

        //if (yaxis.getCoordValue(0, 0) < yaxis.getCoordValue(0, 1)) {//???
        data = data.flip(0);
        //lat = lat.flip(0);
        //}

        if (gcs.isLatLon()) {
            data = geoShiftDataAtLon(data, lon);
            lon  = geoShiftLon(lon);
        }

        double minLon;
        double minLat;
        double maxLon;
        double maxLat;
        double xStart;
        double yStart;  //upper right point

        double xInc = swathInfo[0] * scaler;
        double yInc = swathInfo[1] * scaler;

        // (x1, y1) is upper left point and (x2, y2) is lower right point
        int x1;
        int x2;
        int y1;
        int y2;

        if (llr == null)  //get the whole area
        {
            minLon = swathInfo[4];
            minLat = swathInfo[2];
            maxLon = swathInfo[5];
            maxLat = swathInfo[3];
            xStart = minLon;
            yStart = maxLat;
            x1     = 0;
            y1     = 0;
            x2     = (int) ((maxLon - minLon) / xInc + 0.5);
            y2     = (int) ((maxLat - minLat) / yInc + 0.5);
        } else            //assign the special area  surrounded by the llr
        {
            LatLonPointImpl llp0 = llr.getLowerLeftPoint();
            LatLonPointImpl llpn = llr.getUpperRightPoint();
            minLon = (llp0.getLongitude() < swathInfo[4])
                     ? swathInfo[4]
                     : llp0.getLongitude();
            minLat = (llp0.getLatitude() < swathInfo[2])
                     ? swathInfo[2]
                     : llp0.getLatitude();
            maxLon = (llpn.getLongitude() > swathInfo[5])
                     ? swathInfo[5]
                     : llpn.getLongitude();
            maxLat = (llpn.getLatitude() > swathInfo[3])
                     ? swathInfo[3]
                     : llpn.getLatitude();

            //construct the swath  LatLonRect
            LatLonPointImpl pUpLeft = new LatLonPointImpl(swathInfo[3],
                                          swathInfo[4]);
            LatLonPointImpl pDownRight = new LatLonPointImpl(swathInfo[2],
                                             swathInfo[5]);
            LatLonRect swathLLR   = new LatLonRect(pUpLeft, pDownRight);
            LatLonRect bIntersect = swathLLR.intersect(llr);
            if (bIntersect == null) {
                throw new IllegalArgumentException(
                    "The assigned extent of latitude and longitude is unvalid. "
                    + "No intersection with the swath extent");
            }

            xStart = minLon;
            yStart = maxLat;
            x1     = (int) ((minLon - swathInfo[4]) / xInc + 0.5);
            y1     = (int) Math.abs((maxLat - swathInfo[3]) / yInc + 0.5);
            x2     = (int) ((maxLon - swathInfo[4]) / xInc + 0.5);
            y2     = (int) Math.abs((minLat - swathInfo[3]) / yInc + 0.5);
        }

        if ( !gcs.isLatLon()) {
            ProjectionPoint pjp0 = proj.latLonToProj(maxLat, minLon);
            x1     = getXIndex(lon, pjp0.getX(), 0);
            y1     = getYIndex(lat, pjp0.getY(), 0);
            yStart = pjp0.getY() * 1000.0;  //latArray[y1];
            xStart = pjp0.getX() * 1000.0;  //lonArray[x1];
            ProjectionPoint pjpn = proj.latLonToProj(minLat, maxLon);
            x2 = getXIndex(lon, pjpn.getX(), 1);
            y2 = getYIndex(lat, pjpn.getY(), 1);
        } else {
            //calculate the x1, x2, y1, y2, xstart, ystart.
        }

        Array targetImage = getTargetImagerFromSwath(lat, lon, data,
                                swathInfo);
        Array interpolatedImage = interpolation(targetImage);
        Array clippedImage =
            getClippedImageFrominterpolation(interpolatedImage, x1, x2, y1,
                                             y2);
        //Array clippedImage = getYXDataInBox(interpolatedImage, x1, x2, y1, y2);

        if (pageNumber > 1) {
            geotiff.initTags();
        }

        writeGrid(grid, clippedImage, greyScale, xStart, yStart, xInc, yInc,
                  pageNumber);
        pageNumber++;

    }

    /**
     * _more_
     *
     * @param aAxis _more_
     * @param value _more_
     * @param side _more_
     *
     * @return _more_
     */
    int getXIndex(Array aAxis, double value, int side) {

        IndexIterator aIter  = aAxis.getIndexIterator();
        int           count  = 0;
        int           isInd  = 0;

        double        aValue = aIter.getFloatNext();
        if ((aValue == value) || (aValue > value)) {
            return 0;
        }

        while (aIter.hasNext() && (aValue < value)) {
            count++;
            aValue = aIter.getFloatNext();
            if (aValue == value) {
                isInd = 1;
            }
        }
        if (isInd == 1) {
            count += side;
        }
        count -= side;

        return count;
    }

    /**
     * _more_
     *
     * @param aAxis _more_
     * @param value _more_
     * @param side _more_
     *
     * @return _more_
     */
    int getYIndex(Array aAxis, double value, int side) {

        IndexIterator aIter  = aAxis.getIndexIterator();
        int           count  = 0;
        int           isInd  = 0;

        double        aValue = aIter.getFloatNext();
        if ((aValue == value) || (aValue < value)) {
            return 0;
        }

        while (aIter.hasNext() && (aValue > value)) {
            count++;
            aValue = aIter.getFloatNext();
            if (aValue == value) {
                isInd = 1;
            }
        }

        if (isInd == 1) {
            count += side;
        }
        count -= side;
        return count;
    }



    /**
     * _more_
     *
     * @param lat _more_
     * @param value _more_
     * @param side _more_
     *
     * @return _more_
     */
    int getLatIndex(Array lat, double value, int side) {
        int[]         shape   = lat.getShape();
        IndexIterator latIter = lat.getIndexIterator();
        Index         ind     = lat.getIndex();
        int           count   = 0;
        int           isInd   = 0;

        //LatLonPoint p0 = new LatLonPointImpl(lat.getFloat(ind.set(0)), 0);

        double xlat = latIter.getFloatNext();
        if (xlat == value) {
            return 0;
        }

        while (latIter.hasNext() && (xlat > value)) {
            count++;
            xlat = latIter.getFloatNext();
            if (xlat == value) {
                isInd = 1;
            }
        }

        if (isInd == 1) {
            count += side;
        }
        count -= side;
        return count;
    }

    /**
     * _more_
     *
     * @param lon _more_
     * @param value _more_
     * @param side _more_
     *
     * @return _more_
     */
    int getLonIndex(Array lon, double value, int side) {
        int[]         shape   = lon.getShape();
        IndexIterator lonIter = lon.getIndexIterator();
        Index         ind     = lon.getIndex();
        int           count   = 0;
        int           isInd   = 0;

        // double xlon = lon.getFloat(ind.set(0));
        float xlon = lonIter.getFloatNext();
        if (xlon > 180) {
            xlon = xlon - 360;
        }
        if (xlon == value) {
            return 0;
        }

        while (lonIter.hasNext() && (xlon < value)) {
            count++;
            xlon = lonIter.getFloatNext();
            if (xlon > 180) {
                xlon = xlon - 360;
            }
            if (xlon == value) {
                isInd = 1;
            }
        }

        if (isInd == 1) {
            count += side;
        }
        count -= side;
        return count;
    }

    /**
     * _more_
     *
     * @param data _more_
     * @param x1 _more_
     * @param x2 _more_
     * @param y1 _more_
     * @param y2 _more_
     *
     * @return _more_
     *
     * @throws java.io.IOException _more_
     */
    public Array getYXDataInBox(Array data, int x1, int x2, int y1,
                                int y2) throws java.io.IOException {
        int   rank  = data.getRank();
        int[] start = new int[rank];
        int[] shape = new int[rank];
        for (int i = 0; i < rank; i++) {
            start[i] = 0;
            shape[i] = 1;
        }

        if ((y1 >= 0) && (y2 >= 0)) {
            start[0] = y1;
            shape[0] = y2 - y1;
        }
        if ((x1 >= 0) && (x2 >= 0)) {
            start[1] = x1;
            shape[1] = x2 - x1;
        }

        // read it
        Array dataVolume;
        try {
            dataVolume = data.section(start, shape);
        } catch (Exception e) {
            throw new java.io.IOException(e.getMessage());
        }

        return dataVolume;
    }


    /**
     * _more_
     *
     * @param arr _more_
     * @param x1 _more_
     * @param x2 _more_
     * @param y1 _more_
     * @param y2 _more_
     *
     * @return _more_
     */
    public Array getClippedImageFrominterpolation(Array arr, int x1, int x2,
            int y1, int y2) {
        int[] srcShape = arr.getShape();
        int   rank     = arr.getRank();
        int[] start    = new int[rank];
        int[] shape    = new int[rank];
        for (int i = 0; i < rank; i++) {
            start[i] = 0;
            shape[i] = 1;
        }

        if ((y1 >= 0) && (y2 >= 0)) {
            start[0] = y1;
            shape[0] = y2 - y1;
        }
        if ((x1 >= 0) && (x2 >= 0)) {
            start[1] = x1;
            shape[1] = x2 - x1;
        }

        Array dataVolume = Array.factory(DataType.FLOAT, shape);
        int   count      = 0;
        for (int i = y1; i < y2; i++) {
            for (int j = x1; j < x2; j++) {
                int index = i * srcShape[1] + j;
                if (index >= srcShape[0] * srcShape[1]) {
                    index = srcShape[0] * srcShape[1] - 1;
                }
                float curValue = arr.getFloat(index);
                if (count >= shape[0] * shape[1]) {
                    count = shape[0] * shape[1] - 1;
                }
                dataVolume.setFloat(count, curValue);
                count++;
            }
        }
        return dataVolume;
    }

    /**
     * get the grid dataset
     *
     * @param lat _more_
     * @param lon _more_
     * @param data _more_
     * @param swathInfo _more_
     *
     * @return _more_
     */
    public Array getTargetImagerFromSwath(Array lat, Array lon, Array data,
                                          double[] swathInfo) {
        int srcDataHeight = data.getShape()[0];
        int srcDataWidth  = data.getShape()[1];
        int BBoxHeight = (int) ((swathInfo[3] - swathInfo[2]) / swathInfo[1]
                                + 0.5);
        int BBoxWidth = (int) ((swathInfo[5] - swathInfo[4]) / swathInfo[0]
                               + 0.5);
        int    BBoxShape[] = { BBoxHeight, BBoxWidth };  //[height, width]
        Array  bBoxArray   = Array.factory(DataType.FLOAT, BBoxShape);
        double startLon, startLat;  //upper left and lower right
        startLon = swathInfo[4];
        startLat = swathInfo[3];

        IndexIterator dataIter = data.getIndexIterator();
        IndexIterator latIter  = lat.getIndexIterator();
        IndexIterator lonIter  = lon.getIndexIterator();
        for (int i = 0; i < srcDataHeight; i++) {
            for (int j = 0; j < srcDataWidth; j++) {
                while (latIter.hasNext() && lonIter.hasNext()
                        && dataIter.hasNext()) {
                    float curLat       = latIter.getFloatNext();
                    float curLon       = lonIter.getFloatNext();
                    float curPix       = dataIter.getFloatNext();
                    float alreadyValue = 0;
                    int curPixelInBBoxIndex =
                        getIndexOfBBFromLatlonOfOri(startLat, startLon,
                            swathInfo[1], swathInfo[0], curLat, curLon,
                            BBoxHeight, BBoxWidth);
                    try {
                        alreadyValue =
                            bBoxArray.getFloat(curPixelInBBoxIndex);
                    } catch (Exception e) {
                        alreadyValue = 0;
                    }

                    if (alreadyValue > 0) {  //This pixel had been filled. So calculate the average
                        bBoxArray.setFloat(curPixelInBBoxIndex,
                                           (curPix + alreadyValue) / 2);
                    } else {
                        bBoxArray.setFloat(curPixelInBBoxIndex, curPix);
                    }
                }
            }
        }
        return bBoxArray;
    }

    /**
     * _more_
     *
     * @param sLat _more_
     * @param sLon _more_
     * @param latInc _more_
     * @param lonInc _more_
     * @param curLat _more_
     * @param curLon _more_
     * @param bbHeight _more_
     * @param bbWidth _more_
     *
     * @return _more_
     */
    int getIndexOfBBFromLatlonOfOri(double sLat, double sLon,  //LatLon of the start point
                                    double latInc, double lonInc,  //The increment in Lat/Lon direction
                                    double curLat, double curLon,  //The current Lat/Lon read from the swath image
                                    int bbHeight, int bbWidth)  //The width and height of target image
                                    {
        double lonDelta = Math.abs((curLon - sLon) / lonInc);
        double latDelta = Math.abs((curLat - sLat) / latInc);

        int    row      = (int) (lonDelta + 0.5);
        if (row >= bbWidth - 1) {
            row = bbWidth - 2;
        }
        if (row == 0) {
            row = 1;
        }

        int col   = (int) (latDelta + 0.5);

        int index = col * bbWidth + row;

        if (index >= bbHeight * bbWidth) {
            index = (col - 1) * bbWidth + row;
        }

        return index;
    }

    /**
     * interpolate the swath data to regular grid
     *
     * @param arr _more_
     *
     * @return _more_
     */
    public Array interpolation(Array arr) {
        int[] orishape          = arr.getShape();
        int   width             = orishape[1];
        int   height            = orishape[0];
        int   pixelNum          = width * height;
        Array interpolatedArray = Array.factory(DataType.FLOAT, orishape);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int   curIndex = i * width + j;
                float curValue = arr.getFloat(curIndex);
                if (curValue == 0)  //Black hole. Need to fill.
                {
                    float tempPixelSum        = 0;
                    int   numNeighborHasValue = 0;
                    float left                = 0;
                    float right               = 0;
                    float up                  = 0;
                    float down                = 0;
                    float upleft              = 0;
                    float upright             = 0;
                    float downleft            = 0;
                    float downright           = 0;
                    //Get the values of eight neighborhood
                    if ((curIndex - 1 >= 0) && (curIndex - 1 < pixelNum)) {
                        left = arr.getFloat(curIndex - 1);
                        if (left > 0) {
                            tempPixelSum += left;
                            numNeighborHasValue++;
                        }
                    }
                    if ((curIndex + 1 >= 0) && (curIndex + 1 < pixelNum)) {
                        right = arr.getFloat(curIndex + 1);
                        if (right > 0) {
                            tempPixelSum += right;
                            numNeighborHasValue++;
                        }
                    }
                    if ((curIndex - width >= 0)
                            && (curIndex - width < pixelNum)) {
                        up = arr.getFloat(curIndex - width);
                        if (up > 0) {
                            tempPixelSum += up;
                            numNeighborHasValue++;
                        }
                    }
                    if ((curIndex + width >= 0)
                            && (curIndex + width < pixelNum)) {
                        down = arr.getFloat(curIndex + width);
                        if (down > 0) {
                            tempPixelSum += down;
                            numNeighborHasValue++;
                        }
                    }
                    if ((curIndex - width - 1 >= 0)
                            && (curIndex - width - 1 < pixelNum)) {
                        upleft = arr.getFloat(curIndex - width - 1);
                        if (upleft > 0) {
                            tempPixelSum += upleft;
                            numNeighborHasValue++;
                        }
                    }
                    if ((curIndex - width + 1 >= 0)
                            && (curIndex - width + 1 < pixelNum)) {
                        upright = arr.getFloat(curIndex - width + 1);
                        if (upright > 0) {
                            tempPixelSum += upright;
                            numNeighborHasValue++;
                        }
                    }
                    if ((curIndex + width - 1 >= 0)
                            && (curIndex + width - 1 < pixelNum)) {
                        downleft = arr.getFloat(curIndex + width - 1);
                        if (downleft > 0) {
                            tempPixelSum += downleft;
                            numNeighborHasValue++;
                        }
                    }
                    if ((curIndex + width + 1 >= 0)
                            && (curIndex + width + 1 < pixelNum)) {
                        downright = arr.getFloat(curIndex + width + 1);
                        if (downright > 0) {
                            tempPixelSum += downright;
                            numNeighborHasValue++;
                        }
                    }
                    if (tempPixelSum > 0) {
                        interpolatedArray.setFloat(curIndex,
                                tempPixelSum / numNeighborHasValue);
                    }
                } else {
                    interpolatedArray.setFloat(curIndex, curValue);
                }
            }
        }

        return interpolatedArray;
    }



    /**
     * get lat lon information from the swath
     *
     * @param lat _more_
     * @param lon _more_
     *
     * @return _more_
     */
    public double[] getSwathLatLonInformation(Array lat, Array lon) {
        // Calculate the increment of latitude and longitude of original swath data
        // Calculate the size of the boundingBox
        // element0: Longitude increment
        // element1: Latitude increment
        // element2: minLat
        // element3: maxLat
        // element4: minLon
        // element5: maxLon
        // element6: width of the boundingBox
        // element7: height of the boundingBox
        double        increment[]       = {
            0, 0, 0, 0, 0, 0, 0, 0
        };
        IndexIterator latIter           = lat.getIndexIterator();
        IndexIterator lonIter           = lon.getIndexIterator();

        int           numScan           = (lat.getShape())[0];
        int           numSample         = (lat.getShape())[1];

        double        maxLat            = -91,
                      minLat            = 91,
                      maxLon            = -181,
                      minLon            = 181;

        float         firstLineStartLat = 0;
        float         firstLineStartLon = 0;
        float         firstLineEndLat   = 0;
        float         firstLineEndLon   = 0;
        float         lastLineStartLat  = 0;
        float         lastLineStartLon  = 0;
        float         lastLineEndLat    = 0;
        float         lastLineEndLon    = 0;

        for (int i = 0; i < numScan; i++) {
            for (int j = 0; j < numSample; j++) {
                if (latIter.hasNext() && lonIter.hasNext()) {
                    float curLat = latIter.getFloatNext();
                    float curLon = lonIter.getFloatNext();
                    if ((i == 0) && (j == 0)) {
                        firstLineStartLat = curLat;
                        firstLineStartLon = curLon;
                    } else if ((i == 0) && (j == numSample - 1)) {
                        firstLineEndLat = curLat;
                        firstLineEndLon = curLon;
                    } else if ((i == numScan - 1) && (j == 0)) {
                        lastLineStartLat = curLat;
                        lastLineStartLon = curLon;
                    } else if ((i == numScan - 1) && (j == numSample - 1)) {
                        lastLineEndLat = curLat;
                        lastLineEndLon = curLon;
                    }
                }
            }
        }
        double[] edgeLat = { firstLineStartLat, firstLineEndLat,
                             lastLineStartLat, lastLineEndLat };
        double[] edgeLon = { firstLineStartLon, firstLineEndLon,
                             lastLineStartLon, lastLineEndLon };
        for (int i = 0; i < edgeLat.length; i++) {
            maxLat = ((maxLat > edgeLat[i])
                      ? maxLat
                      : edgeLat[i]);
            minLat = ((minLat < edgeLat[i])
                      ? minLat
                      : edgeLat[i]);
            maxLon = ((maxLon > edgeLon[i])
                      ? maxLon
                      : edgeLon[i]);
            minLon = ((minLon < edgeLon[i])
                      ? minLon
                      : edgeLon[i]);
        }

        double xInc1 = Math.abs((firstLineEndLon - firstLineStartLon)
                                / numSample);
        //double xInc2 = Math.abs((lastLineEndLon - lastLineStartLon)/numSample);
        double yInc1 = Math.abs((lastLineStartLat - firstLineStartLat)
                                / numScan);
        //double yInc2 = Math.abs((lastLineEndLat - firstLineEndLat)/numScan);
        increment[0] = xInc1;  // > xInc2 ? xInc1 : xInc2;
        increment[1] = yInc1;  // > yInc2 ? yInc1 : yInc2;
        increment[2] = minLat;
        increment[3] = maxLat;
        increment[4] = minLon;
        increment[5] = maxLon;
        increment[6] = (maxLon - minLon) / xInc1;
        increment[7] = (maxLat - minLat) / yInc1;
        return increment;
    }

    /**
     * Write Grid data to the geotiff file.
     * Grid currently must:
     * <ol>
     * <li> have a 1D X and Y coordinate axes.
     * <li> be lat/lon or Lambert Conformal Projection
     * <li> be equally spaced
     * </ol>
     *
     * @param grid        original grid
     * @param data        2D array in YX order
     * @param greyScale   if true, write greyScale image, else dataSample.
     * @param xStart
     * @param yStart
     * @param xInc
     * @param yInc
     * @param imageNumber
     * @throws IOException on i/o error
     * @throws IllegalArgumentException if above assumptions not valid
     */
    public void writeGrid(GridDatatype grid, Array data, boolean greyScale,
                          double xStart, double yStart, double xInc,
                          double yInc, int imageNumber) throws IOException {

        int             nextStart = 0;
        GridCoordSystem gcs       = grid.getCoordinateSystem();

        // get rid of this when all projections are implemented
        if ( !gcs.isLatLon()
                && !(gcs.getProjection() instanceof LambertConformal)
                && !(gcs.getProjection() instanceof Stereographic)
                && !(gcs.getProjection() instanceof Mercator)
        //  && !(gcs.getProjection() instanceof TransverseMercator)
                && !(gcs.getProjection() instanceof AlbersEqualAreaEllipse)
        && !(gcs.getProjection() instanceof AlbersEqualArea)) {
            throw new IllegalArgumentException(
                "Must be lat/lon or LambertConformal or Mercator and grid = "
                + gcs.getProjection().getClass().getName());
        }

        // write the data first
        if (greyScale) {
            ArrayByte result = replaceMissingValuesAndScale(grid, data);
            nextStart = geotiff.writeData((byte[]) result.getStorage(),
                                          imageNumber);
        } else {
            ArrayFloat result = replaceMissingValues(grid, data);
            nextStart = geotiff.writeData((float[]) result.getStorage(),
                                          imageNumber);
        }

        // set the width and the height
        int elemSize = greyScale
                       ? 1
                       : 4;
        int height   = data.getShape()[0];         // Y
        int width    = data.getShape()[1];         // X
        int size     = elemSize * height * width;  // size in bytes
        geotiff.addTag(new IFDEntry(Tag.ImageWidth,
                                    FieldType.SHORT).setValue(width));
        geotiff.addTag(new IFDEntry(Tag.ImageLength,
                                    FieldType.SHORT).setValue(height));

        // set the multiple images tag
        int ff   = 1 << 1;
        int page = imageNumber - 1;
        geotiff.addTag(new IFDEntry(Tag.NewSubfileType,
                                    FieldType.SHORT).setValue(ff));
        geotiff.addTag(new IFDEntry(Tag.PageNumber,
                                    FieldType.SHORT).setValue(page, 2));

        // just make it all one big "row"
        geotiff.addTag(new IFDEntry(Tag.RowsPerStrip,
                                    FieldType.SHORT).setValue(1));  //height));
        // the following changes to make it viewable in ARCMAP
        /*
        geotiff.addTag( new IFDEntry(Tag.StripByteCounts, FieldType.LONG).setValue( size));
        // data starts here, header is written at the end
        if( imageNumber == 1 )
          geotiff.addTag( new IFDEntry(Tag.StripOffsets, FieldType.LONG).setValue( 8));
        else
          geotiff.addTag( new IFDEntry(Tag.StripOffsets, FieldType.LONG).setValue(nextStart));
        */
        int[] soffset    = new int[height];
        int[] sbytecount = new int[height];
        if (imageNumber == 1) {
            soffset[0] = 8;
        } else {
            soffset[0] = nextStart;
        }
        sbytecount[0] = width * elemSize;
        for (int i = 1; i < height; i++) {
            soffset[i]    = soffset[i - 1] + width * elemSize;
            sbytecount[i] = width * elemSize;
        }
        geotiff.addTag(new IFDEntry(Tag.StripByteCounts, FieldType.LONG,
                                    width).setValue(sbytecount));
        geotiff.addTag(new IFDEntry(Tag.StripOffsets, FieldType.LONG,
                                    width).setValue(soffset));
        // standard tags
        geotiff.addTag(new IFDEntry(Tag.Orientation,
                                    FieldType.SHORT).setValue(1));
        geotiff.addTag(new IFDEntry(Tag.Compression,
                                    FieldType.SHORT).setValue(1));  // no compression
        geotiff.addTag(new IFDEntry(Tag.Software,
                                    FieldType.ASCII).setValue("nc2geotiff"));
        geotiff.addTag(new IFDEntry(Tag.PhotometricInterpretation,
                                    FieldType.SHORT).setValue(1));  // black is zero : not used?
        geotiff.addTag(new IFDEntry(Tag.PlanarConfiguration,
                                    FieldType.SHORT).setValue(1));

        if (greyScale) {
            // standard tags for Greyscale images ( see TIFF spec, section 4)
            geotiff.addTag(new IFDEntry(Tag.BitsPerSample,
                                        FieldType.SHORT).setValue(8));  // 8 bits per sample
            geotiff.addTag(new IFDEntry(Tag.SamplesPerPixel,
                                        FieldType.SHORT).setValue(1));

            geotiff.addTag(new IFDEntry(Tag.XResolution,
                                        FieldType.RATIONAL).setValue(1, 1));
            geotiff.addTag(new IFDEntry(Tag.YResolution,
                                        FieldType.RATIONAL).setValue(1, 1));
            geotiff.addTag(new IFDEntry(Tag.ResolutionUnit,
                                        FieldType.SHORT).setValue(1));

        } else {
            // standard tags for SampleFormat ( see TIFF spec, section 19)
            geotiff.addTag(new IFDEntry(Tag.BitsPerSample,
                                        FieldType.SHORT).setValue(32));  // 32 bits per sample
            geotiff.addTag(new IFDEntry(Tag.SampleFormat,
                                        FieldType.SHORT).setValue(3));  // Sample Format
            geotiff.addTag(new IFDEntry(Tag.SamplesPerPixel,
                                        FieldType.SHORT).setValue(1));
            MAMath.MinMax dataMinMax = grid.getMinMaxSkipMissingData(data);
            float         min        = (float) (dataMinMax.min);
            float         max        = (float) (dataMinMax.max);
            geotiff.addTag(new IFDEntry(Tag.SMinSampleValue,
                                        FieldType.FLOAT).setValue(min));
            geotiff.addTag(new IFDEntry(Tag.SMaxSampleValue,
                                        FieldType.FLOAT).setValue(max));
            geotiff.addTag(new IFDEntry(Tag.GDALNoData,
                                        FieldType.FLOAT).setValue(min - 1.f));
        }

        /*
              geotiff.addTag( new IFDEntry(Tag.Geo_ModelPixelScale, FieldType.DOUBLE).setValue(
                new double[] {5.0, 2.5, 0.0} ));
              geotiff.addTag( new IFDEntry(Tag.Geo_ModelTiepoint, FieldType.DOUBLE).setValue(
                new double[] {0.0, 0.0, 0.0, -180.0, 90.0, 0.0 } ));
              //  new double[] {0.0, 0.0, 0.0, 183.0, 90.0, 0.0} ));
              IFDEntry ifd = new IFDEntry(Tag.Geo_KeyDirectory, FieldType.SHORT).setValue(
                new int[] {1, 1, 0, 4, 1024, 0, 1, 2, 1025, 0, 1, 1, 2048, 0, 1, 4326, 2054, 0, 1, 9102} );
              geotiff.addTag( ifd);
        */

        // set the transformation from projection to pixel, add tie point tag
        geotiff.setTransform(xStart, yStart, xInc, yInc);

        if (gcs.isLatLon()) {
            addLatLonTags();
        } else if (gcs.getProjection() instanceof LambertConformal) {
            addLambertConformalTags((LambertConformal) gcs.getProjection(),
                                    xStart, yStart);
        } else if (gcs.getProjection() instanceof Stereographic) {
            addPolarStereographicTags((Stereographic) gcs.getProjection(),
                                      xStart, yStart);
        } else if (gcs.getProjection() instanceof Mercator) {
            addMercatorTags((Mercator) gcs.getProjection());
        } else if (gcs.getProjection() instanceof TransverseMercator) {
            addTransverseMercatorTags(
                (TransverseMercator) gcs.getProjection());
        } else if (gcs.getProjection() instanceof AlbersEqualArea) {
            addAlbersEqualAreaTags((AlbersEqualArea) gcs.getProjection());
        } else if (gcs.getProjection() instanceof AlbersEqualAreaEllipse) {
            addAlbersEqualAreaEllipseTags((AlbersEqualAreaEllipse) gcs.getProjection());
        }

        geotiff.writeMetadata(imageNumber);
        //geotiff.close();

    }

    /**
     * _more_
     *
     * @throws IOException _more_
     */
    public void close() throws IOException {
        geotiff.close();
    }

    /**
     * Replace missing values with dataMinMax.min - 1.0; return a floating point data array.
     *
     * @param grid GridDatatype
     * @param data input data array
     * @return floating point data array with missing values replaced.
     */
    private ArrayFloat replaceMissingValues(GridDatatype grid, Array data) {
        MAMath.MinMax dataMinMax = grid.getMinMaxSkipMissingData(data);
        float         minValue   = (float) (dataMinMax.min - 1.0);

        ArrayFloat floatArray = (ArrayFloat) Array.factory(float.class,
                                    data.getShape());
        IndexIterator dataIter  = data.getIndexIterator();
        IndexIterator floatIter = floatArray.getIndexIterator();
        while (dataIter.hasNext()) {
            float v = dataIter.getFloatNext();
            if (grid.isMissingData((double) v)) {
                v = minValue;
            }
            floatIter.setFloatNext(v);
        }

        return floatArray;
    }

    /**
     * Replace missing values with 0; scale other values between 1 and 255, return a byte data array.
     *
     * @param grid GridDatatype
     * @param data input data array
     * @return byte data array with missing values replaced and data scaled from 1- 255.
     */
    private ArrayByte replaceMissingValuesAndScale(GridDatatype grid,
            Array data) {
        MAMath.MinMax dataMinMax = grid.getMinMaxSkipMissingData(data);
        double        scale      = 254.0 / (dataMinMax.max - dataMinMax.min);

        ArrayByte byteArray = (ArrayByte) Array.factory(byte.class,
                                  data.getShape());
        IndexIterator dataIter   = data.getIndexIterator();
        IndexIterator resultIter = byteArray.getIndexIterator();

        byte          bv;
        while (dataIter.hasNext()) {
            double v = dataIter.getDoubleNext();
            if (grid.isMissingData(v)) {
                bv = 0;
            } else {
                int iv = (int) ((v - dataMinMax.min) * scale + 1);
                bv = (byte) (iv & 0xff);
            }
            resultIter.setByteNext(bv);
        }

        return byteArray;
    }

    /**
     * _more_
     */
    private void addLatLonTags1() {
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey,
                                     GeoKey.TagValue.ModelType_Geographic));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogGeodeticDatumGeoKey,
                                     GeoKey.TagValue.GeogGeodeticDatum6267));

    }

    /**
     * _more_
     */
    private void addLatLonTags() {
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey,
                                     GeoKey.TagValue.ModelType_Geographic));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey,
                                     GeoKey.TagValue.RasterType_Area));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey,
                                     GeoKey.TagValue.GeographicType_WGS_84));
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.GeogPrimeMeridianGeoKey,
                GeoKey.TagValue.GeogPrimeMeridian_GREENWICH));
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.GeogAngularUnitsGeoKey,
                GeoKey.TagValue.GeogAngularUnits_DEGREE));
    }

    /**
     * _more_
     *
     * @param proj _more_
     * @param FalseEasting _more_
     * @param FalseNorthing _more_
     */
    private void addPolarStereographicTags(Stereographic proj,
                                           double FalseEasting,
                                           double FalseNorthing) {
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey,
                                     GeoKey.TagValue.ModelType_Projected));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey,
                                     GeoKey.TagValue.RasterType_Area));

        // define the "geographic Coordinate System"
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey,
                                     GeoKey.TagValue.GeographicType_WGS_84));
        //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogPrimeMeridianGeoKey, GeoKey.TagValue.GeogPrimeMeridian_GREENWICH));
        //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogAngularUnitsGeoKey, GeoKey.TagValue.GeogAngularUnits_DEGREE));

        // define the "coordinate transformation"
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjectedCSTypeGeoKey,
                GeoKey.TagValue.ProjectedCSType_UserDefined));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.PCSCitationGeoKey, "Snyder"));
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjectionGeoKey,
                GeoKey.TagValue.ProjectedCSType_UserDefined));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjLinearUnitsGeoKey,
                                     GeoKey.TagValue.ProjLinearUnits_METER));
        //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

        // the specifics for Polar Stereographic
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjCoordTransGeoKey,
                GeoKey.TagValue.ProjCoordTrans_Stereographic));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjCenterLongGeoKey, 0.0));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLatGeoKey,
                                     90.0));
        //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjNatOriginLongGeoKey, proj.getTangentLon()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjScaleAtNatOriginGeoKey,
                                     1.0));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseNorthingGeoKey,
                                     0.0));


    }

    /**
     * _more_
     *
     * @param proj _more_
     * @param FalseEasting _more_
     * @param FalseNorthing _more_
     */
    private void addLambertConformalTags(LambertConformal proj,
                                         double FalseEasting,
                                         double FalseNorthing) {
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey,
                                     GeoKey.TagValue.ModelType_Projected));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey,
                                     GeoKey.TagValue.RasterType_Area));

        // define the "geographic Coordinate System"
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey,
                                     GeoKey.TagValue.GeographicType_WGS_84));
        //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogPrimeMeridianGeoKey, GeoKey.TagValue.GeogPrimeMeridian_GREENWICH));
        //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogAngularUnitsGeoKey, GeoKey.TagValue.GeogAngularUnits_DEGREE));

        // define the "coordinate transformation"
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjectedCSTypeGeoKey,
                GeoKey.TagValue.ProjectedCSType_UserDefined));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.PCSCitationGeoKey, "Snyder"));
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjectionGeoKey,
                GeoKey.TagValue.ProjectedCSType_UserDefined));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjLinearUnitsGeoKey,
                                     GeoKey.TagValue.ProjLinearUnits_METER));
        //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

        // the specifics for lambert conformal
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjCoordTransGeoKey,
                GeoKey.TagValue.ProjCoordTrans_LambertConfConic_2SP));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel1GeoKey,
                                     proj.getParallelOne()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel2GeoKey,
                                     proj.getParallelTwo()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjCenterLongGeoKey,
                                     proj.getOriginLon()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLatGeoKey,
                                     proj.getOriginLat()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLongGeoKey,
                                     proj.getOriginLon()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjScaleAtNatOriginGeoKey,
                                     1.0));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));  // LOOK why not FalseNorthing ??
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseNorthingGeoKey,
                                     0.0));


    }

    /**
     * _more_
     *
     * @param proj _more_
     */
    private void addMercatorTags(Mercator proj) {
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey,
                                     GeoKey.TagValue.ModelType_Projected));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey,
                                     GeoKey.TagValue.RasterType_Area));

        // define the "geographic Coordinate System"
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey,
                                     GeoKey.TagValue.GeographicType_WGS_84));
        // geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogLinearUnitsGeoKey, GeoKey.TagValue.ProjLinearUnits_METER));
        // geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogAngularUnitsGeoKey, GeoKey.TagValue.GeogAngularUnits_DEGREE));

        // define the "coordinate transformation"
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjectedCSTypeGeoKey,
                GeoKey.TagValue.ProjectedCSType_UserDefined));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.PCSCitationGeoKey,
                                     "Mercator"));
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjectionGeoKey,
                GeoKey.TagValue.ProjectedCSType_UserDefined));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjLinearUnitsGeoKey,
                                     GeoKey.TagValue.ProjLinearUnits_METER));
        //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

        // the specifics for mercator
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjCoordTransGeoKey,
                GeoKey.TagValue.ProjCoordTrans_Mercator));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLongGeoKey,
                                     proj.getOriginLon()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLatGeoKey,
                                     proj.getParallel()));
        //    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel1GeoKey, proj.getParallel()));
        //   geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjScaleAtNatOriginGeoKey, 1));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseNorthingGeoKey,
                                     0.0));


    }

    /**
     * _more_
     *
     * @param proj _more_
     */
    private void addTransverseMercatorTags(TransverseMercator proj) {
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey,
                                     GeoKey.TagValue.ModelType_Projected));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey,
                                     GeoKey.TagValue.RasterType_Area));

        // define the "geographic Coordinate System"
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey,
                                     GeoKey.TagValue.GeographicType_WGS_84));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogLinearUnitsGeoKey,
                                     GeoKey.TagValue.ProjLinearUnits_METER));
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.GeogAngularUnitsGeoKey,
                GeoKey.TagValue.GeogAngularUnits_DEGREE));

        // define the "coordinate transformation"
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjectedCSTypeGeoKey,
                GeoKey.TagValue.ProjectedCSType_UserDefined));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.PCSCitationGeoKey,
                                     "Transvers Mercator"));
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjectionGeoKey,
                GeoKey.TagValue.ProjectedCSType_UserDefined));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjLinearUnitsGeoKey,
                                     GeoKey.TagValue.ProjLinearUnits_METER));
        //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

        // the specifics for mercator
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjCoordTransGeoKey,
                GeoKey.TagValue.ProjCoordTrans_TransverseMercator));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLatGeoKey,
                                     proj.getOriginLat()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLongGeoKey,
                                     proj.getTangentLon()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjScaleAtNatOriginGeoKey,
                                     proj.getScale()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjScaleAtNatOriginGeoKey,
                                     1.0));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseNorthingGeoKey,
                                     0.0));


    }

    /**
     * _more_
     *
     * @param proj _more_
     */

    private void addAlbersEqualAreaEllipseTags(AlbersEqualAreaEllipse proj) {
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey,
                                     GeoKey.TagValue.ModelType_Projected));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey,
                                     GeoKey.TagValue.RasterType_Area));

        // define the "geographic Coordinate System"
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey,
                                     GeoKey.TagValue.GeographicType_WGS_84));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogLinearUnitsGeoKey,
                                     GeoKey.TagValue.ProjLinearUnits_METER));

        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogSemiMajorAxisGeoKey,
                                      proj.getEarth().getMajor()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogSemiMinorAxisGeoKey,
                                      proj.getEarth().getMinor()));

        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.GeogAngularUnitsGeoKey,
                GeoKey.TagValue.GeogAngularUnits_DEGREE));

        // define the "coordinate transformation"
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjectedCSTypeGeoKey,
                GeoKey.TagValue.ProjectedCSType_UserDefined));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.PCSCitationGeoKey,
                                     "Albers Conial Equal Area"));
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjectionGeoKey,
                GeoKey.TagValue.ProjectedCSType_UserDefined));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjLinearUnitsGeoKey,
                                     GeoKey.TagValue.ProjLinearUnits_METER));
        //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

        // the specifics for mercator
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjCoordTransGeoKey,
                GeoKey.TagValue.ProjCoordTrans_AlbersEqualAreaEllipse));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLatGeoKey,
                                     proj.getOriginLat()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLongGeoKey,
                                     proj.getOriginLon()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel1GeoKey,
                                     proj.getParallelOne()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel2GeoKey,
                                     proj.getParallelTwo()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseNorthingGeoKey,
                                     0.0));
    }
    /**
     * _more_
     *
     * @param proj _more_
     */
    private void addAlbersEqualAreaTags(AlbersEqualArea proj) {
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey,
                                     GeoKey.TagValue.ModelType_Projected));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey,
                                     GeoKey.TagValue.RasterType_Area));

        // define the "geographic Coordinate System"
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey,
                                     GeoKey.TagValue.GeographicType_WGS_84));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogLinearUnitsGeoKey,
                                     GeoKey.TagValue.ProjLinearUnits_METER));
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.GeogAngularUnitsGeoKey,
                GeoKey.TagValue.GeogAngularUnits_DEGREE));

        // define the "coordinate transformation"
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjectedCSTypeGeoKey,
                GeoKey.TagValue.ProjectedCSType_UserDefined));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.PCSCitationGeoKey,
                                     "Albers Conial Equal Area"));
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjectionGeoKey,
                GeoKey.TagValue.ProjectedCSType_UserDefined));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjLinearUnitsGeoKey,
                                     GeoKey.TagValue.ProjLinearUnits_METER));
        //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

        // the specifics for mercator
        geotiff.addGeoKey(
            new GeoKey(
                GeoKey.Tag.ProjCoordTransGeoKey,
                GeoKey.TagValue.ProjCoordTrans_AlbersConicalEqualArea));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLatGeoKey,
                                     proj.getOriginLat()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLongGeoKey,
                                     proj.getOriginLon()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel1GeoKey,
                                     proj.getParallelOne()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel2GeoKey,
                                     proj.getParallelTwo()));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));
        geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseNorthingGeoKey,
                                     0.0));

    }


    /**
     * _more_
     *
     * @param data _more_
     * @param col _more_
     */
    private void dump(Array data, int col) {
        int[] shape = data.getShape();
        Index ima   = data.getIndex();

        for (int j = 0; j < shape[0]; j++) {
            float dd = data.getFloat(ima.set(j, col));
            System.out.println(j + " value= " + dd);
        }
    }

    /**
     * _more_
     *
     * @param lon _more_
     * @param inc _more_
     *
     * @return _more_
     */
    private double geoShiftGetXstart(Array lon, double inc) {
        int           count    = 0;
        Index         ilon     = lon.getIndex();
        int[]         lonShape = lon.getShape();
        IndexIterator lonIter  = lon.getIndexIterator();
        double        xlon     = 0.0;

        LatLonPoint   p0 = new LatLonPointImpl(0, lon.getFloat(ilon.set(0)));
        LatLonPoint pN =
            new LatLonPointImpl(0, lon.getFloat(ilon.set(lonShape[0] - 1)));

        xlon = p0.getLongitude();
        while (lonIter.hasNext()) {
            float       l  = lonIter.getFloatNext();
            LatLonPoint pn = new LatLonPointImpl(0, l);
            if (pn.getLongitude() < xlon) {
                xlon = pn.getLongitude();
            }
        }

        if (p0.getLongitude() == pN.getLongitude()) {
            xlon = xlon - inc;
        }

        return xlon;
    }

    /**
     * _more_
     *
     * @param data _more_
     * @param lon _more_
     *
     * @return _more_
     */
    private Array geoShiftDataAtLon(Array data, Array lon) {
        int           count    = 0;
        int[]         shape    = data.getShape();
        Index         ima      = data.getIndex();
        Index         ilon     = lon.getIndex();
        int[]         lonShape = lon.getShape();
        ArrayFloat    adata = new ArrayFloat(new int[] { shape[0],
                                  shape[1] });
        Index         imaa     = adata.getIndex();
        IndexIterator lonIter  = lon.getIndexIterator();

        LatLonPoint p0 =
            new LatLonPointImpl(0, lon.getFloat(ilon.set(lonShape[0] - 1)));
        LatLonPoint pN = new LatLonPointImpl(0, lon.getFloat(ilon.set(0)));

        while (lonIter.hasNext()) {
            float l = lonIter.getFloatNext();
            if (l > 180.0) {
                count++;
            }
        }

        //checking if the 0 point and the N point are the same point
        int spoint = 0;
        if (p0.getLongitude() == pN.getLongitude()) {
            spoint = shape[1] - count - 1;
        } else {
            spoint = shape[1] - count;
        }

        if ((count > 0) && (shape[1] > count)) {
            for (int j = 1; j < shape[1]; j++) {
                int jj = 0;

                if (j >= count) {
                    jj = j - count;
                } else {
                    jj = j + spoint;
                }

                for (int i = 0; i < shape[0]; i++) {
                    float dd = data.getFloat(ima.set(i, jj));
                    adata.setFloat(imaa.set(i, j), dd);
                }
            }

            if (p0.getLongitude() == pN.getLongitude()) {
                for (int i = 0; i < shape[0]; i++) {
                    float dd = adata.getFloat(imaa.set(i, shape[1] - 1));
                    adata.setFloat(imaa.set(i, 0), dd);
                }
            }
            return adata;

        } else {
            return data;
        }
    }

    /**
     * _more_
     *
     * @param lon _more_
     *
     * @return _more_
     */
    private Array geoShiftLon(Array lon) {
        int             count     = 0;
        Index           lonIndex  = lon.getIndex();
        int[]           lonShape  = lon.getShape();
        ArrayFloat      slon      = new ArrayFloat(new int[] { lonShape[0] });
        Index           slonIndex = slon.getIndex();
        IndexIterator   lonIter   = lon.getIndexIterator();
        LatLonPointImpl llp       = new LatLonPointImpl();
        LatLonPoint p0 =
            new LatLonPointImpl(0, lon.getFloat(lonIndex.set(lonShape[0]
                - 1)));
        LatLonPoint pN =
            new LatLonPointImpl(0, lon.getFloat(lonIndex.set(0)));

        while (lonIter.hasNext()) {
            float l = lonIter.getFloatNext();
            if (l > 180.0) {
                count++;
            }
        }

        //checking if the 0 point and the N point are the same point
        int spoint = 0;
        if (p0.getLongitude() == pN.getLongitude()) {
            spoint = lonShape[0] - count - 1;
        } else {
            spoint = lonShape[0] - count;
        }

        if ((count > 0) && (lonShape[0] > count)) {
            for (int j = 1; j < lonShape[0]; j++) {
                int jj = 0;
                if (j >= count) {
                    jj = j - count;
                } else {
                    jj = j + spoint;
                }

                float dd = lon.getFloat(lonIndex.set(jj));
                slon.setFloat(slonIndex.set(j),
                              (float) LatLonPointImpl.lonNormal(dd));

            }

            if (p0.getLongitude() == pN.getLongitude()) {
                float dd = slon.getFloat(slonIndex.set(lonShape[0] - 1));
                slon.setFloat(slonIndex.set(0),
                              -(float) LatLonPointImpl.lonNormal(dd));
            }
            return slon;

        } else {
            return lon;
        }
    }

    /**
     * test
     *
     * @param args _more_
     *
     * @throws IOException _more_
     */
    public static void main(String args[]) throws IOException {
       String fileOut = "/home/yuanho/Download/F15_s.tmp_new.tif";
        //String fileOut = "/home/yuanho/tmp/tmbF.tif";
        //LatLonPointImpl p1 = new LatLonPointImpl(38.0625, -80.6875);
        //LatLonPointImpl p2 = new LatLonPointImpl(47.8125, -67.0625);
       LatLonPointImpl p1 = new LatLonPointImpl(-5, -52.0);
       LatLonPointImpl p2 = new LatLonPointImpl(25, -20.0);
       LatLonRect llr = new LatLonRect(p1, p2);
       GeotiffWriter writer = new GeotiffWriter(fileOut);
        //writer.writeGrid("radar.nc", "noice_wat", 0, 0, true);
        //writer.writeGrid("dods://www.cdc.noaa.gov/cgi-bin/nph-nc/Datasets/coads/2degree/enh/cldc.mean.nc?lat[40:1:50],lon[70:1:110],time[2370:1:2375],cldc[2370:1:2375][40:1:50][70:1:110]", "cldc", 0, 0,true);
        //writer.writeGrid("dods://www.cdc.noaa.gov/cgi-bin/nph-nc/Datasets/noaa.oisst.v2/sst.mnmean.nc", "sst", 0, 0,false);
        //writer.writeGrid("2003091116_ruc2.nc", "P_sfc", 0, 0, false);
        //writer.writeGrid("/home/yuanho/dev/netcdf-java/geotiff/2003072918_avn-x.nc", "P_sfc", 0, 0, false,llr);
        //writer.writeGrid("/home/yuanho/tmp/NE_1961-1990_Yearly_Max_Temp.nc", "tmax", 0, 0, false, llr);
        // writer.writeGrid("/home/yuanho/tmp/TMB.nc", "MBchla", 0, 0, false, llr);
        writer.writeGrid("/home/yuanho/GIS/DataAndCode/F15_s.tmp", "infraredImagery", 0, 0, true, llr);
        writer.close();

        // read it back in
        GeoTiff geotiff = new GeoTiff(fileOut);
        geotiff.read();
        System.out.println("geotiff read in = " + geotiff.showInfo());
        //geotiff.testReadData();
        geotiff.close();
    }

}

