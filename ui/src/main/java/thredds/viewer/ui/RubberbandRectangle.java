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
import java.awt.*;

/** Rectangle Rubberbanding.
 * @author David M. Geary
 * @author John Caron
 * @version $Id$
 */
public class RubberbandRectangle extends Rubberband {

  public RubberbandRectangle(Component component, boolean listen) {
    super(component, listen);
  }
  public void drawLast(Graphics2D graphics) {
    Rectangle rect = lastBounds();
    graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
    // System.out.println("RBR drawLast");
  }
  public void drawNext(Graphics2D graphics) {
    Rectangle rect = getBounds();
    graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
    // System.out.println("RBR drawNext");
  }
}

/* Change History:
   $Log: RubberbandRectangle.java,v $
   Revision 1.3  2004/09/24 03:26:39  caron
   merge nj22

   Revision 1.2  2004/05/21 05:57:35  caron
   release 2.0b

   Revision 1.1  2002/12/13 00:53:09  caron
   pass 2

   Revision 1.2  2002/04/29 22:26:57  caron
   minor

   Revision 1.1.1.1  2002/02/26 17:24:51  caron
   import sources
*/
