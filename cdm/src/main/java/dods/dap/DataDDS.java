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
import java.io.*;
import java.util.Enumeration;
import java.util.zip.DeflaterOutputStream;

/**
 * The DataDDS class extends DDS to add new methods for retrieving data from
 * the server, and printing out the contents of the data.
 *
 * @version $Revision: 48 $
 * @author jehamby
 * @see DDS
 */
public class DataDDS extends DDS {
  /** The ServerVersion returned from the open DODS connection. */
  private ServerVersion ver;

  /**
   * Construct the DataDDS with the given server version.
   * @param ver the ServerVersion returned from the open DODS connection.
   */
  public DataDDS(ServerVersion ver) {
    super();
    this.ver = ver;
  }
  
  public DataDDS(ServerVersion ver, BaseTypeFactory btf) {
    super(btf);
    this.ver = ver;
  }

  /**
   * Returns the <code>ServerVersion</code> given in the constructor.
   * @return the <code>ServerVersion</code> given in the constructor.
   */
  public final ServerVersion getServerVersion() {
    return ver;
  }

  /**
   * Read the data stream from the given InputStream.  In the C++ version,
   * this code was in Connect.
   *
   * @param is the InputStream to read from
   * @param statusUI the StatusUI object to use, or null
   * @exception EOFException if EOF is found before the variable is completely
   *     deserialized.
   * @exception IOException thrown on any other InputStream exception.
   * @exception DataReadException when invalid data is read, or if the user
   *     cancels the download.
   * @exception DODSException if the DODS server returned an error.
   */
  public void readData(InputStream is, StatusUI statusUI)
       throws IOException, EOFException, DODSException {
    // Buffer the input stream for better performance
    BufferedInputStream bufferedIS = new BufferedInputStream(is);
    // Use a DataInputStream for deserialize
    DataInputStream dataIS = new DataInputStream(bufferedIS);

    for(Enumeration e = getVariables(); e.hasMoreElements(); ) {
      if (statusUI != null && statusUI.userCancelled())
	throw new DataReadException("User cancelled");
      ClientIO bt = (ClientIO)e.nextElement();
      bt.deserialize(dataIS, ver, statusUI);
    }
    // notify GUI of finished download
    if (statusUI != null)
      statusUI.finished();
  }

  /**
   * Print the dataset just read.  In the C++ version, this code was in
   * <code>geturl</code>.
   *
   * @param os the <code>PrintWriter</code> to use.
   */
  public void printVal(PrintWriter os) {
    for (Enumeration e = getVariables(); e.hasMoreElements(); ) {
      BaseType bt = (BaseType)e.nextElement();
      bt.printVal(os, "", true);
    }
    os.println();
  }

  /**
   * Print the dataset using OutputStream.
   * @param os the <code>OutputStream</code> to use.
   */
  public final void printVal(OutputStream os) {
    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
    printVal(pw);
    pw.flush();
  }

  /**
   * Dump the dataset using externalize methods. This should create
   * a multipart Mime document with the binary representation of the
   * DDS that is currently in memory.
   *
   * @param os the <code>OutputStream</code> to use.
   * @param compress <code>true</code> if we should compress the output.
   * @param headers <code>true</code> if we should print HTTP headers.
   * @exception IOException thrown on any <code>OutputStream</code> exception.
   */
  public final void externalize(OutputStream os, boolean compress, boolean headers)
       throws IOException {
    // First, print headers
    if (headers) {
      PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
      pw.println("HTTP/1.0 200 OK");
      pw.println("Server: " + ServerVersion.getCurrentVersion());
      pw.println("Content-type: application/octet-stream");
      pw.println("Content-Description: dods_data");
      if (compress) {
	pw.println("Content-Encoding: deflate");
      }
      pw.println();
      pw.flush();
    }

    // Buffer the output stream for better performance
    OutputStream bufferedOS;
    if (compress) {
      // deflate has its own buffering jc: NOT TRUE
      // original bufferedOS = new DeflaterOutputStream(os);
      DeflaterOutputStream deflater = new DeflaterOutputStream(os);
      bufferedOS = new BufferedOutputStream(deflater, 10 * 1000);   // JC added

    } else {
      bufferedOS = new BufferedOutputStream(os);
    }

    // Redefine PrintWriter here, so the DDS is also compressed if necessary
    PrintWriter pw = new PrintWriter(new OutputStreamWriter(bufferedOS));
    print(pw);
    // pw.println("Data:");  // JCARON CHANGED
    pw.flush();
    bufferedOS.write("\nData:\n".getBytes()); // JCARON CHANGED
    bufferedOS.flush();

    // Use a DataOutputStream for serialize
    DataOutputStream dataOS = new DataOutputStream(bufferedOS);
    for(Enumeration e = getVariables(); e.hasMoreElements(); ) {
      ClientIO bt = (ClientIO)e.nextElement();
      bt.externalize(dataOS);
    }
    // Note: for DeflaterOutputStream, flush() is not sufficient to flush
    // all buffered data
    dataOS.close();
  }
}
