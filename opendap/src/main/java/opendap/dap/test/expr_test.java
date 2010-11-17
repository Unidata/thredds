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


package opendap.dap.test;

import java.io.*;

import opendap.servers.test.test_ServerFactory;
import opendap.dap.*;
import opendap.dap.Server.*;
import opendap.dap.parser.*;

//import gnu.getopt.Getopt;
import opendap.util.Getopts;

// import opendap.util.InvalidSwitch;

/**
 * Test the constraint evaluation scanner/parser. Unlike the C++
 * implementation of OPeNDAP DAP2, the Java implementations use a scanner and
 * parser built from a single source file. This source file defines a LL(n)
 * grammar for the constraint expressions (where the C++ implementation
 * defined a LALR grammar).
 * <p/>
 * The CE tester must be envoked with one of the following options:
 * -s to test the scanner
 * -p to test the parser
 *
 * @author jhrg
 */
public class expr_test {
    private static final String prompt = "expr-test: ";
    //private static final String version = "version 0.1";

    private static void usage() {
        System.err.println("usage: expr_test" + " [-s <file>] [-p <file> -e <ce>]");
        System.err.println(" s: Test the scanner.");
        System.err.println(" p: Test the parser.");
        System.err.println(" e: CE.");
    }

    public static void main(String argv[]) {
        boolean parser_test = false;
        boolean scanner_test = false;
        boolean expr_test = false;
        String file = "";
        String ce = "";
        
//        Getopt g = new Getopt("expr_test", argv, "s:p:e:");
        Getopts g = new Getopts("expr_test", argv, "s:p:e:");
        //
        int c;
        while ((c = g.getopt()) != -1) {
            switch(c) {
            case 's':
                scanner_test = true;
                file = g.getOptarg();
                System.out.print("You picked " + (char)c +
                                 " with an argument of " +
                                 ((file != null) ? file : "null") + "\n");
                break;
                //
            case 'p':
                parser_test = true;
                file = g.getOptarg();
                System.out.print("You picked " + (char)c +
                                 " with an argument of " +
                                 ((file != null) ? file : "null") + "\n");
                break;
                //
            case 'e':
                expr_test = true;
                ce = g.getOptarg();
                System.out.print("You picked " + (char)c +
                                 " with an argument of " +
                                 ((ce != null) ? ce : "null") + "\n");
                break;
                //
            case '?':
                break; // getopt() already printed an error
                //
            default:
                System.out.print("getopt() returned " + c + "\n");
            }
        }

        if (parser_test && expr_test || scanner_test && !expr_test) {
            if (scanner_test) {
                System.out.println("File: " + file);
                test_scanner();
            }

            if (parser_test) {
                System.out.println("File: " + file + ", ce: " + ce);
                test_parser(file, ce);
            }
        }
        else {
            usage();
            System.exit(1);
        }
    }


    private static void test_scanner() {
        Token tok;
        ExprParserTokenManager token_source;
        //SimpleCharStream jj_input_stream;
        SimpleCharStream jj_input_stream;

        //jj_input_stream = new SimpleCharStream(System.in, 1, 1);
        jj_input_stream = new SimpleCharStream(System.in, 1, 1);
        token_source = new ExprParserTokenManager(jj_input_stream);

        System.out.print(prompt);
        System.out.flush();
        while ((tok = token_source.getNextToken()).kind
                != ExprParserConstants.EOF) {
            switch (tok.kind) {
                case ExprParserConstants.EQUAL:
                    System.out.println("Equal");
                    break;
                case ExprParserConstants.NOT_EQUAL:
                    System.out.println("Not equal");
                    break;
                case ExprParserConstants.GREATER:
                    System.out.println("Greater than");
                    break;
                case ExprParserConstants.GREATER_EQL:
                    System.out.println("Greater than or equal");
                    break;
                case ExprParserConstants.LESS:
                    System.out.println("Less than");
                    break;
                case ExprParserConstants.LESS_EQL:
                    System.out.println("Less than or equal");
                    break;
                case ExprParserConstants.REGEXP:
                    System.out.println("Regular expression");
                    break;

                case ExprParserConstants.LBRACKET:
                    System.out.println("Left Bracket");
                    break;
                case ExprParserConstants.RBRACKET:
                    System.out.println("Right Bracket");
                    break;
                case ExprParserConstants.COLON:
                    System.out.println("Colon");
                    break;
                case ExprParserConstants.COMMA:
                    System.out.println("Comma");
                    break;
                case ExprParserConstants.AMPERSAND:
                    System.out.println("Ampersand");
                    break;
                case ExprParserConstants.LPAREN:
                    System.out.println("Left Parenthesis");
                    break;
                case ExprParserConstants.RPAREN:
                    System.out.println("Right Parenthesis");
                    break;
                case ExprParserConstants.LBRACE:
                    System.out.println("Left Brace");
                    break;
                case ExprParserConstants.RBRACE:
                    System.out.println("Right Brace");
                    break;

                case ExprParserConstants.WORD:
                    System.out.println("WORD: " + tok.image);
                    break;
                default:
                    System.out.println("Error: Unrecognized input");
            }
            // print prompt after output
            System.out.print(prompt);
            System.out.flush();
        }
    }

    private static void test_parser(String file, String ce) {
        try {
            // Note, this does not work exactly as the C++ version. At present,
            // This code will only parse and record projections. Since it
            // does not read data into the DDS, the selection part of a CE
            // cannot be tested.

            test_ServerFactory factory = new test_ServerFactory();
            ServerDDS dds = new ServerDDS("test", factory);

            InputStream in = new FileInputStream(file);
            dds.parse(in);
            dds.print(System.out);
            
            CEEvaluator ceEval = new CEEvaluator(dds);
            ceEval.parseConstraint(ce);

            dds.printConstrained(System.out);
        } catch (FileNotFoundException fe) {
            System.out.println(fe.getMessage());
            System.out.println("Status from parser: 0");
        } catch (ParseException pe) {
            System.out.println(pe.getMessage());
            System.out.println("Status from parser: 0");
        } catch (DAP2Exception de) {
            System.out.println(de.getMessage());
            System.out.println("Status from parser: 0");
        }
    }
}


