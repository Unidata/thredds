// Copyright (C) 1997 by Arieh Markel <arieh@selectjobs.com>.  
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//

package dods.util;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 *  A class for achieving getopts() functionality.
 *<P>
 *  Loosely based on perl5's getopt/Std.pm.
 *<P>
 *  The object is instantiated with a 'flags' String that is the
 *  composed of the set of switches allowed to be passed, and the
 *  'args' String array that has to be parsed opposite the 'flags' 
 *  string.
 *<PRE>
 *	new Getopts("oif:", args)	-o, -i are boolean flags,
 *					-f takes an argument
 *</PRE>
 *	The class processes single-character switches with switch
 *	clustering.
 *<P>
 *  The list of valid switches is accessible through the 'swList()'
 *  method, which returns an Enumeration of the switch names.
 *<P>
 *  A local array including the arguments from the 'args' array that
 *  was passed as an argument but are the actual command line arguments
 *  is generated and is accessible through the 'argList()' method.
 *<P>
 *  Options switch content fields can be accessible through the
 *  'OptSwitch' class.
 *<P>
 *  @author  Arieh Markel  (arieh@selectjobs.com)
	     thanks to Mark Skipper (mcs@dmu.ac.uk) for bug fix
 *  @version 1.1  6/14/98  Updated evaluation following Mark's remarks.
 * 
 *   @see OptSwitch
 *   @see java.util.Enumeration
 * 
 */
 /*  $Id$ */

public class Getopts extends Object {

    // The internal storage of options
    //
    Hashtable switchtab; 
    String    arglist[];

    // getSwitch
    /**  
     *  method to return the OptSwitch object associated with the 
     *  'sw' argument.
     *<P>
     *  @param sw	switch whose class is requested
     *<P>
     */
    public OptSwitch getSwitch(Character sw) {
	return (OptSwitch) switchtab.get(sw);
    }

    /**
     *  getOption
     *
     *  @param sw	Character switch whose option is requested
     */
    public String getOption(Character sw) {
	return getOption( (int) sw.charValue());
    }

    /**
     *  getOption
     *
     *  @param sw	int value switch whose option is requested
     */
    public String getOption(int sw) {
	Character  opt = new Character((char) sw);
	return getOption(opt);
    }

    // swList
    /**
     *  Method to return an Enumeration of the switches
     *	that the Getopts object is able to parse (according
     *	to its initialization).
     *<P>
     *  May be later used to step through the OptSwitch objects.
     */
    public Enumeration swList() {
	return switchtab.keys();
    }

    // argList
    /**
     *  Method to return an array of the actual arguments of the
     *	command line invocation.
     */
    public String[] argList() {
	return arglist;
    }

    // Getopts
    /**
     *  Basic class constructor.  Gets the flags passed, in a
     *	notation similar to the one used by the sh, ksh, bash,
     *	and perl getopts.
     *<P>
     *  String array 'args' is passed and
     *	is parsed according to the flags.
     *<P>
     *  @param flags a string with the valid switch names
     *  @param args array of strings (usually args)
     *<P>
     *  @exception InvalidSwitch	thrown when invalid options are found
     *<P>
     */ 
    public Getopts(String flags, String args[])
    	throws InvalidSwitch
    {
	String throwstring = new String("Invalid Getopts switch(s): ");
	String usage = new String("Usage: Getopts(String flags, String args[])");

	switchtab = new Hashtable(1,1);

	for (int i = 0; i < flags.length(); i++) {
	    boolean found;
	    int  cc = flags.charAt(i);
	    Character c = new Character((char) cc);
	    char alpha[] = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
			     'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 
			     's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

	    //  space characters or punctuation marks are not allowed
	    //  as switch values
	    //
	    if (cc == ' ' || cc == '\t' || cc == '\n' || cc == '\r') {
		throw new InvalidSwitch(throwstring + "Spaces not allowed\n" +
				        usage + "\n");
	    }

	    found = false;
	    for (int j = 0; j < alpha.length; j++) {
		Character  ch = new Character((char) alpha[j]);
		char uc = ch.toUpperCase(ch.charValue());
		if (alpha[j] == cc || uc == cc) {
		    found = true;
		    break;
		}
	    }

	    //  if the character was not found on the set, throw the exception
	    //
	    if (! found && cc != ':' ) {
		throw new InvalidSwitch(throwstring + "Invalid Character " + c +
				    "\n" + usage + "\n");
	    }

	    //  ':' appears when the preceding character accepts a value
	    //
	    if (cc == ':') {
		if (i > 0) {
		    int	 prv = flags.charAt(i-1);
		    
		    if (prv == ':') {
			throw new InvalidSwitch(throwstring + 
					"Can't have consecutive ':'\n" +
				    		usage + "\n");
		    }
		    else {
			Character cp = new Character((char) prv);
			OptSwitch sw = (OptSwitch) switchtab.get(cp);
			sw.SetHasValue(OptSwitch.VAL);
		    }
		}
	    }
	    else {
		OptSwitch sw = new OptSwitch(cc, OptSwitch.BOOL);

		switchtab.put(c, sw);
	    }
	}

	//
	//  Now, step through the arguments in the argument list
	//  and identify the options and values
	//
	int i;
	for (i = 0; i < args.length; i++) {
	    char cc = args[i].charAt(0);	// first character

	    if (cc != '-') {
	    	// end of options
	        break;
	    }
	    // more options, iterate them
	    for (int j = 1; j < args[i].length(); j++) {
		cc = args[i].charAt(j);
		Character fc = new Character(cc);
		OptSwitch cs = (OptSwitch) switchtab.get(fc);
		if (cs == null) {
		    // The supplied switch wasn't recognised.
		    throw new InvalidSwitch(throwstring + "invalid switch " +
			cc + "\n2 Valid switches are: " + flags + "\n" + usage + "\n");
		} else if (!cs.acceptVal()) {
		    // the option is a switch and takes no value
		    cs.SetVal(true);
		} else if (j+1 < args[i].length()) {
		    //  the value may follow immediately after the switch 
		    // (not as a separate token) set value to remainder of string...
		    cs.SetVal(args[i].substring(j+1));
		    // ... and move pointer to end, thus consuming the string
		    j = args[i].length();
		} else if (++i >= args.length) {
		    // there wasn't another token
		     throw new InvalidSwitch(throwstring + 
			"missing value from switch " + cc + 
			"\n1 Valid switches are: " + flags + 
			"\n" + usage + "\n");
		} else if (args[i].charAt(0) == '-') {
		    // there was no value, next token starts with flags
		    throw new InvalidSwitch(
			throwstring + "missing value from switch " + cc + 
			"\n0 Valid switches are: " + flags + "\n" +
			usage + "\n");
		} else {
		    // the next token is the value
		    cs.SetVal(args[i]);
		    // and move j to the end to mark it consumed
		    j = args[i].length();
		}
	    } // end for j
	} // end for i

	//  Now insert the array of arguments into the object
	//
	arglist = new String[args.length - i];
	System.arraycopy(args, i, arglist, 0, args.length-i);
    }

    /**
     *  method for class testing.
     *<P>
     *	Invocation:	
     *<PRE>
     * 		java Getopts "option set" arg0 arg1 ... argn
     *</PRE>
     *
     *  @param args		arguments passed
     *  @exception InvalidSwitch	thrown when invalid options are found
     */
    public static void main(String args[]) 
	throws InvalidSwitch
    {
	int i;
	String args1[] = new String[args.length-1];
	System.arraycopy(args, 1, args1, 0, args.length-1);

	for (i = 0; i < args.length; i++) {
	    System.out.println("args[" + i + "] : " + args[i]);
	}

	try {
	    Getopts opts = new Getopts(args[0], args1);
	    Enumeration names = opts.swList();
	    
	    i = 0;
	    while (names.hasMoreElements()) {
		OptSwitch cs = opts.getSwitch((Character) names.nextElement());
		System.out.println("args[" + i + "] : " + 
				   (char) cs.sw + " " + cs.type + " " +
				   cs.set + " " + cs.val);
		i++;
	    }

	    String argp[] = opts.argList();
	    for (i = 0; i < argp.length; i++) {
		System.out.println("argv[" + i + "] : " + argp[i]);
	    }
	}
	catch (InvalidSwitch e) {
	    System.out.print(e);
	}
    }
}
