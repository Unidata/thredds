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


package opendap.dap.parsers;


import java.io.*;

//import gnu.getopt.Getopt;
import opendap.util.Getopts;
import opendap.util.InvalidSwitch;
import opendap.dap.*;
import opendap.Server.*;


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
