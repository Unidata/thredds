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
package thredds.viewer.ui;

/** A Renderer does the actual work of drawing objects.
 *
 * @author John Caron
 * @version $Id$
 */
public interface Renderer {

    /** Tell the renderer to draw itself.
     *  The Graphics2D object has its AffineTransform set to transform World coordinates to display coordinates.
     *  Typically the Renderer does its drawing in World coordinates, and does not modify the AffineTransform.
     *  If the Renderer wants to draw in constant-pixel coordinates, so that its objects do not change as
     *   the user zooms in and out, use the pixelAT transform, which transforms "Normalized Device"
     *   coordinates (screen pixels) to Device coordinates.
     *  The Graphics2D object also has its clipping rectangle set (in World coordinates), which the Renderer may
     *    use for optimization.
     *  The Graphics2D object has default color and line width set; the Renderer should restore any changes it makes.
     * @param g         the Graphics context
     * @param pixelAT   transforms "Normalized Device" to Device coordinates.  When drawing to the screen,
     *   this will be the identity transform. For other devices like printers, it is not the Identity transform.
     *   Renderers should use "Normalized Device" coordinates if they want to render non-scalable objects.
     *   Basically, you pretend you are working in screen pixels.
     */
  public void draw(java.awt.Graphics2D g, java.awt.geom.AffineTransform pixelAT);

    /** Tell the Renderer to use the given projection from now on.
     *  @param project the projection to use.
     */
  public void setProjection(ucar.unidata.geoloc.ProjectionImpl project);

    /** Tell the Renderer to use the given color.
     *  @param color the Color to use.
     */
  public void setColor(java.awt.Color color);
  public java.awt.Color getColor();

  /** This allows application to automatically switch to some special area defined by the Renderer
   *  @return LatLonRect or null.
   */
  public ucar.unidata.geoloc.LatLonRect getPreferredArea();
}

/* Change History:
   $Log: Renderer.java,v $
   Revision 1.6  2004/09/28 21:39:11  caron
   *** empty log message ***

   Revision 1.5  2004/09/24 03:26:39  caron
   merge nj22

   Revision 1.4  2003/05/29 23:07:52  john
   bug fixes

   Revision 1.3  2003/04/08 18:16:23  john
   nc2 v2.1

   Revision 1.2  2003/03/17 21:12:38  john
   new viewer

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.2  2002/04/29 22:26:57  caron
   minor

   Revision 1.1.1.1  2002/02/26 17:24:50  caron
   import sources
*/

