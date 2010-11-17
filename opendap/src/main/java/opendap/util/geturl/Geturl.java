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



package opendap.util.geturl;

import opendap.dap.*;
import opendap.util.geturl.gui.StatusWindow;
import opendap.util.Getopts;
import opendap.util.InvalidSwitch;

/**
 * Java port of the geturl command-line client.
 */
public class Geturl {

    /**
     * Version of the client.
     */
    private static final String VERSION = "0.1 Beta";

    private static void usage() {
        System.err.println("Usage: geturl " +
                "[dDagVvk] [c <expr>] [t <codes>] [m <num>] <url> [<url> ...]");
        System.err.println("[gVvk] [t <codes>] <file> [<file> ...]");
        System.err.println("In the first form of the command, dereference the URL");
        System.err.println("perform the requested operations. In the second, assume");
        System.err.println("the files are OPeNDAP DAP2 data objects (stored in files or read");
        System.err.println("from pipes) and process them as if -D were given.");
        System.err.println("        d: For each URL, get the DAP2 DDS.");
        System.err.println("        x: For each URL, get the Prototype DDX.");
        System.err.println("        a: For each URL, get the DAP2 DAS.");
        System.err.println("        D: For each URL, get the DAP2 Data.");
        System.err.println("        X: For each URL, get the DAP2 Data via a DDX.");
        System.err.println("        g: Show the progress GUI.");
        System.err.println("        v: Verbose.");
        System.err.println("        V: Version.");
        System.err.println("        c: <expr> is a contraint expression. Used with -D.");
        System.err.println("           NB: You can use a `?' for the CE also.");
        System.err.println("        m: Request the same URL <num> times.");
        System.err.println("        z: Don't ask the server to compress data.");
        System.err.println("        p: Dump the DAP2 Data as binary, not text.");
        System.err.println("       Without D, d or a, print the URL.");
    }

    public static void main(String args[]) {
        boolean get_das = false;
        boolean get_dds = false;
        boolean get_ddx = false;
        boolean get_data = false;
        boolean get_dataX = false;
        boolean dump_data = false;
        boolean gui = false;
        boolean cexpr = false;
        boolean verbose = false;
        boolean multi = false;
        boolean accept_deflate = true;
        int times = 1;
        String expr = "";

        try {
            Getopts opts = new Getopts("dxaDXgVvc:m:zp", args);
            if (opts.getSwitch(new Character('d')).set)
                get_dds = true;
            if (opts.getSwitch(new Character('x')).set)
                get_ddx = true;
            if (opts.getSwitch(new Character('a')).set)
                get_das = true;
            if (opts.getSwitch(new Character('D')).set)
                get_data = true;
            if (opts.getSwitch(new Character('X')).set)
                get_dataX = true;
            if (opts.getSwitch(new Character('p')).set)
                dump_data = get_data = true;
            if (opts.getSwitch(new Character('V')).set) {
                System.err.println("geturl version: " + VERSION);
                System.exit(0);
            }
            if (opts.getSwitch(new Character('v')).set)
                verbose = true;
            if (opts.getSwitch(new Character('g')).set)
                gui = true;

            String optVal;
            optVal = opts.getSwitch(new Character('c')).val;
            if (optVal != null) {
                cexpr = true;
                expr = optVal;
            }

            optVal = opts.getSwitch(new Character('m')).val;
            if (optVal != null) {
                multi = true;
                times = Integer.parseInt(optVal);
            }

            if (opts.getSwitch(new Character('z')).set)
                accept_deflate = false;

            // If after processing all the command line options there is nothing left
            // (no URL or file) assume that we should read from stdin.
            String argp[] = opts.argList();
            if (argp.length == 0) {
                if (verbose)
                    System.err.println("Assuming standard input is a OPeNDAP DAP2 data stream.");

                DConnect url = new DConnect(System.in);
                try {
                    StatusUI ui = null;
                    if (gui)
                        ui = new StatusWindow("stdin");
                    DataDDS dds = url.getData(ui);
                    processData(url, dds, verbose, dump_data, accept_deflate);
                } catch (DAP2Exception e) {
                    System.err.println(e);
                    System.exit(1);
                } catch (Exception e) {
                    System.err.println(e);
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            for (int i = 0; i < argp.length; i++) {
                String nextURL = argp[i];
                if (verbose)
                    System.err.println("Fetching: " + nextURL);
                DConnect url = null;
                try {
                    url = new DConnect(nextURL, accept_deflate);
                    //url.setServerVersion("dods/3.7");
                } catch (java.io.FileNotFoundException e) {
                    System.err.println(nextURL + " is neither a valid URL nor a filename.");
                    System.exit(1);
                }

                if (url.isLocal()) {
                    if (verbose)
                        System.err.println("Assuming that the argument " + nextURL +
                                " is a file\n" +
                                "that contains a OPeNDAP DAP2 data object; decoding.");

                    try {
                        StatusUI ui = null;
                        if (gui)
                            ui = new StatusWindow(nextURL);
                        DataDDS dds = url.getData(ui);
                        processData(url, dds, verbose, dump_data, accept_deflate);
                    } catch (DAP2Exception e) {
                        System.err.println(e);
                        System.exit(1);
                    } catch (Exception e) {
                        System.err.println(e);
                        e.printStackTrace();
                        System.exit(1);
                    }
                }

                if (get_das) {
                    for (int j = 0; j < times; j++) {
                        try {
                            DAS das = url.getDAS();
                            if (verbose) {
                                System.err.println("Server version: " + url.getServerVersion());
                                System.err.println("DAS:");
                            }
                            das.print(System.out);
                        } catch (DAP2Exception e) {
                            System.err.println(e);
                            e.printStackTrace();
                            System.exit(1);
                        } catch (java.io.FileNotFoundException e) {
                            System.err.println(e);
                            System.exit(1);
                        } catch (Exception e) {
                            System.err.println(e);
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                }

                if (get_dds) {
                    for (int j = 0; j < times; j++) {
                        try {
                            DDS dds = url.getDDS(expr);
                            if (verbose) {
                                System.err.println("Server version: " + url.getServerVersion());
                                System.err.println("CE:" + expr + "'");
                                System.err.println("DDS:");
                            }
                            dds.print(System.out);
                        } catch (DAP2Exception e) {
                            System.err.println(e);
                            System.exit(1);
                        } catch (java.io.FileNotFoundException e) {
                            System.err.println(e);
                            System.exit(1);
                        } catch (Exception e) {
                            System.err.println(e);
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                }

                if (get_ddx) {
                    for (int j = 0; j < times; j++) {
                        try {
                            DDS dds = url.getDDX(expr);
                            if (verbose) {
                                System.err.println("Server version: " + url.getServerVersion());
                                System.err.println("CE:'" + expr + "'");
                                System.err.println("DDX:");
                            }
                            dds.printXML(System.out);
                        } catch (DAP2Exception e) {
                            System.err.println(e);
                            System.exit(1);
                        } catch (java.io.FileNotFoundException e) {
                            System.err.println(e);
                            System.exit(1);
                        } catch (Exception e) {
                            System.err.println(e);
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                }

                if (get_data) {
                    if ((cexpr == false) && (nextURL.indexOf('?') == -1)) {
                        System.err.println("Must supply a constraint expression with -D.");
                        continue;
                    }
                    for (int j = 0; j < times; j++) {
                        try {
                            StatusUI ui = null;
                            if (gui)
                                ui = new StatusWindow(nextURL);
                            DataDDS dds = url.getData(expr, ui);
                            //System.out.println("DConnect returned: "+dds);
                            processData(url, dds, verbose, dump_data, accept_deflate);
                        } catch (DAP2Exception e) {
                            System.err.println(e);
                            System.exit(1);
                        } catch (java.io.FileNotFoundException e) {
                            System.err.println(e);
                            System.exit(1);
                        } catch (Exception e) {
                            System.err.println(e);
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                }


                if (get_dataX) {
                    if ((cexpr == false) && (nextURL.indexOf('?') == -1)) {
                        System.err.println("Must supply a constraint expression with -D.");
                        continue;
                    }
                    for (int j = 0; j < times; j++) {
                        try {
                            StatusUI ui = null;
                            if (gui)
                                ui = new StatusWindow(nextURL);
//	      DataDDS dds = url.getDDXData(expr, ui);
                            DataDDS dds = url.getDataDDX(expr);
//	      dds.printXML(System.out);
                            url.getBlobData(dds, ui);
                            processDDXData(url, dds, verbose, dump_data, accept_deflate);
                        } catch (DAP2Exception e) {
                            System.err.println(e);
                            System.exit(1);
                        } catch (java.io.FileNotFoundException e) {
                            System.err.println(e);
                            System.exit(1);
                        } catch (Exception e) {
                            System.err.println(e);
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                }


            }
        } catch (InvalidSwitch e) {
            usage();
            System.exit(1);
        }
        // we must explicitly exit in case we opened a StatusWindow which might
        // otherwise prevent the Java VM from quitting
        System.exit(0);
    }

    private static void processData(DConnect url, DataDDS dds, boolean verbose,
                                    boolean dump_data, boolean compress) {
        if (verbose)
            System.err.println("Server version: " + url.getServerVersion());

        if (dump_data) {
            try {
                dds.externalize(System.out, compress, true);
            } catch (java.io.IOException e) {
                System.err.println(e);
            }
        } else {
            System.out.println("The data:");
            dds.printVal(System.out);
            System.out.println();
        }
    }

    private static void processDDXData(DConnect url, DataDDS dds, boolean verbose,
                                       boolean dump_data, boolean compress) {
        if (verbose)
            System.err.println("Server version: " + url.getServerVersion());

        if (dump_data) {
            try {
                dds.externalize(System.out, compress, true);
            } catch (java.io.IOException e) {
                System.err.println(e);
            }
        } else {
            if (verbose) {
                System.out.println("\nThe DDX:");
                dds.printXML(System.out);
            }
            System.out.println("\nThe data:");

            dds.printVal(System.out);
            System.out.println();
        }
    }
}


