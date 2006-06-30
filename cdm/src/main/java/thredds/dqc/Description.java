// $Id: Description.java,v 1.3 2006/01/20 20:42:04 caron Exp $
package thredds.dqc;

/**
 * A description of the parent DQC element.
 *
 * A DQC description element can contain plain text or mixed text and XHTML markup.
 *
 * User: edavis
 * Date: Jan 22, 2004
 * Time: 10:38:25 PM
 */
public class Description
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( Description.class );

  protected StringBuffer content = null;

  /** Null constructor. */
  protected Description() { this.content = new StringBuffer(); }

  /** Constructor. */
  protected Description( String content ){ this.content = new StringBuffer( content ); }

  /** Get the text that is the content of this description. */
  public String getContent() { return( content.toString() ); }

  /** Set the text content of this description. */
  protected void setContent( String content ) { this.content = new StringBuffer( content); }

  /** Append to the text content of this description. */
  protected void appendContent( String moreContent) { this.content.append( moreContent); }
}

/*
 * $Log: Description.java,v $
 * Revision 1.3  2006/01/20 20:42:04  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.2  2005/04/05 22:37:03  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.1  2004/03/05 06:33:40  edavis
 * Classes to handle DQC and user query information.
 *
 */