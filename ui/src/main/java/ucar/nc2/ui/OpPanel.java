/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui;

import ucar.nc2.ui.widget.BAMutil;
import ucar.nc2.ui.widget.FileManager;
import ucar.nc2.ui.widget.IndependentWindow;
import ucar.nc2.ui.widget.TextHistoryPane;
import ucar.nc2.ui.widget.StopButton;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.ui.ComboBox;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 *  Abstract superclass for ToolsUI panel contents.
 *
 *  Subclasses must implement process()
 */
abstract class OpPanel extends JPanel {
    final static org.slf4j.Logger logger
                            = org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected PreferencesExt prefs;
    protected ComboBox cb;
    protected JPanel buttPanel, topPanel;
    protected AbstractButton coordButt = null;
    protected StopButton stopButton;

    protected boolean addCoords;
    protected boolean busy;
    protected long lastEvent = -1;
    protected boolean eventOK = true;

    protected IndependentWindow detailWindow;
    protected TextHistoryPane detailTA;

    protected static FileManager fileChooser;
//    protected boolean debug = false;
//    protected boolean debugCB = false;

/**
 *
 */
    OpPanel(PreferencesExt prefs, String command) {
        this(prefs, command, true, true);
    }

/**
 *
 */
    OpPanel(PreferencesExt prefs, String command, boolean addFileButton, boolean addCoordButton) {
        this(prefs, command, true, addFileButton, addCoordButton);
    }

/**
 *
 */
    OpPanel(PreferencesExt prefs, String command, boolean addComboBox, boolean addFileButton, boolean addCoordButton) {
        this.prefs = prefs;
        buttPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        cb = new ComboBox(prefs);
        cb.addActionListener(e -> {
            logger.debug(" doit {} cmd={} when={} class={}", cb.getSelectedItem(),
                                e.getActionCommand(), e.getWhen(), getClass().getName());

            // eliminate multiple events from same selection by ignoring events occurring within 100ms of last one.
            if (eventOK && (e.getWhen() > lastEvent + 100)) {
                doit(cb.getSelectedItem());
                lastEvent = e.getWhen();
            }
        });

        AbstractAction closeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    closeOpenFiles();
                }
                catch (IOException e1) {
                    System.out.printf("close failed");
                }
            }
        };
        BAMutil.setActionProperties(closeAction, "Close", "release files", false, 'L', -1);
        BAMutil.addActionToContainer(buttPanel, closeAction);

        if (addFileButton) {
            AbstractAction fileAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String filename = fileChooser.chooseFilename();
                    if (filename == null) { return; }
                    cb.setSelectedItem(filename);
                }
            };
            BAMutil.setActionProperties(fileAction, "FileChooser", "open Local dataset...", false, 'L', -1);
            BAMutil.addActionToContainer(buttPanel, fileAction);
        }

        if (addCoordButton) {
            AbstractAction coordAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addCoords = (Boolean) getValue(BAMutil.STATE);
                    String tooltip = addCoords ? "add Coordinates is ON" : "add Coordinates is OFF";
                    coordButt.setToolTipText(tooltip);
                    //doit( cb.getSelectedItem()); // called from cb action listener
                }
            };
            addCoords = prefs.getBoolean("coordState", false);
            String tooltip2 = addCoords ? "add Coordinates is ON" : "add Coordinates is OFF";
            BAMutil.setActionProperties(coordAction, "addCoords", tooltip2, true, 'C', -1);
            coordAction.putValue(BAMutil.STATE, Boolean.valueOf(addCoords));
            coordButt = BAMutil.addActionToContainer(buttPanel, coordAction);
        }

        if (this instanceof GetDataRunnable) {
            stopButton = new StopButton("Stop");
            buttPanel.add(stopButton);
        }

        topPanel = new JPanel(new BorderLayout());
        if (addComboBox) {
            topPanel.add(new JLabel(command), BorderLayout.WEST);
            topPanel.add(cb, BorderLayout.CENTER);
            topPanel.add(buttPanel, BorderLayout.EAST);
        }
        else {
            topPanel.add(buttPanel, BorderLayout.EAST);
        }

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);

        detailTA = new TextHistoryPane();
        detailTA.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailWindow = new IndependentWindow("Details", BAMutil.getImage("netcdfUI"), new JScrollPane(detailTA));
        Rectangle bounds = (Rectangle) prefs.getBean(ToolsUI.FRAME_SIZE, new Rectangle(200, 50, 500, 700));
        detailWindow.setBounds(bounds);
    }

/**
 *
 */
    void doit(Object command) {
        if (busy) {
          return;
        }
        if (command == null) {
          return;
        }
        if (command instanceof String) {
          command = ((String) command).trim();
        }
        logger.debug("{} process={}", getClass().getName(), command);

        busy = true;
        if (process(command)) {
            setSelectedItem(command);
        }
        busy = false;
    }

/**
 *
 */
    abstract boolean process(Object command);

/**
 *
 */
    void closeOpenFiles() throws IOException {
    }

/**
 *
 */
    void save() {
        cb.save();
        if (coordButt != null) {
            prefs.putBoolean("coordState", coordButt.getModel().isSelected());
        }
        if (detailWindow != null) {
            prefs.putBeanObject(ToolsUI.FRAME_SIZE, detailWindow.getBounds());
        }
      }

/**
 *
 */
    void setSelectedItem(Object item) {
        eventOK = false;
        cb.addItem(item);
        eventOK = true;
    }

/**
 *
 */
    static void setFileChooser(FileManager chooser) {
            fileChooser = chooser;
    }
}
