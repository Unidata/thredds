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
import java.awt.event.*;
import javax.swing.*;

public class MyMouseAdapter extends java.awt.event.MouseAdapter {
  private int startx, starty;
  private int minMove = 4;
  private boolean debugEvent = false;

  public void mousePressed( MouseEvent e) {
    startx = e.getX();
    starty = e.getY();

    if (debugEvent) System.out.println( "mousePressed "+startx+" "+starty);
  }

  public void mouseReleased( MouseEvent e) {
    int deltax = e.getX() - startx;
    int deltay = e.getY() - starty;
    int total = Math.abs(deltax) + Math.abs(deltay);
    if (total <= minMove)
      click(e);
    else
      drag(e, deltax, deltay);

    if (debugEvent) {
      System.out.println( "mouseReleased "+e.getX()+" "+e.getY());
      if ((deltax > 0) || (deltay > 0))
        System.out.println( "  MOVED "+deltax+" "+deltay);
    }
  }

  public void setMinMove( int minMove) { this.minMove = minMove; }

    /// subclasses should override
  public void click(MouseEvent e) { } // System.out.println( "click"); }
  public void drag(MouseEvent e, int deltax, int deltay) { }
    // System.out.println( "drag: "+deltax+" "+deltay);}

  public static void main(String args[]) {

    JFrame frame = new JFrame("Test MyMouseAdapter");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {System.exit(0);}
    });

    JLabel comp = new JLabel("test  sdfk sdf ks;dflk ;sdlkf ldsk lk");
    comp.setOpaque(true);
    comp.setBackground(Color.white);
    comp.setForeground(Color.black);

    comp.addMouseListener( new MyMouseAdapter());

    JPanel main = new JPanel(new FlowLayout());
    frame.getContentPane().add(main);
    main.setPreferredSize(new Dimension(200, 200));
    main.add( comp);

    frame.pack();
    frame.setLocation(300, 300);
    frame.setVisible(true);
  }
}

/* Change History:
   $Log: MyMouseAdapter.java,v $
   Revision 1.1.1.1  2002/11/23 17:49:48  caron
   thredds reorg

   Revision 1.1.1.1  2002/02/15 00:01:48  caron
   import sources

*/