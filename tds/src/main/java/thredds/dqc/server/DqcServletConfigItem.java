/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
// $Id: DqcServletConfigItem.java 51 2006-07-12 17:13:13Z caron $

package thredds.dqc.server;

/**
 * Bean for DqcHandler config information.
 *
 */
public class DqcServletConfigItem
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( DqcServletConfigItem.class);

  private String name = null;
  private String description = null;
  private String handlerClassName = null;
  private String handlerConfigFileName = null;

  public DqcServletConfigItem() {}
  public DqcServletConfigItem( String name, String description,
                               String handlerClassName, String handlerConfigFileName )
  {
    log.debug( "Start constructor.");
    this.name = name;
    this.description= description;
    this.handlerClassName = handlerClassName;
    this.handlerConfigFileName = handlerConfigFileName;
  }

  /** Set the name of this DqcServletConfigItem instance. */
  public void setName( String name)
  { this.name = name; this.hashCode = 0; }
  /** Return the name of this DqcServletConfigItem instance. */
  public String getName() { return( this.name); }

  /** Set the description of this DqcServletConfigItem instance. */
  public void setDescription( String description)
  { this.description = description; this.hashCode = 0; }
  /** Return the description of this DqcServletConfigItem instance. */
  public String getDescription() { return( this.description); }

  /** Set the DqcHandler class name. */
  public void setHandlerClassName( String handler)
  { this.handlerClassName = handler; this.hashCode = 0; }
  /** Return the DqcHandler class name. */
  public String getHandlerClassName() { return( this.handlerClassName); }

  /** Set the config file name for the DqcHandler. */
  public void setHandlerConfigFileName( String handlerConfigFileName)
  { this.handlerConfigFileName = handlerConfigFileName; this.hashCode = 0; }
  /** Return the config file name for the DqcHandler. */
  public String getHandlerConfigFileName() { return( this.handlerConfigFileName); }

  public String toString()
  {
    StringBuffer buf = new StringBuffer();

    buf.append( "DqcServletConfigItem(name=" );
    buf.append( this.name );
    buf.append( ", desc=" );
    buf.append( this.description );
    buf.append( ", handler class=" );
    buf.append( this.handlerClassName );
    buf.append( ", config file=" );
    buf.append( this.handlerConfigFileName + ")" );

    return( buf.toString());
  }

  /**
   * Tests the equality of this DqcServletConfigItem with the given Object.
   * Returns true if and only if the argument is not null and is also a
   * DqcServletConfigItem containing the same information as this
   * DqcServletConfigItem.
   *
   * @param o - The object to be compared with this DqcServletConfigItem.
   * @return true if and only if the Object is equal to this DqcServletConfigItem, false otherwise.
   */
  public boolean equals( Object o )
  {
    if ( this == o )
    {
      return true;
    }
    if ( !( o instanceof DqcServletConfigItem ) )
    {
      return false;
    }
    return o.hashCode() == this.hashCode();
  }

  /**
   * Computes a hash code for this DqcServletConfigItem.
   *
   * @return a hash code for this DqcServletConfigItem.
   */
  public int hashCode()
  {
    if ( hashCode == 0 )
    {
      int result = 17;
      if ( this.getName() != null )
      {
        result = 37 * result + this.getName().hashCode();
      }
      if ( this.getDescription() != null )
      {
        result = 37 * result + this.getDescription().hashCode();
      }
      if ( this.getHandlerClassName() != null )
      {
        result = 37 * result + this.getHandlerClassName().hashCode();
      }
      if ( this.getHandlerConfigFileName() != null )
      {
        result = 37 * result + this.getHandlerConfigFileName().hashCode();
      }

      hashCode = result;
    }
    return hashCode;
  }

  private volatile int hashCode = 0; // Bloch, item 8

}
/*
 * $Log: DqcServletConfigItem.java,v $
 * Revision 1.6  2006/01/20 20:42:04  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.5  2005/04/05 22:37:03  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.4  2004/08/23 16:45:20  edavis
 * Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).
 *
 * Revision 1.3  2003/12/11 01:12:58  edavis
 * Add logging.
 *
 * Revision 1.2  2003/05/06 22:15:30  edavis
 * Add 'description' and 'dqcFileName' elements.
 *
 * Revision 1.1  2003/04/28 17:57:45  edavis
 * Initial checkin of THREDDS DqcServlet.
 *
 */