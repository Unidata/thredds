/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JOptionPane;

/**
 *
 */
public class BufrCdmIndexOpPanel extends OpPanel {
    private BufrCdmIndexPanel table;

/**
 *
 */
    public BufrCdmIndexOpPanel(PreferencesExt p) {
        super(p, "index file:", true, false);
        table = new BufrCdmIndexPanel(prefs, buttPanel);
        add(table, BorderLayout.CENTER);
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        boolean err = false;

        try {
            table.setIndexFile(command);
        }
        catch (FileNotFoundException ioe) {
            JOptionPane.showMessageDialog(null, "BufrCdmIndexPanel cannot open " + command + "\n" + ioe.getMessage());
            err = true;
        }
        catch (Exception e) {
            e.printStackTrace();
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
    public void closeOpenFiles() throws IOException {
      //table.closeOpenFiles();
    }

/** */
    @Override
    public void save() {
        table.save();
        super.save();
    }
}
