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



package opendap.dap.test;


import java.io.*;

import opendap.dap.*;
//import opendap.dap.XMLparser.DDSXMLParser;
import opendap.dap.Server.*;


/**
 * @author ndp
 * @version $Revision: 15901 $
 */
public class ddxTest {

    /**
     * Constructs a new <code>XmlTest</code>.
     */
    public ddxTest() {
        super();
    }


    public static void main(String args[]) throws Exception {
        // Parse each file provided on the
        // command line.


        String dasName = args[0];
        String ddsName = args[1];


        try {
            FileInputStream ddsFIS = new FileInputStream(ddsName);
            System.out.println("Parsing DDS File: '" + ddsName + "'");
            DefaultFactory btf = new DefaultFactory();
            //test_ServerFactory btf = new test_ServerFactory();
            ServerDDS dds = new ServerDDS(btf);
            dds.parse(ddsFIS);
            System.out.println("\n\n\n\nTHE DDS:\n");
            dds.print(System.out);

            FileInputStream dasFIS = new FileInputStream(dasName);
            System.out.println("Parsing DAS File: '" + dasName + "'");
            DAS das = new DAS();
            das.parse(dasFIS);
            System.out.println("\n\nThe DAS:\n");
            das.print(System.out);

            dds.ingestDAS(das);
            System.out.println("\n\nThe DDX:\n");
            dds.printXML(System.out);

        } catch (DDSException de) {
            System.out.println("Exception thrown to top level, exiting...");
            System.out.println("MESSAGE: \n" + de.getMessage());
        }


    }


}


