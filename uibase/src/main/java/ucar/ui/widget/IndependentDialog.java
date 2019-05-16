/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

import ucar.ui.util.ScreenUtils;

import java.awt.*;
import java.beans.*;
import javax.swing.*;

/**
 *  Provides common L&F for managing independent dialogs
 *  Takes RootPaneContainer as parent, to work with both applet and app
 *  Will reset L&F
 *
 *  example of use:
     infoWindow = new IndependentDialog(topLevel.getRootPaneContainer(), false, "Dataset Information");
     datasetInfoTA = new TextHistoryPane(500, 100, true);
     Container cp = infoWindow.getContentPane();
     cp.add(datasetInfoTA, BorderLayout.CENTER);
     infoWindow.pack();
     infoWindow.setSize(700,700);
     infoWindow.setLocation(100,100);
 *
 * @author John Caron
 */
public class IndependentDialog extends JDialog {
  protected JFrame parent;

  /** constructor
     @param parent      JFrame (application) or JApplet (applet)
     @param modal     is modal
     @param title       Window title
   */
  public IndependentDialog(JFrame parent, boolean modal, String title) {
    // having a parent JFrame is better. But what to do about applets?
    super(parent == null ? ScreenUtils.findActiveFrame() : parent);
    this.parent = parent;
    setModal(modal);
    if (title != null)
      setTitle(title);

    // L&F may change
    UIManager.addPropertyChangeListener( new PropertyChangeListener() {
      public void propertyChange( PropertyChangeEvent e) {
        if (e.getPropertyName().equals("lookAndFeel"))
          SwingUtilities.updateComponentTreeUI( IndependentDialog.this);
      }
    });

    // set position based on owned
    Window owner = getOwner();
    if (owner != null) {
      Rectangle b = owner.getBounds();
      int x = (int) ( b.getX() + b.getWidth()/2);
      int y = (int) ( b.getY() + b.getHeight()/2);
      setLocation( x, y);
    }
  }

  public IndependentDialog(JFrame parent, boolean modal, String title, Component comp) {
    this(parent, modal, title);
    setComponent( comp);
  }

  public void setComponent( Component comp) {
    Container cp = getContentPane();
    cp.add(comp, BorderLayout.CENTER);
    pack();
  }

  /* @Override
     public void setBounds(Rectangle r) {
    if (parent != null) {
      Rectangle have = parent.getGraphicsConfiguration().getBounds();
      if (r.getX() < have.getWidth() - 25); // may be off screen when switching between 2 monitor system
      setBounds(r.x, r.y, r.width, r.height);
    }
  } */

  @Override
  public void setBounds(Rectangle r) {
    // keep window on the screen
    Rectangle screenSize = ScreenUtils.getScreenVirtualSize();
    Rectangle result = r.intersection(screenSize);
    if (!result.isEmpty())
      super.setBounds(result);
  }
}

