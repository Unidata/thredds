/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.client.catalog.writer;

import thredds.client.catalog.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDuration;
import ucar.unidata.util.StringUtil2;

import java.net.URI;

/**
 * Describe
 *
 * @author caron
 * @since 1/8/2015
 */
public class DatasetHtmlWriter {

  public void writeHtmlDescription(StringBuilder buff, Dataset ds,
                                   boolean complete, boolean isServer,
                                   boolean datasetEvents, boolean catrefEvents,
                                   boolean resolveRelativeUrls) {

  }

  /**
   * Write an Html representation of the given dataset.
   * <p> With datasetEvents, catrefEvents = true, this is used to construct an HTML page on the client
   * (eg using HtmlPage); the client then detects URL clicks and processes.
   * <p> With datasetEvents, catrefEvents = false, this is used to construct an HTML page on the server.
   * (eg using HtmlPage); the client then detects URL clicks and processes.
   *
   * @param buff          put HTML here.
   * @param ds            the dataset.
   * @param complete      if true, add HTML header and ender so its a complete, valid HTML page.
   * @param isServer      if true, then we are in the thredds data server, so do the following: <ul>
   *                      <li> append "html" to DODS Access URLs
   *                      </ul>
   * @param datasetEvents if true, prepend "dataset:" to any dataset access URLS
   * @param catrefEvents  if true, prepend "catref:" to any catref URLS
   *

  public void writeHtmlDescription(StringBuilder buff, Dataset ds,
                                   boolean complete, boolean isServer,
                                   boolean datasetEvents, boolean catrefEvents,
                                   boolean resolveRelativeUrls) {

    if (ds == null) return;

    if (complete) {
      buff.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n")
              .append("        \"http://www.w3.org/TR/html4/loose.dtd\">\n")
              .append("<html>\n");
      buff.append("<head>");
      buff.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
      buff.append("</head>");
      buff.append("<body>\n");
    }

    buff.append("<h2>Dataset: ").append(ds.getFullName()).append("</h2>\n<ul>\n");
    if ((ds.getDataFormatType() != null) && (ds.getDataFormatType() != DataFormatType.NONE))
      buff.append(" <li><em>Data format: </em>").append(StringUtil2.quoteHtmlContent(ds.getDataFormatType().toString())).append("</li>\n");

    if ((ds.getDataSize() != 0.0) && !Double.isNaN(ds.getDataSize()))
      buff.append(" <li><em>Data size: </em>").append(Format.formatByteSize(ds.getDataSize())).append("</li>\n");

    if ((ds.getFeatureType() != null) && (ds.getFeatureType() != FeatureType.ANY) && (ds.getFeatureType() != FeatureType.NONE))
      buff.append(" <li><em>Data type: </em>").append(StringUtil2.quoteHtmlContent(ds.getFeatureType().toString())).append("</li>\n");

    if (ds.getCollectionType() != null)
      buff.append(" <li><em>Collection type: </em>").append(StringUtil2.quoteHtmlContent(ds.getCollectionType())).append("</li>\n");

    if (ds.isHarvest())
      buff.append(" <li><em>Harvest: </em>").append(ds.isHarvest()).append("</li>\n");

    if (ds.getAuthority() != null)
      buff.append(" <li><em>Naming Authority: </em>").append(StringUtil2.quoteHtmlContent(ds.getAuthority())).append("</li>\n");

    if (ds.getId() != null)
      buff.append(" <li><em>ID: </em>").append(StringUtil2.quoteHtmlContent(ds.getId())).append("</li>\n");

    if (ds.getRestrictAccess() != null)
      buff.append(" <li><em>RestrictAccess: </em>").append(StringUtil2.quoteHtmlContent(ds.getRestrictAccess())).append("</li>\n");

    if (ds instanceof CatalogRef) {
      CatalogRef catref = (CatalogRef) ds;
      String href = resolveRelativeUrls || catrefEvents
              ? resolve(ds, catref.getXlinkHref())
              : catref.getXlinkHref();
      if (catrefEvents) href = "catref:" + href;
      buff.append(" <li><em>CatalogRef: </em>").append(makeHref(href, null)).append("</li>\n");
    }

    buff.append("</ul>\n");

    java.util.List<InvDocumentation> docs = ds.getDocumentation();
    if (docs.size() > 0) {
      buff.append("<h3>Documentation:</h3>\n<ul>\n");
      for (InvDocumentation doc : docs) {
        String type = (doc.getType() == null) ? "" : "<strong>" + StringUtil2.quoteHtmlContent(doc.getType()) + ":</strong> ";
        String inline = doc.getInlineContent();
        if ((inline != null) && (inline.length() > 0))
          buff.append(" <li>").append(type).append(StringUtil2.quoteHtmlContent(inline)).append("</li>\n");
        if (doc.hasXlink()) {
          // buff.append(" <li>" + type + makeHrefResolve(ds, url.toString(), doc.getXlinkTitle()) + "</a>\n");
          buff.append(" <li>").append(type).append(makeHref(doc.getXlinkHref(), doc.getXlinkTitle())).append("</li>\n");
        }
      }
      buff.append("</ul>\n");
    }

    java.util.List<Access> access = ds.getAccess();
    if (access.size() > 0) {
      buff.append("<h3>Access:</h3>\n<ol>\n");
      for (Access a : access) {
        Service s = a.getService();
        String urlString = resolveRelativeUrls || datasetEvents
                ? a.getStandardUrlName()
                : a.getUnresolvedUrlName();
        String fullUrlString = urlString;
        if (datasetEvents) fullUrlString = "dataset:" + fullUrlString;
        if (isServer) {
          ServiceType stype = s.getType();
          if ((stype == ServiceType.OPENDAP) || (stype == ServiceType.DODS))
            fullUrlString = fullUrlString + ".html";
          else if (stype == ServiceType.DAP4)
            fullUrlString = fullUrlString + ".dmr.xml";
          else if (stype == ServiceType.WCS)
            fullUrlString = fullUrlString + "?service=WCS&version=1.0.0&request=GetCapabilities";
          else if (stype == ServiceType.WMS)
            fullUrlString = fullUrlString + "?service=WMS&version=1.3.0&request=GetCapabilities";
            //NGDC update 8/18/2011
          else if (stype == ServiceType.NCML || stype == ServiceType.UDDC || stype == ServiceType.ISO) {
            String catalogUrl = ds.getCatalogUrl();
            String datasetId = ds.id;
            if (catalogUrl.indexOf('#') > 0)
              catalogUrl = catalogUrl.substring(0, catalogUrl.lastIndexOf('#'));
            try {
              catalogUrl = URLEncoder.encode(catalogUrl, "UTF-8");
              datasetId = URLEncoder.encode(datasetId, "UTF-8");
            } catch (UnsupportedEncodingException e) {
              e.printStackTrace();
            }
            fullUrlString = fullUrlString + "?catalog=" + catalogUrl + "&dataset=" + datasetId;
          } else if (stype == ServiceType.NetcdfSubset)
            fullUrlString = fullUrlString + "/dataset.html";
          else if ((stype == ServiceType.CdmRemote) || (stype == ServiceType.CdmrFeature))
            fullUrlString = fullUrlString + "?req=form";
        }
        buff.append(" <li> <b>").append(StringUtil2.quoteHtmlContent(s.getServiceType().toString()));
        buff.append(":</b> ").append(makeHref(fullUrlString, urlString)).append("</li>\n");
      }
      buff.append("</ol>\n");
    }

    java.util.List<Metadata.Contributor> contributors = ds.getContributors();
    if (contributors.size() > 0) {
      buff.append("<h3>Contributors:</h3>\n<ul>\n");
      for (Metadata.Contributor t : contributors) {
        String role = (t.getRole() == null) ? "" : "<strong> (" + StringUtil2.quoteHtmlContent(t.getRole()) + ")</strong> ";
        buff.append(" <li>").append(StringUtil2.quoteHtmlContent(t.getName())).append(role).append("</li>\n");
      }
      buff.append("</ul>\n");
    }

    java.util.List<Metadata.Vocab> keywords = ds.getKeywords();
    if (keywords.size() > 0) {
      buff.append("<h3>Keywords:</h3>\n<ul>\n");
      for (Metadata.Vocab t : keywords) {
        String vocab = (t.getVocabulary() == null) ? "" : " <strong>(" + StringUtil2.quoteHtmlContent(t.getVocabulary()) + ")</strong> ";
        buff.append(" <li>").append(StringUtil2.quoteHtmlContent(t.getText())).append(vocab).append("</li>\n");
      }
      buff.append("</ul>\n");
    }

    java.util.List<DateType> dates = ds.getDates();
    if (dates.size() > 0) {
      buff.append("<h3>Dates:</h3>\n<ul>\n");
      for (DateType d : dates) {
        String type = (d.getType() == null) ? "" : " <strong>(" + StringUtil2.quoteHtmlContent(d.getType()) + ")</strong> ";
        buff.append(" <li>").append(StringUtil2.quoteHtmlContent(d.getText())).append(type).append("</li>\n");
      }
      buff.append("</ul>\n");
    }

    java.util.List<Metadata.Vocab> projects = ds.getProjects();
    if (projects.size() > 0) {
      buff.append("<h3>Projects:</h3>\n<ul>\n");
      for (Metadata.Vocab t : projects) {
        String vocab = (t.getVocabulary() == null) ? "" : " <strong>(" + StringUtil2.quoteHtmlContent(t.getVocabulary()) + ")</strong> ";
        buff.append(" <li>").append(StringUtil2.quoteHtmlContent(t.getText())).append(vocab).append("</li>\n");
      }
      buff.append("</ul>\n");
    }

    java.util.List<Metadata.Source> creators = ds.getCreators();
    if (creators.size() > 0) {
      buff.append("<h3>Creators:</h3>\n<ul>\n");
      for (Metadata.Source t : creators) {
        buff.append(" <li><strong>").append(StringUtil2.quoteHtmlContent(t.getName())).append("</strong><ul>\n");
        buff.append(" <li><em>email: </em>").append(StringUtil2.quoteHtmlContent(t.getEmail())).append("</li>\n");
        if (t.getUrl() != null) {
          String newUrl = resolveRelativeUrls
                  ? makeHrefResolve(ds, t.getUrl(), null)
                  : makeHref(t.getUrl(), null);
          buff.append(" <li> <em>").append(newUrl).append("</em></li>\n");
        }
        buff.append(" </ul></li>\n");
      }
      buff.append("</ul>\n");
    }

    java.util.List<Metadata.Source> publishers = ds.getPublishers();
    if (publishers.size() > 0) {
      buff.append("<h3>Publishers:</h3>\n<ul>\n");
      for (Metadata.Source t : publishers) {
        buff.append(" <li><strong>").append(StringUtil2.quoteHtmlContent(t.getName())).append("</strong><ul>\n");
        buff.append(" <li><em>email: </em>").append(StringUtil2.quoteHtmlContent(t.getEmail())).append("\n");
        if (t.getUrl() != null) {
          String urlLink = resolveRelativeUrls
                  ? makeHrefResolve(ds, t.getUrl(), null)
                  : makeHref(t.getUrl(), null);
          buff.append(" <li> <em>").append(urlLink).append("</em>\n");
        }
        buff.append(" </ul>\n");
      }
      buff.append("</ul>\n");
    }
 
     /*
     4.2:
     <h3>Variables:</h3>
     <ul>
     <li><em>Vocabulary</em> [DIF]:
     <ul>
      <li><strong>Reflectivity</strong> =  <i></i> = EARTH SCIENCE &gt; Spectral/Engineering &gt; Radar &gt; Radar Reflectivity (db)
      <li><strong>Velocity</strong> =  <i></i> = EARTH SCIENCE &gt; Spectral/Engineering &gt; Radar &gt; Doppler Velocity (m/s)
      <li><strong>SpectrumWidth</strong> =  <i></i> = EARTH SCIENCE &gt; Spectral/Engineering &gt; Radar &gt; Doppler Spectrum Width (m/s)
      </ul>
      </ul>
     </ul>
 
     4.3:
     <h3>Variables:</h3>
     <ul>
     <li><em>Vocabulary</em> [CF-1.0]:
     <ul>
      <li><strong>d3d (meters) </strong> =  <i>3D Depth at Nodes
     <p>        </i> = depth_at_nodes
      <li><strong>depth (meters) </strong> =  <i>Bathymetry</i> = depth
      <li><strong>eta (m) </strong> =  <i></i> =
      <li><strong>temp (Celsius) </strong> =  <i>Temperature
     <p>        </i> = sea_water_temperature
      <li><strong>u (m/s) </strong> =  <i>Eastward Water
     <p>          Velocity
     <p>        </i> = eastward_sea_water_velocity
      <li><strong>v (m/s) </strong> =  <i>Northward Water
     <p>          Velocity
     <p>        </i> = northward_sea_water_velocity
     </ul>
     </ul>
      */
     /*
    java.util.List<Metadata.Variables> vars = ds.getVariables();
    if (vars.size() > 0) {
      buff.append("<h3>Variables:</h3>\n<ul>\n");
      for (Metadata.Variables t : vars) {

        buff.append("<li><em>Vocabulary</em> [");
        if (t.getVocabUri() != null) {
          URI uri = t.getVocabUri();
          String vocabLink = resolveRelativeUrls
                  ? makeHrefResolve(ds, uri.toString(), t.getVocabulary())
                  : makeHref(uri.toString(), t.getVocabulary());
          buff.append(vocabLink);
        } else {
          buff.append(StringUtil2.quoteHtmlContent(t.getVocabulary()));
        }
        buff.append("]:\n<ul>\n");

        java.util.List<Metadata.Variable> vlist = t.getVariableList();
        if (vlist.size() > 0) {
          for (Metadata.Variable v : vlist) {
            String units = (v.getUnits() == null || v.getUnits().length() == 0) ? "" : " (" + v.getUnits() + ") ";
            buff.append(" <li><strong>").append(StringUtil2.quoteHtmlContent(v.getName() + units)).append("</strong> = ");
            String desc = (v.getDescription() == null) ? "" : " <i>" + StringUtil2.quoteHtmlContent(v.getDescription()) + "</i> = ";
            buff.append(desc);
            if (v.getVocabularyName() != null)
              buff.append(StringUtil2.quoteHtmlContent(v.getVocabularyName()));
            buff.append("\n");
          }
        }
        buff.append("</ul>\n");
      }
      buff.append("</ul>\n");
    }
    if (ds.getVariableMapLink() != null) {
      buff.append("<h3>Variables:</h3>\n");
      buff.append("<ul><li>" + makeHref(ds.getVariableMapLink(), "VariableMap") + "</li></ul>\n");
    }

    Metadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
    if ((gc != null) && !gc.isEmpty()) {
      buff.append("<h3>GeospatialCoverage:</h3>\n<ul>\n");
      if (gc.isGlobal())
        buff.append(" <li><em> Global </em>\n");

      buff.append(" <li><em> Longitude: </em> ").append(rangeString(gc.getEastWestRange())).append("</li>\n");
      buff.append(" <li><em> Latitude: </em> ").append(rangeString(gc.getNorthSouthRange())).append("</li>\n");
      if (gc.getUpDownRange() != null) {
        buff.append(" <li><em> Altitude: </em> ").append(rangeString(gc.getUpDownRange())).append(" (positive is <strong>").append(StringUtil2.quoteHtmlContent(gc.getZPositive())).append(")</strong></li>\n");
      }

      java.util.List<Metadata.Vocab> nlist = gc.getNames();
      if ((nlist != null) && (nlist.size() > 0)) {
        buff.append(" <li><em>  Names: </em> <ul>\n");
        for (Metadata.Vocab elem : nlist) {
          buff.append(" <li>").append(StringUtil2.quoteHtmlContent(elem.getText())).append("\n");
        }
        buff.append(" </ul>\n");
      }
      buff.append(" </ul>\n");
    }

    CalendarDateRange tc = ds.getCalendarDateCoverage();
    if (tc != null) {
      buff.append("<h3>TimeCoverage:</h3>\n<ul>\n");
      CalendarDate start = tc.getStart();
      if (start != null)
        buff.append(" <li><em>  Start: </em> ").append(start.toString()).append("\n");
      CalendarDate end = tc.getEnd();
      if (end != null) {
        buff.append(" <li><em>  End: </em> ").append(end.toString()).append("\n");
      }
      CalendarDuration duration = tc.getDuration();
      if (duration != null)
        buff.append(" <li><em>  Duration: </em> ").append(StringUtil2.quoteHtmlContent(duration.toString())).append("\n");
      CalendarDuration resolution = tc.getResolution();
      if (resolution != null) {
        buff.append(" <li><em>  Resolution: </em> ").append(StringUtil2.quoteHtmlContent(resolution.toString())).append("\n");
      }
      buff.append(" </ul>\n");
    }

    java.util.List<InvMetadata> metadata = ds.getMetadata();
    boolean gotSomeMetadata = false;
    for (InvMetadata m : metadata) {
      if (m.hasXlink()) gotSomeMetadata = true;
    }

    if (gotSomeMetadata) {
      buff.append("<h3>Metadata:</h3>\n<ul>\n");
      for (InvMetadata m : metadata) {
        String type = (m.getMetadataType() == null) ? "" : m.getMetadataType();
        if (m.hasXlink()) {
          String title = (m.getXlinkTitle() == null) ? "Type " + type : m.getXlinkTitle();
          String mdLink = resolveRelativeUrls
                  ? makeHrefResolve(ds, m.getXlinkHref(), title)
                  : makeHref(m.getXlinkHref(), title);
          buff.append(" <li> ").append(mdLink).append("\n");
        } //else {
        //buff.append(" <li> <pre>"+m.getMetadataType()+" "+m.getContentObject()+"</pre>\n");
        //}
      }
      buff.append("</ul>\n");
    }

    java.util.List<InvProperty> propsOrg = ds.getProperties();
    java.util.List<InvProperty> props = new ArrayList<>(ds.getProperties().size());
    for (InvProperty p : propsOrg) {
      if (!p.getName().startsWith("viewer"))  // eliminate the viewer properties from the html view
        props.add(p);
    }
    if (props.size() > 0) {
      buff.append("<h3>Properties:</h3>\n<ul>\n");
      for (InvProperty p : props) {
        if (p.getName().equals("attachments")) // LOOK whats this ?
        {
          String attachLink = resolveRelativeUrls
                  ? makeHrefResolve(ds, p.getValue(), p.getName())
                  : makeHref(p.getValue(), p.getName());
          buff.append(" <li>").append(attachLink).append("\n");
        } else {
          buff.append(" <li>").append(StringUtil2.quoteHtmlContent(p.getName() + " = \"" + p.getValue())).append("\"\n");
        }
      }
      buff.append("</ul>\n");
    }

    if (complete) buff.append("</body></html>");
  }   */

  /* private String rangeString(Metadata.Range r) {
    if (r == null) return "";
    String units = (r.getUnits() == null) ? "" : " " + r.getUnits();
    String resolution = r.hasResolution() ? " Resolution=" + r.getResolution() : "";
    return StringUtil2.quoteHtmlContent(r.getStart() + " to " + (r.getStart() + r.getSize()) + resolution + units);
  }

  /**
   * resolve reletive URLS against the catalog URL.
   *
   * @param ds   use ds parent catalog, if it exists
   * @param href URL to resolve
   * @return resolved URL
   *
  public String resolve(Dataset ds, String href) {
    Catalog cat = ds.getParentCatalog();
    if (cat != null) {
      try {
        java.net.URI uri = cat.resolveUri(href);
        href = uri.toString();
      } catch (java.net.URISyntaxException e) {
        logger.warn("Dataset.writeHtml: error parsing URL= " + href);
      }
    }
    return href;
  }

  private String makeHref(String href, String title) {
    if (title == null) title = href;
    return "<a href='" + StringUtil2.quoteHtmlContent(href) + "'>" + StringUtil2.quoteHtmlContent(title) + "</a>";
  }

  private String makeHrefResolve(Dataset ds, String href, String title) {
    if (title == null) title = href;
    href = resolve(ds, href);
    return makeHref(href, title);
  }  */
}
