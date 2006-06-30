// $Id: GisPart.java,v 1.2 2004/09/24 03:26:32 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package thredds.datamodel.gis;


/**
 * An interface for simple GIS parts, (analogous to ESRI Shapefile parts).
 *
 * @author Russ Rew
 * @version $Revision: 1.2 $ $Date: 2004/09/24 03:26:32 $
 */

public interface GisPart  {

    /**
     * Get number of points in this part.
     *
     * @return number of points in this part.
     */
    public int getNumPoints();

    /**
     * Get x coordinates for this part.
     *
     * @return array of x coordinates.
     */
    public double[] getX();


    /**
     * Get y coordinates for this part.
     *
     * @return array of y coordinates.
     */
    public double[] getY();

} // GisPart

/* Change History:
   $Log: GisPart.java,v $
   Revision 1.2  2004/09/24 03:26:32  caron
   merge nj22

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:46  caron
   import sources

   Revision 1.4  2000/08/18 04:15:25  russ
   Licensed under GNU LGPL.

   Revision 1.3  2000/05/25 19:33:14  russ
   Fixed empty @version tag.

   Revision 1.2  2000/02/10 17:45:11  caron
   add GisFeatureRenderer,GisFeatureAdapter

*/
