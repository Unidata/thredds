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



package opendap.util;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * A class for achieving getopts() functionality.
 * <p/>
 * Loosely based on perl5's getopt/Std.pm.
 * <p/>
 * The object is instantiated with a 'flags' String that is the
 * composed of the set of switches allowed to be passed, and the
 * 'args' String array that has to be parsed opposite the 'flags'
 * string.
 * <PRE>
 * new Getopts("oif:", args)	-o, -i are boolean flags,
 * -f takes an argument
 * </PRE>
 * The class processes single-character switches with switch
 * clustering.
 * <p/>
 * The list of valid switches is accessible through the 'swList()'
 * method, which returns an Enumeration of the switch names.
 * <p/>
 * A local array including the arguments from the 'args' array that
 * was passed as an argument but are the actual command line arguments
 * is generated and is accessible through the 'argList()' method.
 * <p/>
 * Options switch content fields can be accessible through the
 * 'OptSwitch' class.
 * <p/>
 *
 * @author Arieh Markel  (arieh@selectjobs.com)
 *         thanks to Mark Skipper (mcs@dmu.ac.uk) for bug fix
 * @version 1.1  6/14/98  Updated evaluation following Mark's remarks.
 * @version 1.2  4/10/10  Added some code to provide a gnu style
 *                        getopts.
 * @see OptSwitch
 * @see java.util.Enumeration
 */
/*  $Id: Getopts.java 22593 2010-04-27 20:14:59Z dmh $ */

public class Getopts {

    // The internal storage of options
    //
    Hashtable switchtab;
    String arglist[];

    String progname = null;

    // getSwitch

    /**
     * method to return the OptSwitch object associated with the
     * 'sw' argument.
     * <p/>
     *
     * @param sw switch whose class is requested
     *           <p/>
     */
    public OptSwitch getSwitch(Character sw) {
        return (OptSwitch) switchtab.get(sw);
    }

    /**
     * getOption
     *
     * @param sw Character switch whose option is requested
     */
    public String getOption(Character sw) {
        return getOption( (int) sw.charValue());
    }

    /**
     * getOption
     *
     * @param sw int value switch whose option is requested
     */
    public String getOption(int sw) {
        Character opt = new Character((char) sw);
        return getOption(opt);
    }

    // swList

    /**
     * Method to return an Enumeration of the switches
     * that the Getopts object is able to parse (according
     * to its initialization).
     * <p/>
     * May be later used to step through the OptSwitch objects.
     */
    public Enumeration swList() {
        return switchtab.keys();
    }

    // argList

    /**
     * Method to return an array of the actual arguments of the
     * command line invocation.
     */
    public String[] argList() {
        return arglist;
    }

    // Getopts

    /**
     * Wrapper Constructor
     * @param flags a string with the valid switch names
     * @param args  array of strings (usually args)
     *              <p/>
     * @throws InvalidSwitch thrown when invalid options are found
     *                       <p/>
     */
    public Getopts(String flags, String args[])
            throws InvalidSwitch
    {
	this((String)null,flags,args);
    }

    /**
     * Basic class constructor.  Gets the flags passed, in a
     * notation similar to the one used by the sh, ksh, bash,
     * and perl getopts.
     * <p/>
     * String array 'args' is passed and
     * is parsed according to the flags.
     * <p/>
     *
     * @param progname program name for producing error messages
     * @param flags a string with the valid switch names
     * @param args  array of strings (usually args)
     *              <p/>
     * @throws InvalidSwitch thrown when invalid options are found
     *                       <p/>
     */
    public Getopts(String progname, String flags, String args[])
            throws InvalidSwitch {
        initialize(progname,flags,args);
    }

    protected void initialize(String progname, String flags, String args[])
            throws InvalidSwitch {
        String throwstring =
	    progname == null?"Invalid Getopts switch(s): "
	                    : progname+": Invalid Getopts switch(s): ";
        String usage = "Usage: Getopts(String flags, String args[])"
		       + "or Usage: Getopts(String progname, String flags, String args[])";
	this.progname = progname;

        switchtab = new Hashtable(1, 1);

        for (int i = 0; i < flags.length(); i++) {
            boolean found;
            int cc = flags.charAt(i);
            Character c = new Character((char) cc);
            char alpha[] = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
                    'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
                    's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

            //  space characters or punctuation marks are not allowed
            //  as switch values
            //
            if (cc == ' ' || cc == '\t' || cc == '\n' || cc == '\r') {
                throw new InvalidSwitch(throwstring + "Spaces not allowed\n" +
                        usage + "\n");
            }

            found = false;
            for (int j = 0; j < alpha.length; j++) {
                Character ch = new Character(alpha[j]);
                char uc = Character.toUpperCase(ch.charValue());
                if (alpha[j] == cc || uc == cc) {
                    found = true;
                    break;
                }
            }

            //  if the character was not found on the set, throw the exception
            //
            if (! found && cc != ':') {
                throw new InvalidSwitch(throwstring + "Invalid Flag Character " + c + "\n");
            }

            //  ':' appears when the preceding character accepts a value
            //
            if (cc == ':') {
                if (i > 0) {
                    int prv = flags.charAt(i - 1);

                    if (prv == ':') {
                        throw new InvalidSwitch(throwstring +
                                "Can't have consecutive ':'\n" +
                                usage + "\n");
                    } else {
                        Character cp = new Character((char) prv);
                        OptSwitch sw = (OptSwitch) switchtab.get(cp);
                        sw.SetHasValue(OptSwitch.VAL);
                    }
                }
            } else {
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
            char cc = args[i].charAt(0);    // first character

            // check end of options
	    if(args[i].equals("--")) {
		optind = i+1;
                break;
            } else if( cc != '-') {
		optind = i;
                break;
            }

            // more options, iterate them
            for (int j = 1; j < args[i].length(); j++) {
                cc = args[i].charAt(j);
                Character fc = new Character(cc);
                OptSwitch cs = (OptSwitch) switchtab.get(fc);
                if (cs == null) {
                    // The supplied switch wasn't recognised.
		    if(opterr)
                        throw new InvalidSwitch(throwstring + "invalid switch " +
                            cc + "\n2 Valid switches are: " + flags + "\n" + usage + "\n");
		    else
			illegals.append((char)cc);
                } else if (!cs.acceptVal()) {
                    // the option is a switch and takes no value
                    cs.SetVal(true);
                } else if (j + 1 < args[i].length()) {
                    //  the value may follow immediately after the switch
                    // (not as a separate token) set value to remainder of string...
                    cs.SetVal(args[i].substring(j + 1));
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
	    optind = i;
        } // end for i

        //  Now insert the array of arguments into the object
        //
        arglist = new String[args.length - i];
        System.arraycopy(args, i, arglist, 0, args.length - i);
    }

    /**
     * method for class testing.
     * <p/>
     * Invocation:
     * <PRE>
     * java Getopts "option set" arg0 arg1 ... argn
     * </PRE>
     *
     * @param args arguments passed
     */
    public static void main(String args[]) {
        int i;
        String args1[] = new String[args.length - 1];
        System.arraycopy(args, 1, args1, 0, args.length - 1);

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

    /**************************************************/
    /* Mimic gnu Getopt */

    String optarg = null;
    Enumeration optenum = null;
    boolean opterr = true; /* we report errors */
    int optind = 0;
    char optopt = '\0';
    StringBuilder illegals = new StringBuilder();
    String[] optargv = null;
    String optflags = null;

    /**
     * Wrapper Constructor
     * @param progname program name for producing error messages
     * @param flags a string with the valid switch names
     * @param args  array of strings (usually args)
     *              <p/>
     */
    public Getopts(String progname, String args[], String flags)
    {
	this.progname = progname;
	optargv = args;
	optflags = flags;
    }

    public int getopt()
    {
        optarg = null;
	optopt = '\0';
	if(optenum == null) {
	    /* Process the arguments */
	    try {
	        initialize(progname,optflags,optargv);
	    } catch (InvalidSwitch ise) {
	        System.err.println("new Getopts: "+ise.getMessage());
	    }
	    optenum = swList();
	}
	if(optenum.hasMoreElements()) {
	    while(optenum.hasMoreElements()) {
	        Character c = (Character)optenum.nextElement();
	        OptSwitch sw = getSwitch(c);
	        if(sw.set) {
		    if(sw.acceptVal()) {
		        optarg = sw.val;
		    }
		    return (int)c;
	        }
	    }
	} else if(!opterr && illegals.length() > 0) {// Generate illegals */
	    optopt = illegals.charAt(0);
	    illegals.deleteCharAt(0);
	    return (int)'?';
        }
        return -1;
    }

    public String getOptarg() {return optarg;}
    public void setOpterr(boolean b) {opterr = b;}
    public int getOptind() {return optind;}
    public int getOptopt() {return optopt;}

}
