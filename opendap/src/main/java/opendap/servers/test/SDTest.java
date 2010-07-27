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


package opendap.servers.test;

/**
 * Test routine for the SD classes
 *
 * @version $Revision: 22540 $
 * @author ndp
 */

import java.io.*;
import java.util.Enumeration;

//import gnu.getopt.Getopt;
import opendap.util.*;
import opendap.dap.BaseType;
import opendap.dap.Server.*;

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

            System.out.println(bt.getTypeName() + " " + bt.getName() + ":");
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
                    if (Debug) System.out.print("DDS File: " +
                            ((arg != null) ? arg : "null") + "\n");
                    DDSFile = arg;
                    break;
                case 'c':
                    arg = g.getOptarg();
                    if (Debug) System.out.print("Constraint Expression: \"" +
                            ((arg != null) ? arg : "null") + "\"\n");

                    ConstraintExpression = arg;
                    break;

                case '?':
                    break; // getopt() already printed an error

                default:
                    if (Debug) System.out.print("getopt() returned " + c + "\n");
            }
        }
*/

        Getopts opts = null;
        String arg = null;
        try {
            opts = new Getopts("f:c:", args);
            if (opts.getSwitch(new Character('f')).set) {
                arg = opts.getSwitch(new Character('f')).val;
                if (Debug) System.out.print("DDS File: " +
                            ((arg != null) ? arg : "null") + "\n");
                DDSFile = arg;
            }
            if (opts.getSwitch(new Character('c')).set) {
                arg = g.getOptarg();
                if (Debug) System.out.print("Constraint Expression: \"" +
                            ((arg != null) ? arg : "null") + "\"\n");
                ConstraintExpression = arg;
	    }
        }
        catch (InvalidSwitch e) {
	    System.err.println("Invalid Switch: "+e);
	}
    }
    //***************************************************************


    public static void main(String[] args) throws Exception {

        SDTest sdt = new SDTest();

        try {

            System.out.println("-------------------------------------------");

            System.out.println("Debugging Display: " + (Debug ? "ON" : "OFF"));
            parse_options(args);

            System.out.println("...........................................");

            File fin = new File(DDSFile);
            FileInputStream fp_in = new FileInputStream(fin);
            DataInputStream dds_source = new DataInputStream(fp_in);

            test_ServerFactory sfactory = new test_ServerFactory();
            ServerDDS myDDS = new ServerDDS("bogus", sfactory);

            if (Debug) System.out.println("Parsing DDS...");
            myDDS.parse(dds_source);

            if (Debug) System.out.println("Printing DDS...");
            myDDS.print(System.out);

            print_SDDS(myDDS, false);

            if (Debug) System.out.println("Constructing CEEvaluator...");
            CEEvaluator ce = new CEEvaluator(myDDS);

            File fout = new File("a.out");
            FileOutputStream fp_out = new FileOutputStream(fout);
            DataOutputStream sink = new DataOutputStream(fp_out);

            if (Debug) System.out.println("Parsing Constraint Expression: " + ConstraintExpression);
            ce.parseConstraint(ConstraintExpression);

            if (Debug) System.out.println("Attempting to send data...");
            ce.send(myDDS.getName(), sink, null);


            print_SDDS(myDDS, true);
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



