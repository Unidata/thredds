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

package thredds.ui;

import java.awt.Graphics;
import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class SpinIcon implements javax.swing.Icon {
  public static final SpinIcon.Type TypeUp = new SpinIcon.Type();
  public static final SpinIcon.Type TypeDown = new SpinIcon.Type();
  public static final SpinIcon.Type TypeRight = new SpinIcon.Type();
  public static final SpinIcon.Type TypeLeft = new SpinIcon.Type();

  private SpinIcon.Type type;
  private boolean orientH;

  public SpinIcon(SpinIcon.Type type) {
    this.type = type;
    orientH = (type == TypeUp) || (type == TypeDown);
  }

  public void paintIcon(Component c, Graphics g, int x, int y){
    JComponent component = (JComponent)c;
    int iconWidth = 10;
    g.translate( x, y );

    g.setColor( component.isEnabled() ? MetalLookAndFeel.getControlInfo() :
                                        MetalLookAndFeel.getControlShadow() );
    int line = 0;
    if (type == TypeUp) {
      g.drawLine( 4, line, 4 + (iconWidth - 9), line ); line++;
      g.drawLine( 3, line, 3 + (iconWidth - 7), line ); line++;
      g.drawLine( 2, line, 2 + (iconWidth - 5), line ); line++;
      g.drawLine( 1, line, 1 + (iconWidth - 3), line ); line++;
      g.drawLine( 0, line, iconWidth - 1, line ); line++;
    } else if (type == TypeDown) {
      g.drawLine( 0, line, iconWidth - 1, line ); line++;
      g.drawLine( 1, line, 1 + (iconWidth - 3), line ); line++;
      g.drawLine( 2, line, 2 + (iconWidth - 5), line ); line++;
      g.drawLine( 3, line, 3 + (iconWidth - 7), line ); line++;
      g.drawLine( 4, line, 4 + (iconWidth - 9), line ); line++;
    } else if (type == TypeRight) {
      g.drawLine( line, 0, line, iconWidth - 1 ); line++;
      g.drawLine( line, 1, line, 1 + (iconWidth - 3) ); line++;
      g.drawLine( line, 2, line, 2 + (iconWidth - 5) ); line++;
      g.drawLine( line, 3, line, 3 + (iconWidth - 7) ); line++;
      g.drawLine( line, 4, line, 4 + (iconWidth - 9) ); line++;
    } else {
      g.drawLine( line, 4, line, 4 + (iconWidth - 9) ); line++;
      g.drawLine( line, 3, line, 3 + (iconWidth - 7) ); line++;
      g.drawLine( line, 2, line, 2 + (iconWidth - 5) ); line++;
      g.drawLine( line, 1, line, 1 + (iconWidth - 3) ); line++;
      g.drawLine( line, 0, line, iconWidth - 1 ); line++;
    }

    g.translate( -x, -y );
  }

  /*** stubbed to satisfy the interface. */
  public int getIconWidth() { return orientH ? 10 : 5; }

  /*** stubbed to satisfy the interface. */
  public int getIconHeight()  { return orientH ? 5 : 10; }

  public static class Type {
  }

}

/* Change History:
   $Log: SpinIcon.java,v $
   Revision 1.2  2004/09/24 03:26:34  caron
   merge nj22

   Revision 1.1.1.1  2002/11/23 17:49:48  caron
   thredds reorg

   Revision 1.2  2002/04/29 22:26:58  caron
   minor

*/