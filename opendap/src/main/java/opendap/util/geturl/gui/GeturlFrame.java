/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////


package opendap.util.geturl.gui;

import opendap.dap.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * Geturl frame.
 */
public class GeturlFrame extends Frame {
    /**
     * True if we are an applet.
     */
    protected boolean isApplet;

    /**
     * The TextField containing the URL to retrieve.
     */
    protected TextField urlField;

    /**
     * The TextArea where output should be written.
     */
    protected TextArea outputArea;

    /**
     * The "Get DAS", "Get DDS", and "Get Data" buttons.
     */
    protected Button getDASButton, getDDSButton, getDataButton;

    public GeturlFrame(boolean isApplet) {
        super("OPeNDAP Geturl Applet");
        setSize(640, 480);
        this.isApplet = isApplet;

        // add URL TextField, buttons, and output TextArea
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(gbl);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridwidth = 1;
        gbc.gridy = 0;
        Label urlLabel = new Label("URL:");
        gbl.setConstraints(urlLabel, gbc);
        add(urlLabel);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 4;
        urlField = new TextField(70);
        gbl.setConstraints(urlField, gbc);
        add(urlField);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        getDASButton = new Button("Get DAS");
        getDASButton.addActionListener(new GetDASListener());
        gbl.setConstraints(getDASButton, gbc);
        add(getDASButton);

        getDDSButton = new Button("Get DDS");
        getDDSButton.addActionListener(new GetDDSListener());
        gbl.setConstraints(getDDSButton, gbc);
        add(getDDSButton);

        getDataButton = new Button("Get Data");
        getDataButton.addActionListener(new GetDataListener());
        gbl.setConstraints(getDataButton, gbc);
        add(getDataButton);

        gbc.gridwidth = 5;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 5, 0, 5); // 5 pixel insets on left and right
        outputArea = new TextArea(100, 200);
        outputArea.setEditable(false);
        gbl.setConstraints(outputArea, gbc);
        add(outputArea);

        // set listener for window close
        addWindowListener(new WindowClosedListener(this));

        // finally, show window
        setVisible(true);
    }

    /**
     * A helper method to enable or disable the other buttons while the user
     * is downloading, so they don't click multiple times.
     *
     * @param b if true, enable the buttons; else, disable them.
     */
    private void setButtonsEnabled(boolean b) {
        getDASButton.setEnabled(b);
        getDDSButton.setEnabled(b);
        getDataButton.setEnabled(b);
    }

    private class GetDASListener implements ActionListener, Runnable {
        public void actionPerformed(ActionEvent evt) {
            try {
                // deactivate the other buttons so the user doesn't click them now
                setButtonsEnabled(false);
                // run DConnect in a separate thread from AWT event thread
                Thread runThread = new Thread(this);
                runThread.start();
            }
            catch (IllegalThreadStateException e) {
                outputArea.setText(e.toString());
                setButtonsEnabled(true);
            }
        }

        public void run() {
            try {
                DConnect url = new DConnect(urlField.getText());
                DAS das = url.getDAS();
                CharArrayWriter aw = new CharArrayWriter();
                das.print(new PrintWriter(aw));
                outputArea.setText(aw.toString());
                // reactivate the other buttons
                setButtonsEnabled(true);
            }
            catch (Exception e) {
                outputArea.setText(e.toString());
                setButtonsEnabled(true);
            }
        }
    }

    private class GetDDSListener implements ActionListener, Runnable {
        public void actionPerformed(ActionEvent evt) {
            try {
                // deactivate the other buttons so the user doesn't click them now
                setButtonsEnabled(false);
                // run DConnect in a separate thread from AWT event thread
                Thread runThread = new Thread(this);
                runThread.start();
            }
            catch (IllegalThreadStateException e) {
                outputArea.setText(e.toString());
                setButtonsEnabled(true);
            }
        }

        public void run() {
            try {
                DConnect url = new DConnect(urlField.getText());
                DDS dds = url.getDDS();
                CharArrayWriter aw = new CharArrayWriter();
                dds.print(new PrintWriter(aw));
                outputArea.setText(aw.toString());
                // reactivate the other buttons
                setButtonsEnabled(true);
            }
            catch (Exception e) {
                outputArea.setText(e.toString());
                setButtonsEnabled(true);
            }
        }
    }

    private class GetDataListener implements ActionListener, Runnable {
        public void actionPerformed(ActionEvent evt) {
            try {
                // deactivate the other buttons so the user doesn't click them now
                setButtonsEnabled(false);
                // run DConnect in a separate thread from AWT event thread
                // NOTE: StatusWindow cancel button won't work otherwise!
                Thread runThread = new Thread(this);
                runThread.start();
            }
            catch (IllegalThreadStateException e) {
                outputArea.setText(e.toString());
                setButtonsEnabled(true);
            }
        }

        public void run() {
            try {
                DConnect url = new DConnect(urlField.getText());
                DataDDS dds = url.getData(new StatusWindow(urlField.getText()));
                CharArrayWriter aw = new CharArrayWriter();
                dds.printVal(new PrintWriter(aw));
                outputArea.setText(aw.toString());
                // reactivate the other buttons
                setButtonsEnabled(true);
            }
            catch (Exception e) {
                outputArea.setText(e.toString());
                setButtonsEnabled(true);
            }
        }
    }

    private class WindowClosedListener extends WindowAdapter {
        GeturlFrame myFrame;

        WindowClosedListener(GeturlFrame myFrame) {
            this.myFrame = myFrame;
        }

        public void windowClosing(WindowEvent evt) {
            myFrame.dispose();
            if (myFrame.isApplet == false)
                System.exit(0);
        }
  }
}


