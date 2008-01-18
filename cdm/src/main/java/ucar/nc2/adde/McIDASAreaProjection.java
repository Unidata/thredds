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
package ucar.nc2.adde;

import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.McIDASException;

import ucar.unidata.geoloc.*;

import java.awt.geom.Rectangle2D;

/**
 * McIDASAreaProjection is the ProjectionImpl for McIDAS Area navigation
 * modules.
 */
public class McIDASAreaProjection extends ucar.unidata.geoloc.ProjectionImpl {

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

    // aux Block - needed for conxtructCopy
    private int[] auxBlock;

      /** copy constructor - avoid clone !! */
    public ProjectionImpl constructCopy() {
      return new McIDASAreaProjection(dirBlock, navBlock, auxBlock);
    }

    // needed for beans
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
        anav.setImageStart(dir[5], dir[6]);
        anav.setRes(dir[11], dir[12]);
        anav.setStart(0, 0);
        anav.setMag(1, 1);
        lines    = dir[8];
        elements = dir[9];
        anav.setFlipLineCoordinates(dir[8]);  // invert Y axis coordinates

        addParameter(ATTR_NAME, "mcidas_area");
        addParameter("AreaHeader", dir.toString());
        addParameter("NavHeader", nav.toString());
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
        float[] fromLatA = from[latIndex];
        float[] fromLonA = from[lonIndex];

        float[][] xy = anav.toLinEle(new float[][] { fromLatA, fromLonA });
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
        float[] fromXA = from[INDEX_X];
        float[] fromYA = from[INDEX_Y];
        float[][] latlon = anav.toLatLon(new float[][] { fromXA, fromYA });
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
    public double[][] latLonToProj(double[][] from, double[][] to, int latIndex,
                                  int lonIndex) {
        double[] fromLatA = from[latIndex];
        double[] fromLonA = from[lonIndex];

        double[][] xy = anav.toLinEle(new double[][] { fromLatA, fromLonA });
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
        double[] fromXA = from[INDEX_X];
        double[] fromYA = from[INDEX_Y];
        double[][] latlon = anav.toLatLon(new double[][] { fromXA, fromYA });
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
        String           file = (args.length > 0)
                                ? args[0]
                                : "c:/data/satellite/AREA8760";
        AreaFile         af   = new AreaFile(file);
        McIDASAreaProjection proj = new McIDASAreaProjection(af);
        LatLonPoint      llp  = new LatLonPointImpl(45, -105);
        System.out.println("lat/lon = " + llp);
        ProjectionPoint pp = proj.latLonToProj(llp);
        System.out.println("proj point = " + pp);
        llp = proj.projToLatLon(pp);
        System.out.println("reverse llp = " + llp);
        double[][] latlons = new double[][] { {45},{-105} };
        double[][] linele = proj.latLonToProj(latlons);
        System.out.println("proj point = " + linele[0][0] +","+linele[1][0]);
        double[][] outll = proj.projToLatLon(linele);
        System.out.println("proj point = " + outll[0][0] +","+outll[1][0]);
    }
}

