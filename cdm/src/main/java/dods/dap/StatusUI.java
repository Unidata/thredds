/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////

package dods.dap;

/**
 * This interface is implemented by DODS client user interfaces which give
 * feedback to the user on the status of the current deserialize operation.
 * The user can also cancel the current deserialize through this interface.
 *
 * @version $Revision: 1.1 $
 * @author jehamby
 * @see DataDDS
 */
public interface StatusUI {
  /**
   * Add bytes to the total deserialize count.  This is called by each
   * <code>BaseType</code>'s <code>deserialize</code> method to provide the
   * user with feedback on the number of bytes that have been transferred
   * so far.  If some future version of DODS provides a correct
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
