// $Id: PreferencesExtFactory.java,v 1.2 2004/08/26 17:55:18 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

/**
 *  Implementation of  <tt>PreferencesFactory</tt> to return PreferencesExt objects. Using this
 *  method of obtaining a Preferences object is optional; you can also just pass around the
 *  Preferences object explicitly.
 *
 *  The default Preferences.userRoot() and Preferences.systemRoot() are empty. To use
 *  persistent versions, you must set them explicitly through
 *  <pre>
 *    PreferencesExt.setSystemRoot( PreferencesExt prefs);
 *    PreferencesExt.setUserRoot( PreferencesExt prefs);
 *  </pre>
 *  and also call:
 *  <pre>
 *    System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
 *  </pre>
 *
 */
public class PreferencesExtFactory implements java.util.prefs.PreferencesFactory  {

    public java.util.prefs.Preferences userRoot() {
        return PreferencesExt.userRoot;
    }

    public java.util.prefs.Preferences systemRoot() {
        return PreferencesExt.systemRoot;
    }
}
/* Change History:
   $Log: PreferencesExtFactory.java,v $
   Revision 1.2  2004/08/26 17:55:18  caron
   no message

   Revision 1.1.1.1  2002/12/20 16:40:25  john
   start new cvs root: prefs

   Revision 1.1.1.1  2001/11/10 16:01:23  caron
   checkin prefs

*/