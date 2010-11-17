/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////



package opendap.util.geturl.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;

/**
 * This class implements a simple status window to show OPeNDAP deserialize
 * status.  It shows the URL being retrieved, the number of bytes that
 * have been read so far, the transfer rate in K/sec, and a Cancel button
 * which can be used to abort the download.
 */
public class StatusWindow extends Frame implements opendap.dap.StatusUI {
    /**
     * The Label showing the OPeNDAP URL being downloaded.
     */
    private Label urlLabel;

    /**
     * The number of bytes downloaded so far.
     */
    private int bytesRead;

    /**
     * The start time in milliseconds.
     */
    private long startTime;

    /**
     * The last time the transfer rate was updated, in milliseconds.
     */
    private long lastUpdateTime;

    /**
     * Has the cancel button been pushed by the user?
     */
    private boolean cancelPushed;

    /**
     * The Label showing the number of bytes read
     */
    private Label bytesReadLabel;

    /**
     * The Label showing the current transfer rate
     */
    private Label transferRateLabel;

    /**
     * The cancel button. (NOTE: synchronize methods which access this!)
     */
    private Button cancelButton;

    /**
     * The NumberFormat object used for writing floating point labels.
     */
    NumberFormat nf = NumberFormat.getInstance();

    /**
     * Construct the status window to display download status for the
     * given URL.
     */
    public StatusWindow(String url) {
        super("Status Window");
        bytesRead = 0;
        cancelPushed = false;

        // set to a reasonable size
        setSize(450, 150);

        Label retrievingTag = new Label("Retrieving:");
        urlLabel = new Label(url);

        Label bytesReadTag = new Label("Bytes Read:");
        bytesReadLabel = new Label("                   ");

        Label transferRateTag = new Label("Transfer Rate:");
        transferRateLabel = new Label("                   ");

        cancelButton = new Button("Cancel Download");

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(gbl);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridwidth = 1;
        gbl.setConstraints(retrievingTag, gbc);
        add(retrievingTag);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(urlLabel, gbc);
        add(urlLabel);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridwidth = 1;
        gbl.setConstraints(bytesReadTag, gbc);
        add(bytesReadTag);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(bytesReadLabel, gbc);
        add(bytesReadLabel);

        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridwidth = 1;
        gbl.setConstraints(transferRateTag, gbc);
        add(transferRateTag);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(transferRateLabel, gbc);
        add(transferRateLabel);

        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(cancelButton, gbc);
        add(cancelButton);

        // add action listener for cancel button
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                programCancel();
            }
        });

        // set listener for window close
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                // first, cancel the download
                programCancel();
                // also, dispose the window in case the download has already cancelled
                // due to an abnormal event, such as a SecurityException
                dispose();
            }
        });

        // start the download clock running
        startTime = lastUpdateTime = System.currentTimeMillis();

        // finally, show ourselves
        setVisible(true);
    }

    /**
     * Add bytes to the total deserialize count.  This is called by each
     * BaseType's deserialize() method to provide the user with feedback on the
     * number of bytes that have been transferred so far.  If some future version
     * of OPeNDAP provides a correct Content-Length, then a sophisticated GUI could
     * use this information to estimate the time remaining to download.
     */
    public void incrementByteCount(int bytes) {
        bytesRead += bytes;
        long currentTime = System.currentTimeMillis();
        // update 5 times a second
        if ((currentTime - lastUpdateTime) > 200) {
            if (bytesRead > (1024 * 1024)) {
                float MBread = (float) bytesRead / (1024 * 1024);
                bytesReadLabel.setText(nf.format(MBread) + " MB");
            } else if (bytesRead > 1024) {
                float Kread = (float) bytesRead / 1024;
                bytesReadLabel.setText(nf.format(Kread) + " K");
            } else {
                bytesReadLabel.setText(nf.format(bytesRead));
            }
            bytesReadLabel.repaint();

            float bytesPerSecond = (float) bytesRead / (currentTime - startTime)
                    * 1000;
            if (bytesPerSecond > (1024 * 1024)) {
                float MBperSecond = bytesPerSecond / (1024 * 1024);
                transferRateLabel.setText(nf.format(MBperSecond) + " MB/sec");
            } else if (bytesPerSecond > 1024) {
                float KperSecond = bytesPerSecond / 1024;
                transferRateLabel.setText(nf.format(KperSecond) + " K/sec");
            } else {
                transferRateLabel.setText(nf.format(bytesPerSecond) + "bytes/sec");
            }
            transferRateLabel.repaint();
            lastUpdateTime = currentTime;
        }
    }

    /**
     * User cancellation status.  This returns true when the user has clicked
     * the cancel button of a GUI, or false if the download should proceed.  This
     * is called at various cancellation points throughout the deserialize process
     * so that the download can be cancelled in an orderly fashion.
     */
    public synchronized boolean userCancelled() {
        // If the user has pushed cancel, then dispose of the window
        if (cancelPushed)
            dispose();
        return cancelPushed;
    }

    /**
     * Download finished notice.  This is called when the download is finished
     * and disposes the status window.
     */
    public void finished() {
        dispose();
    }

    /** Allow the program to "press" the cancel button as well as the user. */
    public synchronized void programCancel() {
        cancelPushed = true;
  }
}


