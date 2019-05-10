/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.prefs;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

/** helper class for PrefPanel */
class LayoutM implements LayoutManager2 {
    private String name;
    private Map<Component,Object> constraintMap = new HashMap<>();
    private Rectangle globalBounds = null;

    private boolean debug = false, debugLayout = false;

   /**
     * Constructs a new <code>SpringLayout</code>.
     */
    public LayoutM(String name) {this.name = name; }

    // LayoutManager2

    /**
     * Adds the specified component to the layout, using the specified
     * constraint object.
     * @param comp the component to be added
     * @param constraint  where/how the component is added to the layout.
     */
    public void addLayoutComponent(Component comp, Object constraint) {
      if (debug) System.out.println(name+ " addLayoutComponent= "+ comp.getClass().getName()+" "+comp.hashCode()+" "+constraint);
      if (!(constraint instanceof Constraint))
        throw new IllegalArgumentException( "MySpringLayout must be Constraint");
      constraintMap.put( comp, constraint);
      globalBounds = null;
    }

    /**
     * Calculates the maximum size dimensions for the specified container,
     * given the components it contains.
     * @see java.awt.Component#getMaximumSize
     * @see LayoutManager
     */
    public Dimension maximumLayoutSize(Container parent){
      if (debug) System.out.println("maximumLayoutSize 2");
      if (globalBounds == null) layoutContainer(parent);
      return globalBounds.getSize();
    }

    /**
     * Returns the alignment along the x axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    public float getLayoutAlignmentX(Container target){
      if (debug) System.out.println("getLayoutAlignmentX 2");
      return 0.5f;
    }

    /**
     * Returns the alignment along the y axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    public float getLayoutAlignmentY(Container target){
      if (debug) System.out.println("getLayoutAlignmentY 2");
      return 0.5f;
    }

    /**
     * Invalidates the layout, indicating that if the layout manager
     * has cached information it should be discarded.
     */
    public void invalidateLayout(Container target){
      if (debug) System.out.println(name+" invalidateLayout ");
      globalBounds = null;

      // this probably need to be scheduled later ??
      // layoutContainer( target);
    }

    // LayoutManager

    /**
     * not used
     */
    public void addLayoutComponent(String name, Component comp){
      if (debug) System.out.println("addLayoutComponent ");
    }

    /**
     * Removes the specified component from the layout.
     * @param comp the component to be removed
     */
    public void removeLayoutComponent(Component comp){
      if (debug) System.out.println("removeLayoutComponent");
      constraintMap.remove(comp);
      globalBounds = null;
    }

    /**
     * Calculates the preferred size dimensions for the specified
     * container, given the components it contains.
     * @param parent the container to be laid out
     *
     * @see #minimumLayoutSize
     */
    public Dimension preferredLayoutSize(Container parent){
      if (globalBounds == null) layoutContainer(parent);
      if (debug) System.out.println(name+" preferredLayoutSize "+globalBounds.getSize()+" "+parent.getInsets());
      return globalBounds.getSize();
    }

    /**
     * Calculates the minimum size dimensions for the specified
     * container, given the components it contains.
     * @param parent the component to be laid out
     * @see #preferredLayoutSize
     */
    public Dimension minimumLayoutSize(Container parent){
      if (debug) System.out.println("minimumLayoutSize");
      if (globalBounds == null) layoutContainer(parent);
      return globalBounds.getSize();
    }

    /**
     * Lays out the specified container.
     * @param target the container to be laid out
     */
    public void layoutContainer(Container target) {
      synchronized (target.getTreeLock()) {
        if (debug) System.out.println(name+ " layoutContainer ");

        // first layout any nested LayoutM components
        // it seems that generally Swing laysout from outer to inner ???
        int n = target.getComponentCount();
        for (int i=0 ; i<n ; i++) {
          Component comp = target.getComponent(i);
          if (comp instanceof Container) {
            Container c = (Container) comp;
            LayoutManager m = c.getLayout();
            if (m instanceof LayoutM)
              m.layoutContainer( c);
          }
        }

        // now layout this container
        reset( target);
        globalBounds = new Rectangle(0,0,0,0);
        while ( !layoutPass( target))
          target.setPreferredSize( globalBounds.getSize()); // ??
      }
    }

    private boolean layoutPass(Container parent) {
      if (debugLayout) System.out.println("layout "+name);
      boolean gotAll = true;
      double x,  y;
      Rectangle bounds = new Rectangle();

      int n = parent.getComponentCount();
      for (int i = 0 ; i < n ; i++) {
        Component comp = parent.getComponent(i);
        Constraint cs =  (Constraint) constraintMap.get( comp);

        if (cs.c == null) { // absolute position
          Dimension size = comp.getPreferredSize();
          Rectangle cB = comp.getBounds();

          // negetive means cs.xspace is the right edge
          if (cs.xspace < 0)
            x = -(size.getWidth() + cs.xspace);
          else // otherwise its the left edge
            x = cs.xspace;

          // negative means cs.yspace is the bottom edge
          if (cs.yspace < 0)
            y = -(size.getHeight() + cs.yspace);
          else // otherwise its the top edge
            y = cs.yspace;

          // set the bounds
          bounds.setRect(x, y, size.getWidth(), size.getHeight());
          comp.setBounds( bounds);
          cs.laidout = true;

          globalBounds = globalBounds.union( bounds);
          if (debugLayout) {
            System.out.println("  "+name+" SET (absolute) "+comp.getClass().getName()+" bounds= "+cB+" prefrredSize= "+size);
            System.out.println("  new bounds= "+bounds);
          }

        } else { // position relative to cs.c
          Constraint cs2 =  (Constraint) constraintMap.get( cs.c);
          if (cs2.laidout) {
            Rectangle b2 = cs.c.getBounds();
            Dimension size = comp.getPreferredSize();
            Rectangle cB = comp.getBounds();

            if (cs.xspace == 0) // 0 means align
              x = b2.getX();
            else if (cs.xspace < 0) // negative means cs.xspace is the right edge, absolute
              x = -(size.getWidth() + cs.xspace);
            else // otherwise its reletive to right edge of cs.c
              x = b2.getX() + b2.getWidth() + cs.xspace;

            if (cs.yspace == 0) // 0 means align
              y = b2.getY();
            else if (cs.yspace < 0) // negative means cs.yspace is the bottom edge, absolute
              y = -(size.getHeight() + cs.yspace);
            else // otherwise its reletive to bottom edge of cs.c
              y = b2.getY() + b2.getHeight() + cs.yspace;

            // set bounds
            bounds.setRect(x, y, size.getWidth(), size.getHeight());
            comp.setBounds( bounds);
            cs.laidout = true;

            globalBounds = globalBounds.union( bounds);
            if (debugLayout) {
              System.out.println("  "+name+" SET (reletive) "+comp.getClass().getName()+" bounds= "+cB+" prefrredSize= "+size);
              System.out.println("  new bounds= "+bounds);
            }

          } else {
            gotAll = false;
            if (debugLayout) System.out.println("  "+name+" Missed "+comp.getClass().getName());
          } // if laidout

        } // cs.c == null
      } // loop over components

      return gotAll;
    }

    private void reset(Container parent) {
      int n = parent.getComponentCount();
      for (int i = 0 ; i < n ; i++) {
        Component c = parent.getComponent(i);
        Constraint cs =  (Constraint) constraintMap.get( c);
        cs.laidout = false;
      }
    }


  static class Constraint {
    Component c;
    int xspace, yspace;
    boolean laidout = false;

    public Constraint( Component c, int xspace, int yspace) {
      this.c = c;
      this.xspace = xspace;
      this.yspace = yspace;
    }

    public String toString() {
      String cname = (c == null) ? "null" : (c instanceof JLabel) ? ((JLabel) c).getText() : c.getClass().getName();
      return xspace +" "+yspace + " <"+ cname+">";
    }

  }

  public static void main(String[] args) {

    JFrame frame = new JFrame("Test LayoutM");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {System.exit(0);}
    });

    JPanel main = new JPanel(new LayoutM("test"));
    frame.getContentPane().add(main);
    main.setPreferredSize(new Dimension(200, 200));

    JLabel lab1 = new JLabel( "test1:");
    main.add( lab1, new Constraint(null, 10, 10));
    JTextField f1 = new JTextField( "why dont you just");
    main.add( f1, new Constraint(lab1, 6, 0));

    JLabel lab2 = new JLabel( "test2:");
    main.add( lab2, new Constraint(lab1, 0, 10));
    JTextField f2 = new JTextField( "fade away?");
    main.add( f2, new Constraint(lab2, 6, 0));

    frame.pack();
    frame.setLocation(300, 300);
    frame.setVisible(true);

    Dimension r = f2.getSize();
    System.out.println("getPreferredSize "+ r);
    r.setSize( (int) (r.getWidth() + 50), (int) r.getHeight());
    f2.setPreferredSize( r);
    System.out.println("setPreferredSize "+ r);
    f2.revalidate();
  }
}