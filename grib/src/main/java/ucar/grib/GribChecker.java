/*
 * $Id: IDV-Style.xjs,v 1.2 2006/10/19 22:27:18 dmurray Exp $
 * 
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.grib;


import ucar.grib.grib1.*;
import ucar.grib.grib2.*;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.io.PrintStream;

// import statements
import java.lang.*;  // Standard java functions

import java.util.*;  // Extra utilities from sun


/**
 * Used to check the edition number of a Grib file.
 * @author Robb Kambic  9/15/04
 *
 * @version 1.0
 *
 */
public final class GribChecker {


    /**
     *
     * Dumps usage of the class, if called without arguments.
     * @param className GribChecker
     *
     */
    private static void usage(String className) {
        System.out.println();
        System.out.println("Usage of " + className + ":");
        System.out.println("Parameters:");
        System.out.println("<GribFileToRead> checks for validity");
        System.out.println();
        System.out.println("java " + className + " <GribFileToRead>");
        System.exit(0);
    }

    /**
     * Checks a Grib file for edition.
     *
     * @param  args Gribfile
     *
     */
    public static void main(String args[]) {

        // Function References
        GribChecker func = new GribChecker();

        // Test usage
        if (args.length < 1) {
            // Get class name as String
            Class cl = func.getClass();
            func.usage(cl.getName());
        }

        // Opening of grib data must be inside a try-catch block
        try {
            RandomAccessFile raf     = null;
            PrintStream      ps      = System.out;
            String           outfile = args[0];
            if (args.length == 1) {  // input file given 
                raf = new RandomAccessFile(outfile, "r");
            } else {
                ps.println("no file name given");
                System.exit(0);
            }
            int result = getEdition(raf);
            if (result == 2) {
                ps.println("Valid Grib Edition 2 File");
            } else if (result == 1) {
                ps.println("Valid Grib Edition 1 File");
            } else {
                ps.println("Not a Grib File");
            }

        } catch (IOException noFileError) {
            System.err.println("FileNotFoundException : " + noFileError);
        }

    }                                // end main

    /**
     * Returns the edition number of Grib file (1 or 2) or 0 not a Grib File.
     * @param raf
     * @return edition
     */
    public static int getEdition(RandomAccessFile raf) {

        int        check = 0;
        Grib2Input g2i   = null;
        try {
            // Create Grib2Input instance
            g2i   = new Grib2Input(raf);
            check = g2i.getEdition();
        } catch (NotSupportedException noSupport) {
            System.err.println("NotSupportedException : " + noSupport);
        } catch (IOException ioError) {
            System.err.println("IOException : " + ioError);
        }
        return check;
    }

}  // end GribChecker


