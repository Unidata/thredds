/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Java-DAP" project, a Java implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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


