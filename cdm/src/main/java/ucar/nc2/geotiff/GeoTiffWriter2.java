package ucar.nc2.geotiff;

import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.geoloc.*;

import java.io.IOException;

/**
 * Not sure where this is used - IDV ??
 *
 * @author caron
 * @since 3/15/13
 */
public class GeoTiffWriter2 extends GeotiffWriter {

  public GeoTiffWriter2(String fileOut) {
    super(fileOut);
  }

  public void writeGrid(String gridDataset_filename, String gridName, int time, int level, boolean greyScale, LatLonRect pt) throws IOException {
    double scaler;
    try (GridDataset dataset = ucar.nc2.dt.grid.GridDataset.open(gridDataset_filename)) {
      GridDatatype grid = dataset.findGridDatatype(gridName);
      if (grid == null) {
        throw new IllegalArgumentException("No grid named " + gridName + " in fileName");
      }

      GridCoordSystem gcs = grid.getCoordinateSystem();
      ProjectionImpl proj = grid.getProjection();

      if (!gcs.isRegularSpatial()) {
        Attribute att = dataset.findGlobalAttributeIgnoreCase("datasetId");
        if (att != null && att.getStringValue().contains("DMSP")) {  // LOOK!!
          writeSwathGrid(gridDataset_filename, gridName, time, level, greyScale, pt);
          return;
        } else {
          throw new IllegalArgumentException("Must have 1D x and y axes for " + grid.getFullName());
        }

      }

      CoordinateAxis1D xaxis = (CoordinateAxis1D) gcs.getXHorizAxis();
      CoordinateAxis1D yaxis = (CoordinateAxis1D) gcs.getYHorizAxis();
      if (!xaxis.isRegular() || !yaxis.isRegular()) {
        throw new IllegalArgumentException("Must be evenly spaced grid = " + grid.getFullName());
      }

      // read in data
      Array data = grid.readDataSlice(time, level, -1, -1);
      Array lon = xaxis.read();
      Array lat = yaxis.read();

      //latlon coord does not need to time 1000.0
      if (gcs.isLatLon()) {
        scaler = 1.0;
      } else {
        scaler = 1000.0;
      }

      if (yaxis.getCoordValue(0) < yaxis.getCoordValue(1)) {
        data = data.flip(0);
        lat = lat.flip(0);
      }

      if (gcs.isLatLon()) {
        data = geoShiftDataAtLon(data, lon);
        lon = geoShiftLon(lon);
      }
      // now it is time to subset the data out of latlonrect
      // it is assumed that latlonrect pt is in +-180
      LatLonPointImpl llp0 = pt.getLowerLeftPoint();
      LatLonPointImpl llpn = pt.getUpperRightPoint();
      double minLon = llp0.getLongitude();
      double minLat = llp0.getLatitude();
      double maxLon = llpn.getLongitude();
      double maxLat = llpn.getLatitude();

      // (x1, y1) is upper left point and (x2, y2) is lower right point
      int x1;
      int x2;
      int y1;
      int y2;
      double xStart;
      double yStart;
      if (!gcs.isLatLon()) {

        ProjectionPoint pjp0 = proj.latLonToProj(maxLat, minLon);
        x1 = getXIndex(lon, pjp0.getX(), 0);
        y1 = getYIndex(lat, pjp0.getY(), 0);
        yStart = pjp0.getY() * 1000.0;  //latArray[y1];
        xStart = pjp0.getX() * 1000.0;  //lonArray[x1];
        ProjectionPoint pjpn = proj.latLonToProj(minLat, maxLon);
        x2 = getXIndex(lon, pjpn.getX(), 1);
        y2 = getYIndex(lat, pjpn.getY(), 1);

      } else {
        xStart = minLon;
        yStart = maxLat;
        x1 = getLonIndex(lon, minLon, 0);
        y1 = getLatIndex(lat, maxLat, 0);
        x2 = getLonIndex(lon, maxLon, 1);
        y2 = getLatIndex(lat, minLat, 1);
      }

      // data must go from top to bottom
      double xInc = xaxis.getIncrement() * scaler;
      double yInc = Math.abs(yaxis.getIncrement()) * scaler;

      // subsetting data inside the box
      Array data1 = getYXDataInBox(data, x1, x2, y1, y2);

      if (pageNumber > 1) {
        geotiff.initTags();
      }

      // write it out
      writeGrid(grid, data1, greyScale, xStart, yStart, xInc, yInc, pageNumber);
      pageNumber++;
    }
  }

  /**
   * Write Swath Grid data to the geotiff file.
   *
   * @param fileName  _more_
   * @param gridName  _more_
   * @param time      _more_
   * @param level     _more_
   * @param greyScale _more_
   * @param llr       _more_
   * @throws IOException _more_
   */
  void writeSwathGrid(String fileName, String gridName, int time, int level, boolean greyScale, LatLonRect llr) throws IOException {

    double scaler;
    GridDataset dataset = ucar.nc2.dt.grid.GridDataset.open(fileName);
    GridDatatype grid = dataset.findGridDatatype(gridName);
    GridCoordSystem gcs = grid.getCoordinateSystem();
    ProjectionImpl proj = grid.getProjection();

    CoordinateAxis2D xaxis = (CoordinateAxis2D) gcs.getXHorizAxis();
    CoordinateAxis2D yaxis = (CoordinateAxis2D) gcs.getYHorizAxis();

    // read in data
    Array data = grid.readDataSlice(time, level, -1, -1);
    Array lon = xaxis.read();
    Array lat = yaxis.read();

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
      lon = geoShiftLon(lon);
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

    if (llr == null)  { //get the whole area
      minLon = swathInfo[4];
      minLat = swathInfo[2];
      maxLon = swathInfo[5];
      maxLat = swathInfo[3];
      xStart = minLon;
      yStart = maxLat;
      x1 = 0;
      y1 = 0;
      x2 = (int) ((maxLon - minLon) / xInc + 0.5);
      y2 = (int) ((maxLat - minLat) / yInc + 0.5);

    } else {           //assign the special area  surrounded by the llr
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
      LatLonPointImpl pUpLeft = new LatLonPointImpl(swathInfo[3], swathInfo[4]);
      LatLonPointImpl pDownRight = new LatLonPointImpl(swathInfo[2], swathInfo[5]);
      LatLonRect swathLLR = new LatLonRect(pUpLeft, pDownRight);
      LatLonRect bIntersect = swathLLR.intersect(llr);
      if (bIntersect == null) {
        throw new IllegalArgumentException("The assigned extent of latitude and longitude is unvalid. No intersection with the swath extent");
      }

      xStart = minLon;
      yStart = maxLat;
      x1 = (int) ((minLon - swathInfo[4]) / xInc + 0.5);
      y1 = (int) Math.abs((maxLat - swathInfo[3]) / yInc + 0.5);
      x2 = (int) ((maxLon - swathInfo[4]) / xInc + 0.5);
      y2 = (int) Math.abs((minLat - swathInfo[3]) / yInc + 0.5);
    }

    if (!gcs.isLatLon()) {
      ProjectionPoint pjp0 = proj.latLonToProj(maxLat, minLon);
      x1 = getXIndex(lon, pjp0.getX(), 0);
      y1 = getYIndex(lat, pjp0.getY(), 0);
      yStart = pjp0.getY() * 1000.0;  //latArray[y1];
      xStart = pjp0.getX() * 1000.0;  //lonArray[x1];
      ProjectionPoint pjpn = proj.latLonToProj(minLat, maxLon);
      x2 = getXIndex(lon, pjpn.getX(), 1);
      y2 = getYIndex(lat, pjpn.getY(), 1);
    } else {
      //calculate the x1, x2, y1, y2, xstart, ystart.
    }

    Array targetImage = getTargetImagerFromSwath(lat, lon, data, swathInfo);
    Array interpolatedImage = interpolation(targetImage);
    Array clippedImage = getClippedImageFromInterpolation(interpolatedImage, x1, x2, y1, y2);
    //Array clippedImage = getYXDataInBox(interpolatedImage, x1, x2, y1, y2);

    if (pageNumber > 1) {
      geotiff.initTags();
    }

    writeGrid(grid, clippedImage, greyScale, xStart, yStart, xInc, yInc, pageNumber);
    pageNumber++;

  }

  int getXIndex(Array aAxis, double value, int side) {

    IndexIterator aIter = aAxis.getIndexIterator();
    int count = 0;
    int isInd = 0;

    double aValue = aIter.getFloatNext();
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



  int getYIndex(Array aAxis, double value, int side) {

    IndexIterator aIter = aAxis.getIndexIterator();
    int count = 0;
    int isInd = 0;

    double aValue = aIter.getFloatNext();
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

  int getLatIndex(Array lat, double value, int side) {
    IndexIterator latIter = lat.getIndexIterator();
    int count = 0;
    int isInd = 0;

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

  int getLonIndex(Array lon, double value, int side) {
    IndexIterator lonIter = lon.getIndexIterator();
    int count = 0;
    int isInd = 0;

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

  Array getYXDataInBox(Array data, int x1, int x2, int y1,
                              int y2) throws java.io.IOException {
    int rank = data.getRank();
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

  Array getClippedImageFromInterpolation(Array arr, int x1, int x2, int y1, int y2) {
    int[] srcShape = arr.getShape();
    int rank = arr.getRank();
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

    Array dataVolume = Array.factory(DataType.FLOAT, shape);
    int count = 0;
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

  Array getTargetImagerFromSwath(Array lat, Array lon, Array data, double[] swathInfo) {
    int srcDataHeight = data.getShape()[0];
    int srcDataWidth = data.getShape()[1];
    int BBoxHeight = (int) ((swathInfo[3] - swathInfo[2]) / swathInfo[1] + 0.5);
    int BBoxWidth = (int) ((swathInfo[5] - swathInfo[4]) / swathInfo[0] + 0.5);
    int BBoxShape[] = {BBoxHeight, BBoxWidth};  //[height, width]
    Array bBoxArray = Array.factory(DataType.FLOAT, BBoxShape);
    double startLon, startLat;  //upper left and lower right
    startLon = swathInfo[4];
    startLat = swathInfo[3];

    IndexIterator dataIter = data.getIndexIterator();
    IndexIterator latIter = lat.getIndexIterator();
    IndexIterator lonIter = lon.getIndexIterator();
    for (int i = 0; i < srcDataHeight; i++) {
      for (int j = 0; j < srcDataWidth; j++) {
        while (latIter.hasNext() && lonIter.hasNext()
                && dataIter.hasNext()) {
          float curLat = latIter.getFloatNext();
          float curLon = lonIter.getFloatNext();
          float curPix = dataIter.getFloatNext();
          float alreadyValue;
          int curPixelInBBoxIndex =
                  getIndexOfBBFromLatlonOfOri(startLat, startLon,
                          swathInfo[1], swathInfo[0], curLat, curLon,
                          BBoxHeight, BBoxWidth);
          try {
            alreadyValue = bBoxArray.getFloat(curPixelInBBoxIndex);
          } catch (Exception e) {
            alreadyValue = 0;
          }

          if (alreadyValue > 0) {  //This pixel had been filled. So calculate the average
            bBoxArray.setFloat(curPixelInBBoxIndex, (curPix + alreadyValue) / 2);
          } else {
            bBoxArray.setFloat(curPixelInBBoxIndex, curPix);
          }
        }
      }
    }
    return bBoxArray;
  }

  int getIndexOfBBFromLatlonOfOri(double sLat, double sLon,  //LatLon of the start point
                                  double latInc, double lonInc,  //The increment in Lat/Lon direction
                                  double curLat, double curLon,  //The current Lat/Lon read from the swath image
                                  int bbHeight, int bbWidth)  //The width and height of target image
  {
    double lonDelta = Math.abs((curLon - sLon) / lonInc);
    double latDelta = Math.abs((curLat - sLat) / latInc);

    int row = (int) (lonDelta + 0.5);
    if (row >= bbWidth - 1) {
      row = bbWidth - 2;
    }
    if (row == 0) {
      row = 1;
    }

    int col = (int) (latDelta + 0.5);

    int index = col * bbWidth + row;

    if (index >= bbHeight * bbWidth) {
      index = (col - 1) * bbWidth + row;
    }

    return index;
  }

  /**
   * interpolate the swath data to regular grid
   *
   * @param arr swath array
   * @return regular grid
   */
  Array interpolation(Array arr) {
    int[] orishape = arr.getShape();
    int width = orishape[1];
    int height = orishape[0];
    int pixelNum = width * height;
    Array interpolatedArray = Array.factory(DataType.FLOAT, orishape);
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        int curIndex = i * width + j;
        float curValue = arr.getFloat(curIndex);
        if (curValue == 0)  //Black hole. Need to fill.
        {
          float tempPixelSum = 0;
          int numNeighborHasValue = 0;
          //Get the values of eight neighborhood
          if ((curIndex - 1 >= 0) && (curIndex - 1 < pixelNum)) {
            float left = arr.getFloat(curIndex - 1);
            if (left > 0) {
              tempPixelSum += left;
              numNeighborHasValue++;
            }
          }
          if ((curIndex + 1 >= 0) && (curIndex + 1 < pixelNum)) {
            float right = arr.getFloat(curIndex + 1);
            if (right > 0) {
              tempPixelSum += right;
              numNeighborHasValue++;
            }
          }
          if ((curIndex - width >= 0) && (curIndex - width < pixelNum)) {
            float up = arr.getFloat(curIndex - width);
            if (up > 0) {
              tempPixelSum += up;
              numNeighborHasValue++;
            }
          }
          if ((curIndex + width >= 0) && (curIndex + width < pixelNum)) {
            float down = arr.getFloat(curIndex + width);
            if (down > 0) {
              tempPixelSum += down;
              numNeighborHasValue++;
            }
          }
          if ((curIndex - width - 1 >= 0) && (curIndex - width - 1 < pixelNum)) {
            float upleft = arr.getFloat(curIndex - width - 1);
            if (upleft > 0) {
              tempPixelSum += upleft;
              numNeighborHasValue++;
            }
          }
          if ((curIndex - width + 1 >= 0) && (curIndex - width + 1 < pixelNum)) {
            float upright = arr.getFloat(curIndex - width + 1);
            if (upright > 0) {
              tempPixelSum += upright;
              numNeighborHasValue++;
            }
          }
          if ((curIndex + width - 1 >= 0) && (curIndex + width - 1 < pixelNum)) {
            float downleft = arr.getFloat(curIndex + width - 1);
            if (downleft > 0) {
              tempPixelSum += downleft;
              numNeighborHasValue++;
            }
          }
          if ((curIndex + width + 1 >= 0) && (curIndex + width + 1 < pixelNum)) {
            float downright = arr.getFloat(curIndex + width + 1);
            if (downright > 0) {
              tempPixelSum += downright;
              numNeighborHasValue++;
            }
          }
          if (tempPixelSum > 0) {
            float val =  numNeighborHasValue == 0 ? 0 : tempPixelSum / numNeighborHasValue;
            interpolatedArray.setFloat(curIndex, val);
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
   * @param lat 2D lat array
   * @param lon 2D lon array
   * @return double array for geotiff
   */
  double[] getSwathLatLonInformation(Array lat, Array lon) {
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
    double increment[] = {0, 0, 0, 0, 0, 0, 0, 0};
    IndexIterator latIter = lat.getIndexIterator();
    IndexIterator lonIter = lon.getIndexIterator();

    int numScan = (lat.getShape())[0];
    int numSample = (lat.getShape())[1];

    double maxLat = -91,
            minLat = 91,
            maxLon = -181,
            minLon = 181;

    float firstLineStartLat = 0;
    float firstLineStartLon = 0;
    float firstLineEndLat = 0;
    float firstLineEndLon = 0;
    float lastLineStartLat = 0;
    float lastLineStartLon = 0;
    float lastLineEndLat = 0;
    float lastLineEndLon = 0;

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
    double[] edgeLat = {firstLineStartLat, firstLineEndLat, lastLineStartLat, lastLineEndLat};
    double[] edgeLon = {firstLineStartLon, firstLineEndLon, lastLineStartLon, lastLineEndLon};
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

    double xInc1 = Math.abs((firstLineEndLon - firstLineStartLon) / numSample);
    //double xInc2 = Math.abs((lastLineEndLon - lastLineStartLon)/numSample);
    double yInc1 = Math.abs((lastLineStartLat - firstLineStartLat) / numScan);
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

  private Array geoShiftLon(Array lon) {
    int count = 0;
    Index lonIndex = lon.getIndex();
    int[] lonShape = lon.getShape();
    ArrayFloat slon = new ArrayFloat(new int[]{lonShape[0]});
    Index slonIndex = slon.getIndex();
    IndexIterator lonIter = lon.getIndexIterator();
    LatLonPoint p0 = new LatLonPointImpl(0, lon.getFloat(lonIndex.set(lonShape[0] - 1)));
    LatLonPoint pN = new LatLonPointImpl(0, lon.getFloat(lonIndex.set(0)));

    while (lonIter.hasNext()) {
      float l = lonIter.getFloatNext();
      if (l > 180.0) {
        count++;
      }
    }

    //checking if the 0 point and the N point are the same point
    int spoint;
    if (p0.getLongitude() == pN.getLongitude()) {
      spoint = lonShape[0] - count - 1;
    } else {
      spoint = lonShape[0] - count;
    }

    if ((count > 0) && (lonShape[0] > count)) {
      for (int j = 1; j < lonShape[0]; j++) {
        int jj;
        if (j >= count) {
          jj = j - count;
        } else {
          jj = j + spoint;
        }

        float dd = lon.getFloat(lonIndex.set(jj));
        slon.setFloat(slonIndex.set(j), (float) LatLonPointImpl.lonNormal(dd));
      }

      if (p0.getLongitude() == pN.getLongitude()) {
        float dd = slon.getFloat(slonIndex.set(lonShape[0] - 1));
        slon.setFloat(slonIndex.set(0), -(float) LatLonPointImpl.lonNormal(dd));
      }
      return slon;

    } else {
      return lon;
    }
  }
}
