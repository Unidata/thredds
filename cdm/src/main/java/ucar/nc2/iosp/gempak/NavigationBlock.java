/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
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


package ucar.nc2.iosp.gempak;

//import ucar.nc2.iosp.grid.GridDefRecord;
import ucar.grid.GridDefRecord;


/**
 * Class to hold the grid navigation information.
 *  <pre>
 *     PROJ is the map projection, projection angles, and margins separated
 *     by slashes and an optional image drop flag separated from the rest by
 *     a bar:
 *
 *       map proj / ang1;ang2;ang3 / l;b;r;t (margins) | image drop flag
 *
 *     For all map projections, the lower left and upper right corners of
 *     the graphics area should be specified in GAREA.
 *
 *     The following simple map projections may be specified:
 *
 *       MER     Mercator
 *       NPS     North Polar Stereographic
 *       SPS     South Polar Stereographic
 *       LCC     Northern Hemisphere Lambert Conic Conformal
 *       SCC     Southern Hemisphere Lambert Conic Conformal
 *       CED     Cylindrical Equidistant
 *       MCD     Modified Cylindrical Equidistant
 *       NOR     North Orthographic
 *       SOR     South Orthographic
 *
 *
 *     The following full map projections may also be specified:
 *
 *       MER     (CYL)   Mercator
 *       CED     (CYL)   Cylindrical Equidistant
 *       MCD     (CYL)   Modified Cylindrical Equidistant
 *       STR     (AZM)   Polar Stereographic
 *       AED     (AZM)   Azimuthal Equidistant
 *       ORT     (AZM)   Orthographic
 *       LEA     (AZM)   Lambert equal area
 *       GNO     (AZM)   Gnomonic
 *       LCC     (CON)   Northern Hemisphere Lambert Conic Conformal
 *       SCC     (CON)   Southern Hemisphere Lambert Conic Conformal
 *
 *
 *     In addition for full map projections, three angles MUST be specified
 *     in PROJ.  The angles have the following meanings for the different
 *     projection classes:
 *
 *       CYL     angle1 -- latitude of origin on the projection cylinder
 *                         0 = Equator
 *               angle2 -- longitude of origin on the projection cylinder
 *                         0 = Greenwich meridian
 *               angle3 -- angle to skew the projection by rotation of
 *                         the cylindrical surface of projection about
 *                         the line from the Earth's center passing
 *                         through the origin point. This results in
 *                         curved latitude and longitude lines.
 *
 *                         If angle3 is greater than 360 or less than -360
 *                         degrees, then the rectangular Cartesian coordinate
 *                         system on the projection plane is rotated
 *                         +/- |angle3|-360 degrees. This results in
 *                         latitude and longitude lines that are skewed
 *                         with respect to the edge of the map.  This option
 *                          is only valid when specifying a map projection and
 *                          is not available for grid projections.
 *
 *                         The difference between |angle3| < 360 and
 *                         |angle3| > 360 is that, in the former case,
 *                         the rotation is applied to the developable
 *                         cylindrical surface before projection and
 *                         subsequent development; while, in the latter
 *                         case, the rotation is applied to the Cartesian
 *                         coordinate system in the plane after development.
 *                         Development here refers to the mathematical
 *                         flattening of the surface of projection into a
 *                         planar surface.
 *
 *     Exception:
 *
 *     MCD     angle1 -- scaling factor for latitude
 *                       0 = default scaling (1/cos(avglat))
 *             angle2 -- longitude of origin (center longitude)
 *             angle3 -- not used
 *
 *
 *     AZM     angle1 -- latitude of the projection's point of tangency
 *             angle2 -- longitude of the projection's point of tangency
 *             angle3 -- angle to skew the projection by rotation about
 *                       the line from the Earth's center passing
 *                       through the point of tangency
 *
 *     CON     angle1 -- standard latitude 1
 *             angle2 -- polon is the central longitude
 *             angle3 -- standard latitude 2
 *
 *     The angles for the full map projection types are given as three numbers
 *     separated with semicolons.  Note that THREE angles must be entered even
 *     if some angles are not used.
 *
 *     Note that transverse projections may be obtained using a cylindrical
 *     projection with the first angle set to either 90 or -90.  The second
 *     angle is the longitude at which the cylinder axis intersects the
 *     equator.  This will be the transformed location of the "south" pole
 *     when the first angle is 90 or the "north" pole when the first angle
 *     is -90.  For example, if angle1 = 90 and angle2 = 0, the axis of the
 *     cylinder of projection is perpendicular to the earth's axis and enters
 *     the earth at 0N 0E and emerges at 0N 180E.  The great circle formed
 *     by 90E and 90W becomes the "equator" on the cylinder.  This cylinder
 *     is ideal for a transverse cylindrical projection of locations on the
 *     continent of North America.
 *  </pre>
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class NavigationBlock extends GridDefRecord {

    /** raw values */
    float[] vals = null;

    /** projection type */
    private String proj;

    /**
     * Create a new grid nav block
     */
    public NavigationBlock() {}

    /**
     * Create a new grid nav block with the values
     *
     * @param words   analysis block values
     */
    public NavigationBlock(float[] words) {
        setValues(words);
    }

    /**
     * Set the grid nav block values
     *
     * @param values   the raw values
     */
    public void setValues(float[] values) {
        vals = values;
        proj = GempakUtil.ST_ITOC(Float.floatToIntBits(vals[1])).trim();
        addParam(PROJ, proj);
        addParam(GDS_KEY, this.toString());
        setParams();
    }

    /**
     * Print out the navibation block so it looks something like this:
     * <pre>
     *        PROJECTION:          LCC
     *        ANGLES:                25.0   -95.0    25.0
     *        GRID SIZE:           93  65
     *        LL CORNER:              12.19   -133.46
     *        UR CORNER:              57.29    -49.38
     * </pre>
     *
     * @return  a String representation of this.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("\n    PROJECTION:         ");
        buf.append(proj);
        buf.append("\n    ANGLES:             ");
        buf.append(vals[10]);
        buf.append("  ");
        buf.append(vals[11]);
        buf.append("  ");
        buf.append(vals[12]);
        buf.append("\n    GRID SIZE:          ");
        buf.append(vals[4]);
        buf.append("  ");
        buf.append(vals[5]);
        buf.append("\n    LL CORNER:          ");
        buf.append(vals[6]);
        buf.append("  ");
        buf.append(vals[7]);
        buf.append("\n    UR CORNER:          ");
        buf.append(vals[8]);
        buf.append("  ");
        buf.append(vals[9]);
        return buf.toString();
    }

    /**
     * Set the parameters for the GDS.
     * TODO Add the following:
     *     The following simple map projections may be specified:
     *
     *       NOR     North Orthographic
     *       SOR     South Orthographic
     *       MER     (CYL)   Mercator
     *       CED     (CYL)   Cylindrical Equidistant
     *       MCD     (CYL)   Modified Cylindrical Equidistant
     *       STR     (AZM)   Polar Stereographic
     *       AED     (AZM)   Azimuthal Equidistant
     *       ORT     (AZM)   Orthographic
     *       LEA     (AZM)   Lambert equal area
     *       GNO     (AZM)   Gnomonic
     * </TODO>
     *
     *     In addition for full map projections, three angles MUST be specified
     *     in PROJ.  The angles have the following meanings for the different
     *     projection classes:
     *
     *       CYL     angle1 -- latitude of origin on the projection cylinder
     *                         0 = Equator
     *               angle2 -- longitude of origin on the projection cylinder
     *                         0 = Greenwich meridian
     *               angle3 -- angle to skew the projection by rotation of
     *                         the cylindrical surface of projection about
     *                         the line from the Earth's center passing
     *                         through the origin point. This results in
     *                         curved latitude and longitude lines.
     *
     *                         If angle3 is greater than 360 or less than -360
     *                         degrees, then the rectangular Cartesian coordinate
     *                         system on the projection plane is rotated
     *                         +/- |angle3|-360 degrees. This results in
     *                         latitude and longitude lines that are skewed
     *                         with respect to the edge of the map.  This option
     *                          is only valid when specifying a map projection and
     *                          is not available for grid projections.
     *
     *                         The difference between |angle3| < 360 and
     *                         |angle3| > 360 is that, in the former case,
     *                         the rotation is applied to the developable
     *                         cylindrical surface before projection and
     *                         subsequent development; while, in the latter
     *                         case, the rotation is applied to the Cartesian
     *                         coordinate system in the plane after development.
     *                         Development here refers to the mathematical
     *                         flattening of the surface of projection into a
     *                         planar surface.
     *
     *     Exception:
     *
     *     MCD     angle1 -- scaling factor for latitude
     *                       0 = default scaling (1/cos(avglat))
     *             angle2 -- longitude of origin (center longitude)
     *             angle3 -- not used
     *
     *
     *     AZM     angle1 -- latitude of the projection's point of tangency
     *             angle2 -- longitude of the projection's point of tangency
     *             angle3 -- angle to skew the projection by rotation about
     *                       the line from the Earth's center passing
     *                       through the point of tangency
     *
     *     CON     angle1 -- standard latitude 1
     *             angle2 -- polon is the central longitude
     *             angle3 -- standard latitude 2
     *
     */
    private void setParams() {
        String angle1 = String.valueOf(vals[10]);
        String angle2 = String.valueOf(vals[11]);
        String angle3 = String.valueOf(vals[12]);
        String lllat  = String.valueOf(vals[6]);
        String lllon  = String.valueOf(vals[7]);
        String urlat  = String.valueOf(vals[8]);
        String urlon  = String.valueOf(vals[9]);
        addParam(NX, String.valueOf(vals[4]));
        addParam(NY, String.valueOf(vals[5]));
        addParam(LA1, lllat);
        addParam(LO1, lllon);
        addParam(LA2, urlat);
        addParam(LO2, urlon);
        if (proj.equals("STR") || proj.equals("NPS") || proj.equals("SPS")) {
            addParam(LOV, angle2);
            // TODO:  better to just set pole?
            if (proj.equals("SPS")) {
                addParam("NpProj", "false");
            }
        } else if (proj.equals("LCC") || proj.equals("SCC")) {
            addParam(LATIN1, angle1);
            addParam(LOV, angle2);
            addParam(LATIN2, angle3);
            // TODO: test this
        } else if (proj.equals("MER") || proj.equals("MCD")) {
            String standardLat = "0";
            if (vals[10] == 0) {  // use average latitude
                float lat = (vals[8] + vals[6]) / 2;
                standardLat = String.valueOf(lat);
            } else {
                standardLat = angle1;
            }
            addParam("Latin", standardLat);
            addParam(LOV, angle2);
        } else if (proj.equals("CED")) {
            double lllatv = vals[6];
            double lllonv = vals[7];
            double urlatv = vals[8];
            double urlonv = vals[9];
            if (urlonv < lllonv) {
                urlonv += 360.;
            }
            double dx = Math.abs((urlonv - lllonv) / (vals[4] - 1));
            double dy = Math.abs((urlatv - lllatv) / (vals[5] - 1));
            addParam(DX, String.valueOf(dx));
            addParam(DY, String.valueOf(dy));
            addParam(LO2, String.valueOf(urlonv));


        }
    }

    /**
     * Get a short name for this GDSKey for the netCDF group.
     * Subclasses should implement as a short description
     * @return short name
     */
    public String getGroupName() {
        StringBuffer buf = new StringBuffer();
        buf.append(proj);
        buf.append("_");
        buf.append(getParam(NX));
        buf.append("x");
        buf.append(getParam(NY));
        return buf.toString();
    }

}

