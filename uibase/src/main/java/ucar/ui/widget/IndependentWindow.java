/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import ucar.ui.util.ScreenUtils;

/**
 *  Provides common L&F for managing independent windows
 *  Will reset L&F
 *
 *  example: <pre>
     infoWindow = new IndependentWindow("Dataset Information");
     datasetInfoTA = new TextHistoryPane(500, 100, true);
     Container cp = infoWindow.getContentPane();
     cp.add(datasetInfoTA, BorderLayout.CENTER);
     infoWindow.pack();
     infoWindow.setSize(700,700);
     infoWindow.setLocation(100,100);
 *   </pre>
 *
 * @author John Caron
 */
public class IndependentWindow extends JFrame {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IndependentWindow.class);


  /** constructor
     @param title       Window title
     @param iconImage   image to show when iconified
   */
  public IndependentWindow(String title, Image iconImage) {
    super(title);

    // L&F may change
    UIManager.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("lookAndFeel"))
          SwingUtilities.updateComponentTreeUI(IndependentWindow.this);
      }
    });

    if (null != iconImage)
      setIconImage( iconImage);
  }

  /** constructor
     @param title       Window title.
     @param comp the COmponent to put in the window.
   */
  public IndependentWindow(String title, Image iconImage, Component comp) {
    this(title, iconImage);
    setComponent(comp);
  }

  public void setComponent(Component comp ) {
    Container cp = getContentPane();
    cp.removeAll();
    cp.add(comp, BorderLayout.CENTER);
    try {
      pack();
    } catch (IllegalArgumentException e) {
      // Ticket ID: HEM-237554
      // I'm using IceWM window manager under Linux, and it does not support changing the icon on the top left side of the window.
      // This crashes the whole thing. I dont think this should be a fatal exception.
      // It would be helpful for future releases to catch this exception and let the program go ahead without the icon.
      log.error("Possible problem setting icon (?)", e);
    }
  }

  /** show the window. */
  public void show() {
    setState( Frame.NORMAL );   // deiconify if needed
    super.toFront();
    // need to put on event thread
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        IndependentWindow.super.show();
      }
    });
  }

  /** show if not iconified */
  public void showIfNotIconified() {
    if (getState() == Frame.ICONIFIED) return;
    // need to put on event thread
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        IndependentWindow.super.show();
      }
    });
  }

  @Override
  public void setBounds(Rectangle r) {
    // keep window on the screen
    Rectangle screenSize = ScreenUtils.getScreenVirtualSize();
    Rectangle result = r.intersection(screenSize);
    if (!result.isEmpty())
      super.setBounds(result);
  }

}
