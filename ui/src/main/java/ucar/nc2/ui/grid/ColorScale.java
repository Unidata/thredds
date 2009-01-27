// $Id: ColorScale.java 50 2006-07-12 16:30:06Z caron $
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
package ucar.nc2.ui.grid;

import thredds.ui.FontUtil;

import ucar.nc2.dt.GridDatatype;
import ucar.nc2.ui.util.ListenerManager;
import ucar.unidata.util.Format;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import javax.swing.*;

/**
 * A ColorScale is used for false-color data mapping. It contains an array of java.awt.Color,
 * along with an assignment of a data interval to each Color.
 * <p/>
 * ColorScale.Panel handles the displaying of a ColorScale. It also works with ColorScaleManager
 * to allow editing and defining new ColorScales.
 *
 * @author caron
 * @version $Revision: 50 $ $Date: 2006-07-12 16:30:06Z $
 */

public class ColorScale implements Cloneable, java.io.Serializable {
  public static final int VERTICAL = 0;
  public static final int HORIZONTAL = 1;

  private static final long serialVersionUID = -1L;       // disconnect java version checking
  private static final int objectVersion = 1;             // our version control
  private static final boolean debugColors = false;
  private static int sigfig = 4;

  // this is all that needs to be serialized
  private String name;
  private int ncolors;
  private Color[] colors;

  // reset after deserializing
  private Color[] useColors;
  private ListenerManager lm;

  // this is set for each grid
  private GridDatatype gg;
  private double[] edge;
  private int[] hist;
  private double min, max, interval;
  private boolean hasMissingData = false;
  private Color missingDataColor = Color.white;

  // kludge to make life easier
  static final public Color[] redHot = {
          new java.awt.Color(0x5cacee),    //cyan
          new java.awt.Color(0xfff5eb),
          new java.awt.Color(0xffe6cc),
          new java.awt.Color(0xffd9b0),
          new java.awt.Color(0xffc485),
          new java.awt.Color(0xffb86b),
          new java.awt.Color(0xffad56),
          new java.awt.Color(0xff992b),
          new java.awt.Color(0xff7e00),
          new java.awt.Color(0xff6a00),
          new java.awt.Color(0xf15800),
          new java.awt.Color(0xe24400),
          new java.awt.Color(0xd22f00),
          new java.awt.Color(0xc11900),
          new java.awt.Color(0xa00300),
          new java.awt.Color(0xff00ff)      // magenta
  };

  static final public Color[] redBlue = {
          new java.awt.Color(1, 57, 255),
          new java.awt.Color(0, 140, 255),
          new java.awt.Color(1, 209, 255),
          new java.awt.Color(1, 255, 232),
          new java.awt.Color(1, 255, 171),
          new java.awt.Color(1, 255, 79),
          new java.awt.Color(43, 255, 0),
          new java.awt.Color(166, 255, 2),
          new java.awt.Color(227, 255, 1),
          new java.awt.Color(255, 198, 0),
          new java.awt.Color(255, 168, 1),
          new java.awt.Color(255, 145, 1),
          new java.awt.Color(255, 130, 1),
          new java.awt.Color(255, 107, 0),
          new java.awt.Color(255, 84, 0),
          new java.awt.Color(255, 7, 0)
  };
  static final public Color[] hueBands = {
          new java.awt.Color(204, 0, 0),
          new java.awt.Color(255, 0, 0),
          new java.awt.Color(255, 51, 51),
          new java.awt.Color(255, 102, 102),
          new java.awt.Color(255, 153, 153),
          new java.awt.Color(255, 102, 0),
          new java.awt.Color(255, 153, 0),
          new java.awt.Color(255, 204, 102),
          new java.awt.Color(255, 255, 0),
          new java.awt.Color(255, 255, 51),
          new java.awt.Color(255, 255, 102),
          new java.awt.Color(255, 255, 153),
          new java.awt.Color(0, 255, 0),
          new java.awt.Color(51, 255, 51),
          new java.awt.Color(102, 255, 102),
          new java.awt.Color(153, 255, 153),
          new java.awt.Color(204, 255, 204),
          new java.awt.Color(0, 255, 255),
          new java.awt.Color(51, 255, 255),
          new java.awt.Color(102, 255, 255),
          new java.awt.Color(153, 255, 255),
          new java.awt.Color(204, 255, 255),
          new java.awt.Color(0, 0, 255),
          new java.awt.Color(51, 51, 255),
          new java.awt.Color(102, 102, 255),
          new java.awt.Color(153, 153, 255),
          new java.awt.Color(255, 0, 255),
          new java.awt.Color(255, 51, 255),
          new java.awt.Color(255, 102, 255),
          new java.awt.Color(255, 153, 255),
          new java.awt.Color(255, 204, 255)
  };


  static final public Color[] spectrum2 = {
          new java.awt.Color(204, 0, 0),
          new java.awt.Color(255, 31, 0),
          new java.awt.Color(255, 69, 0),
          new java.awt.Color(255, 107, 0),
          new java.awt.Color(255, 138, 0),
          new java.awt.Color(255, 168, 0),
          new java.awt.Color(255, 199, 0),
          new java.awt.Color(255, 244, 0),
          new java.awt.Color(227, 255, 0),
          new java.awt.Color(196, 255, 0),
          new java.awt.Color(165, 255, 0),
          new java.awt.Color(127, 255, 0),
          new java.awt.Color(89, 255, 0),
          new java.awt.Color(43, 255, 0),
          new java.awt.Color(0, 255, 40),
          new java.awt.Color(0, 255, 80),
          new java.awt.Color(0, 255, 110),
          new java.awt.Color(0, 255, 140),
          new java.awt.Color(0, 255, 170),
          new java.awt.Color(0, 255, 201),
          new java.awt.Color(0, 255, 232),
          new java.awt.Color(0, 232, 255),
          new java.awt.Color(0, 209, 255),
          new java.awt.Color(0, 170, 255),
          new java.awt.Color(0, 140, 255),
          new java.awt.Color(0, 79, 255),
          new java.awt.Color(0, 57, 255),
          new java.awt.Color(0, 34, 255),
          new java.awt.Color(0, 3, 255),
          new java.awt.Color(29, 1, 255),
          new java.awt.Color(59, 1, 255),
          new java.awt.Color(82, 1, 255),
          new java.awt.Color(112, 1, 255),
          new java.awt.Color(125, 1, 255),
          new java.awt.Color(166, 1, 255),
          new java.awt.Color(189, 1, 255),
          new java.awt.Color(219, 1, 255)
  };
  static final private Color[] defaultColors = redBlue;

  /* Constructor.
   * @param name of this colorscale.
   * @param c array of colors.
   */
  public ColorScale(String name, Color [] c) {
    this.name = new String(name);
    this.ncolors = c.length;
    colors = new Color[ ncolors];
    for (int i = 0; i < ncolors; i++)
      colors[i] = c[i];

    constructTransient();
  }

  /* Constructor. Use default colors.
   * @param name of this colorscale.
   */
  public ColorScale(String name) {
    this(name, defaultColors);
/*    this.name = new String(name);
    this.ncolors = ncolors;
    colors = new Color[ ncolors];

      // set default colors
    int n = Math.min(ncolors, defaultColors.length);
    for (int i=0; i<n; i++)
      colors[i] = defaultColors[i];
    for (int i=n; i<ncolors; i++)
      colors[i] = defaultColors[n-1];

    constructTransient(); */
  }

  /* Constructor. Use default colors, no name.
   */
  public ColorScale() {
    this("", defaultColors);
  }

  // rest of stuff for construction/deserialization
  private void constructTransient() {
    useColors = colors;

    edge = new double[ ncolors];
    hist = new int[ ncolors + 1];
    lm = new ListenerManager(
            "java.beans.PropertyChangeListener",
            "java.beans.PropertyChangeEvent",
            "propertyChange");
    missingDataColor = Color.white;
  }

  /**
   * add action event listener
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {
    lm.addListener(l);
  }

  /**
   * remove action event listener
   */
  public void removePropertyChangeListener(PropertyChangeListener l) {
    lm.removeListener(l);
  }

  /**
   * Get the colorscale name.
   */
  public String getName() {
    return name;
  }

  /**
   * Set the colorscale name.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the number of colors in the colorscale.
   */
  public int getNumColors() {
    return ncolors;
  }

  /**
   * Set the number of colors in the colorscale.
   */
  public void setNumColors(int n) {
    if (n != ncolors) {
      colors = new Color[n];
      int prevn = Math.min(ncolors, n);
      for (int i = 0; i < prevn; i++)
        colors[i] = useColors[i];
      for (int i = ncolors; i < n; i++)
        colors[i] = Color.white;

      useColors = colors;
      ncolors = n;
      edge = new double[ ncolors];
      hist = new int[ ncolors + 1];
    }
  }

  /* Get the color at the given index, or the missing data color.
   * @param i index into color array, or ncolors to get missingDataColor.
   * @exception IllegalArgumentException if (i<0) || (i>ncolors)
   */
  public Color getColor(int i) {
    if (i >= 0 && i < ncolors)
      return useColors[i];
    else if (i == ncolors && hasMissingData)
      return missingDataColor;
    else
      throw new IllegalArgumentException("Color Scale getColor " + i);
  }

  /**
   * Get the edge of the ith interval. see @link setMinMax
   */
  public double getEdge(int index) {
    return edge[index];
  }

  /* Set whether there is missing data, what it is, and what color to use.
   * @param i: index into color array, or ncolors for missingDataColor.
   * @exception IllegalArgumentException if (i<0) || (i>ncolors)
   *
  public void setMissingDataColor( Color missingDataColor) {
    this.missingDataColor = missingDataColor;
  } */

  public void setGeoGrid(GridDatatype gg) {
    this.gg = gg;
    hasMissingData = gg.hasMissingData();
  }

  public Color getMissingDataColor() {
    return missingDataColor;
  }

  /*public String getLabel(int i) {
    if (i >=0 && i < ncolors)
      return label[i];
    else
      throw new IllegalArgumentException("Color Scale getLabel "+i);
  } */

  /**
   * Get which color interval this value lies in.
   *
   * @param value: minimum data value.
   * @return the color index.
   */
  public int getIndexFromValue(double value) {
    int index;
    if (hasMissingData && gg.isMissingData(value))
      index = ncolors;  // missing data
    else if (value <= min)
      index = 0;
    else if (value >= max)
      index = ncolors - 1;
    else
      index = (int) ((value - min) / interval) + 1;

    hist[index]++;
    return index;
  }

  /**
   * Set the data min/max interval. The color intervals are set based on this.
   * A PropertyChangeEvent is sent when this is called. Currently the intervals are
   * calculated in the following way (where incr = (max-min)/(n-2)) :
   * <pre>
   * <p/>
   *      edge           data interval
   *  0    min             value <= min
   *  1    min+incr        min <= value < min + incr
   *  2    min+2*incr      min+incr <= value < min+2*incr
   *  ith  min+i*incr      min+(i-1)*incr <= value < min+i*incr
   *  n-2  max             min+(n-3)*incr <= value < max
   *  n-1  max             max < value
   *  n                    value = missingDataValue
   * </pre>
   *
   * @param min: minimum data value
   * @param max: maximum data value
   */
  public void setMinMax(double min, double max) {
    this.min = min;
    this.max = max;
    interval = (max - min) / (ncolors - 2);

    // set edges
    for (int i = 0; i < ncolors; i++)
      edge[i] = min + i * interval;

    lm.sendEvent(new PropertyChangeEvent(this, "ColorScaleLimits", null, this));
  }


  /**
   * This is an optimization for counting the number of colors in each interval.
   * the histpogram is populated by calls to getIndexFromValue().
   *
   * @return the index with the maximum histogram count.
   */
  public int getHistMax() {
    int max = 0, maxi = 0;
    for (int i = 0; i <= ncolors; i++)
      if (hist[i] > max) {
        max = hist[i];
        maxi = i;
      }
    return maxi;
  }

  /**
   * reset the histogram.
   */
  public void resetHist() {
    for (int i = 0; i <= ncolors; i++)
      hist[i] = 0;
  }

  public String toString() {
    return name;
  }

  public Object clone() {
    ColorScale cl = new ColorScale(name, colors);
    /*try {
      cl = (ColorScale) super.clone();
    } catch(CloneNotSupportedException e) {
      return null;
    } // ignore

    // non primitive fields must be cloned separately
    cl.name = new String(name);
    cl.set(this);
    cl.construct();  */
    return (Object) cl;
  }

  /////////// private ////////////////////

  // this is for editing a colorscale
  private void editModeBegin() {
    Color [] editColors = new Color[ ncolors];
    for (int i = 0; i < ncolors; i++)
      editColors[i] = colors[i];
    useColors = editColors;
  }

  private void editModeEnd(boolean accept) {
    if (accept) {
      for (int i = 0; i < ncolors; i++)
        colors[i] = useColors[i];
    }
    useColors = colors;
  }

  private void setColor(int i, Color c) {
    if (i >= 0 && i < ncolors)
      useColors[i] = c;
  }

  private Color [] getColors() {
    return colors;
  }

  /* private void set( ColorScale cs) {
   set(cs.getColors());
 } */
  private void setColors(Color[] c) {
    ncolors = c.length;
    colors = new Color[ ncolors];
    for (int i = 0; i < ncolors; i++)
      colors[i] = c[i];
    edge = new double[ ncolors];
    hist = new int[ ncolors + 1];
    useColors = colors;        // ??
  }

  // serialization
  private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
    int readVer = s.readInt();
    this.name = s.readUTF();
    this.colors = (Color[]) s.readObject();
    this.ncolors = colors.length;
    constructTransient();
  }

  private void writeObject(ObjectOutputStream s) throws IOException {
    s.writeInt(objectVersion);
    s.writeUTF(this.name);
    s.writeObject(this.colors);
  }

  // heres the swing component part of a ColorScale: static so its not part of serialization
  // originally designed to allow popup editor to change colors; this has been disabled; all
  // changes made by COlorManager. design could be cleaned up, but maybe I want that back later?
  public static class Panel extends JPanel {
    private int type;

    private int size = 50;
    private ColorScale cs;
    private JDialog dialog;
    private JLabel unitLabel = new JLabel("unit", SwingConstants.CENTER);
    private JPanel lpanel;

    private boolean editable = false;
    private int selected = -1;

    private int nColorInterval;
    private String[] label;
    private boolean useLabel = true;
    private FontUtil.StandardFont sf = FontUtil.getStandardFont(10);

    public Panel(Component parent) {
      this(parent, ColorScale.VERTICAL, null);
    }

    public Panel(Component parent, ColorScale cscale) {
      this(parent, ColorScale.VERTICAL, cscale);
    }

    public Panel(Component parent, int type, ColorScale cscale) {
      this.cs = (cscale == null) ? new ColorScale("default") : cscale;
      this.type = type;

      if (type == ColorScale.VERTICAL) {
        setPreferredSize(new Dimension(size, 400));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      } else {
        setPreferredSize(new Dimension(400, size));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
      }

      setListener();

      nColorInterval = cs.getNumColors();
      for (int i = 0; i < nColorInterval; i++) {
        ColorInterval intv = new ColorInterval(nColorInterval - i - 1);
        add(intv);
      }
      lpanel = new JPanel();
      lpanel.add(unitLabel);
      //unitLabel.setBorder( new javax.swing.border.EtchedBorder());
      if (type == ColorScale.VERTICAL)
        lpanel.setPreferredSize(new Dimension(size, 0));
      else
        lpanel.setPreferredSize(new Dimension(0, size));
      add(lpanel);

      label = new String[ nColorInterval];
      calcLabels();

      /* creates a popup men, attaches it to this Panel
     PopupMenu popupMenu = new PopupMenu(this, "options");
     popupMenu.add("Edit", new AbstractAction() {
       public void actionPerformed(ActionEvent e) {
         System.out.println("popup edit action");
         dialog.show();
       }
     }); */

    }

    private void calcLabels() {
      label[0] = "<" + Format.d(cs.getEdge(0), sigfig);
      for (int i = 1; i < nColorInterval - 1; i++) {
        label[i] = Format.d(cs.getEdge(i), sigfig);
      }
      label[nColorInterval - 1] = ">" + Format.d(cs.getEdge(nColorInterval - 2), sigfig);
    }

    private void setListener() {
      // listen for changes so we can redraw
      cs.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          if (e.getPropertyName().equals("ColorScaleLimits"))
            calcLabels();
          repaint();
        }
      });
    }

    public ColorScale getColorScale() {
      return cs;
    }

    // change the current cscale
    public void setColorScale(ColorScale cscale) {
      if (nColorInterval != cscale.getNumColors()) {
        removeAll();
        nColorInterval = cscale.getNumColors();
        label = new String[ nColorInterval];
        for (int i = 0; i < nColorInterval; i++) {
          ColorInterval intv = new ColorInterval(nColorInterval - i - 1);
          add(intv);
          label[i] = "none";
        }
        add(lpanel);
        revalidate();
      }

      this.cs = cscale;
      setListener();
      calcLabels();
      repaint();
    }

    // set existing colorscale to have new colors
    public void setColors(Color[] c) {
      if (nColorInterval != c.length) {
        removeAll();
        nColorInterval = c.length;
        label = new String[ nColorInterval];
        for (int i = 0; i < nColorInterval; i++) {
          ColorInterval intv = new ColorInterval(nColorInterval - i - 1);
          add(intv);
          label[i] = "none";
        }
        add(lpanel);
        revalidate();
      }

      cs.setColors(c);
      cs.editModeBegin(); // again

      if (debugColors) {
        for (int i = 0; i < cs.getNumColors(); i++)
          System.out.println(cs.getColor(i));
      }
      calcLabels();
      repaint();
    }


    public void setColor(Color c) {
      cs.setColor(selected, c);
    }

    //public void setEditable( boolean b) { editable = b; }
    public void setEditMode(boolean on, boolean accept) {
      if (on) {
        editable = true;
        cs.editModeBegin();
      } else {
        cs.editModeEnd(accept);
        if (accept)
          setColorScale(cs);
        selected = -1;
        editable = false;
      }
      repaint();
    }

    public void setSelected(int i) {
      selected = i;
    }

    public void setShowText(boolean b) {
      useLabel = b;
    }

    /*private void edit(int which) {
      if (!editable)
        return;
      selected = which;
      repaint();
      cs.setEditMode(true);
      //dialog.show();
    }*/
    public void setUnitString(String s) {
      unitLabel.setText(s);
      //System.out.println("new text = "+s);
      //unitLabel.repaint(s);
    }

    public void print(Graphics2D g, double x, double y, double width, double height) {
      int n = cs.getNumColors();
      double size = (type == ColorScale.VERTICAL) ? height / n : width / n;
      int count = 0;
      for (int i = 0; i < getComponentCount(); i++) {
        Component c = getComponent(i);
        if (c instanceof ColorInterval) {
          ColorInterval intv = (ColorInterval) c;
          if (type == ColorScale.VERTICAL)
            intv.printV(g, (int) x, (int) (y + count * size), (int) width, (int) size);
          else {
            double xpos = x + width - (count + 1) * size;
            intv.printH(g, (int) xpos, (int) y, (int) size, (int) height);
          }
          count++;
        }
      }
    }

    /* private class OkListener implements ActionListener {
      public void actionPerformed(ActionEvent e) {
        selected = -1;
        cs.accept();
        repaint();
      }
    }  // end inner class OkListener

    private class CancelListener implements ActionListener {
      public void actionPerformed(ActionEvent e) {
        selected = -1;
        cs.cancel();
        repaint();
      }
    } // end inner class CancelListener  */

    private class ColorInterval extends JComponent {
      private int rank;

      ColorInterval(int r) {
        this.rank = r;
        //setPreferredSize( new Dimension(40,40));
        addMouseListener(new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
            if (editable) {
              selected = rank;
              Panel.this.repaint();
            }
          }
        });
      }

      public void printV(Graphics2D g, int x, int y, int width, int height) {
        int textSize = 15; // LOOK : neeed to calculate this

        g.setColor(cs.getColor(rank));
        g.fillRect(x, y + textSize, width, height - textSize);

        //g.setColor( Color.black);
        //g.drawRect( x, y+textSize, width, height-textSize);

        if (useLabel) {
          g.setColor(Color.black);
          g.setFont(sf.getFont());
          g.drawString(label[rank], x + 3, y + 10);
        }
      }

      public void printH(Graphics2D g, int x, int y, int width, int height) {
        int textSize = 15; // LOOK : neeed to calculate this

        g.setColor(cs.getColor(rank));
        g.fillRect(x, y + textSize, width, height - 2 * textSize);

        g.setColor(Color.white);
        g.drawRect(x, y + textSize, width, height - 2 * textSize);

        if (useLabel) {
          g.setColor(Color.black);
          g.setFont(sf.getFont());
          if (rank % 2 == 0) // even
            g.drawString(label[rank], x, y + textSize);
          else
            g.drawString(label[rank], x, y + height);
        }
      }

      public void paintComponent(Graphics g) {
        Rectangle b = getBounds();

        g.setColor(cs.getColor(rank));
        g.fillRect(0, 0, b.width - 1, b.height - 1);

        g.setColor((selected == rank) ? Color.magenta : Color.black);
        g.drawRect(0, 0, b.width - 1, b.height - 1);

        if (selected == rank) {
          g.drawLine(0, 0, b.width, b.height);
          g.drawLine(0, b.height, b.width, 0);
        }

        if (useLabel) {
          g.setColor(Color.black);
          g.setFont(sf.getFont());
          g.drawString(label[rank], 3, 10);
        }
      }
    } // end inner class ColorInterval


  } // end inner class ColorScale.Panel
}