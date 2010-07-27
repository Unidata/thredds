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



package opendap.util.gui;

import javax.swing.*;

/**
 * Dispays a simple warning message box.
 *
 * @author ndp
 * @version $Revision: 15901 $
 */

public class warning_box {


    /**
     * Construct and display a warning dialog box.
     *
     * @param msg This string is used as the message text in the dialog box.
     */
    public warning_box(String msg) {

        JLabel jl = new JLabel(msg);

        JOptionPane.showMessageDialog(null,    // parent frame
                jl,    // Object to Display
                "WARNING!", // Title bar label
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Construct and display a warning dialog box.
     *
     * @param title This String is used in the title bar of the dialog box.
     * @param msg   This string is used as the message text in the dialog box.
     */
    public warning_box(String title, String msg) {

        JLabel jl = new JLabel(msg);

        JOptionPane.showMessageDialog(null,    // parent frame
                jl,    // Object to Display
                title, // Title bar label
                JOptionPane.WARNING_MESSAGE);
    }

}



