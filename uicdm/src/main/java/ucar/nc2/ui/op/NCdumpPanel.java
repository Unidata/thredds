/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ui.GetDataRunnable;
import ucar.nc2.ui.GetDataTask;
import ucar.nc2.ui.OpPanel;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.StringWriter;

/**
 *
 */
public class NCdumpPanel extends OpPanel implements GetDataRunnable {
    private GetDataTask task;
    private NetcdfFile ncfile;
    private String filename;
    private String command;
    private String result;
    private TextHistoryPane ta;

/**
 *
 */
    public NCdumpPanel(final PreferencesExt prefs) {
        super(prefs, "command:");

        ta = new TextHistoryPane(true);
        add(ta, BorderLayout.CENTER);

        stopButton.addActionListener(e -> {
            if (task.isSuccess()) {
                ta.setText(result);
            }
            else {
                ta.setText(task.getErrorMessage());
            }

            if (task.isCancel()) {
                ta.appendLine("\n***Cancelled by User");
            }

            ta.gotoTop();
        });
    }

/** */
    @Override
    public void closeOpenFiles() throws IOException {
        if (ncfile != null) { ncfile.close(); }
        ncfile = null;
    }

/** */
    @Override
    public boolean process(Object o) {
        int pos;
        String input = ((String) o).trim();

        // deal with possibility of blanks in the filename
        if ((input.indexOf('"') == 0) && ((pos = input.indexOf('"', 1)) > 0)) {
            filename = input.substring(1, pos);
            command = input.substring(pos + 1);
        }
        else if ((input.indexOf('\'') == 0) && ((pos = input.indexOf('\'', 1)) > 0)) {
            filename = input.substring(1, pos);
            command = input.substring(pos + 1);
        }
        else {
            pos = input.indexOf(' ');
            if (pos > 0) {
                filename = input.substring(0, pos);
                command = input.substring(pos);
            }
            else {
                filename = input;
                command = null;
            }
        }

        task = new GetDataTask(this, filename, null);
        stopButton.startProgressMonitorTask(task);

        return true;
    }

/**
 *
 */
    public void run(Object o) throws IOException {
        try {
            if (addCoords) {
                ncfile = NetcdfDataset.openDataset(filename, true, null);
            }
            else {
                ncfile = NetcdfDataset.openFile(filename, null);
            }

            final StringWriter sw = new StringWriter(50000);
            NCdumpW.print(ncfile, command, sw, task);
            result = sw.toString();
        }
        finally {
            try {
                if (ncfile != null) {
                    ncfile.close();
                }
                ncfile = null;
            }
            catch (IOException ioe) {
                System.out.printf("Error closing %n");
            }
        }
    }

/**
 *  allow calling from outside
 */
    public void setNetcdfFile(NetcdfFile ncf) {
        this.ncfile = ncf;
        this.filename = ncf.getLocation();

        final GetDataRunnable runner = new GetDataRunnable() {
            public void run(Object o) throws IOException {
                final StringWriter sw = new StringWriter(50000);
                NCdumpW.print(ncfile, command, sw, task);
                result = sw.toString();
            }
        };
        task = new GetDataTask(runner, filename, null);
        stopButton.startProgressMonitorTask(task);
    }
}
