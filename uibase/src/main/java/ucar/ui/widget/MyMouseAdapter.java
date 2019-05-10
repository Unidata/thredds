/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serializable;

public class MyMouseAdapter extends java.awt.event.MouseAdapter implements Serializable {
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

  public static void main(String[] args) {

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
