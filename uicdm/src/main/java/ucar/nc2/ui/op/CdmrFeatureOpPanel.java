/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/**
 *
 */
public class CdmrFeatureOpPanel extends OpPanel {
    private CdmrFeaturePanel panel;

/**
 *
 */
    public CdmrFeatureOpPanel(PreferencesExt p) {
        super(p, "file:", true, false);
        panel = new CdmrFeaturePanel(prefs);
        add(panel, BorderLayout.CENTER);

        final AbstractAction infoAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Formatter f = new Formatter();
                try {
                    panel.showInfo(f);
                }
                catch (final Exception ioe) {
                    final StringWriter sw = new StringWriter(5000);
                    ioe.printStackTrace(new PrintWriter(sw));
                    detailTA.setText(sw.toString());
                    detailWindow.show();
                    return;
                }
                detailTA.setText(f.toString());
                detailTA.gotoTop();
                detailWindow.show();
            }
        };
        BAMutil.setActionProperties(infoAction, "Information", "show Info", false, 'I', -1);
        BAMutil.addActionToContainer(buttPanel, infoAction);
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        boolean err = false;

        try {
            panel.setNcStream(command);
        }
        catch (FileNotFoundException ioe) {
            JOptionPane.showMessageDialog(null, "CdmremotePanel cannot open " + command + "\n" + ioe.getMessage());
            err = true;
        }
        catch (Exception e) {
            final StringWriter sw = new StringWriter(5000);
            e.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            err = true;
        }

        return !err;
    }

/** */
    @Override
    public void save() {
        panel.save();
        super.save();
    }

/** */
    @Override
    public void closeOpenFiles() throws IOException {
        panel.closeOpenFiles();
    }
}
