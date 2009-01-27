// $Id: Renderer.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.viewer.ui;

/** A Renderer does the actual work of drawing objects.
 *
 * @author John Caron
 * @version $Id: Renderer.java 50 2006-07-12 16:30:06Z caron $
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

