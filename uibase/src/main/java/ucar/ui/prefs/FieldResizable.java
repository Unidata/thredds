/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: FieldResizable.java,v 1.7 2005/08/22 01:12:29 caron Exp $


package ucar.ui.prefs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Wraps a field to make it resizeable by the user. Should be package private.
 * To make rubberbanding work, we need it to know what component contains it.
 * @deprecated
 */

class FieldResizable extends Field {
  private Field delegate;
  private JComponent parent;
  private JPanel comp;
  private ResizeButton resizeButt = null;

    // resize dragging
  private ResizeButton dragButt = null;
  private JComponent glassPane = null;
  private RubberbandRectangle rb;
  private boolean resizeMode = false;
  private int startx, starty;

  private boolean debug = false;

  /**
   * Constructor.
   * @param d delegated field
   * @param p container
   */
  public FieldResizable(Field d, JComponent p) {
    super( d.getName(), d.getLabel(), d.getPersistenceManager());
    this.delegate = d;
    this.parent = p;

    // the button that gets dragged
    dragButt = new ResizeButton();
    dragButt.setColor(Color.blue);
    dragButt.addMouseMotionListener( new MouseMotionAdapter() {
      public void mouseDragged(MouseEvent de) {
        if (resizeMode) {
          Point pt = SwingUtilities.convertPoint(dragButt, de.getPoint(), glassPane);
          pt.setLocation( pt.getX(), starty);
          dragButt.setLocation( pt);
          rb.stretch( pt);
        }
      }
    });
    dragButt.addMouseListener( new MouseAdapter() {
      public void mouseReleased( MouseEvent e) {
        if (resizeMode) {
          Point pt = SwingUtilities.convertPoint(dragButt, e.getPoint(), glassPane);
          rb.stop(pt);

          resizeButt.setColor( Color.black);
          glassPane.setVisible(false);
          glassPane.remove(dragButt);
          resizeMode = false;

          //JComponent comp = delegate.getEditComponent();
          int newWidth = (int) (comp.getWidth() + pt.getX() - startx);
          if (debug) System.out.println( "***FieldResizable width= "+comp.getWidth()+"  new width= "+newWidth);

          Dimension d = comp.getSize();
          d.setSize( newWidth, (int) d.getHeight());
          comp.setPreferredSize( d);
          comp.revalidate();
        }
      }
    });

    // the resize button: must click twice!!!
    resizeButt = new ResizeButton();
    resizeButt.addMouseListener( new MouseAdapter() {
      public void mouseReleased( MouseEvent e) {
        if (glassPane == null) {
          // set up the glass panel for resizing
          JRootPane root = SwingUtilities.getRootPane( parent);
          glassPane = (JComponent) root.getGlassPane();
          glassPane.setLayout( null); // we use absolute positioning

          // the rubber band
          rb = new RubberbandRectangle(glassPane);
        }

        resizeButt.setColor( Color.blue);

        Rectangle r = resizeButt.getBounds();
        Point pt = SwingUtilities.convertPoint(comp, (int) r.getX(), (int) r.getY(), glassPane);
        r.setLocation( pt);
        startx = (int) pt.getX();
        starty = (int) pt.getY();
        dragButt.setBounds( r);

        pt.setLocation( pt.getX(), starty + r.getHeight());
        rb.start(pt);

        glassPane.add(dragButt);
        glassPane.setVisible(true);
        resizeMode = true;
      }
    }); // end addMouseListener

    /* comp = new MyJPanel();
    comp.setLayout(null);
    comp.add( delegate.getEditComponent());
    comp.add( resizeButt); */

    comp = new MyPanel();
    comp.setLayout( new BorderLayout());
    // comp.setLayout( new LayoutM(d.getName()));
    // comp.setLayout( null);
    comp.add( delegate.getEditComponent(), BorderLayout.CENTER);
    comp.add( resizeButt, BorderLayout.EAST);
    //comp.add( delegate.getEditComponent(), new LayoutM.Constraint(null,0,0)); // , BorderLayout.CENTER);
    //comp.add( resizeButt, new LayoutM.Constraint(delegate.getEditComponent(),1,0)); // , BorderLayout.EAST);
  }

  /** Return the wrapped field */
  public Field getDelegate() { return delegate; }

  /** Return the editing JComponent */
  public JComponent getEditComponent() { return comp; }

  // pass these calls on to delegates

   /** get current value from editComponent */
  public Object getEditValue() { return delegate.getEditValue(); }

  /** set value of editComponent */
  public void setEditValue(Object value) { delegate.setEditValue( value); }

  /** get value from Store, may return null */
  protected Object getStoreValue(Object defValue) { return delegate.getStoreValue( defValue); }

  /** put new value into Store */
  protected void setStoreValue(Object newValue) { delegate.setStoreValue(newValue); }

  protected boolean accept(StringBuffer buff){ return delegate.accept(buff); }
  protected boolean acceptIfDifferent(Object newValue) { return delegate.acceptIfDifferent( newValue); }
  protected void restoreValue( Object defValue) { delegate.restoreValue( defValue); }
  protected void sendEvent() { delegate.sendEvent(); }
  protected void setNewValueFromStore() { delegate.setNewValueFromStore(); }
  protected void next() { delegate.next(); }
  protected boolean validate( StringBuffer buff) { return delegate.validate(buff); }
  protected boolean _validate( StringBuffer buff) { return delegate._validate(buff); }

  private static class ResizeButton extends JButton {
    ResizeIcon icon = new ResizeIcon(Color.black);
    ResizeButton() {
      setIcon(icon);
      setBorder(BorderFactory.createEmptyBorder());
      setMargin(new Insets(0,0,0,0));
    }

    void setColor( Color c) { icon.setColor(c); }
  }

  private static class ResizeIcon implements javax.swing.Icon {
    private int h = 10, w = 5;
    private Color color;

    ResizeIcon( Color c) { color = c; }
    void setColor( Color c) { color = c; }

    public void paintIcon(Component c, Graphics g, int x, int y){
      JComponent component = (JComponent)c;
      h = component.getHeight();

      g.setColor( component.isEnabled() ? color : Color.gray);
      g.translate( x, y );
      g.drawLine( 1, -4, 1, h);
      g.drawLine( 3, -4, 3, h);
      g.translate( -x, -y );
    }

    public int getIconWidth() { return w; }
    public int getIconHeight()  { return h; }
  }

  private static class RubberbandRectangle {
    private boolean debugRB = false;
    private Point anchorPt    = new Point(0,0);
    private Point stretchedPt = new Point(0,0);
    private Point lastPt      = new Point(0,0);
    private Point endPt       = new Point(0,0);

    private Component component;
    private boolean   firstStretch = true;
    private boolean   active = false;

    public RubberbandRectangle(Component c) {
      component = c;
      component.addMouseMotionListener( new MouseMotionAdapter() {
         public void mouseDragged(MouseEvent event) {
            if (active) stretch(event.getPoint());
         }
      });
    }

    public void drawLast(Graphics graphics) {
      Rectangle rect = lastBounds();
      graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
      if (debugRB) System.out.println( "RB drawLast "+rect);
    }

    public void drawNext(Graphics graphics) {
      Rectangle rect = getBounds();
      graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
      if (debugRB) System.out.println( "RB drawNext "+rect);
    }

    public void setActive(boolean b) { active = b; }
    public boolean isActive    () { return active;      }
    public Point   getAnchor   () { return anchorPt;    }
    public Point   getStretched() { return stretchedPt; }
    public Point   getLast     () { return lastPt;      }
    public Point   getEnd      () { return endPt;       }

    public void start(Point p) {
      firstStretch = true;
      anchorPt.x = p.x;
      anchorPt.y = p.y;

      stretchedPt.x = lastPt.x = anchorPt.x;
      stretchedPt.y = lastPt.y = anchorPt.y;
    }

    public void stretch(Point p) {
      lastPt.x      = stretchedPt.x;
      lastPt.y      = stretchedPt.y;
      stretchedPt.x = p.x;
      stretchedPt.y = p.y;
      if (debugRB) System.out.println( "RB stretch "+getBounds());

      Graphics g = component.getGraphics();
      if (g == null) return;

      try {
        g.setXORMode(component.getBackground());
        g.setColor( Color.blue);
        if (firstStretch)
          firstStretch = false;
        else
          drawLast(g);

        drawNext(g);
      } finally {
        g.dispose();
      }
    }

    public void stop(Point p) {
      lastPt.x = endPt.x = p.x;
      lastPt.y = endPt.y = p.y;

      Graphics g = component.getGraphics();
      if(g != null) {
       try {
         g.setXORMode(component.getBackground());
         g.setColor( Color.blue);
         drawLast(g);
       }
       finally {
         g.dispose();
       }
      }
    }

    public Rectangle getBounds() {
      return new Rectangle(stretchedPt.x < anchorPt.x ?
                           stretchedPt.x : anchorPt.x,
                           stretchedPt.y < anchorPt.y ?
                           stretchedPt.y : anchorPt.y,
                           Math.abs(stretchedPt.x - anchorPt.x),
                           Math.abs(stretchedPt.y - anchorPt.y));
    }

    public Rectangle lastBounds() {
      return new Rectangle(
                  lastPt.x < anchorPt.x ? lastPt.x : anchorPt.x,
                  lastPt.y < anchorPt.y ? lastPt.y : anchorPt.y,
                  Math.abs(lastPt.x - anchorPt.x),
                  Math.abs(lastPt.y - anchorPt.y));
    }
  }   // RubberbandRectangle

  // debug
  private static class MyPanel extends JPanel {

    public void setPreferredSize(Dimension preferredSize) {
      //System.out.println("MyPanel: setPreferredSize = "+preferredSize);
      super.setPreferredSize( preferredSize);
    }

    public Dimension getPreferredSize() {
      //System.out.println("MyPanel: getPreferredSize = "+super.getPreferredSize());
      return super.getPreferredSize();
    }

  }

  /** test */
  public static void main(String[] args) {

    JFrame frame = new JFrame("Test prefs Field");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {System.exit(0);}
    });

    PrefPanel.Dialog d = new PrefPanel.Dialog(frame, false, "title", null, null);
    PrefPanel pp = d.getPrefPanel();
    final Field.Text tf = pp.addTextField("text", "text", "defValue");
    pp.setCursor(1, 0);
    pp.addTextField("text2", "text2", "text2");
    //final Field.Int intf = pp.addIntField("int", "int", 66);

    tf.setText("better value");
    d.finish();
    d.show();

    JPanel main = new JPanel(new FlowLayout());
    frame.getContentPane().add(main);
    main.setPreferredSize(new Dimension(200, 200));

    frame.pack();
    frame.setLocation(300, 300);
    frame.setVisible(true);

    pp.addActionListener(e -> {
        String text = tf.getText();
        tf.setText( text+"1");
    });

  }

}

/* Change History:
   $Log: FieldResizable.java,v $
   Revision 1.7  2005/08/22 01:12:29  caron
   DatasetEditor

   Revision 1.6  2005/08/17 18:36:27  caron
   no message

   Revision 1.5  2005/08/17 00:13:58  caron
   Dataset Editor

   Revision 1.4  2004/08/26 17:55:18  caron
   no message

   Revision 1.3  2003/05/29 23:33:28  john
   latest release

   Revision 1.2  2003/01/14 19:36:18  john
   get the delegate vs inheritence thing right

   Revision 1.1.1.1  2002/12/20 16:40:26  john
   start new cvs root: prefs

   Revision 1.2  2002/03/09 01:51:55  caron
   add BeanTable, fix FieldResizable

*/
