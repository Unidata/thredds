/*
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

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.regex.*;

/**
 * A class that allows external packages to define how to read resource files.
 *
 * @author Jeff McWhirter  02/07/2007
 * @version 2.0
 */

public abstract class GribResourceReader {

    /** The singleton reader. Can be set by other packages to read resources */
    private static GribResourceReader gribResourceReader;

    /**
     * Set the singleton reader. This is used to read in resources and can be set by external
     * packages
     *
     * @param reader The reader
     */
    public static void setGribResourceReader(GribResourceReader reader) {
        gribResourceReader = reader;
    }

    /**
     * Ovderridden by instances of the singleton
     *
     * @param resourceName The resource name. May be a file, url, java resource, etc.
     *
     * @return The input stream to the resource
     *
     * @throws IOException _more_
     */
    public abstract InputStream openInputStream(String resourceName)
     throws IOException;


    /**
     * Get the input stream to the given resource
     *
     * @param resourceName The resource name. May be a file, url, java resource, etc.
     *
     * @return The input stream to the resource
     */
    public static InputStream getInputStream(String resourceName) {
        return getInputStream(resourceName, null);
    }


    /**
     * Get the input stream to the given resource
     *
     * @param resourceName The resource name. May be a file, url, java resource, etc.
     * @param originClass If non-null we use this to find java resources that are relative to a class
     *
     * @return The input stream to the resource
     */

    public static InputStream getInputStream(String resourceName,
                                             Class originClass) {
        if (gribResourceReader != null) {
            try {
                InputStream inputStream =
                    gribResourceReader.openInputStream(resourceName);
                if (inputStream != null) {
                    return inputStream;
                }
            } catch (IOException ioe) {
                System.err.println("Failed to open:" + resourceName + "\n"
                                   + ioe);
            }
        }

        InputStream s = null;
        while (originClass != null) {
            s = originClass.getResourceAsStream(resourceName);
            if (s != null) {
                break;
            }
            originClass = originClass.getSuperclass();
        }



        // Try class loader to get resource
        ClassLoader cl = GribResourceReader.class.getClassLoader();
        s = cl.getResourceAsStream(resourceName);
        if (s != null) {
            return s;
        }

        //Try the file system
        File f = new File(resourceName);
        if (f.exists()) {
            try {
                s = new FileInputStream(f);
            } catch (Exception e) {}
        }
        if (s != null) {
            return s;
        }

        //Try it as a url
        try {
            Matcher       m = Pattern.compile(" ").matcher(resourceName);
            String        encodedUrl = m.replaceAll("%20");
            URL           dataUrl    = new URL(encodedUrl);
            URLConnection connection = dataUrl.openConnection();
            s = connection.getInputStream();
        } catch (Exception exc) {}

        return s;
    }


    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    public static String getFileRoot(String f) {
        int idx = f.lastIndexOf("/");
        if (idx < 0) {
            idx = f.lastIndexOf(File.separator);
        }
        if (idx < 0) {
            return f;
        }
        return f.substring(0, idx);
    }




}

