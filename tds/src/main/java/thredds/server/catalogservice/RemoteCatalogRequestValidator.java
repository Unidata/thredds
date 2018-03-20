/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalogservice;

import org.springframework.validation.Validator;
import org.springframework.validation.Errors;

import java.net.URI;
import java.net.URISyntaxException;

public class RemoteCatalogRequestValidator implements Validator {

  public boolean supports(Class clazz) {
    return RemoteCatalogRequest.class.equals(clazz);
  }

  public void validate(Object obj, Errors errs) {
    RemoteCatalogRequest rcr = (RemoteCatalogRequest) obj;

    try {
      URI catUri = new URI(rcr.getCatalog());
      if (!catUri.isAbsolute())
        errs.rejectValue("catalogUri", "catalogUri.notAbsolute", "The catalog parameter must be an absolute URI.");
      if ( catUri.getScheme() != null
            && ! catUri.getScheme().equalsIgnoreCase( "HTTP" )
            && ! catUri.getScheme().equalsIgnoreCase( "HTTPS" ))
        errs.rejectValue( "catalogUri", "catalogUri.notHttpUri",
                "The \"catalogUri\" field must be an HTTP|HTTPS URI.");
      rcr.setCatalogUri(catUri);

    } catch (URISyntaxException e) {
      errs.rejectValue("catalog", "catalogUri.notAbsolute", "catalog parameter is not a valid URI");
    }

    if (rcr.getDataset() != null)
      rcr.setCommand(RemoteCatalogRequest.Command.SUBSET);
    else if (rcr.getCommand() == null)
      rcr.setCommand(RemoteCatalogRequest.Command.SHOW);
  }
}
