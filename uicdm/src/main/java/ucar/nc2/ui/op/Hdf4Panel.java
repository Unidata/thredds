/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.ui.widget.BAMutil;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import javax.swing.AbstractButton;
import javax.swing.JOptionPane;

/**
 *
 */
public class Hdf4Panel extends OpPanel {
    private RandomAccessFile raf;
    private Hdf4Table hdf4Table;

/**
 *
 */
    public Hdf4Panel(PreferencesExt p) {
        super(p, "file:", true, false);
        hdf4Table = new Hdf4Table(prefs);
        add(hdf4Table, BorderLayout.CENTER);

        final AbstractButton eosdump = BAMutil.makeButtcon("alien", "Show EOS processing", false);
        eosdump.addActionListener(e -> {
            try {
                final Formatter f = new Formatter();
                hdf4Table.getEosInfo(f);
                detailTA.setText(f.toString());
                detailWindow.show();
            }
            catch (final IOException ioe) {
                final StringWriter sw = new StringWriter(5000);
                ioe.printStackTrace(new PrintWriter(sw));
                detailTA.setText(sw.toString());
                detailWindow.show();
            }
        });
        buttPanel.add(eosdump);
    }

/** */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        boolean err = false;

        try {
            if (raf != null) {
                raf.close();
            }
            raf = new RandomAccessFile(command, "r");

            hdf4Table.setHdf4File(raf);
      }
      catch (FileNotFoundException ioe) {
            JOptionPane.showMessageDialog(null, "NetcdfDataset cannot open " + command + "\n" + ioe.getMessage());
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
    public void closeOpenFiles() throws IOException {
        hdf4Table.closeOpenFiles();
    }

/** */
    @Override
    public void save() {
        hdf4Table.save();
        super.save();
    }
}
