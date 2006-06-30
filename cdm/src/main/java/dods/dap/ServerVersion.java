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
 * This class is a convenient place to store the major and minor
 * version number of the remote server, as well as the full version string.
 * It is used so that classes which implement ClientIO don't need any knowledge
 * of the DDS class.
 *
 * @version $Revision: 1.1 $
 * @author jehamby
 * @see ClientIO
 * @see DDS
 */
public class ServerVersion {
  /** Major version number. */
  private int major;
  /** Minor version number. */
  private int minor;
  /** Full version string. */
  private String versionString;

  /**
   * Construct a new ServerVersion, setting major and minor version based
   * on the full version string.
   *
   * @param ver the full version string.
   */
  public ServerVersion(String ver) {
    
    this.versionString = ver;
    this.major = this.minor = 0;  // set version to default values
    // search for the String, e.g. DODS/2.15, and set major and minor
    // accordingly
    int verIndex = ver.indexOf("/");
    if (verIndex != -1) {
      verIndex += 1;  // skip over "/" to number
      int dotIndex = ver.indexOf('.', verIndex);
      if (dotIndex != -1) {
	String majorString = ver.substring(verIndex, dotIndex);
	major = Integer.parseInt(majorString);
	String minorString = ver.substring(dotIndex+1);
	int minorDotIndex = minorString.indexOf('.');
	if(minorDotIndex != -1) 
	    minor = Integer.parseInt(minorString.substring(0,minorDotIndex));
	else 
	    minor = Integer.parseInt(minorString);
      }
    }
  }

  /**
   * Construct a new ServerVersion, setting major and minor version explicitly.
   *
   * @param major the major version number.
   * @param minor the minor version number.
   */
  public ServerVersion(int major, int minor) {
    this.versionString = "DODS/" + major + "." + minor;
    this.major = major;
    this.minor = minor;
  }

  /**
   * Returns the major version number.
   * @return the major version number.
   */
  public final int getMajor() {
    return major;
  }

  /** 
   * Returns the minor version number.
   * @return the minor version number.
   */
  public final int getMinor() {
    return minor;
  }

  /**
   * Returns the full version string.
   * @return the full version string.
   */
  public final String toString() {
    return versionString;
  }

  /**
   * Returns the DODS core version as a <code>String</code>.
   * This was a convenient place to put this information, rather than
   * creating a new class.
   *
   * @return the current DODS version.
   */
  public static String getCurrentVersion() {
    return "DODS/2.18";
  }
}
