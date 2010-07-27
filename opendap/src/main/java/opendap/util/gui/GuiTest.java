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

import opendap.util.gui.msg_box;
import opendap.util.gui.warning_box;
import opendap.util.gui.error_box;

/**
 * Test routine for the opendap/util/gui classes
 *
 * @author ndp
 * @version $Revision: 15901 $
 */

public class GuiTest {


    // Constructor
    public GuiTest() {
    }


    public static void main(String[] args) {

        msg_box mbox = new msg_box("Wow! A Message Box!");
        msg_box mbox1 = new msg_box("My Title1",
                "Wow! A Message Box! Maybe I should test a really long string in it to see if it automatically resizes for the text contained within it?\nOk!\nLets do That!");

        warning_box wbox = new warning_box("Wow! A Warning Message Box!");
        warning_box wbox1 = new warning_box("My Title2",
                "Wow! A Warning Message Box!");

        error_box ebox = new error_box("Wow! A Error Message Box!");
        error_box ebox1 = new error_box("My Title3",
                "Wow! A Error Message Box!");

        System.exit(0);
    }


}



