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



package ucar.nc2.iosp.mcidas;


import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.McIDASException;

import ucar.unidata.geoloc.*;
import ucar.unidata.util.Parameter;

import java.awt.geom.Rectangle2D;


/**
 * McIDASAreaProjection is the ProjectionImpl for McIDAS Area navigation
 * modules.
 */
public class McIDASAreaProjection extends ucar.unidata.geoloc.ProjectionImpl {

    /** Attribute for the Area Directory */
    public static String ATTR_AREADIR = "AreaDirectory";

    /** Attribute for the Navigation Block */
    public static String ATTR_NAVBLOCK = "NavBlock";

    /** Attribute for the Navigation Block */
    public static String ATTR_AUXBLOCK = "AuxBlock";

    /** Attribute for the Navigation Block */
    public static String GRID_MAPPING_NAME = "mcidas_area";

    /** Area navigation */                                                  
    private AREAnav anav = null;

    /** number of lines */
    private int lines;

    /** number of elements */
    private int elements;

    /** directory block */
    private int[] dirBlock;

    /** navigation block */
    private int[] navBlock;

    /** aux block */
    private int[] auxBlock;

    /**
     * copy constructor - avoid clone !!
     *
     * @return construct a copy of this
     */
    public ProjectionImpl constructCopy() {
        return new McIDASAreaProjection(dirBlock, navBlock, auxBlock);
    }

    /**
     * Default bean constructor
     */
    public McIDASAreaProjection() {}

    /**
     * create a McIDAS AREA projection from the Area file's
     *  directory and navigation blocks.
     *
     *  This routine uses a flipped Y axis (first line of
     *  the image file is number 0)
     *
     *  @param af is the associated AreaFile
     *
     */
    public McIDASAreaProjection(AreaFile af) {
        this(af.getDir(), af.getNav(), af.getAux());
    }

    /**
     * Create a AREA coordinate system from the Area file's
     * directory and navigation blocks.
     *
     * This routine uses a flipped Y axis (first line of
     * the image file is number 0)
     *
     * @param dir is the AREA file directory block
     * @param nav is the AREA file navigation block
     */
    public McIDASAreaProjection(int[] dir, int[] nav) {
        this(dir, nav, null);
    }

    /**
     * Create a AREA coordinate system from the Area file's
     * directory and navigation blocks.
     *
     * This routine uses a flipped Y axis (first line of
     * the image file is number 0)
     *
     * @param dir is the AREA file directory block
     * @param nav is the AREA file navigation block
     * @param aux is the AREA file auxillary block
     */
    public McIDASAreaProjection(int[] dir, int[] nav, int[] aux) {

        try {
            anav = AREAnav.makeAreaNav(nav, aux);
        } catch (McIDASException excp) {
            throw new IllegalArgumentException(
                "McIDASAreaProjection: problem creating projection" + excp);
        }
        dirBlock = dir;
        navBlock = nav;
        auxBlock = aux;
        anav.setImageStart(dir[5], dir[6]);
        anav.setRes(dir[11], dir[12]);
        anav.setStart(0, 0);
        anav.setMag(1, 1);
        lines    = dir[8];
        elements = dir[9];
        anav.setFlipLineCoordinates(dir[8]);  // invert Y axis coordinates

        addParameter(ATTR_NAME, GRID_MAPPING_NAME);
        addParameter(new Parameter(ATTR_AREADIR, makeDoubleArray(dir)));
        addParameter(new Parameter(ATTR_NAVBLOCK, makeDoubleArray(nav)));
        if (aux != null) {
            addParameter(new Parameter(ATTR_AUXBLOCK, makeDoubleArray(aux)));
        }
    }


    /**
     * Get the directory block used to initialize this McIDASAreaProjection
     *
     * @return the area directory
     */
    public int[] getDirBlock() {
        return dirBlock;
    }

    /**
     * Get the navigation block used to initialize this McIDASAreaProjection
     *
     * @return the navigation block
     */
    public int[] getNavBlock() {
        return navBlock;
    }

    /**
     * Get the auxilliary block used to initialize this McIDASAreaProjection
     *
     * @return the auxilliary block (may be null)
     */
    public int[] getAuxBlock() {
        return auxBlock;
    }

    /*MACROBODY
      latLonToProj {} {
        double[][] xy = anav.toLinEle(new double[][] {{fromLat},{fromLon}});
        toX = xy[0][0];
        toY = xy[1][0];
      }

      projToLatLon {} {
        double[][] latlon = anav.toLatLon(new double[][] {{fromX},{fromY}});
        toLat = latlon[0][0];
        toLon = latlon[1][0];
      }

    MACROBODY*/
    /*BEGINGENERATED*/

    /*
    Note this section has been generated using the convert.tcl script.
    This script, run as:
    tcl convert.tcl McIDASAreaProjection.java
    takes the actual projection conversion code defined in the MACROBODY
    section above and generates the following 6 methods
    */


    /**
     * Convert a LatLonPoint to projection coordinates
     *
     * @param latLon convert from these lat, lon coordinates
     * @param result the object to write to
     *
     * @return the given result
     */
    public ProjectionPoint latLonToProj(LatLonPoint latLon,
                                        ProjectionPointImpl result) {
        double     toX, toY;
        double     fromLat = latLon.getLatitude();
        double     fromLon = latLon.getLongitude();


        double[][] xy      = anav.toLinEle(new double[][] {
            { fromLat }, { fromLon }
        });
        toX = xy[0][0];
        toY = xy[1][0];

        result.setLocation(toX, toY);
        return result;
    }

    /**
     * Convert projection coordinates to a LatLonPoint
     *   Note: a new object is not created on each call for the return value.
     *
     * @param world convert from these projection coordinates
     * @param result the object to write to
     *
     * @return LatLonPoint convert to these lat/lon coordinates
     */
    public LatLonPoint projToLatLon(ProjectionPoint world,
                                    LatLonPointImpl result) {
        double     toLat, toLon;
        double     fromX  = world.getX();
        double     fromY  = world.getY();


        double[][] latlon = anav.toLatLon(new double[][] {
            { fromX }, { fromY }
        });
        toLat = latlon[0][0];
        toLon = latlon[1][0];

        result.setLatitude(toLat);
        result.setLongitude(toLon);
        return result;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n],
     *                 where from[0][i], from[1][i] is the (lat,lon)
     *                 coordinate of the ith point
     * @param to       resulting array of projection coordinates,
     *                 where to[0][i], to[1][i] is the (x,y) coordinate
     *                 of the ith point
     * @param latIndex index of latitude in "from"
     * @param lonIndex index of longitude in "from"
     *
     * @return the "to" array.
     */
    public float[][] latLonToProj(float[][] from, float[][] to, int latIndex,
                                  int lonIndex) {
        float[]   fromLatA = from[latIndex];
        float[]   fromLonA = from[lonIndex];

        float[][] xy       = anav.toLinEle(new float[][] {
            fromLatA, fromLonA
        });
        to[INDEX_X] = xy[0];
        to[INDEX_Y] = xy[1];
        return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n], where
     *                 (from[0][i], from[1][i]) is the (lat,lon) coordinate
     *                 of the ith point
     * @param to       resulting array of projection coordinates: to[2][n]
     *                 where (to[0][i], to[1][i]) is the (x,y) coordinate
     *                 of the ith point
     * @return the "to" array
     */
    public float[][] projToLatLon(float[][] from, float[][] to) {
        float[]   fromXA = from[INDEX_X];
        float[]   fromYA = from[INDEX_Y];
        float[][] latlon = anav.toLatLon(new float[][] {
            fromXA, fromYA
        });
        to[INDEX_LAT] = latlon[0];
        to[INDEX_LON] = latlon[1];
        return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n],
     *                 where from[0][i], from[1][i] is the (lat,lon)
     *                 coordinate of the ith point
     * @param to       resulting array of projection coordinates,
     *                 where to[0][i], to[1][i] is the (x,y) coordinate
     *                 of the ith point
     * @param latIndex index of latitude in "from"
     * @param lonIndex index of longitude in "from"
     *
     * @return the "to" array.
     */
    public double[][] latLonToProj(double[][] from, double[][] to,
                                   int latIndex, int lonIndex) {
        double[]   fromLatA = from[latIndex];
        double[]   fromLonA = from[lonIndex];

        double[][] xy       = anav.toLinEle(new double[][] {
            fromLatA, fromLonA
        });
        to[INDEX_X] = xy[0];
        to[INDEX_Y] = xy[1];
        return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n], where
     *                 (from[0][i], from[1][i]) is the (lat,lon) coordinate
     *                 of the ith point
     * @param to       resulting array of projection coordinates: to[2][n]
     *                 where (to[0][i], to[1][i]) is the (x,y) coordinate
     *                 of the ith point
     * @return the "to" array
     */
    public double[][] projToLatLon(double[][] from, double[][] to) {
        double[]   fromXA = from[INDEX_X];
        double[]   fromYA = from[INDEX_Y];
        double[][] latlon = anav.toLatLon(new double[][] {
            fromXA, fromYA
        });
        to[INDEX_LAT] = latlon[0];
        to[INDEX_LON] = latlon[1];
        return to;
    }

    /*ENDGENERATED*/

    /**
     * Get the bounds for this image
     *
     * @return the projection bounds
     */
    public ProjectionRect getDefaultMapArea() {
        return new ProjectionRect(new Rectangle2D.Float(0, 0, elements,
                lines));
    }

    /**
     *  This returns true when the line between pt1 and pt2 crosses the seam.
     *  When the cone is flattened, the "seam" is lon0 +- 180.
     *
     * @param pt1   point 1
     * @param pt2   point 2
     * @return true when the line between pt1 and pt2 crosses the seam.
     */
    public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
        // either point is infinite
        if (ProjectionPointImpl.isInfinite(pt1)
                || ProjectionPointImpl.isInfinite(pt2)) {
            return true;
        }
        if (Double.isNaN(pt1.getX()) || Double.isNaN(pt1.getY())
                || Double.isNaN(pt2.getX()) || Double.isNaN(pt2.getY())) {
            return true;
        }
        // opposite signed X values, larger then 5000 km
        return (pt1.getX() * pt2.getX() < 0)
               && (Math.abs(pt1.getX() - pt2.getX()) > 5000.0);
    }

    /**
     * Determines whether or not the <code>Object</code> in question is
     * the same as this <code>McIDASAreaProjection</code>.  The specified
     * <code>Object</code> is equal to this <CODE>McIDASAreaProjection</CODE>
     * if it is an instance of <CODE>McIDASAreaProjection</CODE> and it has
     * the same navigation module and default map area as this one.
     *
     * @param obj the Object in question
     *
     * @return true if they are equal
     */
    public boolean equals(Object obj) {
        if ( !(obj instanceof McIDASAreaProjection)) {
            return false;
        }
        McIDASAreaProjection that = (McIDASAreaProjection) obj;
        return (this == that)
               || (anav.equals(that.anav) && (this.lines == that.lines)
                   && (this.elements == that.elements));
    }

    /**
     * Return a String which tells some info about this navigation
     * @return wordy String
     */
    public String toString() {
        return "Image (" + anav.toString() + ") Projection";
    }

    /**
     * Get the parameters as a String
     * @return the parameters as a String
     */
    public String paramsToString() {
        return " nav " + anav.toString();
    }


    /**
     * Test routine
     *
     * @param args  Area file name
     *
     * @throws Exception  problem reading data
     */
    public static void main(String[] args) throws Exception {
        String               file = (args.length > 0)
                                    ? args[0]
                                    : "c:/data/satellite/AREA8760";
        AreaFile             af   = new AreaFile(file);
        McIDASAreaProjection proj = new McIDASAreaProjection(af);
        LatLonPoint          llp  = new LatLonPointImpl(45, -105);
        System.out.println("lat/lon = " + llp);
        ProjectionPoint pp = proj.latLonToProj(llp);
        System.out.println("proj point = " + pp);
        llp = proj.projToLatLon(pp);
        System.out.println("reverse llp = " + llp);
        double[][] latlons = new double[][] {
            { 45 }, { -105 }
        };
        double[][] linele  = proj.latLonToProj(latlons);
        System.out.println("proj point = " + linele[0][0] + ","
                           + linele[1][0]);
        double[][] outll = proj.projToLatLon(linele);
        System.out.println("proj point = " + outll[0][0] + "," + outll[1][0]);
    }

    /**
     * make a double array out of an int array
     *
     * @param ints  array of ints
     *
     * @return  array of doubles
     */
    private double[] makeDoubleArray(int[] ints) {
        double[] newArray = new double[ints.length];
        for (int i = 0; i < ints.length; i++) {
            newArray[i] = ints[i];
        }
        return newArray;
    }
}

