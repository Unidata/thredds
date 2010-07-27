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

import java.applet.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Java geturl applet.
 */
public class GeturlApplet extends Applet {
    /**
     * The currently open Geturl window.
     */
    protected GeturlFrame frame;

    /**
     * Open the frame and add the start button to the layout
     */
    public void init() {
        frame = new GeturlFrame(true);
        Button restartButton = new Button("Restart");
        restartButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                frame = new GeturlFrame(true);
            }
        });
        add(restartButton);
    }

    /**
     * Dispose of the frame
     */
    public void dispose() {
        frame.dispose();
    }

    /** Main function to call as an application. */
    public static void main(String args[]) {
        GeturlFrame frame = new GeturlFrame(false);
    }
}


