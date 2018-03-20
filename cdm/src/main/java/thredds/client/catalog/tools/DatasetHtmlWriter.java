/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog.tools;

import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;
import com.google.common.net.UrlEscapers;
import thredds.client.catalog.*;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.units.TimeDuration;
import ucar.unidata.util.Format;

import java.util.ArrayList;
import java.util.Formatter;

/**
 * Create Html from a dataset
 *
 * @author caron
 * @since 1/8/2015
 */
public class DatasetHtmlWriter {
  private Escaper htmlEscaper = HtmlEscapers.htmlEscaper();
  private Escaper urlPathEscaper = UrlEscapers.urlPathSegmentEscaper();
  private Escaper urlParamEscaper = UrlEscapers.urlFormParameterEscaper();

  /**
   * Write an Html representation of the given dataset.
   * <p> With datasetEvents, catrefEvents = true, this is used to construct an HTML page on the client
   * (eg using HtmlPage); the client then detects URL clicks and processes.
   * <p> With datasetEvents, catrefEvents = false, this is used to construct an HTML page on the server.
   * (eg using HtmlPage); the client then detects URL clicks and processes.
   *
   * @param out           put HTML here.
   * @param ds            the dataset.
   * @param complete      if true, add HTML header and ender so its a complete, valid HTML page.
   * @param isServer      if true, then we are in the thredds data server, so do the following: <ul>
   *                      <li> append "html" to DODS Access URLs
   *                      </ul>
   * @param datasetEvents if true, prepend "dataset:" to any dataset access URLS
   * @param catrefEvents  if true, prepend "catref:" to any catref URLS
   */

  public void writeHtmlDescription(Formatter out, Dataset ds,
                                   boolean complete, boolean isServer,
                                   boolean datasetEvents, boolean catrefEvents,
                                   boolean resolveRelativeUrls) {

    if (ds == null) return;

    if (complete) {
      out.format("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"%n");
      out.format("        \"http://www.w3.org/TR/html4/loose.dtd\">%n");
      out.format("<html>%n");
      out.format("<head>%n");
      out.format("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">%n");
      out.format("</head>%n");
      out.format("<body>%n");
    }

    out.format("<h2>Dataset: %s</h2>%n<ul>", ds.getName());
    if (ds.getDataFormatName() != null)
      out.format(" <li><em>Data format: </em>%s</li>%n", htmlEscaper.escape(ds.getDataFormatName()));

    if ((ds.getDataSize() > 0))
      out.format(" <li><em>Data size: </em>%s</li>%n", Format.formatByteSize(ds.getDataSize()));

    if (ds.getFeatureTypeName() != null)
      out.format(" <li><em>Feature type: </em>%s</li>%n", htmlEscaper.escape(ds.getFeatureTypeName()));

    if (ds.getCollectionType() != null)
      out.format(" <li><em>Collection type: </em>%s</li>%n", htmlEscaper.escape(ds.getCollectionType()));

    if (ds.isHarvest())
      out.format(" <li><em>Harvest:</em> true</li>%n");

    if (ds.getAuthority() != null)
      out.format(" <li><em>Naming Authority: </em>%s</li>%n%n", htmlEscaper.escape(ds.getAuthority()));

    if (ds.getId() != null)
      out.format(" <li><em>ID: </em>%s</li>%n", htmlEscaper.escape(ds.getId()));

    if (ds.getRestrictAccess() != null)
      out.format(" <li><em>RestrictAccess: </em>%s</li>%n", htmlEscaper.escape(ds.getRestrictAccess()));

    if (ds instanceof CatalogRef) {
      CatalogRef catref = (CatalogRef) ds;
      String href = resolveRelativeUrls | catrefEvents ? resolve(ds, catref.getXlinkHref()) : catref.getXlinkHref();
      if (catrefEvents) href = "catref:" + href;
      out.format(" <li><em>CatalogRef: </em>%s</li>%n", makeHref(href, null, null));
    }

    out.format("</ul>%n");

    java.util.List<Documentation> docs = ds.getDocumentation();
    if (docs.size() > 0) {
      out.format("<h3>Documentation:</h3>%n<ul>%n");
      for (Documentation doc : docs) {
        String type = (doc.getType() == null) ? "" : "<strong>" + htmlEscaper.escape(doc.getType()) + ":</strong> ";
        String inline = doc.getInlineContent();
        if ((inline != null) && (inline.length() > 0))
          out.format(" <li>%s %s</li>%n", type, htmlEscaper.escape(inline));
        if (doc.hasXlink()) {
          out.format(" <li>%s %s</li>%n", type, makeHref(doc.getXlinkHref(), null, doc.getXlinkTitle()));
        }
      }
      out.format("</ul>%n");
    }

    java.util.List<Access> access = ds.getAccess();
    if (access.size() > 0) {
      out.format("<h3>Access:</h3>%n<ol>%n");
      for (Access a : access) {
        Service s = a.getService();
        String urlString = resolveRelativeUrls || datasetEvents ? a.getStandardUrlName() : a.getUnresolvedUrlName();
        String queryString = null;
        // String fullUrlString = urlString;
        if (datasetEvents) urlString = "dataset:" + urlString;

        ServiceType stype = s.getType();
        if (isServer && stype != null)
          switch (stype) {
            case OPENDAP:
            case DODS:
              urlString = urlString + ".html";
              break;

            case DAP4:
              urlString = urlString + ".dmr.xml";
              break;

            case WCS:
              queryString = "service=WCS&version=1.0.0&request=GetCapabilities";
              break;

            case WMS:
              queryString = "service=WMS&version=1.3.0&request=GetCapabilities";
              break;

            case NCML:
            case UDDC:
            case ISO:
              String catalogUrl = ds.getCatalogUrl();
              String datasetId = ds.getId();
              if (catalogUrl != null && datasetId != null) {
                if (catalogUrl.indexOf('#') > 0)
                  catalogUrl = catalogUrl.substring(0, catalogUrl.lastIndexOf('#'));
                /* try {
                  catalogUrl = URLEncoder.encode(catalogUrl, "UTF-8");
                  datasetId = URLEncoder.encode(datasetId, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                  e.printStackTrace();
                } */
                queryString = "catalog=" + urlParamEscaper.escape(catalogUrl) + "&dataset=" + urlParamEscaper.escape(datasetId);
              }
              break;

            case NetcdfSubset:
              urlString = urlString + "/dataset.html";
              break;

            case CdmRemote:
              queryString = "req=cdl";
              break;
            case CdmrFeature:
              queryString = "req=form";
          }
        out.format(" <li> <b>%s: </b>%s</li>%n", s.getServiceTypeName(), makeHref(urlString, queryString, null));
      }
      out.format("</ol>%n");
    }

    java.util.List<ThreddsMetadata.Contributor> contributors = ds.getContributors();
    if (contributors.size() > 0) {
      out.format("<h3>Contributors:</h3>%n<ul>%n");
      for (ThreddsMetadata.Contributor t : contributors) {
        String role = (t.getRole() == null) ? "" : "<strong> (" + htmlEscaper.escape(t.getRole()) + ")</strong> ";
        out.format(" <li>%s %s</li>%n", htmlEscaper.escape(t.getName()), role);
      }
      out.format("</ul>%n");
    }

    java.util.List<ThreddsMetadata.Vocab> keywords = ds.getKeywords();
    if (keywords.size() > 0) {
      out.format("<h3>Keywords:</h3>%n<ul>%n");
      for (ThreddsMetadata.Vocab t : keywords) {
        String vocab = (t.getVocabulary() == null) ? "" : " <strong>(" + htmlEscaper.escape(t.getVocabulary()) + ")</strong> ";
        out.format(" <li>%s %s</li>%n", htmlEscaper.escape(t.getText()), vocab);
      }
      out.format("</ul>%n");
    }

    java.util.List<DateType> dates = ds.getDates();
    if (dates.size() > 0) {
      out.format("<h3>Dates:</h3>%n<ul>%n");
      for (DateType d : dates) {
        String type = (d.getType() == null) ? "" : " <strong>(" + htmlEscaper.escape(d.getType()) + ")</strong> ";
        out.format(" <li>%s %s</li>%n", htmlEscaper.escape(d.getText()), type);
      }
      out.format("</ul>%n");
    }

    java.util.List<ThreddsMetadata.Vocab> projects = ds.getProjects();
    if (projects.size() > 0) {
      out.format("<h3>Projects:</h3>%n<ul>%n");
      for (ThreddsMetadata.Vocab t : projects) {
        String vocab = (t.getVocabulary() == null) ? "" : " <strong>(" + htmlEscaper.escape(t.getVocabulary()) + ")</strong> ";
        out.format(" <li>%s %s</li>%n", htmlEscaper.escape(t.getText()), vocab);
      }
      out.format("</ul>%n");
    }

    java.util.List<ThreddsMetadata.Source> creators = ds.getCreators();
    if (creators.size() > 0) {
      out.format("<h3>Creators:</h3>%n<ul>%n");
      for (ThreddsMetadata.Source t : creators) {
        out.format(" <li><strong>%s</strong><ul>%n", htmlEscaper.escape(t.getName()));
        out.format(" <li><em>email: </em>%s</li>%n", htmlEscaper.escape(t.getEmail()));
        if (t.getUrl() != null) {
          String newUrl = resolveRelativeUrls ? makeHrefResolve(ds, t.getUrl(), null) : makeHref(t.getUrl(), null, null);
          out.format(" <li> <em>%s</em></li>%n", newUrl);
        }
        out.format(" </ul></li>%n");
      }
      out.format("</ul>%n");
    }

    java.util.List<ThreddsMetadata.Source> publishers = ds.getPublishers();
    if (publishers.size() > 0) {
      out.format("<h3>Publishers:</h3>%n<ul>%n");
      for (ThreddsMetadata.Source t : publishers) {
        out.format(" <li><strong>%s</strong><ul>%n", htmlEscaper.escape(t.getName()));
        out.format(" <li><em>email: </em>%s%n", htmlEscaper.escape(t.getEmail()));
        if (t.getUrl() != null) {
          String urlLink = resolveRelativeUrls ? makeHrefResolve(ds, t.getUrl(), null) : makeHref(t.getUrl(), null, null);
          out.format(" <li> <em>%s</em></li>%n", urlLink);
        }
        out.format(" </ul>%n");
      }
      out.format("</ul>%n");
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
    java.util.List<ThreddsMetadata.VariableGroup> vars = ds.getVariables();
    if (vars.size() > 0) {
      out.format("<h3>Variables:</h3>%n<ul>%n");
      for (ThreddsMetadata.VariableGroup t : vars) {

        out.format("<li><em>Vocabulary</em> [");
        if (t.getVocabUri() != null) {
          ThreddsMetadata.UriResolved uri = t.getVocabUri();
          String vocabLink = resolveRelativeUrls ? makeHref(uri.resolved.toString(), null, t.getVocabulary()) : makeHref(uri.href, null, t.getVocabulary());
          out.format(vocabLink);
        } else {
          out.format(htmlEscaper.escape(t.getVocabulary()));
        }
        out.format("]:%n<ul>%n");

        java.util.List<ThreddsMetadata.Variable> vlist = t.getVariableList();
        if (vlist.size() > 0) {
          for (ThreddsMetadata.Variable v : vlist) {
            String units = (v.getUnits() == null || v.getUnits().length() == 0) ? "" : " (" + v.getUnits() + ") ";
            out.format(" <li><strong>%s</strong> = ", htmlEscaper.escape(v.getName() + units));
            if (v.getDescription() != null)
              out.format(" <i>%s</i> = ", htmlEscaper.escape(v.getDescription()));
            if (v.getVocabularyName() != null)
              out.format("%s", htmlEscaper.escape(v.getVocabularyName()));
            out.format("%n");
          }
        }
        out.format("</ul>%n");
      }
      out.format("</ul>%n");
    }

    // LOOK what about VariableMapLink string ??
    if (ds.getVariableMapLink() != null) {
      out.format("<h3>Variables:</h3>%n");
      ThreddsMetadata.UriResolved uri = ds.getVariableMapLink();
      out.format("<ul><li>%s</li></ul>%n", makeHref(uri.resolved.toASCIIString(), null, "VariableMap"));
    }

    ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
    if (gc != null) {
      out.format("<h3>GeospatialCoverage:</h3>%n<ul>%n");
      // if (gc.isGlobal()) out.format(" <li><em> Global </em>%n");

      out.format(" <li><em> Longitude: </em> %s</li>%n", rangeString(gc.getEastWestRange()));
      out.format(" <li><em> Latitude: </em> %s</li>%n", rangeString(gc.getNorthSouthRange()));
      if (gc.getUpDownRange() != null) {
        out.format(" <li><em> Altitude: </em> %s (positive is <strong>%s)</strong></li>%n", rangeString(gc.getUpDownRange()), gc.getZPositive());
      }

      java.util.List<ThreddsMetadata.Vocab> nlist = gc.getNames();
      if ((nlist != null) && (nlist.size() > 0)) {
        out.format(" <li><em>  Names: </em> <ul>%n");
        for (ThreddsMetadata.Vocab elem : nlist) {
          out.format(" <li>%s</li>%n", htmlEscaper.escape(elem.getText()));
        }
        out.format(" </ul>%n");
      }
      out.format(" </ul>%n");
    }

    DateRange tc = ds.getTimeCoverage();
    if (tc != null) {
      out.format("<h3>TimeCoverage:</h3>%n<ul>%n");
      DateType start = tc.getStart();
      if (start != null)
        out.format(" <li><em>  Start: </em> %s</li>%n", start.toString());
      DateType end = tc.getEnd();
      if (end != null) {
        out.format(" <li><em>  End: </em> %s</li>%n", end.toString());
      }
      TimeDuration duration = tc.getDuration();
      if (duration != null)
        out.format(" <li><em>  Duration: </em> %s</li>%n", htmlEscaper.escape(duration.toString()));
      TimeDuration resolution = tc.getResolution();
      if (resolution != null) {
        out.format(" <li><em>  Resolution: </em> %s</li>%n", htmlEscaper.escape(resolution.toString()));
      }
      out.format(" </ul>%n");
    }

    java.util.List<ThreddsMetadata.MetadataOther> metadata = ds.getMetadataOther();
    boolean gotSomeMetadata = false;
    for (ThreddsMetadata.MetadataOther m : metadata) {
      if (m.getXlinkHref() != null) gotSomeMetadata = true;
    }

    if (gotSomeMetadata) {
      out.format("<h3>Metadata:</h3>%n<ul>%n");
      for (ThreddsMetadata.MetadataOther m : metadata) {
        String type = (m.getType() == null) ? "" : m.getType();
        if (m.getXlinkHref() != null) {
          String title = (m.getTitle() == null) ? "Type " + type : m.getTitle();
          String mdLink = resolveRelativeUrls ? makeHrefResolve(ds, m.getXlinkHref(), title) : makeHref(m.getXlinkHref(), null, title);
          out.format(" <li> %s</li>%n", mdLink);
        } //else {
        //out.format(" <li> <pre>"+m.getMetadataType()+" "+m.getContentObject()+"</pre>%n");
        //}
      }
      out.format("</ul>%n");
    }

    java.util.List<Property> propsOrg = ds.getProperties();
    java.util.List<Property> props = new ArrayList<>(ds.getProperties().size());
    for (Property p : propsOrg) {
      if (!p.getName().startsWith("viewer"))  // eliminate the viewer properties from the html view
        props.add(p);
    }
    if (props.size() > 0) {
      out.format("<h3>Properties:</h3>%n<ul>%n");
      for (Property p : props) {
        if (p.getName().equals("attachments")) { // LOOK whats this ?
          String attachLink = resolveRelativeUrls ? makeHrefResolve(ds, p.getValue(), p.getName()) : makeHref(p.getValue(), null, p.getName());
          out.format(" <li>%s</li>%n", attachLink);
        } else {
          out.format(" <li>%s = \"%s\"</li>%n", htmlEscaper.escape(p.getName()), htmlEscaper.escape(p.getValue()));
        }
      }
      out.format("</ul>%n");
    }

    if (complete) out.format("</body></html>");
  }

  private String rangeString(ThreddsMetadata.GeospatialRange r) {
    if (r == null) return "";
    String units = (r.getUnits() == null) ? "" : " " + r.getUnits();
    String resolution = r.hasResolution() ? " Resolution=" + r.getResolution() : "";
    return htmlEscaper.escape(r.getStart() + " to " + (r.getStart() + r.getSize()) + resolution + units);
  }

  /**
   * resolve reletive URLS against the catalog URL.
   *
   * @param ds   use ds parent catalog, if it exists
   * @param href URL to resolve
   * @return resolved URL
   */
  public String resolve(Dataset ds, String href) {
    Catalog cat = ds.getParentCatalog();
    if (cat != null) {
      try {
        java.net.URI uri = cat.resolveUri(href);
        href = uri.toString();
      } catch (java.net.URISyntaxException e) {
        return "DatasetHtmlWriter: error parsing URL= " + href;
      }
    }
    return href;
  }

  private String makeHref(String urlPath, String query, String title) {
    if (title == null) title = urlPath;
    if (query != null)
      urlPath += "?" + query;
    return "<a href='" + htmlEscaper.escape(urlPath) + "'>" + htmlEscaper.escape(title) + "</a>";
  }

  private String makeHrefResolve(Dataset ds, String href, String title) {
    href = resolve(ds, href);
    return makeHref(href, null, title);
  }
}
