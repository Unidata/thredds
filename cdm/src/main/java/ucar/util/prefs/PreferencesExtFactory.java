/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
