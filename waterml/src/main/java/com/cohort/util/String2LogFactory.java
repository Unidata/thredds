/*
 * [This class is based on Log4jFactory.java:]
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.cohort.util;

import com.cohort.util.String2;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Concrete subclass of LogFactory specific to String2.
 *
 * officially deprecated. Per discussion on COMMONS-DEV, the behind-the-scenes use
 *  of this class as a proxy factory has been removed.  For 1.0, you
 *  can still request it directly if you wish, but it doesn't really
 *  do anything useful, and will be removed in 1.1.
 *
 * @author Costin Manolache
 * [Modified by Bob Simons (bob.simons@noaa.gov) to work with String2.log]
 */
public final class String2LogFactory extends LogFactory {

    /**
     * The configuration attributes for this LogFactory.
     */
    private Hashtable attributes = new Hashtable(); //don't change to ConcurrentHashMap since not heavily used.

    private String2Log string2Log;


    // --------------------------------------------------------- Public Methods

    /** Constructor */
    public String2LogFactory() {
        super();
    }

    /**
     * Return the configuration attribute with the specified name (if any),
     * or <code>null</code> if there is no such attribute.
     *
     * @param name Name of the attribute to return
     */
    public Object getAttribute(String name) {
        return (attributes.get(name));
    }


    /**
     * Return an array containing the names of all currently defined
     * configuration attributes.  If there are no such attributes, a zero
     * length array is returned.
     */
    public String[] getAttributeNames() {
        synchronized (attributes) {
            Vector names = new Vector();
            Enumeration keys = attributes.keys();
            while (keys.hasMoreElements()) {
                names.addElement((String) keys.nextElement());
            }
            String results[] = new String[names.size()];
            for (int i = 0; i < results.length; i++) {
                results[i] = (String) names.elementAt(i);
            }
            return (results);
        }
    }


    /**
     * Convenience method to derive a name from the specified class and
     * call <code>getInstance(String)</code> with it.
     * Bob added: if there is a system property "com.cohort.util.String2Log.level"
     * with an int stored in a String as its value (e.g, "4"),
     * that error level will be used; otherwise INFO_LEVEL will be used.
     *
     * @param clazz Class for which a suitable Log name will be derived
     *
     * @exception LogConfigurationException if a suitable <code>Log</code>
     *  instance cannot be returned
     */
    public Log getInstance(Class clazz)
        throws LogConfigurationException
    {
        return getInstance();
    }


    /**
     * Bob added: if there is a system property "com.cohort.util.String2Log.level"
     * with an int stored in a String as its value (e.g, "4"),
     * that error level will be used; otherwise INFO_LEVEL will be used.
     *
     * @param name the name of a class for which a suitable Log name will be derived
     *
     * @exception LogConfigurationException if a suitable <code>Log</code>
     *  instance cannot be returned
     */
    public Log getInstance(String name)
        throws LogConfigurationException
    {
        return getInstance();
    }

    //Bob added this, since I just work with one instance.
    private Log getInstance() throws LogConfigurationException {
        //String2.log("String2LogFactory.getInstance()");
        if (string2Log == null) {
            String s = System.getProperty("com.cohort.util.String2Log.level");
            int level = String2.parseInt(s);
            if (level == Integer.MAX_VALUE) 
                level = String2Log.INFO_LEVEL;
            string2Log = new String2Log(level);
        }
        return string2Log;
    }


    /**
     * Release any internal references to previously created { @ link Log}
     * instances returned by this factory.  This is useful in environments
     * like servlet containers, which implement application reloading by
     * throwing away a ClassLoader.  Dangling references to objects in that
     * class loader would prevent garbage collection.
     */
    public void release() {

        string2Log = null;

    }


    /**
     * Remove any configuration attribute associated with the specified name.
     * If there is no such attribute, no action is taken.
     *
     * @param name Name of the attribute to remove
     */
    public void removeAttribute(String name) {
        attributes.remove(name);
    }


    /**
     * Set the configuration attribute with the specified name.  Calling
     * this with a <code>null</code> value is equivalent to calling
     * <code>removeAttribute(name)</code>.
     *
     * @param name Name of the attribute to set
     * @param value Value of the attribute to set, or <code>null</code>
     *  to remove any setting for this attribute
     */
    public void setAttribute(String name, Object value) {
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
    }

}
