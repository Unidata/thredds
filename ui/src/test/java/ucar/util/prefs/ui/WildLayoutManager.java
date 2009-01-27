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
package ucar.util.prefs.ui;

import java.awt.*;
import javax.swing.*;

public class WildLayoutManager implements LayoutManager {
  // these are the constraints possible with the WildLayoutManager
  public static final String LEFT = "Left";
  public static final String RIGHT = "Right";
  public static final String MIDDLE = "Middle";

  // We keep handles to three components, left, right and middle
  private Component left;
  private Component right;
  private Component middle;

  // we need to be able to add components.  if two components are added
  // with the same constraint we keep the last one
  public void addLayoutComponent(String name, Component comp) {
    if (LEFT.equals(name)) {
      left = comp;
    } else if (RIGHT.equals(name)) {
      right = comp;
    } else if (MIDDLE.equals(name)) {
      middle = comp;
    } else {
      throw new IllegalArgumentException(
        "cannot add to layout: unknown constraint: " + name);
    }
  }

  // here we remove the component - first find it!
  public void removeLayoutComponent(Component comp) {
    if (comp == left) {
      left = null;
    } else if (comp == right) {
      right = null;
    } else if (comp == middle) {
      middle = null;
    }
  }

  // The minimum dimension we're happy with is the preferred size
  // this could be more fancy by using the minimum sizes of each component
  public Dimension minimumLayoutSize(Container parent) {
    return preferredLayoutSize(parent);
  }

  // Here we work out the preferred size of the component, which is used
  // by methods such as pack() to work out how big the window should be
  public Dimension preferredLayoutSize(Container parent) {
    Dimension dim = new Dimension(0, 0);
    // get widest preferred width for left && right
    // get highest preferred height for left && right
    // add preferred width of middle
    int widestWidth = 0;
    int highestHeight = 0;
    if ((left != null) && left.isVisible()) {
      widestWidth = Math.max(widestWidth, left.getPreferredSize().width);
      highestHeight =
        Math.max(highestHeight, left.getPreferredSize().height);
    }
    if ((right != null) && right.isVisible()) {
      widestWidth = Math.max(widestWidth, right.getPreferredSize().width);
      highestHeight =
        Math.max(highestHeight, right.getPreferredSize().height);
    }
    dim.width = widestWidth * 2;
    dim.height = highestHeight;
    if ((middle != null) && middle.isVisible()) {
      dim.width += middle.getPreferredSize().width;
      dim.height = Math.max(dim.height, middle.getPreferredSize().height);
    }
    Insets insets = parent.getInsets();
    dim.width += insets.left + insets.right;
    dim.height += insets.top + insets.bottom;
    return dim;
  }

  // this is the brain of the layout manager, albeit rather small.
  // I told you this is straightforward...
  public void layoutContainer(Container target) {
    // these variables hold the position where we can draw components
    // taking into account insets
    Insets insets = target.getInsets();
    int north = insets.top;
    int south = target.getSize().height - insets.bottom;
    int west = insets.left;
    int east = target.getSize().width - insets.right;
    // we first find the width of the left and right components
    int widestWidth = 0;
    if ((left != null) && left.isVisible()) {
      widestWidth = Math.max(widestWidth, left.getPreferredSize().width);
    }
    if ((right != null) && right.isVisible()) {
      widestWidth = Math.max(widestWidth, right.getPreferredSize().width);
    }
    if ((middle != null) && middle.isVisible()) {
      widestWidth = Math.max(widestWidth,
        (east - west - middle.getPreferredSize().width) / 2);
    }
    // next we set the size of the left component equal to the widest width
    // and whole height, and we set the bounds from North-West corner
    if ((left != null) && left.isVisible()) {
      left.setSize(widestWidth, south - north);
      left.setBounds(west, north, widestWidth, south - north);
    }
    // next we set the size of right component equal to the widest width
    // and whole height, and we set the bounds from North-East corner
    if ((right != null) && right.isVisible()) {
      right.setSize(widestWidth, south - north);
      right.setBounds(east-widestWidth, north, widestWidth, south - north);
    }
    // lastly we set the size of the middle component equals to the
    // remaining width, which should be equal to the middle object's
    // preferred width and we set the height equal to the middle object's
    // preferred height
    if ((middle != null) && middle.isVisible()) {
      middle.setSize(east - west - widestWidth * 2,
        middle.getPreferredSize().height);
      middle.setBounds(
        west+widestWidth,
        north + (south - north - middle.getPreferredSize().height)/2,
        east - west - widestWidth * 2,
        middle.getPreferredSize().height);
    }
  }

 // test
 private static class WildLayoutExample extends JFrame {
   public WildLayoutExample() {
     super("WildLayoutExample");
     setSize(new Dimension(400, 300));
     getContentPane().setLayout(new WildLayoutManager());
     // construct the left panel
     JPanel leftPanel = new JPanel(new BorderLayout());
     leftPanel.add(new JLabel("Left Label"), BorderLayout.NORTH);
     leftPanel.add(new JTree(), BorderLayout.CENTER);
     // construct the middle panel
     JPanel middlePanel = new JPanel(new GridLayout(0, 1, 5, 5));
     middlePanel.add(new JButton("Add >"), null);
     middlePanel.add(new JButton("<< Remove All"), null);
     // construct the right panel
     JPanel rightPanel = new JPanel(new BorderLayout());
     rightPanel.add(new JLabel("Right Label"), BorderLayout.NORTH);
     rightPanel.add(new JTextArea("jTextArea1"), BorderLayout.CENTER);
     // add the panels to the content pane using our new layout manager
     getContentPane().add(leftPanel, WildLayoutManager.LEFT);
     getContentPane().add(middlePanel, WildLayoutManager.MIDDLE);
     getContentPane().add(rightPanel, WildLayoutManager.RIGHT);
   }
  }

  public static void main(String[] args) {
    WildLayoutExample frame = new WildLayoutExample();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // JDK 1.3 !
    frame.setVisible(true);
  }
}

