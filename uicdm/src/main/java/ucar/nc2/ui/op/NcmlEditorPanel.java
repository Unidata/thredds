/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.nc2.ui.OpPanel;
import ucar.util.prefs.PreferencesExt;

import java.awt.BorderLayout;
import java.io.IOException;

/**
 *
 */
public class NcmlEditorPanel extends OpPanel {
    private NcmlEditor editor;

/**
 *
 */
    public NcmlEditorPanel(PreferencesExt p) {
        super(p, "dataset:", true, false);
        editor = new NcmlEditor(buttPanel, prefs);
        add(editor, BorderLayout.CENTER);
    }

/** */
    @Override
    public boolean process(Object o) {
        return editor.setNcml((String) o);
    }

/** */
    @Override
    public void closeOpenFiles() throws IOException {
        editor.closeOpenFiles();
    }

/** */
    @Override
    public void save() {
        super.save();
        editor.save();
    }
}
