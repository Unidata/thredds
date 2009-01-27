// $Id: MFlowLayout.java 50 2006-07-12 16:30:06Z caron $
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
import java.awt.*;

/**
 * Extends java.awt.FlowLayout, which has a bug where it cant deal with multiple lines.
 * @author John Caron
 * @version $Id: MFlowLayout.java 50 2006-07-12 16:30:06Z caron $
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
