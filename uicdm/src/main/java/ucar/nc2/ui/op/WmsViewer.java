/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ui.op;

import ucar.httpservices.*;
import ucar.nc2.constants.CDM;
import ucar.ui.event.ActionValueEvent;
import ucar.ui.event.ActionValueListener;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.SuperComboBox;
import ucar.unidata.util.StringUtil2;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import ucar.nc2.util.IO;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.RootPaneContainer;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.apache.http.Header;
import org.jdom2.input.SAXBuilder;

/**
 * View WMS datasets
 *
 * @author caron
 * @since Feb 17, 2009
 */
public class WmsViewer extends JPanel {

  private Namespace wmsNamespace = Namespace.getNamespace("http://www.opengis.net/wms");

  private PreferencesExt prefs;

  private JPanel imagePanel;
  private SuperComboBox crsChooser, formatChooser, styleChooser, timeChooser, levelChooser;
  private BeanTable ftTable;

  private JSplitPane split;
  private Formatter info = new Formatter();

  private String version;
  private String endpoint;

  private BufferedImage currImage;

  /**
   *
   */
  public WmsViewer(PreferencesExt prefs, RootPaneContainer root) {
    this.prefs = prefs;

    // field choosers
    final JPanel chooserPanel = new JPanel();
    chooserPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    crsChooser = new SuperComboBox(root, "crs", false, null);
    chooserPanel.add(new JLabel("CRS:"));
    chooserPanel.add(crsChooser);
    formatChooser = new SuperComboBox(root, "format", false, null);
    chooserPanel.add(new JLabel("format:"));
    chooserPanel.add(formatChooser);
    styleChooser = new SuperComboBox(root, "style", false, null);
    chooserPanel.add(new JLabel("Style:"));
    chooserPanel.add(styleChooser);
    timeChooser = new SuperComboBox(root, "time", false, null);
    chooserPanel.add(new JLabel("Time:"));
    chooserPanel.add(timeChooser);
    levelChooser = new SuperComboBox(root, "level", false, null);
    chooserPanel.add(new JLabel("Level:"));
    chooserPanel.add(levelChooser);

    // allow time looping
    timeChooser.addActionValueListener(new ActionValueListener() {
      public void actionPerformed(ActionValueEvent e) {
        if (null == timeChooser.getSelectedObject()) {
          return;
        }
        LayerBean ftb = (LayerBean) ftTable.getSelectedBean();
        getMap(ftb);
      }
    });

    final AbstractButton mapButton = BAMutil.makeButtcon("nj22/WorldDetailMap", "getMap", false);
    mapButton.addActionListener(e -> {
      LayerBean ftb = (LayerBean) ftTable.getSelectedBean();
      getMap(ftb);
    });
    chooserPanel.add(mapButton);

    final AbstractButton redrawButton = BAMutil.makeButtcon("alien", "redraw image", false);
    redrawButton.addActionListener(e -> showImage(currImage));
    chooserPanel.add(redrawButton);

    imagePanel = new JPanel();

    ftTable = new BeanTable(LayerBean.class, (PreferencesExt) prefs.node("LayerBeans"), false);
    ftTable.addListSelectionListener(e -> {
      final LayerBean ftb = (LayerBean) ftTable.getSelectedBean();
      styleChooser.setCollection(ftb.styles.iterator());
      timeChooser.setCollection(ftb.times.iterator());
      levelChooser.setCollection(ftb.levels.iterator());
    });
    ftTable.setBeans(new ArrayList());

    split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, ftTable, imagePanel);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(chooserPanel, BorderLayout.NORTH);
    add(split, BorderLayout.CENTER);
  }

  /**
   *
   */
  public boolean setDataset(String version, String endpoint) {
    this.version = version;
    this.endpoint = endpoint;
    return getCapabilities();
  }

  /**
   *
   */
  public String getDetailInfo() {
    return info.toString();
  }

  /**
   *
   */
  public void save() {
    ftTable.saveState(false);
    if (split != null) {
      prefs.putInt("splitPos", split.getDividerLocation());
    }
  }

  /**
   *
   */
  private void showImage(BufferedImage img) {

    //BufferedImage img = ImageIO.read(new File("D:/data/images/labyrinth.jpg"));
    if (img != null) {
      final Graphics g = imagePanel.getGraphics();
      g.drawImage(img, 0, 0, null);
      g.dispose();
      currImage = img;
    } else {
      final Graphics2D g = (Graphics2D) imagePanel.getGraphics();
      g.clearRect(0, 0, getWidth(), getHeight());
      g.dispose();
      currImage = null;
    }
  }

  /**
   *
   */
  private boolean getCapabilities() {

    Formatter f = new Formatter();
    if (endpoint.indexOf("?") > 0) {
      f.format("%s&request=GetCapabilities&service=WMS&version=%s", endpoint, version);
    } else {
      f.format("%s?request=GetCapabilities&service=WMS&version=%s", endpoint, version);
    }
    System.out.printf("getCapabilities request = '%s'%n", f);
    String url = f.toString();
    info = new Formatter();
    info.format("%s%n", url);

    try (HTTPSession session = HTTPFactory.newSession(url);
        HTTPMethod method = HTTPFactory.Get(session, url)) {
      int statusCode = method.execute();

      info.format(" Status = %d %s%n", method.getStatusCode(), method.getStatusText());
      info.format(" Status Line = %s%n", method.getStatusLine());
      printHeaders(" Response Headers", method.getResponseHeaders());
      info.format("GetCapabilities:%n%n");

      if (statusCode == 404) {
        throw new FileNotFoundException(method.getPath() + " " + method.getStatusLine());
      }
      if (statusCode >= 300) {
        throw new IOException(method.getPath() + " " + method.getStatusLine());
      }

      final SAXBuilder builder = new SAXBuilder();
      final Document tdoc = builder.build(method.getResponseAsStream());
      final Element root = tdoc.getRootElement();
      parseGetCapabilities(root);
    } catch (Exception e) {
      info.format("%s%n", e.getMessage());
      JOptionPane.showMessageDialog(this, "Failed " + e.getMessage());
      return false;
    }

    return true;
  }

  /**
   *
   */
  private void parseGetCapabilities(Element root) {
    final Element capElem = root.getChild("Capability", wmsNamespace);
    final Element layer1Elem = capElem.getChild("Layer", wmsNamespace);

    final List<String> crsList = new ArrayList<>(100);
    final List<Element> crs = layer1Elem.getChildren("CRS", wmsNamespace);
    for (Element crsElem : crs) {
      crsList.add(crsElem.getText());
    }
    crsChooser.setCollection(crsList.iterator());

    final Element reqElem = capElem.getChild("Request", wmsNamespace);
    final Element mapElem = reqElem.getChild("GetMap", wmsNamespace);
    final List<String> formatList = new ArrayList<>(100);
    final List<Element> formats = mapElem.getChildren("Format", wmsNamespace);
    for (Element formatElem : formats) {
      formatList.add(formatElem.getText());
    }
    formatChooser.setCollection(formatList.iterator());

    final List<LayerBean> beans = new ArrayList<>(100);
    final Element layer2Elem = layer1Elem.getChild("Layer", wmsNamespace);
    final List<Element> layers = layer2Elem.getChildren("Layer", wmsNamespace);
    for (Element layer3Elem : layers) {
      beans.add(new LayerBean(layer3Elem));
    }
    ftTable.setBeans(beans);
    ftTable.refresh();
  }

  /**
   *
   */
  private boolean getMap(LayerBean layer) {
    final Formatter f = new Formatter();
    f.format("%s?request=GetMap&service=WMS&version=%s&", endpoint, version);
    f.format("layers=%s&CRS=%s&", layer.getName(), layer.getCRS());
    f.format("bbox=%s,%s,%s,%s&", layer.getMinx(), layer.getMiny(), layer.getMaxx(),
        layer.getMaxy());
    f.format("width=500&height=500&");
    f.format("styles=%s&", styleChooser.getSelectedObject());
    f.format("format=%s&", formatChooser.getSelectedObject());
    if (layer.hasTime) {
      f.format("time=%s&", timeChooser.getSelectedObject());
    }
    if (layer.hasLevel) {
      f.format("elevation=%s&", levelChooser.getSelectedObject());
    }
    String url = f.toString();
    info.format("%s%n", url);

    try {
      try (HTTPMethod method = HTTPFactory.Get(url)) {
        int statusCode = method.execute();

        info.format(" Status = %d %s%n", method.getStatusCode(), method.getStatusText());
        info.format(" Status Line = %s%n", method.getStatusLine());
        printHeaders(" Response Headers", method.getResponseHeaders());

        if (statusCode == 404) {
          throw new FileNotFoundException(method.getPath() + " " + method.getStatusLine());
        }

        if (statusCode >= 300) {
          throw new IOException(method.getPath() + " " + method.getStatusLine());
        }

        Header h = method.getResponseHeader("Content-Type");
        String mimeType = (h == null) ? "" : h.getValue();
        info.format(" mimeType = %s%n", mimeType);

        try (InputStream isFromHttp = method.getResponseBodyAsStream()) {
          byte[] contents = IO.readContentsToByteArray(isFromHttp);
          info.format(" content len = %s%n", contents.length);

          ByteArrayInputStream is = new ByteArrayInputStream(contents);

          BufferedImage img = ImageIO.read(is);
          showImage(img);

          if (img == null) {
            info.format("getMap:%n%n");
            if (mimeType.equals("application/vnd.google-earth.kmz")) {
              final File temp = File.createTempFile("Temp", ".kmz");
              // File temp = new File("C:/temp/temp.kmz");
              IO.writeToFile(contents, temp);
              contents = null;

              try (ZipFile zfile = new ZipFile(temp)) {
                final Enumeration entries = zfile.entries();
                while (entries.hasMoreElements()) {
                  final ZipEntry entry = (ZipEntry) entries.nextElement();
                  info.format(" entry= %s%n", entry);
                  if (entry.getName().endsWith(".kml")) {
                    try (InputStream kml = zfile.getInputStream(entry)) {
                      contents = IO.readContentsToByteArray(kml);
                    }
                  }
                }
              }
            }

            if (contents != null) {
              info.format("%s%n", new String(contents, CDM.utf8Charset));
            }
          }
        }
      }
    } catch (IOException e) {
      info.format("%s%n", e.getMessage());
      JOptionPane.showMessageDialog(this, "Failed " + e.getMessage());
      return false;
    }

    return true;
  }

  /**
   *
   */
  private void printHeaders(String title, Header[] heads) {
    info.format("%s%n", title);
    for (Header head : heads) {
      info.format("%s ", head.toString());
    }
    info.format("%n");
  }

  /**
   *
   */
  public class LayerBean {

    String name;
    String title;
    String CRS;

    String minx;
    String maxx;
    String miny;
    String maxy;

    boolean hasTime, hasLevel;

    List<String> styles = new ArrayList<>();
    List<String> levels = new ArrayList<>();
    List<String> times = new ArrayList<>();

    /**
     * no-arg constructor
     */
    public LayerBean() {
    }

    /**
     *
     */
    LayerBean(Element layer3Elem) {
      this.name = getVal(layer3Elem, "Name");
      this.title = getVal(layer3Elem, "Title");

      Element bbElem = layer3Elem.getChild("BoundingBox", wmsNamespace);
      this.CRS = bbElem.getAttributeValue("CRS");
      this.minx = bbElem.getAttributeValue("minx");
      this.maxx = bbElem.getAttributeValue("maxx");
      this.miny = bbElem.getAttributeValue("miny");
      this.maxy = bbElem.getAttributeValue("maxy");

      for (Element elem : layer3Elem.getChildren("Style", wmsNamespace)) {
        final Element nameElem = elem.getChild("Name", wmsNamespace);
        styles.add(nameElem.getText());
      }

      for (Element elem : layer3Elem.getChildren("Dimension", wmsNamespace)) {
        String name = elem.getAttributeValue("name");
        if (name.equals("time")) {
          String[] st = elem.getText().split(",");
          for (String s : st) {
            times.add(StringUtil2.removeWhitespace(s));
          }
          hasTime = (times.size() > 0);
        }
        if (name.equals("elevation")) {
          final String[] st = elem.getText().split(",");
          for (String s : st) {
            levels.add(StringUtil2.removeWhitespace(s));
          }
          hasLevel = (levels.size() > 0);
        }
      }
    }

    /**
     *
     */
    String getVal(Element parent, String name) {
      Element elem = parent.getChild(name, wmsNamespace);
      return (name == null) ? "" : elem.getText();
    }

    /**
     *
     */
    List<String> getVals(Element parent, String name) {
      final List<String> result = new ArrayList<>(10);
      for (Element elem : parent.getChildren(name, wmsNamespace)) {
        result.add(elem.getText());
      }
      return result;
    }

    /**
     *
     */
    public String getName() {
      return name;
    }

    public String getTitle() {
      return title;
    }

    public String getCRS() {
      return CRS;
    }

    public String getMinx() {
      return minx;
    }

    public String getMaxx() {
      return maxx;
    }

    public String getMiny() {
      return miny;
    }

    public String getMaxy() {
      return maxy;
    }
  }
}
