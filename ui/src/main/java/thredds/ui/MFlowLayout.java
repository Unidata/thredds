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
import java.awt.*;

/**
 * Extends java.awt.FlowLayout, which has a bug where it cant deal with multiple lines.
 * @author John Caron
 * @version $Id$
 */

public class MFlowLayout extends FlowLayout {

    public MFlowLayout(int align, int hgap, int vgap) {
      super( align, hgap, vgap);
    }

    // deal with having components on more than one line
    public Dimension preferredLayoutSize(Container target) {
      synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);

            for (int i = 0 ; i < target.getComponentCount() ; i++) {
              Component m = target.getComponent(i);
              if (m.isVisible()) {
                Dimension d = m.getPreferredSize();

                // original
                // dim.height = Math.max(dim.height, d.height);
                //if (i > 0) { dim.width += hgap; }
                // dim.width += d.width;

                // new  way
                Point p = m.getLocation();
                dim.width =  Math.max(dim.width,  p.x+d.width);
                dim.height = Math.max(dim.height, p.y+d.height);
              }
            }
        Insets insets = target.getInsets();
        dim.width += insets.left + insets.right + getHgap()*2;
        dim.height += insets.top + insets.bottom + getVgap()*2;
        return dim;
      }
    }
}

/* Change History:
   $Log: MFlowLayout.java,v $
   Revision 1.2  2004/09/24 03:26:34  caron
   merge nj22

   Revision 1.1.1.1  2002/11/23 17:49:48  caron
   thredds reorg

   Revision 1.1.1.1  2002/02/15 00:01:48  caron
   import sources

*/
