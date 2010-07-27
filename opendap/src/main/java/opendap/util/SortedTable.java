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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Vector;

/**
 * SortedTable looks exactly like Hashtable but preserves the insertion order
 * of elements.  While this results in slower performance, it ensures that
 * the DAS will always be printed in the same order in which it was read.
 */
public final class SortedTable extends Dictionary implements java.io.Serializable {

    static final long serialVersionUID = 1;

    private Vector keys, elements;

    public SortedTable() {
        keys = new Vector();
        elements = new Vector();
    }

    /**
     * Returns the number of keys in this table.
     */
    public int size() {
        return keys.size();
    }

    /**
     * Tests if this table is empty.
     */
    public boolean isEmpty() {
        return keys.isEmpty();
    }

    /**
     * Returns an enumeration of the keys in this table.
     */
    public Enumeration keys() {
        return keys.elements();
    }

    /**
     * Returns an enumeration of the values in this table.
     */
    public Enumeration elements() {
        return elements.elements();
    }

    /**
     * Returns the value to which the key is mapped in this table.
     *
     * @param key a key in this table.
     * @return the value to which the key is mapped, or null if the key is not
     *         mapped to any value in the table.
     */
    public synchronized Object get(Object key) {
        int index = keys.indexOf(key);
        if (index != -1)
            return elements.elementAt(index);
        else
            return null;
    }

    /**
     * Returns the key at the specified index.
     *
     * @param index the index to return
     * @return the key at the specified index.
     */
    public synchronized Object getKey(int index) {
        return keys.elementAt(index);
    }

    /**
     * Returns the element at the specified index.
     *
     * @param index the index to return
     * @return the element at the specified index.
     */
    public synchronized Object elementAt(int index) {
        return elements.elementAt(index);
    }

    /**
     * Maps the specified key to the specified value in this table.
     *
     * @param key   the key
     * @param value the value
     * @return the previous value to which the key is mapped, or null if the
     *         key did not have a previous mapping.
     * @throws NullPointerException if the key or value is null.
     */
    public synchronized Object put(Object key, Object value) throws NullPointerException {
        if (key == null || value == null)
            throw new NullPointerException();

        int index = keys.indexOf(key);
        if (index != -1) {
            Object prev = elements.elementAt(index);
            elements.setElementAt(value, index);
            return prev;
        } else {
            keys.addElement(key);
            elements.addElement(value);
            return null;
        }
    }

    /**
     * Removes the key (and its corresponding value) from this table.  If the
     * key is not in the table, do nothing.
     *
     * @param key the key to remove.
     * @return the value to which the key had been mapped, or null if the key
     *         did not have a mapping.
     */
    public synchronized Object remove(Object key) {
        int index = keys.indexOf(key);
        if (index != -1) {
            Object prev = elements.elementAt(index);
            keys.removeElementAt(index);
            elements.removeElementAt(index);
            return prev;
        } else {
            return null;
        }
    }

    /** Returns a Vector containing the elements in the SortedTable.  This is
     used for more efficient implementation of opendap.dap.Util.uniqueNames() by
     opendap.dap.DDS.checkSemantics()
     @return A Vector containing the elements in this SortedTable.
     */
    public Vector getElementVector() {
        return elements;
  }
}


