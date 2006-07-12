/*
 * $Id:Earth.java 63 2006-07-12 21:50:51Z edavis $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.geoloc;


/**
 * Defines any necessary properties of the earth for use in georeferencing.
 *
 * @author Russ Rew
 * @version $Id:Earth.java 63 2006-07-12 21:50:51Z edavis $
 */
public class Earth {

    /** eccentricity squared */
    private double e2;

    /** equatorial radius (semimajor axis) */
    private double a;

    /** polar radius (semiminor axis) */
    private double b;

    /** flattening */
    private double f;

    /** radius of the earth */
    private static final double radius = 6371229.;

    /**
     * Specify earth with equatorial and polar radius.
     *
     * @param a semimajor (equatorial) radius, in meters.
     * @param b  semiminor (polar) radius, in meters.
     */
    Earth(double a, double b) {
        this.a = a;
        this.b = b;
        e2     = 1.0 - (b * b) / (a * a);
        f      = 1.0 - b / a;
    }

    /**
     * Specify earth with semimajor radius a, and inverse flattening f.
     *  b = a(1-f)
     *
     * @param a semimajor (equatorial) radius, in meters.
     * @param f inverse flattening.
     * @param bb  fake
     */
    Earth(double a, double f, boolean bb) {
        this.a = a;
        this.f = f;
        b      = a * (1.0 - f);
        e2     = 1.0 - (b * b) / (a * a);
    }

    /**
     * Get the eccentricity of the earth ellipsoid.
     * @return eccentricity
     */
    public double getEccentricity() {
        return Math.sqrt(e2);
    }

    /**
     * Get the flattening of the earth ellipsoid.
     * @return flattening
     */
    public double getFlattening() {
        return f;
    }

    /**
     * Get the semimajor axis of the earth, in meters.
     * @return semimajor axis in meters
     */
    public double getMajor() {
        return a;
    }

    /**
     * Get the semiminor axis of the earth, in meters.
     * @return semiminor axis in meters
     */
    public double getMinor() {
        return b;
    }

    /**
     * Get radius of spherical earth, in meters
     * @return radius in meters
     */
    public static double getRadius() {
        return radius;
    }

}

/* Change History:
   $Log: Earth.java,v $
   Revision 1.14  2005/11/02 20:04:13  dmurray
   add the Orthographic projection, refactor some of the constants up to
   ProjectionImpl, move the radius declaration in Earth up to the top,
   fix a problem in Mercator where infinite points were set to 0,0 instead
   of infinity.

   Revision 1.13  2005/05/13 18:29:08  jeffmc
   Clean up the odd copyright symbols

   Revision 1.12  2005/05/06 14:01:55  dmurray
   run jindent on these.  It would be good if we could agree on using
   Jindent for the common packages.

   Revision 1.11  2005/02/01 01:35:51  caron
   no message

   Revision 1.10  2004/11/09 21:16:44  caron
   earth radius 6371229.

   Revision 1.9  2004/09/22 21:22:58  caron
   mremove nc2 dependence

   Revision 1.8  2004/07/30 16:24:40  dmurray
   Jindent and javadoc

   Revision 1.7  2004/02/27 21:21:27  jeffmc
   Lots of javadoc warning fixes

   Revision 1.6  2004/01/29 17:34:57  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.5  2003/04/08 15:59:06  caron
   rework for nc2 framework

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:44  caron
   import sources

   Revision 1.4  2000/08/18 04:15:15  russ
   Licensed under GNU LGPL.

   Revision 1.3  1999/06/03 01:43:48  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:26:13  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:51  caron
   startAgain

# Revision 1.3  1999/03/03  19:58:08  caron
# more java2D changes
#
# Revision 1.2  1998/12/14  17:10:45  russ
# Add comment for accumulating change histories.
#
*/







