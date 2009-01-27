// $Id:GisPart.java 63 2006-07-12 21:50:51Z edavis $
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
package thredds.datamodel.gis;


/**
 * An interface for simple GIS parts, (analogous to ESRI Shapefile parts).
 *
 * @author Russ Rew
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
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
