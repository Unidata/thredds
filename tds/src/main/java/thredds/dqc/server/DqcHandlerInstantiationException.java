// $Id$
package thredds.dqc.server;

/**
 * Thrown when a call to DqcHandler.factory() fails.
 */
public class DqcHandlerInstantiationException extends Exception
{
  public DqcHandlerInstantiationException() { super(); }
  public DqcHandlerInstantiationException( String message ) { super( message ); }
  public DqcHandlerInstantiationException( String message, Throwable cause ) { super( message, cause ); }
  public DqcHandlerInstantiationException( Throwable cause ) { super( cause ); }
}

/*
 * $Log: DqcHandlerInstantiationException.java,v $
 * Revision 1.3  2004/08/23 16:50:34  edavis
 * Update DqcServlet to work with DQC spec v0.3 and InvCatalog v1.0. Folded DqcServlet into the THREDDS server framework/build/distribution. Updated documentation (DqcServlet and THREDDS server).
 *
 */