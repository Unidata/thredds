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


package opendap.dts;

/**
 * Test routine for the SD classes
 *
 * @version $Revision: 22540 $
 * @author ndp
 */

import java.io.*;
import java.util.Enumeration;

//import gnu.getopt.Getopt;
import opendap.servlet.AbstractServlet;
import opendap.util.Getopts;
import opendap.util.InvalidSwitch;
import opendap.dap.BaseType;
import opendap.servers.*;

public class SDTest {


    public static boolean Debug = false;

    public static String DDSFile, ConstraintExpression;


    // Constructor
    public SDTest() {
    }


    //***************************************************************
    // Dump the Server DDS contents to stSystem.out.
    public static void print_SDDS(ServerDDS sdds, boolean constrained) {

        System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
        System.out.println("ServerDDS:");
        Enumeration e = sdds.getVariables();

        while (e.hasMoreElements()) {
            Object o = e.nextElement();
            ServerMethods s = (ServerMethods) o;
            BaseType bt = (BaseType) o;

            System.out.println(bt.getTypeName() + " " + bt.getEncodedName() + ":");
            System.out.println("Constrained DDS:");

            bt.printDecl(System.out, "    ", true, constrained);


            System.out.println("Declaration and Value:");
/*

            if(s.isRead()){

		try {
		    bt.printVal(System.out, "    ",true);
		}
		    catch(NullPointerException except){
		    System.out.println(" Instance not Allocated.");
		}
	    }
	    else {
		System.out.println(" Item not yet initialized.");
	    }

*/


            if (s.isRead()) {
                bt.printVal(System.out, "    ", true);
            } else {
                bt.printDecl(System.out, "    ");
            }


            System.out.print(" isProj: " + s.isProject());
            System.out.print("    isRead: " + s.isRead());
            System.out.println("    isSynth: " + s.isSynthesized());
            if (e.hasMoreElements())
                System.out.println("- - - - - - - - - - - - - - - - - -");
        }
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");

    }
    //***************************************************************


    //***************************************************************
    public static void parse_options(String[] args) {

        Getopts optsg;
        Getopts g = new Getopts("SDTest", args, "f:c:");

/*
        Getopt g = new Getopt("SDTest", args, "f:c:");

        int c;
        String arg;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'f':
                    arg = g.getOptarg();
                    if (Debug)
//			System.out.print("DDS File: " + ((arg != null) ? arg : "null") + "\n");
                    DDSFile = arg;
                    break;
                case 'c':
                    arg = g.getOptarg();
                    if (Debug)
//			System.out.print("Constraint Expression: \"" + ((arg != null) ? arg : "null") + "\"\n");

                    ConstraintExpression = arg;
                    break;

                case '?':
                    break; // getopt() already printed an error

                default:
                    if (Debug)
//			System.out.print("getopt() returned " + c + "\n");
            }
        }
*/

        Getopts opts = null;
        String arg = null;
        try {
            opts = new Getopts("f:c:", args);
            if (opts.getSwitch(new Character('f')).set) {
                arg = opts.getSwitch(new Character('f')).val;
                if (Debug)
//			System.out.print("DDS File: " + ((arg != null) ? arg : "null") + "\n");
                DDSFile = arg;
            }
            if (opts.getSwitch(new Character('c')).set) {
                arg = g.getOptarg();
                if (Debug)
//		    System.out.print("Constraint Expression: \"" + ((arg != null) ? arg : "null") + "\"\n");
                ConstraintExpression = arg;
	    }
        }
        catch (InvalidSwitch e) {
	    System.err.println("Invalid Switch: "+e);
	}
    }
    //***************************************************************


    public static void main(String[] args) throws Exception {

        //SDTest sdt = new SDTest();

        try {

            System.out.println("-------------------------------------------");

            System.out.println("Debugging Display: " + (Debug ? "ON" : "OFF"));
            parse_options(args);

            System.out.println("...........................................");
            ServerDDS myDDS = null;
            File fin = new File(DDSFile);
            try (
                FileInputStream fp_in = new FileInputStream(fin);
                DataInputStream dds_source = new DataInputStream(fp_in);
            ) {

                test_ServerFactory sfactory = new test_ServerFactory();
                myDDS = new ServerDDS("bogus", sfactory);

                if(Debug) System.out.println("Parsing DDS...");
                myDDS.parse(dds_source);
            } // end try with resource

            if (Debug) System.out.println("Printing DDS...");
            myDDS.print(System.out);

//            print_SDDS(myDDS, false);

            if (Debug) System.out.println("Constructing CEEvaluator...");
            CEEvaluator ce = new CEEvaluator(myDDS);

            File fout = new File("a.out");
            try (
                FileOutputStream fp_out = new FileOutputStream(fout);
                DataOutputStream sink = new DataOutputStream(fp_out);
            ) {

                if(Debug) System.out.println("Parsing Constraint Expression: " + ConstraintExpression);
                ce.parseConstraint(ConstraintExpression, null);

                if(Debug) System.out.println("Attempting to send data...");
                ce.send(myDDS.getEncodedName(), sink, null);
            }

//            print_SDDS(myDDS, true);
            myDDS.printConstrained(System.out);

            System.out.println("-------------------------------------------");


        }
        catch (opendap.dap.DAP2Exception e) {
            System.out.println("\n\nERROR of Type: " + e.getClass().getName() + "\n");
            System.out.println("Message:\n" + e.getMessage() + "\n");
            System.out.println("Stack Trace: ");
            e.printStackTrace(System.out);
            System.out.println("\n\n");
        }

        System.exit(0);
    }


}



