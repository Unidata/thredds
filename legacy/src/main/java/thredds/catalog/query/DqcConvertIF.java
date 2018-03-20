/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: DqcConvertIF.java 48 2006-07-12 16:15:40Z caron $


package thredds.catalog.query;

/**
 * Converts XML DOM to DQC Objects.
 *
 * @see DqcFactory
 * @author John Caron
 */

public interface DqcConvertIF {

  public QueryCapability parseXML( DqcFactory fac, org.jdom2.Document domDoc, java.net.URI docURI) throws java.io.IOException;

  public void writeXML(QueryCapability qc, java.io.OutputStream os) throws java.io.IOException;

}