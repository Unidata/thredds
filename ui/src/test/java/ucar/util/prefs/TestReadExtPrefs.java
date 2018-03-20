/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestReadExtPrefs.java,v 1.2 2005/08/22 01:15:07 caron Exp $

package ucar.util.prefs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.prefs.*;
import java.io.*;

public class TestReadExtPrefs  {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static {
      System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
  }

  public static void main(String args[]) throws IOException {
    //System.getProperty("ucar.util.prefs.PreferencesExtFactory");
    TestReadExtPrefs pf = new TestReadExtPrefs();
    //pf.doit("work/extPrefs2.xml");
    pf.doit(File.createTempFile("foo", "bar").getAbsolutePath());
  }

  void doit(String filename) {
    XMLStore store = null;
    PreferencesExt userRoot = null;
    try {
      TestAllPrefs.copyFile(filename, System.out);
      store = XMLStore.createFromFile(filename, null);
      userRoot = store.getPreferences();
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed "+e);
      System.exit(1);
    }

    try {
      userRoot.dump();
    } catch (BackingStoreException ex) {

    }



    try {
      //OutputStream os = new FileOutputStream("standardPrefs.xml");
      userRoot.exportSubtree( System.out);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
/* Change History:
   $Log: TestReadExtPrefs.java,v $
   Revision 1.2  2005/08/22 01:15:07  caron
   no message

   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/
