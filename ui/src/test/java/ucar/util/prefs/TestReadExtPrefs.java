// $Id: TestReadExtPrefs.java,v 1.2 2005/08/22 01:15:07 caron Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.util.prefs;

import java.util.prefs.*;
import java.io.*;

public class TestReadExtPrefs  {

  public static void main(String args[]) {
    //System.getProperty("ucar.util.prefs.PreferencesExtFactory");
    TestReadExtPrefs pf = new TestReadExtPrefs();
    //pf.doit("work/extPrefs2.xml");
    pf.doit(TestAllPrefs.dir+"testBeans.xml");
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