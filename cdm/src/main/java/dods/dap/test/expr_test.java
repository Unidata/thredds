/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1999, University of Rhode Island
// ALL RIGHTS RESERVED.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: James Gallagher <jgallagher@gso.uri.edu>
//
/////////////////////////////////////////////////////////////////////////////

package dods.dap.test;
//import java.util.Enumeration;
import dods.dap.*;
import dods.dap.Server.*;
//import dods.servers.test.test_ServerFactory;
import dods.dap.parser.*;
import dods.util.Getopts;
import dods.util.InvalidSwitch;

/** Test the constraint evaluation scanner/parser. Unlike the C++
    implementation of DODS, the Java implementations use a scanner and
    parser built from a single source file. This source file defines a LL(n)
    grammar for the constraint expressions (where the C++ implementation
    defined a LALR grammar).

    The CE tester must be envoked with one of the following options:
        -s to test the scanner
	-p to test the parser

    @author jhrg */
public class expr_test {
    private static final String prompt = "expr-test: ";
//    private static final String version = "version 0.1";

    private static void usage() {
	System.err.println("usage: expr-test" + " [s] [p]");
	System.err.println(" s: Test the scanner.");
	System.err.println(" p: Test the parser; reads from stdin and prints the");
	System.err.println("    internal structure to stdout.");
    }

    public static void main(String args[]) {
	boolean parser_test = false;
	boolean scanner_test = false;

	try {
	    Getopts opts = new Getopts("sp", args);
	    if(opts.getSwitch(new Character('p')).set)
		parser_test = true;
	    if(opts.getSwitch(new Character('s')).set)
		scanner_test = true;
	}
	catch (InvalidSwitch e) {
	    usage();
	    System.exit(1);
	}

	if (!parser_test && !scanner_test) {
	    usage();
	    System.exit(1);
	}

	if (scanner_test)
	    test_scanner();

	if (parser_test)
	    test_parser();
    }


    private static void test_scanner() {
	Token tok;
	ExprParserTokenManager token_source;
	//SimpleCharStream jj_input_stream;
	SimpleCharStream jj_input_stream;

	//jj_input_stream = new SimpleCharStream(System.in, 1, 1);
	jj_input_stream = new SimpleCharStream(System.in, 1, 1);
	token_source = new ExprParserTokenManager(jj_input_stream);

	System.out.print(prompt);  System.out.flush();
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
	      case ExprParserConstants.ASTERISK:
		System.out.println("Asterisk");
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

	      case ExprParserConstants.ID:
		System.out.println("ID: " + tok.image);
		break;

//	      case ExprParserConstants.FIELD:
//		System.out.println("FIELD: " + tok.image);
//		break;

	      case ExprParserConstants.INT:
		System.out.println("INT: " + tok.image);
		break;

	      case ExprParserConstants.FLOAT:
		System.out.println("FLOAT: " + tok.image);
		break;

	      case ExprParserConstants.STR:
		System.out.println("STR: " + tok.image);
		break;

	      case ExprParserConstants.UNTERM_QUOTE:
		System.out.println("UNTERN_QUOTE: " + tok.image);
		break;

	      default:
		System.out.println("Error: Unrecognized input");
	    }
	    // print prompt after output
	    System.out.print(prompt); System.out.flush();
	}
    }

    private static void test_parser() {
	ExprParser expr = new ExprParser(System.in);
	try {
	    // *** This will compile, but it will not run until fixed so that
	    // the DDS is filled with variables *and* when the test cases and
	    // ServerFactory stuff is sorted out. 9/8/99 jhrg

        DefaultFactory factory = new DefaultFactory();
        //test_ServerFactory factory = new test_ServerFactory();

            ServerDDS dds = new ServerDDS("ThisIsATestDDS",factory);

	    CEEvaluator ceEval = new CEEvaluator(dds);
	    expr.constraint_expression(ceEval, factory, new ClauseFactory());
	    System.out.println("Status from parser: 1");  // success
	}
	catch (ParseException pe) {
	    System.out.println(pe.getMessage());
	    System.out.println("Status from parser: 0");
	}
	catch (DODSException de) {
	    System.out.println(de.getMessage());
	    System.out.println("Status from parser: 0");
	}
    }
}
