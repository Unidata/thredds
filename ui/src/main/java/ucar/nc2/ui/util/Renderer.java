/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.util;

/** A Renderer does the actual work of drawing objects.
 *
 * @author John Caron
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
    void draw(java.awt.Graphics2D g, java.awt.geom.AffineTransform pixelAT);

    /** Tell the Renderer to use the given projection from now on.
     *  @param project the projection to use.
     */
    void setProjection(ucar.unidata.geoloc.ProjectionImpl project);

    /** Tell the Renderer to use the given color.
     *  @param color the Color to use.
     */
    void setColor(java.awt.Color color);
  java.awt.Color getColor();

  /** This allows application to automatically switch to some special area defined by the Renderer
   *  @return LatLonRect or null.
   */
  ucar.unidata.geoloc.LatLonRect getPreferredArea();
}
