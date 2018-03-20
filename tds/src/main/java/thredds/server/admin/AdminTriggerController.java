/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import thredds.core.AllowedServices;
import thredds.core.ConfigCatalogInitialization;
import thredds.server.catalog.tracker.DatasetTracker;
import thredds.server.catalog.tracker.DatasetTrackerNoop;
import thredds.server.config.TdsContext;
import thredds.util.ContentType;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Catalog trigger controller
 *
 * @author caron
 * @since 6/26/2015
 */
@Controller
@RequestMapping(value = {"/admin/trigger"})
public class AdminTriggerController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminTriggerController.class);

    @Autowired
    DebugCommands debugCommands;

    @Autowired
    private TdsContext tdsContext;  // used for  getContentDirectory, contextPath

    @Autowired
    private ConfigCatalogInitialization catInit;

    @PostConstruct
    public void afterPropertiesSet() {

      DebugCommands.Category debugHandler = debugCommands.findCategory("Catalogs");
      DebugCommands.Action act;

      act = new DebugCommands.Action("reportStats", "Make Catalog Report") {
        public void doAction(DebugCommands.Event e) {
          // look want background thread ?
          e.pw.printf("%n%s%n", makeReport());
        }
      };
      debugHandler.addAction(act);

      act = new DebugCommands.Action("reinit", "Read all catalogs") {
        public void doAction(DebugCommands.Event e) {
          // look want background thread ?
          boolean ok = catInit.reread(ConfigCatalogInitialization.ReadMode.always, false);
          e.pw.printf("<p/>Reading all catalogs%n");
          if (ok) e.pw.printf("reinit ok%n");
          else e.pw.printf("reinit failed%n");
        }
      };
      debugHandler.addAction(act);

      act = new DebugCommands.Action("recheck", "Read changed catalogs") {
        public void doAction(DebugCommands.Event e) {
          // look want background thread ?
          boolean ok = catInit.reread(ConfigCatalogInitialization.ReadMode.check, false);
          e.pw.printf("<p/>Reading changed catalogs%n");
          if (ok) e.pw.printf("reinit ok%n");
        }
      };
      debugHandler.addAction(act);
    }

    @RequestMapping(value = "/catalog", method = RequestMethod.GET, params = "req=readAll")
    protected ResponseEntity<String> handleReadAll() throws Exception {
      catInit.reread(ConfigCatalogInitialization.ReadMode.always, false);

      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.set(ContentType.HEADER, ContentType.text.getContentHeader());
      String result = "Reading all catalogs";
      return new ResponseEntity<>(result, responseHeaders, HttpStatus.OK);
    }


  @RequestMapping(value = "/catalog", method = RequestMethod.GET, params = "req=readChanged")
  protected ResponseEntity<String> handleReadChanged() throws Exception {
    catInit.reread(ConfigCatalogInitialization.ReadMode.check, false);

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set(ContentType.HEADER, ContentType.text.getContentHeader());
    String result = "Reading changed catalogs";
    return new ResponseEntity<>(result, responseHeaders, HttpStatus.OK);
  }

  private String makeReport() {
    DatasetTrackerNoop tracker = new DatasetTrackerNoop();
    AllowedServices allowedServices = new AllowedServices();
    DatasetTracker.Callback callback = new ConfigCatalogInitialization.StatCallback(ConfigCatalogInitialization.ReadMode.always);

    //   public ConfigCatalogInitialization(ReadMode readMode, String contentRootPath, String trackerDir, DatasetTracker datasetTracker,
    //                                     AllowedServices allowedServices, DatasetTracker.Callback callback, long maxDatasets)

    try {
      ConfigCatalogInitialization ccInit = new ConfigCatalogInitialization(ConfigCatalogInitialization.ReadMode.always,
              tdsContext.getThreddsDirectory(), null, tracker, allowedServices, callback, -1);
      callback.finish();
      return callback.toString();

    } catch (IOException e) {
      log.error("AdminTrigerController makeReport failed", e);
      return e.getMessage();
    }
  }

}
