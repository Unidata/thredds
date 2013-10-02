package thredds.gribtables;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import thredds.bufrtables.FileValidateBean;
import ucar.nc2.iosp.bufr.Message;
import ucar.nc2.iosp.bufr.MessageScanner;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.io.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Description
 *
 * @author John
 * @since 7/23/11
 */
public class StandardWmoController extends AbstractController {

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StandardWmoController.class);

  private String filename;

  private List<WMoTable> tableList;
  private class WMoTable {
    String type;
    String version;
    String filename;

    private WMoTable(String type, String version, String filename) {
      this.type = type;
      this.version = version;
      this.filename = filename;
    }
  }

  public void setTableList(String filename) {
    this.filename = filename;
  }

  private void initList() {
    String rootPath = getServletContext().getRealPath("/"); // dont have context until now
    tableList = new ArrayList<WMoTable>();
    try {
      InputStream is = new FileInputStream(rootPath + "/" + filename);
      DataInputStream dis = new DataInputStream(is);
      while (true) {
        String line = dis.readLine();
        if (line == null) break;
        System.out.printf(" %s%n",line);
        String[] tokes = line.split(" ");
        tableList.add(new WMoTable(tokes[0], tokes[1], tokes[2]));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    if (tableList == null) initList();
    String path = req.getPathInfo();
    System.out.println(" StandardWmoController.handleRequestInternal path= " + path);

    try {
      Document doc = makeXml(path);
      return new ModelAndView("xsltView", "source", new JDOMSource(doc));
    } catch (Exception e) {
      log.warn("Exception on file " + path, e);
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, "File=" + path);
      return null;
    }
  }

  protected Document makeXml(String path) throws Exception {
    Element rootElem = new Element("bufrValidation");
    Document doc = new Document(rootElem);
    rootElem.setAttribute("path", path);
    return doc;
  }
}
