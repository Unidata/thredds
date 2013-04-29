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

package ucar.nc2.util;

import junit.framework.TestCase;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.unidata.test.Diff;

import java.io.*;

public class UnitTestCommon extends TestCase
{
    static public boolean debug = false;

    static public org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfFile.class);

    // Look for these to verify we have found the thredds root
    static final String[] SUBROOTS = new String[]{"cdm", "tds", "opendap"};

    static public final String threddsRoot = locateThreddsRoot();

    // Walk around the directory structure to locate
    // the path to a given directory.

    static String locateThreddsRoot()
    {
        // Walk up the user.dir path looking for a node that has
        // all the directories in SUBROOTS.

        String path = System.getProperty("user.dir");

        // clean up the path
        path = path.replace('\\', '/'); // only use forward slash
        assert (path != null);
        if(path.endsWith("/")) path = path.substring(0, path.length() - 1);

        while(path != null) {
            boolean allfound = true;
            for(String dirname : SUBROOTS) {
                // look for dirname in current directory
                String s = path + "/" + dirname;
                File tmp = new File(s);
                if(!tmp.exists()) {
                    allfound = false;
                    break;
                }
            }
            if(allfound)
                return path; // presumably the thredds root
            int index = path.lastIndexOf('/');
            path = path.substring(0, index);
        }
        return null;
    }

    public void
    clearDir(File dir, boolean clearsubdirs)
        throws Exception
    {
        // wipe out the dir contents
        if(!dir.exists()) return;
        for(File f : dir.listFiles()) {
            if(f.isDirectory()) {
                if(clearsubdirs)
                    clearDir(f, true); // clear subdirs
                else
                    throw new Exception("InnerClass directory encountered: " + f.getAbsolutePath());
            }
            f.delete();
        }
    }

    //////////////////////////////////////////////////
    // Instance data

    String title = "Testing";
    String name = "testcommon";

    public UnitTestCommon()
    {
        this("UnitTest");
    }

    public UnitTestCommon(String name)
    {
        super(name);
        this.name = name;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return this.title;
    }

    public String compare(String tag, String baseline, String s)
    {
        try {
            // Diff the two print results
            Diff diff = new Diff(tag);
            StringWriter sw = new StringWriter();
            boolean pass = !diff.doDiff(baseline,s,sw);
            return (pass?null:sw.toString());
        } catch (Exception e) {
            System.err.println("UnitTest: Diff failure: "+e);
            return null;
        }

    }
}

