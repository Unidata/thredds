/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Point;

/**
 * Extends java.awt.FlowLayout, which has a bug where it cant deal with multiple lines.
 * @author John Caron
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
