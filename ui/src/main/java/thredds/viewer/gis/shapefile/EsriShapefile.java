// $Id$
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
package thredds.viewer.gis.shapefile;

import thredds.datamodel.gis.*;

import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * EsriShapefile.java
 *
 * Encapsulates details of ESRI Shapefile format, documented at
 * http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf
 *
 * @author Russ Rew
 * @version $Revision$  $Date$
 */
public class EsriShapefile {

    public final static int SHAPEFILE_CODE = 9994; // shapefile magic number

    // these are only shape types handled by this package, so far
    public final static int NULL = 0;
    public final static int POINT = 1;
    public final static int POLYLINE = 3;
    public final static int POLYGON = 5;
    public final static int MULTIPOINT = 8;

    // Eventually we should handle these new types also (is anyone using these?)
    public final static int POINTZ = 11;
    public final static int POLYLINEZ = 13;
    public final static int POLYGONZ = 15;
    public final static int MULTIPOINTZ = 18;
    public final static int POINTM = 21;
    public final static int POLYLINEM = 23;
    public final static int POLYGONM = 25;
    public final static int MULTIPOINTM = 28;
    public final static int MULTIPATCH = 31;

    private BeLeDataInputStream bdis; // the shapefile data stream
    private int fileBytes;	       // bytes in file, according to header
    private int bytesSeen = 0;	       // so far, in bytes.
    private int version;	       // of shapefile format (currently 1000)
    private int fileShapeType;         // not used here
    private ArrayList features;        // EsriFeatures in List
    private Rectangle2D listBounds;    // bounds from shapefile
    private double resolution;	       // computed from coarseness
    private final static double defaultCoarseness = 0.0;
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
     *
     * Read an ESRI shapefile and extract all features into
     * an in-memory structure.
     *
     * @param filename name of ESRI shapefile (typically has ".shp"
     *        extension)
     */
    public EsriShapefile(String filename)
        throws IOException {
        this(filename, null);
    }

    /**
     *
     * Read an ESRI shapefile from a URL and extract all features into
     * an in-memory structure.
     *
     * @param url URL of ESRI shapefile
     */
    public EsriShapefile(URL url)
        throws IOException {
        this(url, null);
    }


    /**
     *
     * Read an ESRI shapefile and extract all features into
     * an in-memory structure, with control of time versus resolution.
     *
     * @param filename name of ESRI shapefile (typically has ".shp"
     *        extension)
     * @param coarseness to tradeoff plot quality versus speed.
     */
    public EsriShapefile(String filename, double coarseness)
        throws IOException {
        this(filename, null, coarseness);
    }

    /**
     *
     * Read an ESRI shapefile from a URL and extract all features into
     * an in-memory structure, with control of time versus resolution.
     *
     * @param url URL of ESRI shapefile
     * @param coarseness to tradeoff plot quality versus speed.
     */
    public EsriShapefile(URL url, double coarseness)
        throws IOException {
        this(url, null, coarseness);
    }


    /**
     *
     * Read an ESRI shapefile and extract the subset of features that have
     * bounding boxes that intersect a specified bounding box
     *
     * @param url URL of ESRI shapefile
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     * @param coarseness to tradeoff plot quality versus speed.
     */
    public EsriShapefile(URL url, Rectangle2D bBox, double coarseness)
        throws IOException {
        this(new DataInputStream(url.openStream()), bBox, coarseness);
    }


    /**
     *
     * Read an ESRI shapefile and extract all features into an in-memory
     * structure, with control of time versus resolution.
     *
     * @param filename name of ESRI shapefile
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     * @param coarseness to tradeoff plot quality versus speed.
     */
    public EsriShapefile(String filename, Rectangle2D bBox, double coarseness)
        throws IOException {
        this(new FileInputStream(filename), bBox, coarseness);
    }

    /**
     *
     * Read an ESRI shapefile and extract the subset of features that have
     * bounding boxes that intersect a specified bounding box
     *
     * @param url URL of ESRI shapefile
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     */
    public EsriShapefile(URL url, Rectangle2D bBox)
        throws IOException {
        this(new DataInputStream(url.openStream()), bBox, 0.0f);
    }


    /**
     *
     * Read an ESRI shapefile and extract the subset of features that have
     * bounding boxes that intersect a specified bounding box.
     *
     * @param filename name of ESRI shapefile
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     */
    public EsriShapefile(String filename, Rectangle2D bBox)
        throws IOException {
        this(new FileInputStream(filename), bBox, 0.0f);
    }


    /**
     *
     * Read an ESRI shapefile and extract the subset of features that
     * have bounding boxes that intersect a specified bounding box,
     * with control of time versus resolution.
     *
     * @param iStream input from which to read
     * @param bBox bounding box specifying which features to select,
     * namely those whose bounding boxes intersect this one. If null,
     * bounding box of whole shapefile is used
     */
    public EsriShapefile(InputStream iStream, Rectangle2D bBox,
                         double coarseness)
        throws IOException {
        BufferedInputStream bin = new BufferedInputStream(iStream);

        if (coarseness < 0.0f)
            this.coarseness = defaultCoarseness;
        else
            this.coarseness = coarseness;
        if (isZipStream(bin)) {
            ZipInputStream zin = new ZipInputStream(bin);
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.getName().endsWith(".shp") ||
                    ze.getName().endsWith(".SHP")) {
                    bdis = new BeLeDataInputStream(zin);
                    Init(bBox);
                    return;
                }
                zin.closeEntry();
            }
            throw new IOException("no .shp entry found in zipped input");
        } else {
            bdis = new BeLeDataInputStream(bin);
            Init(bBox);
        }
    }


    static boolean isZipStream(InputStream is)
        throws IOException {
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

    private void Init(Rectangle2D bBox)
        throws IOException {
        int fileCode = readInt();
        if (fileCode != SHAPEFILE_CODE) {
            throw (new IOException("Not a shapefile"));
        }
        skipBytes(20);		// 5 unused ints
        fileBytes = 2 * readInt();
        version = readLEInt();
        fileShapeType = readLEInt();
        listBounds = readBoundingBox();

        // if no bounds specified, use shapefile bounds
        if (bBox == null) {
            bBox = listBounds;
        }

        double xu = bBox.getMaxX();
        double yu = bBox.getMaxY();
        double xl = bBox.getMinX();
        double yl = bBox.getMinY();
        double w = 1000; // for resolution, just assume 1000x1000 display
        double h = 1000;
        resolution = 1.0/(coarseness * Math.min(Math.abs(xu-xl)/w,
                                                Math.abs(yu-yl)/h));

        skipBytes(32); // skip to start of first record header

        /* Read through file, filtering out features that don't
           intersect bounding box. */
        features = new ArrayList();
        while (bytesSeen < fileBytes) {
            GisFeature gf = nextFeature();
            if (gf.getBounds2D().intersects(bBox))
                features.add(gf);
        }
    }


  /**
   * Return percent of file read, so far.
   *
   * @return percent of file read, so far.
   */
  public double percentRead() {
    return (double)bytesSeen / (double)fileBytes;
  }

    /**
     * @return number of features in shapefile
     */
    public int getNumFeatures() {
        return features.size();
    }

    /**
     * @return number of features in shapefile
     * @deprecated
     */
    public int numShapes() {
        return features.size();
    }

    private Rectangle2D readBoundingBox()
        throws IOException {

        double xMin = readLEDouble();
        double yMin = readLEDouble();
        double xMax = readLEDouble();
        double yMax = readLEDouble();
        double width  = xMax-xMin;
        double height = yMax-yMin;

        return new Rectangle2D.Double(xMin, yMin, width, height);
    }

    private EsriFeature nextFeature() throws IOException {

        int recordNumber  = readInt(); // starts at 1, not 0
        int contentLength = readInt(); // in 16-bit words
        int featureType   = readLEInt();

        switch(featureType){
        case EsriShapefile.NULL:			// placeholder
            return new EsriNull();
        case EsriShapefile.POINT:			// point data
            return new EsriPoint();
        case EsriShapefile.MULTIPOINT:		// multipoint, only 1 part
            return new EsriMultipoint();
        case EsriShapefile.POLYLINE:		// arcs
            return new EsriPolyline();
        case EsriShapefile.POLYGON:		// polygon
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
        bytesSeen += 8*n;
    }

    private void skipBytes(int n) throws IOException {
        bdis.skip(n);
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
     * the returned features bounding boxes have a non-empty
     * intersection.
     * @return a new list of features in the shapefile whose bounding
     * boxes intersect the specified bounding box.
     */
    public java.util.List getFeatures(Rectangle2D bBox) {
        if (bBox == null)
            return features;
        List list = new ArrayList();
        for (Iterator i = features.iterator(); i.hasNext(); ) {
            EsriFeature gf = (EsriFeature) i.next();
            if (gf.getBounds2D().intersects(bBox))
                list.add(gf);
        }
        return list;
    }

    /**
     * EsriFeature.java
     *
     *
     * Created: Sat Feb 20 17:19:53 1999
     *
     * @author Russ Rew
     */
    public abstract class EsriFeature
        extends AbstractGisFeature
    {
        protected Rectangle2D bounds;
        protected int numPoints;
        protected int numParts;
        protected List partsList = new ArrayList();

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
         * is a GisPart.
         */
        public java.util.Iterator getGisParts() {
            return partsList.iterator();
        }

    } // EsriFeature


    private double[] xyPoints = new double[100]; // buffer for points input

    /**
     * Discretize elements of array to a lower resolution.  For
     * example, if resolution = 100., the value 3.14159265358979 will
     * be changed to 3.14.
     *
     * @param d array of values to discretize to lower resolution
     * @param n number of values in array to discretize */
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
     *
     * Created: Sat Feb 20 17:19:53 1999
     *
     * @author Russ Rew
     */
    public class EsriPolygon extends EsriFeature
    {
        public EsriPolygon()
            throws java.io.IOException {
            bounds = readBoundingBox();
            numParts = readLEInt();
            numPoints = readLEInt();
            int[] parts = new int[numParts + 1];
            for (int j = 0; j < numParts; j++){
                parts[j] = readLEInt();
            }
            parts[numParts] = numPoints;

            if (xyPoints.length < 2*numPoints)
                xyPoints = new double[2*numPoints];
            readLEDoubles(xyPoints, 2*numPoints);
            discretize(xyPoints, 2*numPoints); // overwrites xyPoints

            /* numPoints is reduced by removing dupl. discretized points */
            numPoints = 0;
            int ixy = 0;
            int numPartsLeft = 0; // may be < numParts after eliminating 1-point parts
            for (int part = 0; part < numParts; part++) {
                int pointsInPart = parts[part+1] - parts[part];
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
     *
     * Created: Sat Feb 20 17:19:53 1999
     *
     * @author Russ Rew
     */
    public class EsriPolyline extends EsriFeature
    {
        public EsriPolyline()
            throws java.io.IOException {
            bounds = readBoundingBox();
            numParts = readLEInt();
            numPoints = readLEInt();
            int[] parts = new int[numParts + 1];
            for (int j = 0; j < numParts; j++){
                parts[j] = readLEInt();
            }
            parts[numParts] = numPoints;

            if (xyPoints.length < 2*numPoints)
                xyPoints = new double[2*numPoints];
            readLEDoubles(xyPoints, 2*numPoints);
            discretize(xyPoints, 2*numPoints); // overwrites xyPoints

            /* numPoints is reduced by removing dupl. discretized points */
            numPoints = 0;
            int ixy = 0;
            int numPartsLeft = 0; // may be < numParts after eliminating 1-point parts
            for (int part = 0; part < numParts; part++) {
                int pointsInPart = parts[part+1] - parts[part];
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
     *
     * Created: Sat Feb 20 17:19:53 1999
     *
     * @author Russ Rew
     */
    public class EsriMultipoint extends EsriFeature
    {

        public EsriMultipoint()
            throws java.io.IOException {

            bounds = readBoundingBox();
            int numPoints = readLEInt();
            if (xyPoints.length < 2*numPoints)
                xyPoints = new double[2*numPoints];
            readLEDoubles(xyPoints, 2*numPoints);
            discretize(xyPoints, 2*numPoints);
            GisPart gp = new EsriPart(numPoints, xyPoints, 0);
            partsList.add(gp);
        }
    } // EsriMultipoint


    /**
     * Represents a single point in an ESRI shapefile.
     *
     * Created: Sat Feb 20 17:19:53 1999
     *
     * @author Russ Rew
     */
    public class EsriPoint extends EsriFeature
    {

        public EsriPoint()
            throws java.io.IOException {

            int numPoints = 1;
            readLEDoubles(xyPoints, 2*numPoints);
            discretize(xyPoints, 2*numPoints);
            GisPart gp = new EsriPart(numPoints, xyPoints, 0);
            partsList.add(gp);
            bounds = new Rectangle2D.Double(xyPoints[0], xyPoints[1], 0., 0.);
        }
    } // EsriPoint


    /**
     * A NULL shape in an ESRI shapefile.
     *
     * Created: Sat Feb 20 17:19:53 1999
     *
     * @author Russ Rew
     */
    public class EsriNull extends EsriFeature
    {

        public EsriNull() {
            int numPoints = 0;
        }
    } // EsriNull

    class EsriPart implements GisPart {
        private int numPoints = 0;
        private double[] x;
        private double[] y;

        /**
         * Construct an EsriPart by eliding duplicates from array
         * representing points.
         *
         * @param num number of input points to use
         * @param xyPoints array containing consecutive (x,y) pair for
         * each point.
         * @param xyOffset index in array from which to start
         */
        public EsriPart(int num, double[] xyPoints, int xyOffset) {
            double xi, yi;
            /* In first pass over data, just count nonduplicated points */
            int ixy = xyOffset;
            numPoints = 1;
            double xx = xyPoints[ixy++];
            double yy = xyPoints[ixy++];
            for (int i=1; i < num; i++) {
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
            for (int i=1; i < num; i++) {
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
}


/* Change History:
   $Log: EsriShapefile.java,v $
   Revision 1.3  2005/04/02 19:50:56  caron
   cat 1.1 - first pass

   Revision 1.2  2004/09/24 03:26:38  caron
   merge nj22

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:50  caron
   import sources

   Revision 1.18  2000/08/18 04:15:27  russ
   Licensed under GNU LGPL.

   Revision 1.17  2000/05/25 19:33:23  russ
   Fixed empty @version tags.

   Revision 1.16  2000/01/21 19:45:42  russ
   Add "coarseness" constructors to permit control of
   time versus resolution tradeoff in displaying and manipulating map
   outlines.

   Revision 1.15  2000/01/04 20:25:02  russ
   Took out main() method used for testing, moved to separate testing
   class, to make EsriShapefile independent of VisAD.
   Added new ucar.visad.ShapefileAdapter class to hide EsriShapefile stuff.

   Revision 1.14  2000/01/03 15:59:07  russ
   Factor the constructors better, in preparation for ucar.visad.ShapefileAdapter.

   Revision 1.13  1999/12/30 23:10:42  russ
   Use isZipStream(InputStream) for testing whether input is zipped.

   Revision 1.12  1999/12/30 22:46:55  russ
   Documentation fixes.
   Make numShapes() method deprecated (bad name) and add getNumFeatures()
   as replacement.
   Add isZipStream() method for determining if input is zipped.

   Revision 1.11  1999/12/29 21:47:45  russ
   Add testing and timing code to main() for GisFeature.getSampledSet() method.

   Revision 1.10  1999/12/29 18:06:48  russ
   Added getNumParts() implementation to EsriFeature inner class.

   Revision 1.9  1999/12/29 02:45:54  russ
   Finish implementing bounding box methods.

   Revision 1.8  1999/12/28 23:38:51  russ
   Make bounding box supplied to constructor work as advertised.

   Revision 1.7  1999/12/28 17:13:18  russ
   Eliminate unnecesssary dependence on Java2D.  Removed coarseness
   parameter for smaller coarser resolution maps (may add back in
   later).  Allow use of .zip files in constructor with bounding box.
   Made EsriFeature extend AbstractGisFeature for getShape() method.
   Have getGisParts() return iterator for list of GisPart.  Cosmetic
   changes to EsriShapefileRenderer, ShapefileShapeList.

   Revision 1.6  1999/12/15 15:14:49  russ
   Add capabilities for reading from URLs and .zip -compressed streams.

   Revision 1.5  1999/07/28 19:30:56  russ
   Adapted EsriShapefile to read from a DataInputStream instead of a
   RandomAccessFile.  Added URL constructor, so can read from a URL.
   Instead of using file length, read until EOF.  Still need to make
   independent of Java2D ...

   Removed java.awt.Dimension parameter from ShapefileShapeList
   constructor used in determining line segments to omit (just assume
   1000 pixel display).

   Removed unused PathIterator from EsriShapefileRenderer.

   Revision 1.4  1999/06/11 21:27:59  russ
   Cosmetic changes, preperatory to eliminating use of RandomAccessFile I/O.

   Revision 1.3  1999/06/03 01:43:57  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:26:24  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:42  caron
   startAgain

# Revision 1.4  1999/03/16  20:46:09  russ
# Added a main() and a numShapes() method for testing.
#
   # Revision 1.3  1999/03/05  21:35:54  russ
   # Oops, accidentatl re-indenting.
   # Also fixed some inner class constructors to not take file parameter.
   # Added add90 mmethod to ShapefileShapeList so can create new lists easily.
   #
   # Revision 1.2  1999/03/01  22:48:18  russ
   # Added explanatory comments.  Made default coarseness 0.1 instead of 1.0.
   #
   # Revision 1.1  1999/03/01  17:53:38  russ
   # Remove most of first draft at this.  Use List instead of Set to
   # collect Shapes.  Adapt to Java2D and use Java2D Shapes.
   #
*/
