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


package opendap.dap;

/**
 * This interface is implemented by OPeNDAP client user interfaces which give
 * feedback to the user on the status of the current deserialize operation.
 * The user can also cancel the current deserialize through this interface.
 *
 * @author jehamby
 * @version $Revision: 15901 $
 * @see DataDDS
 */
public interface StatusUI {
    /**
     * Add bytes to the total deserialize count.  This is called by each
     * <code>BaseType</code>'s <code>deserialize</code> method to provide the
     * user with feedback on the number of bytes that have been transferred
     * so far.  If some future version of OPeNDAP provides a correct
     * Content-Length, then a sophisticated GUI could use this information to
     * estimate the time remaining to download.
     *
     * @param bytes the number of bytes to add.
     */
    public void incrementByteCount(int bytes);

    /**
     * User cancellation status.  This returns true when the user has clicked
     * the cancel button of a GUI, or false if the download should proceed.  This
     * is called at various cancellation points throughout the deserialize
     * process so that the download can be cancelled in an orderly fashion.
     *
     * @return true if the download should be cancelled.
     */
    public boolean userCancelled();

    /**
     * Download finished notice.  This allows the GUI to close itself or print
     * a message to the user that the transfer is finished.
     */
    public void finished();
}


