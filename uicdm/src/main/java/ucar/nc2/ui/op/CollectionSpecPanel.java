/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import javax.swing.AbstractButton;

/**
 *
 */
public class CollectionSpecPanel extends OpPanel {
    private CollectionSpecTable table;

/**
 *
 */
    public CollectionSpecPanel(PreferencesExt dbPrefs) {
        super(dbPrefs, "collection spec:", true, false);
        table = new CollectionSpecTable(prefs);
        add(table, BorderLayout.CENTER);

        final AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
        infoButton.addActionListener(e -> {
            Formatter f = new Formatter();
            try {
                table.showCollection(f);
            }
            catch (Exception e1) {
                final StringWriter sw = new StringWriter(5000);
                e1.printStackTrace(new PrintWriter(sw));
                f.format("%s", sw.toString());
            }
            detailTA.setText(f.toString());
            detailTA.gotoTop();
            detailWindow.show();
        });
        buttPanel.add(infoButton);
    }

/** */
    @Override
    public boolean process(Object o) {
        final String command = (String) o;
        if (command == null) {
            return false;
        }

        try {
            table.setCollection(command);
            return true;
        }
        catch (Exception ioe) {
            final StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailTA.gotoTop();
            detailWindow.show();
        }

        return false;
    }

/** */
    @Override
    public void closeOpenFiles() {
        // Nothing to do here.
    }

/** */
    @Override
    public void save() {
        table.save();
        super.save();
    }
  }
