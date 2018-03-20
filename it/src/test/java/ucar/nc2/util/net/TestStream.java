/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.net;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.tools.CatalogXmlWriter;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.nc2.constants.CDM;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Describe
 *
 * @author caron
 * @since 3/5/14
 */
public class TestStream {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testStream1() throws URISyntaxException, IOException {
    String catalogName = TestOnLocalServer.withHttpPath("catalog.xml");
    URI catalogURI = new URI(catalogName);

    try (HTTPSession client = HTTPFactory.newSession(catalogName)) {
      HTTPMethod m = HTTPFactory.Get(client,catalogName);

      int statusCode = m.execute();
      logger.debug("status = {}", statusCode);

      InputStream stream = m.getResponseBodyAsStream();
      CatalogBuilder builder = new CatalogBuilder();
      Catalog cat = builder.buildFromStream(stream, catalogURI);
      CatalogXmlWriter writer = new CatalogXmlWriter();

      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        writer.writeXML(cat, baos, false);
        logger.debug(baos.toString());
      }
    }
  }

  @Test
  public void testString() throws URISyntaxException, IOException {
    String catalogName = "http://localhost:8081/thredds/catalog.xml";
    URI catalogURI = new URI(catalogName);

    try (HTTPSession client = HTTPFactory.newSession(catalogName)) {
      HTTPMethod m = HTTPFactory.Get(client,catalogName);

      int statusCode = m.execute();
      logger.debug("status = {}", statusCode);

      String catAsString = m.getResponseAsString(CDM.UTF8);
      logger.debug("cat = {}", catAsString);

      CatalogBuilder builder = new CatalogBuilder();
      Catalog cat = builder.buildFromString(catAsString, catalogURI);
      CatalogXmlWriter writer = new CatalogXmlWriter();

      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        writer.writeXML(cat, baos, false);
        logger.debug(baos.toString());
      }
    }
  }
}
