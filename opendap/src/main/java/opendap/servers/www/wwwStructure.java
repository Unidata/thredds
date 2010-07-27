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




package opendap.servers.www;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

import opendap.dap.*;

/**
 */
public class wwwStructure extends DStructure implements BrowserForm {


    private static boolean _Debug = false;

    /**
     * Constructs a new <code>wwwStructure</code>.
     */
    public wwwStructure() {
        this(null);
    }

    /**
     * Constructs a new <code>wwwStructure</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public wwwStructure(String n) {
        super(n);
    }

    public void printBrowserForm(PrintWriter pw, DAS das) {

        /*-----------------------------------------------
    // C++ Implementation looks like this....

        os << "<b>Structure " << name() << "</b><br>\n";
        os << "<dl><dd>\n";

        for (Pix p = first_var(); p; next_var(p)) {
        var(p)->print_val(os, "", print_decls);
        wo.write_variable_attributes(var(p), global_das);
        os << "<p><p>\n";
        }
        os << "</dd></dl>\n";

        ------------------------------------------------*/

        wwwOutPut wOut = new wwwOutPut(pw);

        pw.println("<b>Structure " + getName() + "</b><br>");
        pw.println("<dl><dd>");

        Enumeration e = getVariables();
        while (e.hasMoreElements()) {
            BaseType bt = (BaseType) e.nextElement();

            ((BrowserForm) bt).printBrowserForm(pw, das);

            wOut.writeVariableAttributes(bt, das);
            pw.print("<p><p>\n");
        }
        pw.println("</dd></dl>");

    }


}


