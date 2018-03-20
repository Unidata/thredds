/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.geotiff;

import java.io.Closeable;
import java.io.IOException;

import ucar.ma2.Array;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.IsMissingEvaluator;
import ucar.ma2.MAMath;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.ft2.coverage.CoverageCoordAxis1D;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.GeoReferencedArray;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.AlbersEqualArea;
import ucar.unidata.geoloc.projection.LambertConformal;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.Mercator;
import ucar.unidata.geoloc.projection.Stereographic;
import ucar.unidata.geoloc.projection.TransverseMercator;
import ucar.unidata.geoloc.projection.proj4.AlbersEqualAreaEllipse;

/**
 * Write GeoTIFF files.
 * Regular data only
 *
 * @author caron, yuan
 */
public class GeotiffWriter implements Closeable {

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

  public void close() throws IOException {
    geotiff.close();
  }

  /**
   * Write GridDatatype data to the geotiff file.
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

    // units may need to be scaled to meters
    double scaler = (xaxis.getUnitsString().equalsIgnoreCase("km")) ? 1000.0 : 1.0;

    // data must go from top to bottom
    double xStart = xaxis.getCoordEdge(0) * scaler;
    double yStart = yaxis.getCoordEdge(0) * scaler;
    double xInc = xaxis.getIncrement() * scaler;
    double yInc = Math.abs(yaxis.getIncrement()) * scaler;

    if (yaxis.getCoordValue(0) < yaxis.getCoordValue(1)) {
      data = data.flip(0);
      yStart = yaxis.getCoordEdge((int) yaxis.getSize()) * scaler;
    }

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
  protected void writeGrid(GridDatatype grid, Array data, boolean greyScale, double xStart, double yStart, double xInc,
                        double yInc, int imageNumber) throws IOException {

    int nextStart = 0;
    GridCoordSystem gcs = grid.getCoordinateSystem();

    // get rid of this when all projections are implemented
    if (!gcs.isLatLon()
            && !(gcs.getProjection() instanceof LambertConformal)
            && !(gcs.getProjection() instanceof Stereographic)
            && !(gcs.getProjection() instanceof Mercator)
            //  && !(gcs.getProjection() instanceof TransverseMercator)   LOOK broken ??
            && !(gcs.getProjection() instanceof AlbersEqualAreaEllipse)
            && !(gcs.getProjection() instanceof AlbersEqualArea)) {
      throw new IllegalArgumentException("Unsupported projection = " + gcs.getProjection().getClass().getName());
    }

    // write the data first
    MAMath.MinMax dataMinMax = grid.getMinMaxSkipMissingData(data);
    if (greyScale) {
      ArrayByte result = replaceMissingValuesAndScale(grid, data, dataMinMax);
      nextStart = geotiff.writeData((byte[]) result.getStorage(), imageNumber);
    } else {
      ArrayFloat result = replaceMissingValues(grid, data, dataMinMax);
      nextStart = geotiff.writeData((float[]) result.getStorage(), imageNumber);
    }

    // set the width and the height
    int height = data.getShape()[0];         // Y
    int width = data.getShape()[1];         // X

    writeMetadata(greyScale, xStart, yStart, xInc, yInc, height, width, imageNumber, nextStart, dataMinMax, gcs.getProjection());
  }

  private void writeMetadata(boolean greyScale, double xStart, double yStart, double xInc, double yInc, int height, int width, int imageNumber, int nextStart,
                     MAMath.MinMax dataMinMax, Projection proj) throws IOException {

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

    int elemSize = greyScale ? 1 : 4;

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

    if (proj instanceof LatLonProjection) {
      addLatLonTags();
    } else if (proj instanceof LambertConformal) {
      addLambertConformalTags((LambertConformal) proj, xStart, yStart);
    } else if (proj instanceof Stereographic) {
      addPolarStereographicTags((Stereographic) proj, xStart, yStart);
    } else if (proj instanceof Mercator) {
      addMercatorTags((Mercator) proj);
    } else if (proj instanceof TransverseMercator) {
      addTransverseMercatorTags((TransverseMercator) proj);
    } else if (proj instanceof AlbersEqualArea) {
      addAlbersEqualAreaTags((AlbersEqualArea) proj);
    } else if (proj instanceof AlbersEqualAreaEllipse) {
      addAlbersEqualAreaEllipseTags((AlbersEqualAreaEllipse) proj);
    } else {
      throw new IllegalArgumentException("Unsupported projection = " + proj.getClass().getName());
    }

    geotiff.writeMetadata(imageNumber);
  }

  /**
   * Replace missing values with dataMinMax.min - 1.0; return a floating point data array.
   *
   * @param grid GridDatatype
   * @param data input data array
   * @return floating point data array with missing values replaced.
   */
  private ArrayFloat replaceMissingValues(IsMissingEvaluator grid, Array data, MAMath.MinMax dataMinMax) {
    float minValue = (float) (dataMinMax.min - 1.0);

    ArrayFloat floatArray = (ArrayFloat) Array.factory(DataType.FLOAT, data.getShape());
    IndexIterator dataIter = data.getIndexIterator();
    IndexIterator floatIter = floatArray.getIndexIterator();
    while (dataIter.hasNext()) {
      float v = dataIter.getFloatNext();
      if (grid.isMissing((double) v)) {
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
  private ArrayByte replaceMissingValuesAndScale(IsMissingEvaluator grid, Array data, MAMath.MinMax dataMinMax) {
    double scale = 254.0 / (dataMinMax.max - dataMinMax.min);

    ArrayByte byteArray = (ArrayByte) Array.factory(DataType.BYTE, data.getShape());
    IndexIterator dataIter = data.getIndexIterator();
    IndexIterator resultIter = byteArray.getIndexIterator();

    byte bv;
    while (dataIter.hasNext()) {
      double v = dataIter.getDoubleNext();
      if (grid.isMissing(v)) {
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

  // LOOK WTF ?? is this the seam crossing ??
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

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Write GridCoverage data to the geotiff file.
   *
   * @param array      GeoReferencedArray array in YX order
   * @param greyScale if true, write greyScale image, else dataSample.
   * @throws IOException on i/o error
   */
  public void writeGrid(GeoReferencedArray array, boolean greyScale) throws IOException {

    CoverageCoordSys gcs = array.getCoordSysForData();
    if (!gcs.isRegularSpatial())
      throw new IllegalArgumentException("Must have 1D x and y axes for " + array.getCoverageName());

    Projection proj = gcs.getProjection();
    CoverageCoordAxis1D xaxis = (CoverageCoordAxis1D) gcs.getXAxis();
    CoverageCoordAxis1D yaxis = (CoverageCoordAxis1D) gcs.getYAxis();

    // latlon coord does not need to be scaled
    double scaler = (xaxis.getUnits().equalsIgnoreCase("km")) ? 1000.0 : 1.0;

    // data must go from top to bottom
    double xStart = xaxis.getCoordEdge1(0) * scaler;
    double yStart = yaxis.getCoordEdge1(0) * scaler;
    double xInc = xaxis.getResolution() * scaler;
    double yInc = Math.abs(yaxis.getResolution()) * scaler;

    Array data = array.getData().reduce();
    if (yaxis.getCoordMidpoint(0) < yaxis.getCoordMidpoint(1)) {
      data = data.flip(0);
      yStart = yaxis.getCoordEdgeLast();
    }

    /*  remove - i think unneeded, monotonic lon handled in CoordinateAxis1D. JC 3/18/2013
     if (gcs.isLatLon()) {
      Array lon = xaxis.read();
      data = geoShiftDataAtLon(data, lon);
      xStart = geoShiftGetXstart(lon, xInc);
      //xStart = -180.0;
    }  */

    if (pageNumber > 1) {
      geotiff.initTags();
    }

    // write the data first
    int nextStart = 0;
    MAMath.MinMax dataMinMax = MAMath.getMinMaxSkipMissingData(data, array);
    if (greyScale) {
      ArrayByte result = replaceMissingValuesAndScale(array, data, dataMinMax);
      nextStart = geotiff.writeData((byte[]) result.getStorage(), pageNumber);
    } else {
      ArrayFloat result = replaceMissingValues(array, data, dataMinMax);
      nextStart = geotiff.writeData((float[]) result.getStorage(), pageNumber);
    }

    // set the width and the height
    int height = data.getShape()[0];         // Y
    int width = data.getShape()[1];         // X

    writeMetadata(greyScale, xStart, yStart, xInc, yInc, height, width, pageNumber, nextStart, dataMinMax, proj);
    pageNumber++;
  }
}

