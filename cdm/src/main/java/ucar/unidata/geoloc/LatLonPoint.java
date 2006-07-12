/*
 * $Id$
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
 * Points on the Earth's surface, represented as (longitude,latitude),
 * in units of degrees.
 * Longitude is always between -180 and +180 deg.
 * Latitude is always between -90 and +90 deg.
 *
 * @author John Caron
 * @version $Id$
 */
public interface LatLonPoint {

    /**
     * Returns the longitude, between +/-180 degrees
     * @return longitude (degrees)
     */
    public double getLongitude();

    /**
     * Returns the latitude, between +/- 90 degrees.
     * @return latitude (degrees)
     */
    public double getLatitude();

    /**
     * Returns true if this represents the same point as pt.
     *
     * @param pt  point to check
     * @return true if this represents the same point
     */
    public boolean equals(LatLonPoint pt);
}

/* Change History:
   $Log: LatLonPoint.java,v $
   Revision 1.14  2005/05/13 18:29:08  jeffmc
   Clean up the odd copyright symbols

   Revision 1.13  2004/09/22 21:22:58  caron
   mremove nc2 dependence

   Revision 1.12  2004/07/30 16:24:40  dmurray
   Jindent and javadoc

   Revision 1.11  2004/02/27 21:21:27  jeffmc
   Lots of javadoc warning fixes

   Revision 1.10  2004/01/29 17:34:57  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.9  2003/04/08 15:59:06  caron
   rework for nc2 framework

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:45  caron
   import sources

   Revision 1.8  2000/08/18 04:15:16  russ
   Licensed under GNU LGPL.

   Revision 1.7  1999/12/16 22:57:20  caron
   gridded data viewer checkin

*/







