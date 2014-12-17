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

  protected GeoTiff geotiff;
  protected short pageNumber = 1;

  /**
   * Constructor
   *
   * @param fileOut name of output file.
   */
  public GeotiffWriter(String fileOut) {
    geotiff = new GeoTiff(fileOut);
  }

  /**
   * Write Grid data to the geotiff file.
   *
   * @param dataset   grid in contained in this dataset
   * @param grid      data is in this grid
   * @param data      2D array in YX order
   * @param greyScale if true, write greyScale image, else dataSample.
   * @throws IOException on i/o error
   */
  public void writeGrid(GridDataset dataset, GridDatatype grid, Array data, boolean greyScale) throws IOException {
    GridCoordSystem gcs = grid.getCoordinateSystem();

    if (!gcs.isRegularSpatial()) {
      throw new IllegalArgumentException("Must have 1D x and y axes for " + grid.getFullName());
    }

    CoordinateAxis1D xaxis = (CoordinateAxis1D) gcs.getXHorizAxis();
    CoordinateAxis1D yaxis = (CoordinateAxis1D) gcs.getYHorizAxis();

    //latlon coord does not need to be scaled
    double scaler = (gcs.isLatLon()) ? 1.0 : 1000.0;

    // data must go from top to bottom
    double xStart = xaxis.getCoordEdge(0) * scaler;
    double yStart = yaxis.getCoordEdge(0) * scaler;
    double xInc = xaxis.getIncrement() * scaler;
    double yInc = Math.abs(yaxis.getIncrement()) * scaler;

    if (yaxis.getCoordValue(0) < yaxis.getCoordValue(1)) {
      data = data.flip(0);
      yStart = yaxis.getCoordEdge((int) yaxis.getSize()) * scaler;
    }

    /*  remove - i think unneeded, monotonic lon handled in CoordinateAxis1D. JC 3/18/2013
     if (gcs.isLatLon()) {
      Array lon = xaxis.read();
      data = geoShiftDataAtLon(data, lon);
      xStart = geoShiftGetXstart(lon, xInc);
      //xStart = -180.0;
    }  */

    if (!xaxis.isRegular() || !yaxis.isRegular()) {
      throw new IllegalArgumentException("Must be evenly spaced grid = " + grid.getFullName());
    }

    if (pageNumber > 1) {
      geotiff.initTags();
    }

    // write it out
    writeGrid(grid, data, greyScale, xStart, yStart, xInc, yInc, pageNumber);
    pageNumber++;
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
   * @throws IOException              on i/o error
   * @throws IllegalArgumentException if above assumptions not valid
   */
  void writeGrid(GridDatatype grid, Array data, boolean greyScale, double xStart, double yStart, double xInc,
                        double yInc, int imageNumber) throws IOException {

    int nextStart = 0;
    GridCoordSystem gcs = grid.getCoordinateSystem();

    // get rid of this when all projections are implemented
    if (!gcs.isLatLon()
            && !(gcs.getProjection() instanceof LambertConformal)
            && !(gcs.getProjection() instanceof Stereographic)
            && !(gcs.getProjection() instanceof Mercator)
            //  && !(gcs.getProjection() instanceof TransverseMercator)
            && !(gcs.getProjection() instanceof AlbersEqualAreaEllipse)
            && !(gcs.getProjection() instanceof AlbersEqualArea)) {
      throw new IllegalArgumentException("Unsupported projection = " + gcs.getProjection().getClass().getName());
    }

    // write the data first
    if (greyScale) {
      ArrayByte result = replaceMissingValuesAndScale(grid, data);
      nextStart = geotiff.writeData((byte[]) result.getStorage(), imageNumber);
    } else {
      ArrayFloat result = replaceMissingValues(grid, data);
      nextStart = geotiff.writeData((float[]) result.getStorage(), imageNumber);
    }

    // set the width and the height
    int elemSize = greyScale ? 1 : 4;
    int height = data.getShape()[0];         // Y
    int width = data.getShape()[1];         // X
    geotiff.addTag(new IFDEntry(Tag.ImageWidth, FieldType.SHORT).setValue(width));
    geotiff.addTag(new IFDEntry(Tag.ImageLength, FieldType.SHORT).setValue(height));

    // set the multiple images tag
    int ff = 1 << 1;
    int page = imageNumber - 1;
    geotiff.addTag(new IFDEntry(Tag.NewSubfileType, FieldType.SHORT).setValue(ff));
    geotiff.addTag(new IFDEntry(Tag.PageNumber, FieldType.SHORT).setValue(page, 2));

    // just make it all one big "row"
    geotiff.addTag(new IFDEntry(Tag.RowsPerStrip, FieldType.SHORT).setValue(1));  //height));
    // the following changes to make it viewable in ARCMAP
        /*
        int size = elemSize * height * width;  // size in bytes
        geotiff.addTag( new IFDEntry(Tag.StripByteCounts, FieldType.LONG).setValue( size));
        // data starts here, header is written at the end
        if( imageNumber == 1 )
          geotiff.addTag( new IFDEntry(Tag.StripOffsets, FieldType.LONG).setValue( 8));
        else
          geotiff.addTag( new IFDEntry(Tag.StripOffsets, FieldType.LONG).setValue(nextStart));
        */
    int[] soffset = new int[height];
    int[] sbytecount = new int[height];
    if (imageNumber == 1) {
      soffset[0] = 8;
    } else {
      soffset[0] = nextStart;
    }
    sbytecount[0] = width * elemSize;
    for (int i = 1; i < height; i++) {
      soffset[i] = soffset[i - 1] + width * elemSize;
      sbytecount[i] = width * elemSize;
    }
    geotiff.addTag(new IFDEntry(Tag.StripByteCounts, FieldType.LONG, width).setValue(sbytecount));
    geotiff.addTag(new IFDEntry(Tag.StripOffsets, FieldType.LONG, width).setValue(soffset));
    // standard tags
    geotiff.addTag(new IFDEntry(Tag.Orientation, FieldType.SHORT).setValue(1));
    geotiff.addTag(new IFDEntry(Tag.Compression, FieldType.SHORT).setValue(1));  // no compression
    geotiff.addTag(new IFDEntry(Tag.Software, FieldType.ASCII).setValue("nc2geotiff"));
    geotiff.addTag(new IFDEntry(Tag.PhotometricInterpretation, FieldType.SHORT).setValue(1));  // black is zero : not used?
    geotiff.addTag(new IFDEntry(Tag.PlanarConfiguration, FieldType.SHORT).setValue(1));

    if (greyScale) {
      // standard tags for Greyscale images ( see TIFF spec, section 4)
      geotiff.addTag(new IFDEntry(Tag.BitsPerSample, FieldType.SHORT).setValue(8));  // 8 bits per sample
      geotiff.addTag(new IFDEntry(Tag.SamplesPerPixel, FieldType.SHORT).setValue(1));

      geotiff.addTag(new IFDEntry(Tag.XResolution, FieldType.RATIONAL).setValue(1, 1));
      geotiff.addTag(new IFDEntry(Tag.YResolution, FieldType.RATIONAL).setValue(1, 1));
      geotiff.addTag(new IFDEntry(Tag.ResolutionUnit, FieldType.SHORT).setValue(1));

    } else {
      // standard tags for SampleFormat ( see TIFF spec, section 19)
      geotiff.addTag(new IFDEntry(Tag.BitsPerSample, FieldType.SHORT).setValue(32));  // 32 bits per sample
      geotiff.addTag(new IFDEntry(Tag.SampleFormat, FieldType.SHORT).setValue(3));  // Sample Format
      geotiff.addTag(new IFDEntry(Tag.SamplesPerPixel, FieldType.SHORT).setValue(1));
      MAMath.MinMax dataMinMax = grid.getMinMaxSkipMissingData(data);
      float min = (float) (dataMinMax.min);
      float max = (float) (dataMinMax.max);
      geotiff.addTag(new IFDEntry(Tag.SMinSampleValue, FieldType.FLOAT).setValue(min));
      geotiff.addTag(new IFDEntry(Tag.SMaxSampleValue, FieldType.FLOAT).setValue(max));
      geotiff.addTag(new IFDEntry(Tag.GDALNoData, FieldType.FLOAT).setValue(min - 1.f));
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
      addLambertConformalTags((LambertConformal) gcs.getProjection(), xStart, yStart);
    } else if (gcs.getProjection() instanceof Stereographic) {
      addPolarStereographicTags((Stereographic) gcs.getProjection(), xStart, yStart);
    } else if (gcs.getProjection() instanceof Mercator) {
      addMercatorTags((Mercator) gcs.getProjection());
    } else if (gcs.getProjection() instanceof TransverseMercator) {
      addTransverseMercatorTags((TransverseMercator) gcs.getProjection());
    } else if (gcs.getProjection() instanceof AlbersEqualArea) {
      addAlbersEqualAreaTags((AlbersEqualArea) gcs.getProjection());
    } else if (gcs.getProjection() instanceof AlbersEqualAreaEllipse) {
      addAlbersEqualAreaEllipseTags((AlbersEqualAreaEllipse) gcs.getProjection());
    }

    geotiff.writeMetadata(imageNumber);
    //geotiff.close();

  }

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
    float minValue = (float) (dataMinMax.min - 1.0);

    ArrayFloat floatArray = (ArrayFloat) Array.factory(float.class,
            data.getShape());
    IndexIterator dataIter = data.getIndexIterator();
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
    double scale = 254.0 / (dataMinMax.max - dataMinMax.min);

    ArrayByte byteArray = (ArrayByte) Array.factory(byte.class,
            data.getShape());
    IndexIterator dataIter = data.getIndexIterator();
    IndexIterator resultIter = byteArray.getIndexIterator();

    byte bv;
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

  private void addLatLonTags1() {
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey,
            GeoKey.TagValue.ModelType_Geographic));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogGeodeticDatumGeoKey,
            GeoKey.TagValue.GeogGeodeticDatum6267));
  }

  private void addLatLonTags() {
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey, GeoKey.TagValue.ModelType_Geographic));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey, GeoKey.TagValue.RasterType_Area));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey, GeoKey.TagValue.GeographicType_WGS_84));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogPrimeMeridianGeoKey, GeoKey.TagValue.GeogPrimeMeridian_GREENWICH));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogAngularUnitsGeoKey, GeoKey.TagValue.GeogAngularUnits_DEGREE));
  }


  private void addPolarStereographicTags(Stereographic proj, double FalseEasting, double FalseNorthing) {
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey, GeoKey.TagValue.ModelType_Projected));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey, GeoKey.TagValue.RasterType_Area));

    // define the "geographic Coordinate System"
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey, GeoKey.TagValue.GeographicType_WGS_84));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogPrimeMeridianGeoKey, GeoKey.TagValue.GeogPrimeMeridian_GREENWICH));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogAngularUnitsGeoKey, GeoKey.TagValue.GeogAngularUnits_DEGREE));

    // define the "coordinate transformation"
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjectedCSTypeGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.PCSCitationGeoKey, "Snyder"));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjectionGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjLinearUnitsGeoKey, GeoKey.TagValue.ProjLinearUnits_METER));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

    // the specifics for Polar Stereographic
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjCoordTransGeoKey, GeoKey.TagValue.ProjCoordTrans_Stereographic));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjCenterLongGeoKey, 0.0));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLatGeoKey, 90.0));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjNatOriginLongGeoKey, proj.getTangentLon()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjScaleAtNatOriginGeoKey, 1.0));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseNorthingGeoKey, 0.0));
  }

  private void addLambertConformalTags(LambertConformal proj, double FalseEasting, double FalseNorthing) {
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey, GeoKey.TagValue.ModelType_Projected));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey, GeoKey.TagValue.RasterType_Area));

    // define the "geographic Coordinate System"
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey, GeoKey.TagValue.GeographicType_WGS_84));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogPrimeMeridianGeoKey, GeoKey.TagValue.GeogPrimeMeridian_GREENWICH));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogAngularUnitsGeoKey, GeoKey.TagValue.GeogAngularUnits_DEGREE));

    // define the "coordinate transformation"
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjectedCSTypeGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.PCSCitationGeoKey, "Snyder"));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjectionGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjLinearUnitsGeoKey, GeoKey.TagValue.ProjLinearUnits_METER));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

    // the specifics for lambert conformal
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjCoordTransGeoKey, GeoKey.TagValue.ProjCoordTrans_LambertConfConic_2SP));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel1GeoKey, proj.getParallelOne()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel2GeoKey, proj.getParallelTwo()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjCenterLongGeoKey, proj.getOriginLon()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLatGeoKey, proj.getOriginLat()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLongGeoKey, proj.getOriginLon()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjScaleAtNatOriginGeoKey, 1.0));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));  // LOOK why not FalseNorthing ??
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseNorthingGeoKey, 0.0));
  }

  private void addMercatorTags(Mercator proj) {
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey, GeoKey.TagValue.ModelType_Projected));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey, GeoKey.TagValue.RasterType_Area));

    // define the "geographic Coordinate System"
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey, GeoKey.TagValue.GeographicType_WGS_84));
    // geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogLinearUnitsGeoKey, GeoKey.TagValue.ProjLinearUnits_METER));
    // geotiff.addGeoKey( new GeoKey( GeoKey.Tag.GeogAngularUnitsGeoKey, GeoKey.TagValue.GeogAngularUnits_DEGREE));

    // define the "coordinate transformation"
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjectedCSTypeGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.PCSCitationGeoKey, "Mercator"));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjectionGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjLinearUnitsGeoKey, GeoKey.TagValue.ProjLinearUnits_METER));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

    // the specifics for mercator
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjCoordTransGeoKey, GeoKey.TagValue.ProjCoordTrans_Mercator));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLongGeoKey, proj.getOriginLon()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLatGeoKey, proj.getParallel()));
    //    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel1GeoKey, proj.getParallel()));
    //   geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjScaleAtNatOriginGeoKey, 1));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseNorthingGeoKey, 0.0));
  }

  private void addTransverseMercatorTags(TransverseMercator proj) {
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey, GeoKey.TagValue.ModelType_Projected));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey, GeoKey.TagValue.RasterType_Area));

    // define the "geographic Coordinate System"
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey, GeoKey.TagValue.GeographicType_WGS_84));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogLinearUnitsGeoKey, GeoKey.TagValue.ProjLinearUnits_METER));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogAngularUnitsGeoKey, GeoKey.TagValue.GeogAngularUnits_DEGREE));

    // define the "coordinate transformation"
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjectedCSTypeGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.PCSCitationGeoKey, "Transvers Mercator"));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjectionGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjLinearUnitsGeoKey, GeoKey.TagValue.ProjLinearUnits_METER));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

    // the specifics for mercator
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjCoordTransGeoKey, GeoKey.TagValue.ProjCoordTrans_TransverseMercator));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLatGeoKey, proj.getOriginLat()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLongGeoKey, proj.getTangentLon()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjScaleAtNatOriginGeoKey, proj.getScale()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjScaleAtNatOriginGeoKey, 1.0));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseNorthingGeoKey, 0.0));
  }

  private void addAlbersEqualAreaEllipseTags(AlbersEqualAreaEllipse proj) {
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey, GeoKey.TagValue.ModelType_Projected));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey, GeoKey.TagValue.RasterType_Area));

    // define the "geographic Coordinate System"
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey, GeoKey.TagValue.GeographicType_WGS_84));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogLinearUnitsGeoKey, GeoKey.TagValue.ProjLinearUnits_METER));

    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogSemiMajorAxisGeoKey, proj.getEarth().getMajor()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogSemiMinorAxisGeoKey, proj.getEarth().getMinor()));

    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogAngularUnitsGeoKey, GeoKey.TagValue.GeogAngularUnits_DEGREE));

    // define the "coordinate transformation"
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjectedCSTypeGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.PCSCitationGeoKey, "Albers Conial Equal Area"));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjectionGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjLinearUnitsGeoKey, GeoKey.TagValue.ProjLinearUnits_METER));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

    // the specifics for mercator
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjCoordTransGeoKey, GeoKey.TagValue.ProjCoordTrans_AlbersEqualAreaEllipse));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLatGeoKey, proj.getOriginLat()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLongGeoKey, proj.getOriginLon()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel1GeoKey, proj.getParallelOne()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel2GeoKey, proj.getParallelTwo()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseNorthingGeoKey, 0.0));
  }

  private void addAlbersEqualAreaTags(AlbersEqualArea proj) {
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTModelTypeGeoKey, GeoKey.TagValue.ModelType_Projected));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GTRasterTypeGeoKey, GeoKey.TagValue.RasterType_Area));

    // define the "geographic Coordinate System"
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeographicTypeGeoKey, GeoKey.TagValue.GeographicType_WGS_84));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogLinearUnitsGeoKey, GeoKey.TagValue.ProjLinearUnits_METER));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.GeogAngularUnitsGeoKey, GeoKey.TagValue.GeogAngularUnits_DEGREE));

    // define the "coordinate transformation"
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjectedCSTypeGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.PCSCitationGeoKey, "Albers Conial Equal Area"));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjectionGeoKey, GeoKey.TagValue.ProjectedCSType_UserDefined));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjLinearUnitsGeoKey, GeoKey.TagValue.ProjLinearUnits_METER));
    //geotiff.addGeoKey( new GeoKey( GeoKey.Tag.ProjLinearUnitsSizeGeoKey, 1.0)); // units of km

    // the specifics for mercator
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjCoordTransGeoKey, GeoKey.TagValue.ProjCoordTrans_AlbersConicalEqualArea));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLatGeoKey, proj.getOriginLat()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjNatOriginLongGeoKey, proj.getOriginLon()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel1GeoKey, proj.getParallelOne()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjStdParallel2GeoKey, proj.getParallelTwo()));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseEastingGeoKey, 0.0));
    geotiff.addGeoKey(new GeoKey(GeoKey.Tag.ProjFalseNorthingGeoKey, 0.0));
  }

  private void dump(Array data, int col) {
    int[] shape = data.getShape();
    Index ima = data.getIndex();

    for (int j = 0; j < shape[0]; j++) {
      float dd = data.getFloat(ima.set(j, col));
      System.out.println(j + " value= " + dd);
    }
  }

  // WTF ?? is this the seam crossing ??
  private double geoShiftGetXstart(Array lon, double inc) {
    Index ilon = lon.getIndex();
    int[] lonShape = lon.getShape();
    IndexIterator lonIter = lon.getIndexIterator();
    double xlon = 0.0;

    LatLonPoint p0 = new LatLonPointImpl(0, lon.getFloat(ilon.set(0)));
    LatLonPoint pN = new LatLonPointImpl(0, lon.getFloat(ilon.set(lonShape[0] - 1)));

    xlon = p0.getLongitude();
    while (lonIter.hasNext()) {
      float l = lonIter.getFloatNext();
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

  Array geoShiftDataAtLon(Array data, Array lon) {
    int count = 0;
    int[] shape = data.getShape();
    Index ima = data.getIndex();
    Index ilon = lon.getIndex();
    int[] lonShape = lon.getShape();
    ArrayFloat adata = new ArrayFloat(new int[]{shape[0], shape[1]});
    Index imaa = adata.getIndex();
    IndexIterator lonIter = lon.getIndexIterator();

    LatLonPoint p0 = new LatLonPointImpl(0, lon.getFloat(ilon.set(lonShape[0] - 1)));
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


}

