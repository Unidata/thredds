/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.nc2.ui.ReportPanel;
import ucar.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

/**
 *
 */
public class ReportOpPanel extends OpPanel {
    private ReportPanel reportPanel;
    private boolean useIndex = true;
    private boolean eachFile = false;
    private boolean extra = false;
    private JComboBox reports;

/**
 *
 */
    public ReportOpPanel(PreferencesExt p, ReportPanel reportPanel) {
        super(p, "collection:", true, false);
        this.reportPanel = reportPanel;
        add(reportPanel, BorderLayout.CENTER);
        reportPanel.addOptions(buttPanel);

        reports = new JComboBox(reportPanel.getOptions());
        buttPanel.add(reports);

        AbstractAction useIndexButt = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                useIndex = (Boolean) getValue(BAMutil.STATE);
            }
        };
        useIndexButt.putValue(BAMutil.STATE, useIndex);
        BAMutil.setActionProperties(useIndexButt, "nj22/Doit", "use Index", true, 'C', -1);
        BAMutil.addActionToContainer(buttPanel, useIndexButt);

        final AbstractAction eachFileButt = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                eachFile = (Boolean) getValue(BAMutil.STATE);
            }
        };
        eachFileButt.putValue(BAMutil.STATE, eachFile);
        BAMutil.setActionProperties(eachFileButt, "nj22/Doit", "report on each file", true, 'E', -1);
        BAMutil.addActionToContainer(buttPanel, eachFileButt);

        final AbstractAction extraButt = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                extra = (Boolean) getValue(BAMutil.STATE);
            }
        };
        extraButt.putValue(BAMutil.STATE, extra);
        BAMutil.setActionProperties(extraButt, "nj22/Doit", "extra info", true, 'X', -1);
        BAMutil.addActionToContainer(buttPanel, extraButt);

        final AbstractAction doitButt = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                process();
            }
        };
        BAMutil.setActionProperties(doitButt, "alien", "make report", false, 'C', -1);
        BAMutil.addActionToContainer(buttPanel, doitButt);
    }

/**
 *
 */
    @Override
    public void closeOpenFiles() {
        // Nothing to do here.
    }

/**
 *
 */
    @Override
    public boolean process(Object o) {
        return reportPanel.showCollection((String) o);
    }

/**
 *
 */
    public boolean process() {
        boolean err = false;
        String command = (String) cb.getSelectedItem();

        try {
            reportPanel.doReport(command, useIndex, eachFile, extra, reports.getSelectedItem());
        }
        catch (IOException ioe) {
            JOptionPane.showMessageDialog(null, "Grib2ReportPanel cannot open " + command + "\n" + ioe.getMessage());
            ioe.printStackTrace();
            err = true;
        }
        catch (Exception e) {
            StringWriter sw = new StringWriter(5000);
            e.printStackTrace(new PrintWriter(sw));
            detailTA.setText(sw.toString());
            detailWindow.show();
            err = true;
        }

        return !err;
    }

/**
 *
 */
    @Override
    public void save() {
        // reportPanel.save();
        super.save();
    }
}
