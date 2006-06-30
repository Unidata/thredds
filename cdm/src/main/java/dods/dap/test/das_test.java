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

public class das_test {
  private static final String prompt = "das-test: ";
  private static final String version = "version 0.1";

  private static void usage() {
    System.err.println("usage: das-test" +
		       " [-v] [-s] [-d] [-c] [-p] {< in-file > out-file}");
    System.err.println(" s: Test the DAS scanner.");
    System.err.println(" p: Scan and parse from <in-file>; print to <out-file>.");
    System.err.println(" c: Test building the DAS from code.");
    System.err.println(" v: Print the version of das-test and exit.");
    System.err.println(" d: Print parser debugging information.");
  }

  public static void main(String args[]) {
    boolean parser_test = false;
    boolean scanner_test = false;
    boolean code_test = false;

    try {
      Getopts opts = new Getopts("scpvd", args);
      if(opts.getSwitch(new Character('p')).set)
	parser_test = true;
      if(opts.getSwitch(new Character('s')).set)
	scanner_test = true;
      if(opts.getSwitch(new Character('c')).set)
	code_test = true;
      if(opts.getSwitch(new Character('v')).set) {
	System.err.println("das-test: " + version);
	System.exit(0);
      }
    }
    catch (InvalidSwitch e) {
      usage();
      System.exit(1);
    }

    DAS das = new DAS();

    if (!parser_test && !scanner_test && !code_test) {
      usage();
      System.exit(1);
    }

    if (parser_test)
      parser_driver(das);

    if (scanner_test)
      test_scanner();

    if (code_test)
      plain_driver(das);
  }

  private static void test_scanner() {
    Token tok;
    DASParserTokenManager token_source;
    SimpleCharStream jj_input_stream;

    jj_input_stream = new SimpleCharStream(System.in, 1, 1);
    token_source = new DASParserTokenManager(jj_input_stream);

    System.out.print(prompt);  System.out.flush();
    while ((tok = token_source.getNextToken()).kind != DASParserConstants.EOF) {
      switch (tok.kind) {
      case DASParserConstants.ATTR:
	System.out.println("ATTR");
	break;
      case DASParserConstants.ALIAS:
	System.out.println("ALIAS");
	break;
      case DASParserConstants.BYTE:
	System.out.println("BYTE");
	break;
      case DASParserConstants.INT32:
	System.out.println("INT32");
	break;
      case DASParserConstants.FLOAT64:
	System.out.println("FLOAT64");
	break;
      case DASParserConstants.STRING:
	System.out.println("STRING");
	break;
      case DASParserConstants.URL:
	System.out.println("URL");
	break;

	// Both WORD and STR --> WORD to match what the C++ expect. 5/23/2002
	// jhrg
      case DASParserConstants.WORD:
      case DASParserConstants.STR:
	System.out.println("WORD=" + tok.image);
	break;


      default:
	System.out.println("Found: " + tok.image);
      }
      System.out.print(prompt);
      System.out.flush();  // print prompt after output
    }
  }

  private static void parser_driver(DAS das) {
    try {
      das.parse(System.in);
      das.print(System.out);
    } catch(ParseException e) {
      System.err.println(e.getMessage());
      System.err.println("parse() returned: 0");
    } catch(DASException e) {
      System.err.println(e.getMessage());
      System.err.println("parse() returned: 0");
    } catch(TokenMgrError e) {
      System.err.println(e.getMessage()); System.err.println();
      System.exit(2);  // in the C++ version, lexer errors are fatal
    }
  }

  // Given a DAS, add some stuff to it
  private static void plain_driver(DAS das) {
    AttributeTable atp;
    AttributeTable dummy;

    String name = "test";
    atp = new AttributeTable(name);
    load_attr_table(atp);
    dummy = das.getAttributeTable(name);
    das.addAttributeTable(name, atp);

    name = "test2";
    atp = new AttributeTable(name);
    load_attr_table(atp);
    das.addAttributeTable(name, atp);

    das.print(System.out);
  }

  // stuff an AttributeTable full of values. Also, print it out.
  private static void load_attr_table(AttributeTable at) {
   try {
    at.appendAttribute("month", Attribute.STRING, "Feb");
    at.appendAttribute("month", Attribute.STRING, "Feb");

    at.appendAttribute("month_a", Attribute.STRING, "Jan");
    at.appendAttribute("month_a", Attribute.STRING, "Feb");
    at.appendAttribute("month_a", Attribute.STRING, "Mar");

    at.appendAttribute("Date", Attribute.INT32, "12345");
    at.appendAttribute("day", Attribute.INT32, "01");
    at.appendAttribute("Time", Attribute.FLOAT64, "3.1415");

    System.out.println("Using the Pix:");
    Enumeration e = at.getNames();
    while(e.hasMoreElements()) {
      String name = (String)e.nextElement();
      Attribute a = at.getAttribute(name);
      System.out.print(name + " " + a.getTypeString() + " ");
      Enumeration es = a.getValues();
      while(es.hasMoreElements()) {
	System.out.print((String)es.nextElement() + " ");
      }
      System.out.println();
    }

    String name = "month";
    Attribute a = at.getAttribute(name);
    System.out.println("Using String: " + a.getTypeString() + " " +
		       a.getValueAt(0) + " " + a.getValueAt(1));
    System.out.println("Using char *: " + a.getTypeString() + " " +
		       a.getValueAt(0) + " " + a.getValueAt(1));

    at.delAttribute("month");

    System.out.println("After deletion:");
    e = at.getNames();
    while(e.hasMoreElements()) {
      name = (String)e.nextElement();
      a = at.getAttribute(name);
      System.out.print(name + " " + a.getTypeString() + " ");
      Enumeration es = a.getValues();
      while(es.hasMoreElements()) {
	System.out.print((String)es.nextElement() + " ");
      }
      System.out.println();
    }

    at.print(System.out);

    System.out.println("After print:");
    e = at.getNames();
    while(e.hasMoreElements()) {
      name = (String)e.nextElement();
      a = at.getAttribute(name);
      System.out.print(name + " " + a.getTypeString() + " ");
      Enumeration es = a.getValues();
      while(es.hasMoreElements()) {
	System.out.print((String)es.nextElement() + " ");
      }
      System.out.println();
    }
   }
   catch (AttributeExistsException e) {
     System.err.println(e.getMessage());
   }
   catch (AttributeBadValueException e) {
     System.err.println(e.getMessage());
   }
  }
}

// $Log: das_test.java,v $
// Revision 1.1  2005/12/16 22:07:04  caron
// dods src under our CVS
//
// Revision 1.3.2.2  2004/08/26 21:47:49  ndp
// *** empty log message ***
//
// Revision 1.3.2.1  2004/07/25 23:47:38  ndp
// Refactored dods.servlet.ReqState
//
// Revision 1.3  2002/05/30 23:34:00  jimg
// I modified this so that it will work with the new DAS parser. That meant
// removing a bunch of token symbols that are no longer used. I also changed
// the stuff written to stdout so that the driver will work with the C++
// testsuite. There may be more work here.
//
