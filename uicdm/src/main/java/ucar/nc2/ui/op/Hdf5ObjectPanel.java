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
public class Hdf5ObjectPanel extends OpPanel {
    RandomAccessFile raf = null;
    Hdf5ObjectTable hdf5Table;

    public Hdf5ObjectPanel(PreferencesExt p) {
      super(p, "file:", true, false);
      hdf5Table = new Hdf5ObjectTable(prefs);
      add(hdf5Table, BorderLayout.CENTER);

      final AbstractButton infoButton = BAMutil.makeButtcon("Information", "Compact Representation", false);
      infoButton.addActionListener(e -> {
          final Formatter f = new Formatter();
          try {
            hdf5Table.showInfo(f);
          }
          catch (IOException ioe) {
            final StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            return;
          }
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
      });
      buttPanel.add(infoButton);

      final AbstractButton infoButton2 = BAMutil.makeButtcon("Information", "Detail Info", false);
      infoButton2.addActionListener(e -> {
          final Formatter f = new Formatter();
          try {
            hdf5Table.showInfo2(f);
          }
          catch (final IOException ioe) {
            final StringWriter sw = new StringWriter(5000);
            ioe.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            return;
          }
          detailTA.setText(f.toString());
          detailTA.gotoTop();
          detailWindow.show();
      });
      buttPanel.add(infoButton2);

      final AbstractButton eosdump = BAMutil.makeButtcon("alien", "Show EOS processing", false);
      eosdump.addActionListener(e -> {
          try {
            final Formatter f = new Formatter();
            hdf5Table.getEosInfo(f);
            detailTA.setText(f.toString());
            detailWindow.show();
          }
          catch (IOException ioe) {
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

            hdf5Table.setHdf5File(raf);
      }
      catch (FileNotFoundException ioe) {
            JOptionPane.showMessageDialog(null, "Hdf5ObjectTable cannot open " + command + "\n" + ioe.getMessage());
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
        hdf5Table.closeOpenFiles();
    }

/** */
    @Override
    public void save() {
        hdf5Table.save();
        super.save();
    }
}
