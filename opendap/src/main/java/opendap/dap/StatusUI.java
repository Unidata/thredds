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


