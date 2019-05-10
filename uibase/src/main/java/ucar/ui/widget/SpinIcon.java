/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.widget;

import java.awt.Component;
import java.awt.Graphics;
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
