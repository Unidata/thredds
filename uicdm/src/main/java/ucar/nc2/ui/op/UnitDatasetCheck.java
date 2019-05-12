/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.ui.OpPanel;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.units.SimpleUnit;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 */
public class UnitDatasetCheck extends OpPanel {
    private TextHistoryPane ta;

/**
 *
 */
    UnitDatasetCheck(final PreferencesExt p) {
        super(p, "dataset:");
        ta = new TextHistoryPane(true);
        add(ta, BorderLayout.CENTER);
    }

/**
 *
 */
    @Override
    public boolean process(Object o) {
        String command = (String) o;
        boolean err = false;

        try (NetcdfFile ncfile = NetcdfDataset.openDataset(command, addCoords, null)) {
            ta.setText("Variables for " + command + ":");
            for (final Variable o1 : ncfile.getVariables()) {
                final VariableEnhanced vs = (VariableEnhanced) o1;
                final String units = vs.getUnitsString();
                final StringBuilder sb = new StringBuilder();
                sb.append("   ").append(vs.getShortName()).append(" has unit= <").append(units).append(">");
                if (units != null) {
                    try {
                        final SimpleUnit su = SimpleUnit.factoryWithExceptions(units);
                        sb.append(" unit convert = ").append(su.toString());
                        if (su.isUnknownUnit()) {
                            sb.append(" UNKNOWN UNIT");
                        }
                    }
                    catch (Exception ioe) {
                        sb.append(" unit convert failed ");
                        sb.insert(0, "**** Fail ");
                    }
                }

                ta.appendLine(sb.toString());
            }
      }
      catch (final FileNotFoundException ioe) {
          ta.setText("Failed to open <" + command + ">");
          err = true;
      }
      catch (final IOException ioe) {
          ioe.printStackTrace();
          err = true;
      }

      return !err;
    }

/**
 *
 */
    @Override
    public void closeOpenFiles() throws IOException {
        ta.clear();
    }
}
