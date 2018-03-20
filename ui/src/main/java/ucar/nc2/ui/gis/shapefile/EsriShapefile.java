/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.gis.shapefile;

import ucar.nc2.ui.gis.AbstractGisFeature;
import ucar.nc2.ui.gis.GisPart;

import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * EsriShapefile.java
 * <p/>
 * Encapsulates details of ESRI Shapefile format, documented at
 * http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf
 *
 * @author Russ Rew
 */
public class EsriShapefile {

  // these are only shape types handled by this package, so far
  enum Type {none, point, polyline, polygon, multipoint}

  private final static int SHAPEFILE_CODE = 9994; // shapefile magic number
  private final static double defaultCoarseness = 0.0;

  private BeLeDataInputStream bdis; // the shapefile data stream
  private int fileBytes;         // bytes in file, according to header
  private int bytesSeen = 0;         // so far, in bytes.
  private int version;         // of shapefile format (currently 1000)
  private Type type;
  private boolean debug;

  private List<EsriFeature> features;        // EsriFeatures in List
  private Rectangle2D listBounds;    // bounds from shapefile
  private double resolution;         // computed from coarseness

  /*
    * Relative accuracy of plotting.  Larger means coarser but
    * faster, 0.0 is all available resolution.  Anything less than 1
    * is wasted sub-pixel plotting, but retains quality for closer
    * zooming.  Anything over about 2.0 is ugly.  1.50 makes things
    * faster than 1.0 at the cost of barely discernible ugliness, but
    * for best quality (without zooms), set to 1.0.  If you still
    * want quality at 10:1 zooms, set to 1/10, etc.  */
  private double coarseness = defaultCoarseness;

  /**
   * Read an ESRI shapefile and extract all features into
   * an in-memory structure.
   *
   * @param filename name of ESRI shapefile (typically has ".shp"
   *                 extension)
   */
  public EsriShapefile(String filename) throws IOException {
    this(filename, null);
  }

  /**
   * Read an ESRI shapefile from a URL and extract all features into
   * an in-memory structure.
   *
   * @param url URL of ESRI shapefile
   */
  public EsriShapefile(URL url) throws IOException {
    this(url, null);
  }


  /**
   * Read an ESRI shapefile and extract all features into
   * an in-memory structure, with control of time versus resolution.
   *
   * @param filename   name of ESRI shapefile (typically has ".shp"
   *                   extension)
   * @param coarseness to tradeoff plot quality versus speed.
   */
  public EsriShapefile(String filename, double coarseness) throws IOException {
    this(filename, null, coarseness);
  }

  /**
   * Read an ESRI shapefile from a URL and extract all features into
   * an in-memory structure, with control of time versus resolution.
   *
   * @param url        URL of ESRI shapefile
   * @param coarseness to tradeoff plot quality versus speed.
   */
  public EsriShapefile(URL url, double coarseness) throws IOException {
    this(url, null, coarseness);
  }


  /**
   * Read an ESRI shapefile and extract the subset of features that have
   * bounding boxes that intersect a specified bounding box
   *
   * @param url        URL of ESRI shapefile
   * @param bBox       bounding box specifying which features to select,
   *                   namely those whose bounding boxes intersect this one. If null,
   *                   bounding box of whole shapefile is used
   * @param coarseness to tradeoff plot quality versus speed.
   */
  public EsriShapefile(URL url, Rectangle2D bBox, double coarseness) throws IOException {
    this(new DataInputStream(url.openStream()), bBox, coarseness);
  }


  /**
   * Read an ESRI shapefile and extract all features into an in-memory
   * structure, with control of time versus resolution.
   *
   * @param filename   name of ESRI shapefile
   * @param bBox       bounding box specifying which features to select,
   *                   namely those whose bounding boxes intersect this one. If null,
   *                   bounding box of whole shapefile is used
   * @param coarseness to tradeoff plot quality versus speed.
   */
  public EsriShapefile(String filename, Rectangle2D bBox, double coarseness) throws IOException {
    this(new FileInputStream(filename), bBox, coarseness);
  }

  /**
   * Read an ESRI shapefile and extract the subset of features that have
   * bounding boxes that intersect a specified bounding box
   *
   * @param url  URL of ESRI shapefile
   * @param bBox bounding box specifying which features to select,
   *             namely those whose bounding boxes intersect this one. If null,
   *             bounding box of whole shapefile is used
   */
  public EsriShapefile(URL url, Rectangle2D bBox) throws IOException {
    this(new DataInputStream(url.openStream()), bBox, 0.0f);
  }


  /**
   * Read an ESRI shapefile and extract the subset of features that have
   * bounding boxes that intersect a specified bounding box.
   *
   * @param filename name of ESRI shapefile
   * @param bBox     bounding box specifying which features to select,
   *                 namely those whose bounding boxes intersect this one. If null,
   *                 bounding box of whole shapefile is used
   */
  public EsriShapefile(String filename, Rectangle2D bBox) throws IOException {
    this(new FileInputStream(filename), bBox, 0.0f);
  }


  /**
   * Read an ESRI shapefile and extract the subset of features that
   * have bounding boxes that intersect a specified bounding box,
   * with control of time versus resolution.
   *
   * @param iStream input from which to read
   * @param bBox    bounding box specifying which features to select,
   *                namely those whose bounding boxes intersect this one. If null,
   *                bounding box of whole shapefile is used
   */
  public EsriShapefile(InputStream iStream, Rectangle2D bBox, double coarseness) throws IOException {
    BufferedInputStream bin = new BufferedInputStream(iStream);

    if (coarseness < 0.0f)
      this.coarseness = defaultCoarseness;
    else
      this.coarseness = coarseness;

    if (isZipStream(bin)) {
      ZipInputStream zin = new ZipInputStream(bin);
      ZipEntry ze;
      while ((ze = zin.getNextEntry()) != null) {
        if (ze.getName().endsWith(".shp") || ze.getName().endsWith(".SHP")) {
          bdis = new BeLeDataInputStream(zin);
          init(bBox);
          return;
        }
        zin.closeEntry();
      }
      throw new IOException("no .shp entry found in zipped input");
    } else {
      bdis = new BeLeDataInputStream(bin);
      init(bBox);
    }
  }

  static boolean isZipStream(InputStream is) throws IOException {
    is.mark(5);
    int c1 = is.read();
    int c2 = is.read();
    int c3 = is.read();
    int c4 = is.read();
    is.reset();
    if (c1 == 'P' && c2 == 'K' && c3 == 0x03 && c4 == 0x04)
      return true;
    return false;
  }

  private void init(Rectangle2D bBox) throws IOException {
    int fileCode = readInt();
    if (fileCode != SHAPEFILE_CODE)
      throw (new IOException("Not a shapefile"));

    skipBytes(20);    // 5 unused ints
    fileBytes = 2 * readInt();
    version = readLEInt();

    type = assignType(readLEInt());
    listBounds = readBoundingBox();

    // if no bounds specified, use shapefile bounds
    if (bBox == null)
      bBox = listBounds;

    double xu = bBox.getMaxX();
    double yu = bBox.getMaxY();
    double xl = bBox.getMinX();
    double yl = bBox.getMinY();
    double w = 1000; // for resolution, just assume 1000x1000 display
    double h = 1000;
    resolution = 1.0 / (coarseness * Math.min(Math.abs(xu - xl) / w, Math.abs(yu - yl) / h));

    skipBytes(32); // skip to start of first record header

    /* Read through file, filtering out features that don't
       intersect bounding box. */
    features = new ArrayList<>();
    while (bytesSeen < fileBytes) {
      EsriFeature gf = nextFeature();
      if (gf.getBounds2D().intersects(bBox))
        features.add(gf);
    }
  }

  private Type assignType(int type) {
    switch (type) {
      case 1 : return Type.point;
      case 3 : return Type.polyline;
      case 5 : return Type.polygon;
      case 8 : return Type.multipoint;
      default:
        throw new RuntimeException("shapefile type "+type+" not supported");
    }
  }

  /**
   * Return percent of file read, so far.
   *
   * @return percent of file read, so far.
   */
  public double percentRead() {
    return (double) bytesSeen / (double) fileBytes;
  }

  /**
   * @return number of features in shapefile
   */
  public int getNumFeatures() {
    return features.size();
  }

  private Rectangle2D readBoundingBox() throws IOException {

    double xMin = readLEDouble();
    double yMin = readLEDouble();
    double xMax = readLEDouble();
    double yMax = readLEDouble();
    double width = xMax - xMin;
    double height = yMax - yMin;

    return new Rectangle2D.Double(xMin, yMin, width, height);
  }

  private EsriFeature nextFeature() throws IOException {

    int recordNumber = readInt(); // starts at 1, not 0
    int contentLength = readInt(); // in 16-bit words
    if (debug) System.out.printf("recordNumber=%d contentLength=%d%n", recordNumber, contentLength);
    int type = readLEInt();
    Type featureType = assignType(type);
    switch (featureType) {
      case point:
        return new EsriPoint();
      case multipoint:    // multipoint, only 1 part
        return new EsriMultipoint();
      case polyline:
        return new EsriPolyline();
      case polygon:
        return new EsriPolygon();
      default:
        throw new IOException("can't handle shapefile shape type " + featureType);
    }
  }

  private int readLEInt() throws IOException {
    bytesSeen += 4;
    return bdis.readLEInt();
  }

  private int readInt() throws IOException {
    bytesSeen += 4;
    return bdis.readInt();
  }

  private double readLEDouble() throws IOException {
    bytesSeen += 8;
    return bdis.readLEDouble();
  }

  private void readLEDoubles(double[] d, int n) throws IOException {
    bdis.readLEDoubles(d, n);
    bytesSeen += 8 * n;
  }

  private void skipBytes(int n) throws IOException {
    long skipped = 0;
    while (skipped < n)
      skipped += bdis.skip(n-skipped);
    bytesSeen += n;
  }

  /**
   * Returns shapefile format version (currently 1000)
   *
   * @return version, as stored in shapefile.
   */
  public int getVersion() {
    return version;
  }

  /**
   * Get bounding box, according to file (not computed from features)
   *
   * @return bounding box for shapefilew, as stored in header.
   */
  public Rectangle2D getBoundingBox() {
    return listBounds;
  }

  /**
   * Get a List of all the GisFeatures in the shapefile.  This is
   * very fast after the constructor has been called, since it is
   * created during construction.
   *
   * @return a List of features
   */
  public java.util.List getFeatures() {
    return features;
  }

  /**
   * Get a List of all the features in the shapefile that intersect
   * the specified bounding box.  This requires testing every
   * feature in the List created at construction, so it's faster to
   * just give a bounding box o the constructor if you will only do
   * this once.
   *
   * @param bBox specifying the bounding box with which all
   *             the returned features bounding boxes have a non-empty
   *             intersection.
   * @return a new list of features in the shapefile whose bounding
   *         boxes intersect the specified bounding box.
   */
  public List<EsriFeature> getFeatures(Rectangle2D bBox) {
    if (bBox == null)
      return features;

    List<EsriFeature> list = new ArrayList<>();
    for (EsriFeature gf : features) {
      if (gf.getBounds2D().intersects(bBox))
        list.add(gf);
    }
    return list;
  }

  /**
   * EsriFeature.java
   * <p/>
   * <p/>
   * Created: Sat Feb 20 17:19:53 1999
   *
   * @author Russ Rew
   */
  public abstract class EsriFeature extends AbstractGisFeature {
    protected Rectangle2D bounds;
    protected int numPoints;
    protected int numParts;
    protected List<GisPart> partsList = new ArrayList<>();

    // private DbaseFile dbfile;
    // private int recordNumber;
    // Together these can be used to access more info about feature,
    // using the associated .dbf Dbase file.
    // TODO: extend interface to permit access to this info

    /**
     * Get bounding rectangle for this feature.
     *
     * @return bounding rectangle for this feature.
     */
    public Rectangle2D getBounds2D() {
      return bounds;
    }

    /**
     * Get total number of points in all parts of this feature.
     *
     * @return total number of points in all parts of this feature.
     */
    public int getNumPoints() {
      return numPoints;
    }

    /**
     * Get number of parts comprising this feature.
     *
     * @return number of parts comprising this feature.
     */
    public int getNumParts() {
      return numParts;
    }

    /**
     * Get the parts of this feature, in the form of an iterator.
     *
     * @return the iterator over the parts of this feature.  Each part
     *         is a GisPart.
     */
    public java.util.Iterator getGisParts() {
      return partsList.iterator();
    }

    @Override
    public String toString() {
      return "EsriFeature{" +
              "numPoints=" + numPoints +
              ", numParts=" + numParts +
      //        ", npartsList=" + partsList +
              ", bounds=" + bounds +
              '}';
    }
  } // EsriFeature


  private double[] xyPoints = new double[100]; // buffer for points input

  /**
   * Discretize elements of array to a lower resolution.  For
   * example, if resolution = 100., the value 3.14159265358979 will
   * be changed to 3.14.
   *
   * @param d array of values to discretize to lower resolution
   * @param n number of values in array to discretize
   */
  private void discretize(double[] d, int n) {
    if (coarseness == 0.0)
      return;
    for (int i = 0; i < n; i++) {
      d[i] = (Math.rint(resolution * d[i]) / resolution);
    }
  }

  /**
   * Represents a Polygon in an ESRI shapefile as a List of
   * GisParts.  A Polygon is just an ordered set of vertices of 1 or
   * more parts, where a part is a closed connected sequence of
   * points.  A state boundary might be represented by a Polygon,
   * for example, where each part might be the main part or islands.
   * <p/>
   * Created: Sat Feb 20 17:19:53 1999
   *
   * @author Russ Rew
   */
  public class EsriPolygon extends EsriFeature {
    public EsriPolygon()
            throws java.io.IOException {
      bounds = readBoundingBox();
      numParts = readLEInt();
      numPoints = readLEInt();
      int[] parts = new int[numParts + 1];
      for (int j = 0; j < numParts; j++) {
        parts[j] = readLEInt();
      }
      parts[numParts] = numPoints;

      if (xyPoints.length < 2 * numPoints)
        xyPoints = new double[2 * numPoints];
      readLEDoubles(xyPoints, 2 * numPoints);
      discretize(xyPoints, 2 * numPoints); // overwrites xyPoints

      /* numPoints is reduced by removing dupl. discretized points */
      numPoints = 0;
      int ixy = 0;
      int numPartsLeft = 0; // may be < numParts after eliminating 1-point parts
      for (int part = 0; part < numParts; part++) {
        int pointsInPart = parts[part + 1] - parts[part];
        /* remove duplicate discretized points in part constructor */
        GisPart gp = new EsriPart(pointsInPart, xyPoints, ixy);
        /* Only add a part if it has 2 or more points, after duplicate
      point removal */
        if (gp.getNumPoints() > 1) {
          partsList.add(gp);
          numPoints += gp.getNumPoints();
          numPartsLeft++;
        }
        ixy += 2 * pointsInPart;
      }
      numParts = numPartsLeft;
    }
  } // EsriPolygon


  /**
   * Represents a Polyline in an ESRI shapefile as a List of
   * GisParts.  A Polyline is just an ordered set of vertices of 1
   * or more parts, where a part is a connected sequence of points.
   * A river including its tributaries might be represented by a
   * Polyine, for example, where each part would be a branch of the
   * river.
   * <p/>
   * Created: Sat Feb 20 17:19:53 1999
   *
   * @author Russ Rew
   */
  public class EsriPolyline extends EsriFeature {

    public EsriPolyline() throws java.io.IOException {
      bounds = readBoundingBox();
      numParts = readLEInt();
      numPoints = readLEInt();
      int[] parts = new int[numParts + 1];
      for (int j = 0; j < numParts; j++) {
        parts[j] = readLEInt();
      }
      parts[numParts] = numPoints;

      if (xyPoints.length < 2 * numPoints)
        xyPoints = new double[2 * numPoints];
      readLEDoubles(xyPoints, 2 * numPoints);
      discretize(xyPoints, 2 * numPoints); // overwrites xyPoints

      /* numPoints is reduced by removing dupl. discretized points */
      numPoints = 0;
      int ixy = 0;
      int numPartsLeft = 0; // may be < numParts after eliminating 1-point parts
      for (int part = 0; part < numParts; part++) {
        int pointsInPart = parts[part + 1] - parts[part];
        /* remove duplicate discretized points in part constructor */
        GisPart gp = new EsriPart(pointsInPart, xyPoints, ixy);
        /* Only add a part if it has 2 or more points, after duplicate
      point removal */
        if (gp.getNumPoints() > 1) {
          partsList.add(gp);
          numPoints += gp.getNumPoints();
          numPartsLeft++;
        }
        ixy += 2 * pointsInPart;
      }
      numParts = numPartsLeft;
    }
  } // EsriPolyline


  /**
   * Represents a Multipoint in an ESRI shapefile.  A
   * Multipoint is a set of 2D points.
   * <p/>
   * Created: Sat Feb 20 17:19:53 1999
   *
   * @author Russ Rew
   */
  public class EsriMultipoint extends EsriFeature {

    public EsriMultipoint() throws java.io.IOException {
      bounds = readBoundingBox();
      int numPoints = readLEInt();
      if (xyPoints.length < 2 * numPoints)
        xyPoints = new double[2 * numPoints];
      readLEDoubles(xyPoints, 2 * numPoints);
      discretize(xyPoints, 2 * numPoints);
      GisPart gp = new EsriPart(numPoints, xyPoints, 0);
      partsList.add(gp);
    }
  } // EsriMultipoint


  /**
   * Represents a single point in an ESRI shapefile.
   * <p/>
   * Created: Sat Feb 20 17:19:53 1999
   *
   * @author Russ Rew
   */
  public class EsriPoint extends EsriFeature {

    public EsriPoint() throws java.io.IOException {

      int numPoints = 1;
      readLEDoubles(xyPoints, 2 * numPoints);
      discretize(xyPoints, 2 * numPoints);
      GisPart gp = new EsriPart(numPoints, xyPoints, 0);
      partsList.add(gp);
      bounds = new Rectangle2D.Double(xyPoints[0], xyPoints[1], 0., 0.);
    }
  } // EsriPoint


  /**
   * A NULL shape in an ESRI shapefile.
   * <p/>
   * Created: Sat Feb 20 17:19:53 1999
   *
   * @author Russ Rew
   */
  public class EsriNull extends EsriFeature {
  }

  private static class EsriPart implements GisPart {
    private int numPoints = 0;
    private double[] x;
    private double[] y;

    /**
     * Construct an EsriPart by eliding duplicates from array
     * representing points.
     *
     * @param num      number of input points to use
     * @param xyPoints array containing consecutive (x,y) pair for
     *                 each point.
     * @param xyOffset index in array from which to start
     */
    public EsriPart(int num, double[] xyPoints, int xyOffset) {
      double xi, yi;
      /* In first pass over data, just count nonduplicated points */
      int ixy = xyOffset;
      numPoints = 1;
      double xx = xyPoints[ixy++];
      double yy = xyPoints[ixy++];
      for (int i = 1; i < num; i++) {
        xi = xyPoints[ixy++];
        yi = xyPoints[ixy++];
        if (xi != xx || yi != yy) {
          numPoints++;
          xx = xi;
          yy = yi;
        }
      }

      /* second pass: store nonduplicated points */
      x = new double[numPoints];
      y = new double[numPoints];
      ixy = xyOffset;
      int j = 0;
      x[j] = xyPoints[ixy++];
      y[j] = xyPoints[ixy++];
      xx = x[j];
      yy = y[j];
      for (int i = 1; i < num; i++) {
        xi = xyPoints[ixy++];
        yi = xyPoints[ixy++];
        if (xi != xx || yi != yy) {
          j++;
          x[j] = xi;
          y[j] = yi;
          xx = x[j];
          yy = y[j];
        }
      }
    }

    public int getNumPoints() {
      return numPoints;
    }

    public double[] getX() {
      return x;
    }

    public double[] getY() {
      return y;
    }

  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    f.format("Type=%s version=%d bounds=%s resolution=%f%n", type, version, listBounds, resolution);
    for (EsriFeature ft : features)
      f.format(" %s%n", ft);
    return f.toString();
  }

  static public void main(String args[]) throws IOException {
    String fname = "C:\\data\\g4g/EcoAtlas_modern_baylands.shp";
    EsriShapefile esri = new EsriShapefile(fname);
    System.out.printf("%s%n",esri);
  }
}
