/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.image.ImageViewPanel;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JSplitPane;

/**
 *
 */
public class ImagePanel extends OpPanel {
    private ImageViewPanel imagePanel;
    private JSplitPane split;

/**
*
*/
    public ImagePanel(PreferencesExt dbPrefs) {
        super(dbPrefs, "dataset:", true, false);
        imagePanel = new ImageViewPanel(buttPanel);
        add(imagePanel, BorderLayout.CENTER);
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;

        try {
            if (null != command) {
                imagePanel.setImageFromUrl(command);
            }
        }
        catch (Exception ioe) {
            ioe.printStackTrace();
            final StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            return false;
        }

        return true;
    }

/**
 *
 */
    public void setImageLocation(final String location) {
        imagePanel.setImageFromUrl(location);
        setSelectedItem(location);
    }

/** */
    @Override
    public void closeOpenFiles() throws IOException {
        // Do nothing
    }
}
