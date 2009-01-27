// $Id: SpinIcon.java 50 2006-07-12 16:30:06Z caron $
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