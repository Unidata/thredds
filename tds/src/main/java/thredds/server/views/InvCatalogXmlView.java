/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.views;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.io.OutputStream;

import thredds.client.catalog.Catalog;
import thredds.client.catalog.tools.CatalogXmlWriter;
import thredds.util.ContentType;

/**
 * Configured by Spring MVC, used in thredds.server.catalogservice
 *
 * @author edavis
 * @since 4.0
 */
public class InvCatalogXmlView extends AbstractView {
  static private final Logger logger = LoggerFactory.getLogger(InvCatalogXmlView.class);

  protected void renderMergedOutputModel(Map model, HttpServletRequest req, HttpServletResponse res) throws Exception {
    res.setContentType(getContentType());

    try {
      if (model == null || model.isEmpty())
        throw new IllegalArgumentException("Model must not be null or empty.");

      if (!model.containsKey("catalog"))
        throw new IllegalArgumentException("Model must contain 'catalog' key.");

      Object o = model.get("catalog");
      if (!(o instanceof Catalog))
        throw new IllegalArgumentException("Model must contain a Catalog object.");

      Catalog cat = (Catalog) o;

      if (!req.getMethod().equals("HEAD")) {
        try (OutputStream os = res.getOutputStream()) {
          CatalogXmlWriter catFactory = new CatalogXmlWriter();
          catFactory.writeXML(cat, os);
        }
      }
    } catch (Exception e) {
      logger.error("InvCatalogXmlView failed", e);
    }
  }

  public String getContentType() {
    return ContentType.xml.getContentHeader();
  }
}