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



package opendap.dap.XMLparser;


import java.io.*;

//import gnu.getopt.Getopt;
import opendap.util.*;
import opendap.dap.*;
import opendap.dap.Server.*;


/**
 * @author ndp
 * @version $Revision: 22540 $
 * @opendap.ddx.experimental
 */
public class XmlTest {

    /**
     * Constructs a new <code>XmlTest</code>.
     */
    public XmlTest() {
        super();
    }


    public static void main(String args[]) throws Exception {


        boolean validation = false;
        int acount = 0;

/*
        int c;
        Getopt g = new Getopt("XmlTest", args, "v::");
        while ((c = g.getopt()) != -1) {
            acount++;
            switch (c) {
                case 'v':
                    validation = true;
                    break;

                case '?':
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("Usage: XmlTest [-v] ddxfile1 [ddxfile2] [ddxfile3] ...");
                    System.out.println("Uses the DDSXMLParser to parse OPeNDAP DDX files.");
                    System.out.println("Successful parsing leads to the display of the DDS, DAS");
                    System.out.println("and the original DDX files onto <stdout>.");
                    System.out.println("");
                    System.out.println("Options:");
                    System.out.println("    v   This option causes the XML parser to validate");
                    System.out.println("        the DDX document against the OPeNDAP Schema. The");
                    System.out.println("        Schema used to validate is the one referenced in ");
                    System.out.println("        the DDX file. If the instance is unreachable then");
                    System.out.println("        the parse will fail. Without this option the parser");
                    System.out.println("        WILL NOT schema validate the document.");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.exit(0);
                    break; // getopt() already printed an error

                default:
                    System.out.print("getopt() returned " + c + "\n");
            }
        }
*/
        Getopts opts = null;
        try {
            opts = new Getopts("v", args);
            if (opts.getSwitch(new Character('v')).set) {
                validation = true;
            }
        }
        catch (InvalidSwitch e) {
            System.out.println(" ");
            System.out.println(" ");
            System.out.println("Usage: XmlTest [-v] ddxfile1 [ddxfile2] [ddxfile3] ...");
            System.out.println("Uses the DDSXMLParser to parse OPeNDAP DDX files.");
            System.out.println("Successful parsing leads to the display of the DDS, DAS");
            System.out.println("and the original DDX files onto <stdout>.");
            System.out.println("");
            System.out.println("Options:");
            System.out.println("    v   This option causes the XML parser to validate");
            System.out.println("        the DDX document against the OPeNDAP Schema. The");
            System.out.println("        Schema used to validate is the one referenced in ");
            System.out.println("        the DDX file. If the instance is unreachable then");
            System.out.println("        the parse will fail. Without this option the parser");
            System.out.println("        WILL NOT schema validate the document.");
            System.out.println(" ");
            System.out.println(" ");
            System.exit(0);
        }

        if (validation) {
            System.out.println("Parser will validate documents using Schema reference.");
        } else {
            System.out.println("Parser will NOT validate documents.");
        }

        // Parse each file provided on the
        // command line.
        args = opts.argList();
        for (int i = acount; i < args.length; i++) {
            System.out.println("Parsing File: '" + args[i] + "'");
            FileInputStream fis = new FileInputStream(args[i]);
            DefaultFactory btf = new DefaultFactory();
            ServerDDS dds = new ServerDDS(btf);

//	    try {
            dds.parseXML(fis, validation);
//                dds.parse(fis);
            System.out.println("\n\n\n\nTHE DDS:\n");
            dds.print(System.out);
            System.out.println("\n\nThe DAS, from DDS.printDAS():\n");
            dds.printDAS(System.out);
            System.out.println("\n\nThe DAS object, from DDS.getDAS():\n");
            DAS thisDAS = dds.getDAS();
            thisDAS.print(System.out);
            dds.printXML(System.out);

        }
    }


}
