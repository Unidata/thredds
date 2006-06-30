/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////

package dods.dap.test;
import java.util.Enumeration;
import dods.dap.*;
import dods.dap.parser.*;
import dods.util.Getopts;
import dods.util.InvalidSwitch;

public class dds_test {
  private static final String prompt = "dds-test: ";
  private static final String version = "version 0.1";

  private static void usage() {
    System.err.println("usage: dds-test" +
		       " [s] [pd] [c]");
    System.err.println(" s: Test the scanner.");
    System.err.println(" p: Test the parser; reads from stdin and prints the");
    System.err.println("    internal structure to stdout.");
    System.err.println(" d: Turn on parser debugging.[Broken 11/9/99 jhrg]");
    System.err.println(" c: Test the code for manipulating DDS objects.");
    System.err.println("    Reads from stdin, parses and writes the modified DDS");
    System.err.println("    to stdout.");
  }
  
  public static void main(String args[]) {
    boolean parser_test = false;
    boolean scanner_test = false;
    boolean class_test = false;

    try {
      Getopts opts = new Getopts("spdc", args);
      if(opts.getSwitch(new Character('p')).set)
	parser_test = true;
      if(opts.getSwitch(new Character('s')).set)
	scanner_test = true;
      if(opts.getSwitch(new Character('c')).set)
	class_test = true;
    }
    catch (InvalidSwitch e) {
      usage();
      System.exit(1);
    }

    if (!parser_test && !scanner_test && !class_test) {
      usage();
      System.exit(1);
    }

    if (scanner_test)
      test_scanner();

    if (parser_test)
      test_parser();

    if (class_test)
      test_class();
  }

  private static void test_scanner() {
    Token tok;
    DDSParserTokenManager token_source;
    SimpleCharStream jj_input_stream;

    jj_input_stream = new SimpleCharStream(System.in, 1, 1);
    token_source = new DDSParserTokenManager(jj_input_stream);

    System.out.print(prompt);  System.out.flush();
    while ((tok = token_source.getNextToken()).kind != DDSParserConstants.EOF) {
      switch (tok.kind) {
      case DDSParserConstants.DATASET:
	System.out.println("DATASET");
	break;
      case DDSParserConstants.LIST:
	System.out.println("LIST");
	break;
      case DDSParserConstants.SEQUENCE:
	System.out.println("SEQUENCE");
	break;
      case DDSParserConstants.STRUCTURE:
	System.out.println("STRUCTURE");
	break;
      case DDSParserConstants.GRID:
	System.out.println("GRID");
	break;
      case DDSParserConstants.BYTE:
	System.out.println("BYTE");
	break;
      case DDSParserConstants.INT32:
	System.out.println("INT32");
	break;
      case DDSParserConstants.FLOAT64:
	System.out.println("FLOAT64");
	break;
      case DDSParserConstants.STRING:
	System.out.println("STRING");
	break;
      case DDSParserConstants.URL:
	System.out.println("Url");
	break;
      case DDSParserConstants.WORD:
	System.out.println("WORD: " + tok.image);
	break;
      default:
	System.out.println("Found: " + tok.image);
      }
      System.out.print(prompt); 
      System.out.flush();  // print prompt after output
    }
  }

  private static void test_parser() {
    DDS table = new DDS();
    try {
      table.parse(System.in);
      // I removed this line because the C++ dds-test program does not output
      // this information and we need the two drivers to write exactly the
      // same text out if both the C++ and Java code are to use the same test
      // suites (which they should). 5/22/2002 jhrg
      //      System.out.println("Status from parser: 1");  // success
    }
    catch (ParseException e) {
      System.err.println(e.getMessage() + "\n");
      System.out.println("Status from parser: 0");
    }
    catch (DDSException e) {
      System.err.println(e.getMessage() + "\n");
      System.out.println("Status from parser: 0");
    }
    try {
      table.checkSemantics();
      // NOTE: misspellings are for bug-compatibility with C++ dds-testsuite
      System.out.println("DDS past semantic check");
    }
    catch (BadSemanticsException e) {
      System.out.println("DDS failed semantic check");
    }
    
    try {
      table.checkSemantics(true);
      // NOTE: misspellings are for bug-compatibility with C++ dds-testsuite
      System.out.println("DDS past full semantic check");
    }
    catch (BadSemanticsException e) {
      System.out.println("DDS failed full semantic check");
    }

    table.print(System.out);
  }

  private static void test_class() {
    DDS table = new DDS();
    try {
      table.parse(System.in);
      System.out.println("Status from parser: 1");  // success
    }
    catch (ParseException e) {
      System.err.println(e.getMessage() + "\n");
      System.out.println("Status from parser: 0");
    }
    catch (DDSException e) {
      System.err.println(e.getMessage() + "\n");
      System.out.println("Status from parser: 0");
    }
    try {
      table.checkSemantics();
      // NOTE: misspellings are for bug-compatibility with C++ dds-testsuite
      System.out.println("DDS past semantic check");
    }
    catch (BadSemanticsException e) {
      // NOTE: misspellings are for bug-compatibility with C++ dds-testsuite
      System.out.println("DDS filed semantic check");
    }
    
    try {
      table.checkSemantics(true);
      // NOTE: misspellings are for bug-compatibility with C++ dds-testsuite
      System.out.println("DDS past full semantic check");
    }
    catch (BadSemanticsException e) {
      // NOTE: misspellings are for bug-compatibility with C++ dds-testsuite
      System.out.println("DDS filed full semantic check");
    }

    table.print(System.out);

    DDS table2 = (DDS)table.clone();  // test Cloneable interface
    table2.print(System.out);

    System.out.println("Dataset name: " + table.getName());

    String name = "goofy";
    BaseTypeFactory factory = new DefaultFactory();
    table.addVariable(factory.newDInt32(name));

    table.print(System.out);

    BaseType bt;
    try {
      bt = table.getVariable(name);
      bt.printDecl(System.out, ""); // print out goofy w/ no spaces
    } catch (NoSuchVariableException e) {
      System.out.println(e.getMessage());
    }

    table.delVariable(name);

    table.print(System.out);

    table.addVariable(factory.newDInt32(name));

    table.print(System.out);

    try {
      bt = table.getVariable(name);
      bt.printDecl(System.out, ""); // print out goofy w/ no spaces
    } catch (NoSuchVariableException e) {
      System.out.println(e.getMessage());
    }

    table.delVariable(name);

    table.print(System.out);

    for(Enumeration e = table.getVariables(); e.hasMoreElements(); ) {
      bt = (BaseType)e.nextElement();
      bt.printDecl(System.out, "");  // print them all w/ semicolons
    }
  }
}
