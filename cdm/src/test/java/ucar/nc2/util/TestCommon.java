package ucar.nc2.util;

import com.sun.org.apache.bcel.internal.classfile.InnerClass;
import junit.framework.TestCase;

import org.junit.Test;
import ucar.nc2.NetcdfFile;

import java.io.File;

public class TestCommon extends TestCase
{
    static public boolean debug = false;

    static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfFile.class);

    // Look for these to verify we have found the thredds root
    static final String[] SUBROOTS = new String[] {"cdm", "tds", "opendap"} ;

    static public final String threddsRoot = locateThreddsRoot();

    // Walk around the directory structure to locate
    // the path to a given directory.

    static String locateThreddsRoot()
    {
        // Walk up the user.dir path looking for a node that has
        // all the directories in SUBROOTS.

        String path = System.getProperty("user.dir");

	// clean up the path
        path = path.replace('\\','/'); // only use forward slash
        assert (path != null);
        if(path.endsWith("/")) path = path.substring(0,path.length()-1);

        while(path != null) {
            boolean allfound = true;
            for(String dirname: SUBROOTS) {
                // look for dirname in current directory
                String s = path + "/" + dirname;
                File tmp = new File(s);
                if(!tmp.exists()) {allfound = false; break; }
            }
            if(allfound)
                return path; // presumably the thredds root
            int index = path.lastIndexOf('/');
            path = path.substring(0,index);
        }
        return null;
    }

    public void
    clearDir(File dir, boolean clearsubdirs)
            throws Exception
    {
        // wipe out the dir contents
        if(!dir.exists()) return;
        for(File f: dir.listFiles()) {
            if(f.isDirectory()) {
                if(clearsubdirs)
                    clearDir(f,true); // clear subdirs
                else
                    throw new Exception("InnerClass directory encountered: "+f.getAbsolutePath());
            }
            f.delete();
        }
    }

    //////////////////////////////////////////////////
    // Instance data

    String title = "Testing";


    public TestCommon(String name)
    {
        super(name);
    }

    public void setTitle(String title) {this.title = title;}

}

