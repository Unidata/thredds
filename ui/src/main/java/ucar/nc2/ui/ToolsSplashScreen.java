/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui;

import ucar.nc2.ui.util.Resource;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 *
 */
class ToolsSplashScreen extends javax.swing.JWindow {
    private static ToolsSplashScreen instance;

/**
 *
 */
    public static ToolsSplashScreen getSharedInstance()
    {
        if (instance == null) { instance = new ToolsSplashScreen(); }

        return instance;
    }

/**
 *
 */
    private ToolsSplashScreen() {
        Image image = Resource.getImage("/resources/nj22/ui/pix/ring2.jpg");
        if (image != null) {
            final ImageIcon icon = new ImageIcon(image);
            final JLabel lab = new JLabel(icon);
            getContentPane().add(lab);
            pack();

            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            final int width  = image.getWidth(null);
            final int height = image.getHeight(null);
            setLocation(screenSize.width / 2 - (width / 2), screenSize.height / 2 - (height / 2));

            // ANy click on the window hides it.
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    setVisible(false);
                }
            });
        }
    }
}
