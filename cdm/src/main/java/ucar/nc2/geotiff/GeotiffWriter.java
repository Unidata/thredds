// $Id:GeotiffWriter.java 63 2006-07-12 21:50:51Z edavis $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.geotiff;

import ucar.ma2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import java.io.*;

/**
 *
 * @author caron, yuan
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */
public class GeotiffWriter {
  private String fileOut;
  private GeoTiff geotiff;
  private short pageNumber = 1;

  /**
   * Geotiff writer.
   * @param fileOut name of output file.
   */
  public GeotiffWriter(String fileOut) {
    this.fileOut = fileOut;
    geotiff = new GeoTiff(fileOut);
  }

  /**
   * Write Grid data to the geotiff file.
   *
   * @param dataset
   * @param grid
   * @param data 2D array in YX order
   * @param greyScale if true, write greyScale image, else dataSample.
   * @throws IOException
   */
  public void writeGrid(GridDataset dataset, GridDatatype grid, Array data, boolean greyScale) throws IOException {
    GridCoordSystem gcs = grid.getCoordinateSystem();

    if (!gcs.isRegularSpatial())
      throw new IllegalArgumentException("Must have 1D x and y axes for "+ grid.getName());

    CoordinateAxis1D xaxis = (CoordinateAxis1D) gcs.getXHorizAxis();
    CoordinateAxis1D yaxis = (CoordinateAxis1D) gcs.getYHorizAxis();

    //latlon coord does not need to be scaled
    double scaler = (gcs.isLatLon()) ? 1.0 : 1000.0;

    // data must go from top to bottom LOOK IS THIS REALLY NEEDED ?
    double xStart = xaxis.getCoordValue(0) * scaler;
    double yStart = yaxis.getCoordValue(0) * scaler;
    double xInc = xaxis.getIncrement() * scaler;
    double yInc = Math.abs(yaxis.getIncrement()) * scaler;

    if (yaxis.getCoordValue(0) < yaxis.getCoordValue(1)) {
      data = data.flip(0);
      yStart = yaxis.getCoordValue((int)yaxis.getSize()-1) * scaler;
    }

    if (gcs.isLatLon()) {
      Array lon = xaxis.read();
      data = geoShiftDataAtLon(data, lon);
      xStart = geoShiftGetXstart(lon, xInc);
      //xStart = -180.0;
    }

    if (!xaxis.isRegular() || !yaxis.isRegular())
      throw new IllegalArgumentException("Must be evenly spaced grid = "+ grid.getName());

    if (pageNumber > 1)
       geotiff.initTags();

    // write it out
    writeGrid(grid, data, greyScale, xStart, yStart, xInc, yInc, pageNumber);
    pageNumber++;
  }

  public void writeGrid(String fileName, String gridName, int time, int level, boolean greyScale, LatLonRect pt) throws IOException {
    double scaler;
    GridDataset dataset = ucar.nc2.dt.grid.GridDataset.open(fileName);
    GridDatatype grid = dataset.findGridDatatype( gridName);
    GridCoordSystem gcs = grid.getCoordinateSystem();

    if (grid == null)
      throw new IllegalArgumentException("No grid named "+ gridName+" in fileName");
    if (!gcs.isRegularSpatial())
      throw new IllegalArgumentException("Must have 1D x and y axes for "+ grid.getName());

    CoordinateAxis1D xaxis = (CoordinateAxis1D) gcs.getXHorizAxis();
    CoordinateAxis1D yaxis = (CoordinateAxis1D) gcs.getYHorizAxis();
    if (!xaxis.isRegular() || !yaxis.isRegular())
      throw new IllegalArgumentException("Must be evenly spaced grid = "+ grid.getName());

    // read in data
    Array data = grid.readDataSlice(time, level, -1, -1);
    Array lon = xaxis.read();
    Array lat = yaxis.read();

    //latlon coord does not need to time 1000.0
    if (gcs.isLatLon()) scaler = 1.0;
    else scaler = 1000.0;

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
    int x1 = getLonIndex(lon, minLon, 0);
    int y1 = getLatIndex(lat, maxLat, 0);
    int x2 = getLonIndex(lon, maxLon, 1);
    int y2 = getLatIndex(lat, minLat, 1);

    // data must go from top to bottom LOOK IS THIS REALLY NEEDED ?
    double xStart = minLon;
    double yStart = maxLat;
    double xInc = xaxis.getIncrement() * scaler;
    double yInc = Math.abs(yaxis.getIncrement()) * scaler;

    // subseting data inside the box
    Array data1 = getYXDataInBox(data, x1, x2, y1, y2);

    if (pageNumber > 1)
       geotiff.initTags();

    // write it out
    writeGrid(grid, data1, greyScale, xStart, yStart, xInc, yInc, pageNumber);
    pageNumber++;

  }

  int getLatIndex( Array lat, double value, int side)
  {
    int[] shape = lat.getShape();
    IndexIterator latIter = lat.getIndexIterator();
    Index ind = lat.getIndex();
    int count = 0;
    int isInd = 0;

    //LatLonPoint p0 = new LatLonPointImpl(lat.getFloat(ind.set(0)), 0);

    double xlat = latIter.getFloatNext();
    if ( xlat == value ) return 0;

    while (latIter.hasNext() && xlat > value) {
      count++;
      xlat = latIter.getFloatNext();
      if (xlat == value) isInd = 1;
    }

    if( isInd == 1) count += side;
    count -= side;
    return count;
  }

  int getLonIndex( Array lon, double value, int side)
  {
    int[] shape = lon.getShape();
    IndexIterator lonIter = lon.getIndexIterator();
    Index ind = lon.getIndex();
    int count = 0;
    int isInd = 0;

   // double xlon = lon.getFloat(ind.set(0));
    float xlon = lonIter.getFloatNext();
    if ( xlon == value ) return 0;

    while (lonIter.hasNext() && xlon < value) {
      count++;
      xlon = lonIter.getFloatNext();
      if ( xlon == value )  isInd = 1;
    }

    if(isInd == 1) count += side;
    count -= side;
    return count;
  }

    public Array getYXDataInBox(Array data, int x1, int x2, int y1, int y2) throws java.io.IOException {
    int rank = data.getRank();
    int [] start = new int[rank];
    int [] shape = new int[rank];
    for (int i=0; i<rank; i++) {
      start[i] = 0;
      shape[i] = 1;
    }

    if ( y1 >= 0 && y2 >=0 ) {
        start[ 0] = y1;
        shape[ 0] = y2 - y1;
    }
    if (x1 >= 0 && x2 >=0 ) {
        start[ 1] = x1;
        shape[ 1] = x2 - x1;
    }

    // read it
    Array dataVolume;
    try {
      dataVolume = data.section( start, shape);
    } catch (Exception e) {
      throw new java.io.IOException(e.getMessage());
    }

    return dataVolume;
  }

  /**
   * Write Grid data to the geotiff file.
   * Grid currently must:
   * <ol>
   *  <li> have a 1D X and Y coordinate axes.
   *  <li> be lat/lon or Lambert Conformal Projection
   *  <li> be equally spaced
   * </ol>
   * @param grid original grid
   * @param data 2D array in YX order
   * @param greyScale if true, write greyScale image, else dataSample.
   * @param xStart
   * @param yStart
   * @param xInc
   * @param yInc
   * @param imageNumber
   * @throws IOException
   * @throws IllegalArgumentException if above assumptions not valid
   */
  public void writeGrid(GridDatatype grid, Array data, boolean greyScale, double xStart, double yStart, double xInc, double yInc, int imageNumber) throws IOException {
    int nextStart = 0;
    GridCoordSystem gcs = grid.getCoordinateSystem();

    // get rid of this when all projections are implemented
    if (!gcs.isLatLon() && !(gcs.getProjection() instanceof LambertConformal)
                        && !(gcs.getProjection() instanceof Stereographic))
      throw new IllegalArgumentException("Must be lat/lon or LambertConformal grid = "+ gcs.getProjection().getClass().getName());

      // write the data first
    if (greyScale) {
      ArrayByte result = replaceMissingValuesAndScale( grid, data);
      nextStart = geotiff.writeData( (byte []) result.getStorage(), imageNumber);
    } else {
      ArrayFloat result = replaceMissingValues( grid, data);
      nextStart = geotiff.writeData( (float []) result.getStorage(), imageNumber);
    }

    // set the width and the height
    int elemSize = greyScale ? 1 : 4;
    int height = data.getShape()[0]; // Y
    int width = data.getShape()[1]; // X
    int size = elemSize * height * width;  // size in bytes
    geotiff.addTag( new IFDEntry(Tag.ImageWidth, FieldType.SHORT).setValue( width));
    geotiff.addTag( new IFDEntry(Tag.ImageLength, FieldType.SHORT).setValue( height));

    // set the multiple images tag
    int ff = 1 << 1;
    int page = imageNumber -1;
    geotiff.addTag( new IFDEntry(Tag.NewSubfileType, FieldType.SHORT).setValue(ff));
    geotiff.addTag( new IFDEntry(Tag.PageNumber, FieldType.SHORT).setValue( page, 2));

    // just make it all one big "row"
    geotiff.addTag( new IFDEntry(Tag.RowsPerStrip, FieldType.SHORT).setValue( height));
    geotiff.addTag( new IFDEntry(Tag.StripByteCounts, FieldType.LONG).setValue( size));
    // data starts here, header is written at the end
    if( imageNumber == 1 )
      geotiff.addTag( new IFDEntry(Tag.StripOffsets, FieldType.LONG).setValue( 8));
    else
      geotiff.addTag( new IFDEntry(Tag.StripOffsets, FieldType.LONG).setValue(nextStart));

    // standard tags
    geotiff.addTag( new IFDEntry(Tag.Orientation, FieldType.SHORT).setValue( 1));
    geotiff.addTag( new IFDEntry(Tag.Compression, FieldType.SHORT).setValue( 1));  // no compression
    geotiff.addTag( new IFDEntry(Tag.Software, FieldType.ASCII).setValue( "nc2geotiff"));
    geotiff.addTag( new IFDEntry(Tag.PhotometricInterpretation, FieldType.SHORT).setValue( 1)); // black is zero : not used?
    geotiff.addTag( new IFDEntry(Tag.PlanarConfiguration, FieldType.SHORT).setValue( 1));

    if (greyScale) {
      // standard tags for Greyscale images ( see TIFF spec, section 4)
      geotiff.addTag( new IFDEntry(Tag.BitsPerSample, FieldType.SHORT).setValue( 8)); // 8 bits per sample
      geotiff.addTag( new IFDEntry(Tag.SamplesPerPixel, FieldType.SHORT).setValue( 1));

      geotiff.addTag( new IFDEntry(Tag.XResolution, FieldType.RATIONAL).setValue(1, 1));
      geotiff.addTag( new IFDEntry(Tag.YResolution, FieldType.RATIONAL).setValue(1, 1));
      geotiff.addTag( new IFDEntry(Tag.ResolutionUnit, FieldType.SHORT).setValue( 1));

    } else {
      // standard tags for SampleFormat ( see TIFF spec, section 19)
      geotiff.addTag( new IFDEntry(Tag.BitsPerSample, FieldType.SHORT).setValue( 8, 8, 8)); // 32 bits per sample
      geotiff.addTag( new IFDEntry(Tag.SampleFormat, FieldType.SHORT).setValue( 3)); // Sample Format
      geotiff.addTag( new IFDEntry(Tag.SamplesPerPixel, FieldType.SHORT).setValue( 1));
      MAMath.MinMax dataMinMax = grid.getMinMaxSkipMissingData( data);
      float min = (float) (dataMinMax.min);
      float max = (float) (dataMinMax.max);
      geotiff.addTag( new IFDEntry(Tag.SMinSampleValue, FieldType.FLOAT).setValue( min));
      geotiff.addTag( new IFDEntry(Tag.SMaxSampleValue, FieldType.FLOAT).setValue( max));
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
    geotiff.setTransform( xStart, yStart, xInc, yInc);

    if (gcs.isLatLon())
      addLatLonTags();
    else if (gcs.getProjection() instanceof LambertConformal)
      addLambertConformalTags((LambertConformal) gcs.getProjection(), xStart, yStart);
    else if (gcs.getProjection() instanceof Stereographic)
      addPolarStereographicTags((Stereographic) gcs.getProjection(), xStart, yStart);
    else
      addPolarStereographicTags((Stereographic) gcs.getProjection(), xStart, yStart);

    geotiff.writeMetadata(imageNumber);
    //geotiff.close();
  }

  public void close()throws IOException
  {
    geotiff.close();
  }
   /**
   * Replace missing values with dataMinMax.min - 1.0; return a floating point data array.
   * @param grid GridDatatype
   * @param data input data array
   * @return floating point data array with missing values replaced.
   */
  private ArrayFloat replaceMissingValues( GridDatatype grid, Array data) {
    MAMath.MinMax dataMinMax = grid.getMinMaxSkipMissingData( data);
    float minValue = (float) (dataMinMax.min - 1.0);

    ArrayFloat floatArray = (ArrayFloat) Array.factory( float.class, data.getShape());
    IndexIterator dataIter = data.getIndexIterator();
    IndexIterator floatIter = floatArray.getIndexIterator();
    while (dataIter.hasNext()) {
      float v = dataIter.getFloatNext();
      if ( grid.isMissingData( (double) v))  v = minValue;
      floatIter.setFloatNext( v);
    }

    return floatArray;
  }

  /**
   * Replace missing values with 0; scale other values between 1 and 255, return a byte data array.
   * @param grid GridDatatype
   * @param data input data array
   * @return byte data array with missing values replaced and data scaled from 1- 255.
   */
  private ArrayByte replaceMissingValuesAndScale( GridDatatype grid, Array data) {
    MAMath.MinMax dataMinMax = grid.getMinMaxSkipMissingData( data);
    double scale = 254.0/(dataMinMax.max - dataMinMax.min);

    ArrayByte byteArray = (ArrayByte) Array.factory( byte.class, data.getShape());
    IndexIterator dataIter = data.getIndexIterator();
    IndexIterator resultIter = byteArray.getIndexIterator();

    byte bv;
    while (dataIter.hasNext()) {
      double v = dataIter.getDoubleNext();
      if ( grid.isMissingData( v))
        bv = 0;
      else {
        int iv = (int) ((v - dataMinMax.min) * scale + 1);
        bv = (byte) (iv & 0xff);
      }
      resultIter.setByteNext( bv);
    }

    return byteArray;
  }

  private void addLatLonTags() {
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GTModelTypeGeoKey, GeoKey.TagValue.ModelType_Geographic));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GTRasterTypeGeoKey, GeoKey.TagValue.RasterType_Area));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeographicTypeGeoKey, GeoKey.TagValue.GeographicType_WGS_84));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogPrimeMeridianGeoKey, GeoKey.TagValue.GeogPrimeMeridian_GREENWICH));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogAngularUnitsGeoKey, GeoKey.TagValue.GeogAngularUnits_DEGREE));
  }

  private void addPolarStereographicTags(Stereographic proj, double FalseEasting, double FalseNorthing) {
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GTModelTypeGeoKey, GeoKey.TagValue.ModelType_Projected));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GTRasterTypeGeoKey, GeoKey.TagValue.RasterType_Area));

    // define the "geographic Coordinate System"
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeographicTypeGeoKey, GeoKey.TagValue.GeographicType_WGS_84));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogPrimeMeridianGeoKey, GeoKey.TagValue.GeogPrimeMeridian_GREENWICH));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogAngularUnitsGeoKey, GeoKey.TagValue.GeogAngularUnits_DEGREE));

    // define the "coordinate transformation"
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjectedCSTypeGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.PCSCitationGeoKey, "Snyder"));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjectionGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsGeoKey, GeoKey.TagValue.ProjLinearUnits_METER));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

    // the specifics for Polar Stereographic
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjCoordTransGeoKey, GeoKey.TagValue.ProjCoordTrans_Stereographic));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjCenterLongGeoKey, 0.0));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjNatOriginLatGeoKey, 90.0));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjNatOriginLongGeoKey, proj.getTangentLon()));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjScaleAtNatOriginGeoKey, 1.0));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjFalseNorthingGeoKey, 0.0));


  }

  private void addLambertConformalTags(LambertConformal proj, double FalseEasting, double FalseNorthing) {
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GTModelTypeGeoKey, GeoKey.TagValue.ModelType_Projected));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GTRasterTypeGeoKey, GeoKey.TagValue.RasterType_Area));

    // define the "geographic Coordinate System"
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeographicTypeGeoKey, GeoKey.TagValue.GeographicType_WGS_84));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogPrimeMeridianGeoKey, GeoKey.TagValue.GeogPrimeMeridian_GREENWICH));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogAngularUnitsGeoKey, GeoKey.TagValue.GeogAngularUnits_DEGREE));

    // define the "coordinate transformation"
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjectedCSTypeGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.PCSCitationGeoKey, "Snyder"));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjectionGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsGeoKey, GeoKey.TagValue.ProjLinearUnits_METER));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

    // the specifics for lambert conformal
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjCoordTransGeoKey, GeoKey.TagValue.ProjCoordTrans_LambertConfConic_2SP));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjStdParallel1GeoKey, proj.getParallelOne()));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjStdParallel2GeoKey, proj.getParallelTwo()));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjCenterLongGeoKey, proj.getOriginLon()));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjNatOriginLatGeoKey, proj.getOriginLat()));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjNatOriginLongGeoKey, proj.getOriginLon()));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjScaleAtNatOriginGeoKey, 1.0));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));
    geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjFalseNorthingGeoKey, 0.0));


  }

  private void dump( Array data, int col) {
    int[] shape = data.getShape();
    Index ima = data.getIndex();

    for(int j = 0; j< shape[0]; j++ ) {
      float dd = data.getFloat(ima.set(j, col));
      System.out.println(j+" value= "+dd);
    }
  }

  private double geoShiftGetXstart(Array lon, double inc) {
    int count = 0;
    Index ilon = lon.getIndex();
    int[] lonShape = lon.getShape();
    IndexIterator lonIter = lon.getIndexIterator();
    double xlon = 0.0;

    LatLonPoint p0 = new LatLonPointImpl(0, lon.getFloat(ilon.set(0)));
    LatLonPoint pN = new LatLonPointImpl(0, lon.getFloat(ilon.set(lonShape[0] -1)));

    xlon = p0.getLongitude();
    while (lonIter.hasNext()) {
      float l = lonIter.getFloatNext();
      LatLonPoint pn = new LatLonPointImpl(0, l);
      if ( pn.getLongitude() < xlon ) xlon = pn.getLongitude();
    }

    if ( p0.getLongitude() == pN.getLongitude() ) xlon = xlon - inc;

    return xlon;
  }

  private Array geoShiftDataAtLon(Array data, Array lon) {
    int count = 0;
    int[] shape = data.getShape();
    Index ima = data.getIndex();
    Index ilon = lon.getIndex();
    int[] lonShape = lon.getShape();
    ArrayFloat adata = new ArrayFloat(new int[] {shape[0], shape[1]});
    Index imaa = adata.getIndex();
    IndexIterator lonIter = lon.getIndexIterator();

    LatLonPoint p0 = new LatLonPointImpl(0, lon.getFloat(ilon.set(lonShape[0] -1)));
    LatLonPoint pN = new LatLonPointImpl(0, lon.getFloat(ilon.set( 0 )));

    while (lonIter.hasNext()) {
      float l = lonIter.getFloatNext();
      if (l > 180.0 ) count++;
    }

    //checking if the 0 point and the N point are the same point
    int spoint = 0;
    if ( p0.getLongitude() == pN.getLongitude() )
    {
        spoint = shape[1] - count -1 ;
    }
    else
    {
        spoint = shape[1] - count;
    }

    if ( count > 0 && (shape[1] > count) ) {
      for(int j = 1; j< shape[1]; j++ ) {
        int jj = 0;

        if( j >= count ) jj = j - count;
        else  jj = j + spoint;

        for(int i = 0; i < shape[0]; i++) {
          float dd = data.getFloat(ima.set(i, jj));
          adata.setFloat(imaa.set(i, j), dd );
        }
      }

      if ( p0.getLongitude() == pN.getLongitude() )
      {
          for(int i = 0; i < shape[0]; i++) {
            float dd = adata.getFloat(imaa.set(i, shape[1] -1));
            adata.setFloat(imaa.set(i, 0), dd );
          }
      }
      return adata;

    } else
      return data;
  }

  private Array geoShiftLon(Array lon) {
    int count = 0;
    Index lonIndex = lon.getIndex();
    int[] lonShape = lon.getShape();
    ArrayFloat slon = new ArrayFloat(new int[] {lonShape[0]});
    Index slonIndex= slon.getIndex();
    IndexIterator lonIter = lon.getIndexIterator();
    LatLonPointImpl llp = new LatLonPointImpl();
    LatLonPoint p0 = new LatLonPointImpl(0, lon.getFloat(lonIndex.set(lonShape[0] -1)));
    LatLonPoint pN = new LatLonPointImpl(0, lon.getFloat(lonIndex.set( 0 )));

    while (lonIter.hasNext()) {
      float l = lonIter.getFloatNext();
      if (l > 180.0 ) count++;
    }

    //checking if the 0 point and the N point are the same point
    int spoint = 0;
    if ( p0.getLongitude() == pN.getLongitude() )
    {
        spoint = lonShape[0] - count -1 ;
    }
    else
    {
        spoint = lonShape[0] - count;
    }

    if ( count > 0 && (lonShape[0] > count) ) {
      for(int j = 1; j< lonShape[0]; j++ ) {
        int jj = 0;
        if( j >= count ) jj = j - count;
        else  jj = j + spoint;

        float dd = lon.getFloat(lonIndex.set(jj));
        slon.setFloat(slonIndex.set(j), (float)LatLonPointImpl.lonNormal(dd) );

      }

      if ( p0.getLongitude() == pN.getLongitude() )
      {
            float dd = slon.getFloat(slonIndex.set(lonShape[0] -1));
            slon.setFloat(slonIndex.set( 0), -(float)LatLonPointImpl.lonNormal(dd));
      }
      return slon;

    } else
      return lon;
  }

  /** test */
  public static void main(String args[]) throws IOException {
    String fileOut = "totalr1.tif";
    LatLonPointImpl p1 = new LatLonPointImpl(-15.0, -180.0);
    LatLonPointImpl p2 = new LatLonPointImpl(60.0, 180.0);
    LatLonRect llr = new LatLonRect(p1, p2);
    GeotiffWriter writer = new GeotiffWriter(fileOut);
    //writer.writeGrid("radar.nc", "noice_wat", 0, 0, true);
    //writer.writeGrid("dods://www.cdc.noaa.gov/cgi-bin/nph-nc/Datasets/coads/2degree/enh/cldc.mean.nc?lat[40:1:50],lon[70:1:110],time[2370:1:2375],cldc[2370:1:2375][40:1:50][70:1:110]", "cldc", 0, 0,true);
    //writer.writeGrid("dods://www.cdc.noaa.gov/cgi-bin/nph-nc/Datasets/noaa.oisst.v2/sst.mnmean.nc", "sst", 0, 0,false);
    //writer.writeGrid("2003091116_ruc2.nc", "P_sfc", 0, 0, false);
    //writer.writeGrid("/home/yuanho/dev/netcdf-java/geotiff/2003072918_avn-x.nc", "P_sfc", 0, 0, true);
    writer.writeGrid("/home/yuanho/dev/netcdf-java/geotiff/2003072918_avn-x.nc", "T", 0, 0, true, llr);
    writer.close();

    // read it back in
    GeoTiff geotiff = new GeoTiff(fileOut);
    geotiff.read();
    System.out.println("geotiff read in = "+geotiff.showInfo());
    geotiff.close();
  }

}




