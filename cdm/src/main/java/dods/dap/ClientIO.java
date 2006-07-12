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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;

/** 
 * Client-side serialization for DODS variables (sub-classes of
 * <code>BaseType</code>).
 * This does not send the entire class as the Java <code>Serializable</code>
 * interface does, rather it sends only the binary data values. Other software
 * is responsible for sending variable type information (see <code>DDS</code>).
 *
 * @version $Revision$
 * @author jehamby
 * @see BaseType
 * @see DDS
 */
public interface ClientIO {

  /**
   * Reads data from a <code>DataInputStream</code>. This method is only used
   * on the client side of the DODS client/server connection.
   *
   * @param source a <code>DataInputStream</code> to read from.
   * @param sv The <code>ServerVersion</code> returned by the server.
   *    (used by <code>DSequence</code> to determine which protocol version was
   *    used).
   * @param statusUI The <code>StatusUI</code> object to use for GUI updates
   *    and user cancellation notification (may be null).
   * @exception DataReadException when invalid data is read, or if the user
   *     cancels the download.
   * @exception EOFException if EOF is found before the variable is completely
   *     deserialized.
   * @exception IOException thrown on any other InputStream exception.
   * @see DataDDS
   */
  public void deserialize(DataInputStream source,
				       ServerVersion sv,
				       StatusUI statusUI)
       throws IOException, EOFException, DataReadException;



    /**
     * Writes data to a <code>DataOutputStream</code>. This method is used
     * primarily by GUI clients which need to download DODS data, manipulate 
     * it, and then re-save it as a binary file.
     *
     * @param sink a <code>DataOutputStream</code> to write to.
     * @exception IOException thrown on any <code>OutputStream</code>
     * exception. 
     */
    public void externalize(DataOutputStream sink) throws IOException;


}
