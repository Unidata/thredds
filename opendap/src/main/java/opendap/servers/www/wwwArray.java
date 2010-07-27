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

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

import opendap.dap.*;
import opendap.util.*;

/**
 */
public class wwwArray extends DArray implements BrowserForm {

    private static boolean _Debug = false;

    /**
     * Constructs a new <code>asciiArray</code>.
     */
    public wwwArray() {
        this(null);
    }

    /**
     * Constructs a new <code>asciiArray</code> with name <code>n</code>.
     *
     * @param n the name of the variable.
     */
    public wwwArray(String n) {
        super(n);
    }

    public void printBrowserForm(PrintWriter pw, DAS das) {

        /*--------------------------------------------------------
    // C++ implementation looks like this...

        os << "<script type=\"text/javascript\">\n"
           << "<!--\n"
           << name_for_js_code(name())
           << " = new dods_var(\""
           << name()
           << "\", \""
           << name_for_js_code(name())
           << "\", 1);\n"
           << "DODS_URL.add_dods_var("
           << name_for_js_code(name())
           << ");\n"
           << "// -->\n"
           << "</script>\n";

        os << "<b>"
           << "<input type=\"checkbox\" name=\"get_"
           << name_for_js_code(name())
           << "\"\n"
           << "onclick=\""
           << name_for_js_code(name())
           << ".handle_projection_change(get_"
           << name_for_js_code(name())
           << ")\">\n"
           << "<font size=\"+1\">"
           << name()
           << "</font>"
           << ": "
           << fancy_typename(this)
           << "</b><br>\n\n";

        Pix p = first_dim();
        for (int i = 0; p; ++i, next_dim(p)) {
        int size = dimension_size(p, true);
        string n = dimension_name(p);
        if (n != "")
            os << n << ":";
        os << "<input type=\"text\" name=\""
           << name_for_js_code(name())
           << "_"
           << i
           << "\" size=8 onfocus=\"describe_index()\""
           << " onChange=\"DODS_URL.update_url()\">\n";

        os << "<script type=\"text/javascript\">\n"
           << "<!--\n"
           << name_for_js_code(name())
           << ".add_dim("
           << size
           << ");\n"
           << "// -->\n"
           << "</script>\n";
        }

        os << "<br>\n\n";

        --------------------------------------------------------*/


        pw.print(
                "<script type=\"text/javascript\">\n"
                        + "<!--\n"
                        + wwwOutPut.nameForJsCode(getName())
                        + " = new dods_var(\""
                        + getName()
                        + "\", \""
                        + wwwOutPut.nameForJsCode(getName())
                        + "\", 1);\n"
                        + "DODS_URL.add_dods_var("
                        + wwwOutPut.nameForJsCode(getName())
                        + ");\n"
                        + "// -->\n"
                        + "</script>\n"
        );

        pw.print(
                "<b>"
                        + "<input type=\"checkbox\" name=\"get_"
                        + wwwOutPut.nameForJsCode(getName())
                        + "\"\n"
                        + "onclick=\""
                        + wwwOutPut.nameForJsCode(getName())
                        + ".handle_projection_change(get_"
                        + wwwOutPut.nameForJsCode(getName())
                        + ")\">\n"
                        + "<font size=\"+1\">"
                        + getName()
                        + "</font>"
                        + ": "
                        + dasTools.fancyTypeName(this)
                        + "</b><br>\n\n"
        );

        int dims = numDimensions();
        int dimSize;
        int i = 0;
        String dimName;
        for (Enumeration e = getDimensions(); e.hasMoreElements();) {
            DArrayDimension d = (DArrayDimension) e.nextElement();
            dimSize = d.getSize();
            dimName = d.getName();

            if (dimName != null)
                pw.print(dimName + ":");

            pw.print(
                    "<input type=\"text\" name=\""
                            + wwwOutPut.nameForJsCode(getName())
                            + "_"
                            + i
                            + "\" size=8 onfocus=\"describe_index()\""
                            + " onChange=\"DODS_URL.update_url()\">\n"
            );

            pw.print(
                    "<script type=\"text/javascript\">\n"
                            + "<!--\n"
                            + wwwOutPut.nameForJsCode(getName())
                            + ".add_dim("
                            + dimSize
                            + ");\n"
                            + "// -->\n"
                            + "</script>\n"
            );

            i++;
        }


        pw.println("<br>\n");
    }


}


