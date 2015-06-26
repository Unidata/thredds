/* Copyright */
package thredds.server.admin;

import com.coverity.security.Escape;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import thredds.core.ConfigCatalogInitialization;
import thredds.core.DataRootManager;
import thredds.core.DatasetManager;
import thredds.featurecollection.CollectionUpdater;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.InvDatasetFeatureCollection;
import thredds.server.catalog.FeatureCollectionRef;
import thredds.server.config.TdsContext;
import thredds.util.ContentType;
import ucar.unidata.util.StringUtil2;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

/**
 * Describe
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
    private ConfigCatalogInitialization catInit;

    @PostConstruct
    public void afterPropertiesSet() {

      DebugCommands.Category debugHandler = debugCommands.findCategory("Catalogs");
      DebugCommands.Action act;

      act = new DebugCommands.Action("reinit", "Read all catalogs") {
        public void doAction(DebugCommands.Event e) {
          // look background thread
          catInit.reread(ConfigCatalogInitialization.ReadMode.always, false);
          e.pw.printf("<p/>Reading all catalogs%n");
        }
      };
      debugHandler.addAction(act);

      act = new DebugCommands.Action("recheck", "Read changed catalogs") {
        public void doAction(DebugCommands.Event e) {
          // look background thread
          catInit.reread(ConfigCatalogInitialization.ReadMode.check, false);
          e.pw.printf("<p/>Reading changed catalogs%n");
        }
      };
      debugHandler.addAction(act);
    }

    @RequestMapping(value = "/catalog", method = RequestMethod.GET, params = "req=all")
    protected ResponseEntity<String> handleReadAll() throws Exception {
      catInit.reread(ConfigCatalogInitialization.ReadMode.always, false);

      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.set(ContentType.HEADER, ContentType.text.getContentHeader());
      String result = "Reading all catalogs";
      return new ResponseEntity<>(result, responseHeaders, HttpStatus.OK);
    }


  @RequestMapping(value = "/catalog", method = RequestMethod.GET, params = "req=changed")
  protected ResponseEntity<String> handleReadChanged() throws Exception {
    catInit.reread(ConfigCatalogInitialization.ReadMode.check, false);

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set(ContentType.HEADER, ContentType.text.getContentHeader());
    String result = "Reading changed catalogs";
    return new ResponseEntity<>(result, responseHeaders, HttpStatus.OK);
  }

}
