package thredds.server.catalogservice;

import org.springframework.beans.factory.annotation.Autowired;
import thredds.client.catalog.*;
import thredds.server.catalog.CatalogScan;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollectionRef;
import thredds.server.config.HtmlConfigBean;
import thredds.server.config.TdsContext;
import ucar.nc2.units.DateType;
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil2;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;


public class CatalogViewContextParser {

    public static Map<String, Object> getCatalogViewContext(Catalog cat, boolean isLocalCatalog) {
        Map<String, Object> model = new HashMap<>();

        // parse basic catalog contect
        String uri = cat.getUriString();
        if (uri == null) uri = cat.getName();
        if (uri == null) uri = "unknown";
        model.put("uri", uri);
        model.put("name", cat.getName());

        List<CatalogItemContext> catalogItems = new ArrayList<>();
        for (Dataset ds : cat.getDatasets()) {
            catalogItems.add(new CatalogItemContext(ds, 0, true));
        }
        model.put("items", catalogItems);
        return model;
    }

    public static Map<String, Object> getDatasetViewContext(Dataset ds, boolean isLocalCatalog)
    {
        Map<String, Object> model = new HashMap<>();

        return model;
    }
}

class CatalogItemContext {

    static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CatalogItemContext.class);

    @Autowired
    private HtmlConfigBean htmlConfig;

    @Autowired
    private TdsContext tdsContext;

    String displayName;
    String iconSrc;
    int level;
    String dataSize;
    String lastModified;
    String itemhref;
    boolean usehref = true;
    List<CatalogItemContext> items;

    public CatalogItemContext(Dataset ds, int level, boolean isLocalCatalog)
    {
        // Get display name
        this.displayName = ds.getName();

        // Store nesting level
        this.level = level;

        // Get href
        if (ds instanceof CatalogRef) {
            CatalogRef catref = (CatalogRef) ds;
            String href = catref.getXlinkHref();
            if (!isLocalCatalog) {
                URI hrefUri = ds.getParentCatalog().getBaseURI().resolve(href);
                href = hrefUri.toString();
            }
            try {
                URI uri = new URI(href);
                if (uri.isAbsolute()) {
                    boolean defaultUseRemoteCatalogService = htmlConfig.getUseRemoteCatalogService(); // read default as set in threddsConfig.xml
                    Boolean dsUseRemoteCatalogSerivce = ((CatalogRef) ds).useRemoteCatalogService();  // check to see if catalogRef contains tag that overrides default
                    boolean useRemoteCatalogService = defaultUseRemoteCatalogService; // by default, use the option found in threddsConfig.xml
                    if (dsUseRemoteCatalogSerivce == null)
                        dsUseRemoteCatalogSerivce = defaultUseRemoteCatalogService; // if the dataset does not have the useRemoteDataset option set, opt for the default behavior

                    // if the default is not the same as what is defined in the catalog, go with the catalog option
                    // as the user has explicitly overridden the default
                    if (defaultUseRemoteCatalogService != dsUseRemoteCatalogSerivce) {
                        useRemoteCatalogService = dsUseRemoteCatalogSerivce;
                    }

                    // now, do the right thing with using the remoteCatalogService, or not
                    if (useRemoteCatalogService) {
                        href = tdsContext.getContextPath() + "/remoteCatalogService?catalog=" + href;
                    } else {
                        int pos = href.lastIndexOf('.');
                        href = href.substring(0, pos) + ".html";
                    }
                } else {
                    int pos = href.lastIndexOf('.');
                    href = href.substring(0, pos) + ".html";
                }

            } catch (Exception e) {//(URISyntaxException e) {
                log.error(href, e);
            }

            this.itemhref = href;

            if (ds instanceof CatalogScan || ds.hasProperty("CatalogScan"))
                this.iconSrc = "cat_folder.png";
            else if (ds instanceof DatasetScan || ds.hasProperty("DatasetScan"))
                this.iconSrc = "scan_folder.png";
            else if (ds instanceof FeatureCollectionRef)
                this.iconSrc = "fc_folder.png";
            else
                this.iconSrc = "folder.png";

        } else { // Not a CatalogRef
            if (ds.hasNestedDatasets())
                this.iconSrc = "folder.png";

            // Check if dataset has single resolver service.
            if (ds.getAccess().size() == 1 && ServiceType.Resolver == ds.getAccess().get(0).getService().getType()) {
                Access access = ds.getAccess().get(0);
                String accessUrlName = access.getUnresolvedUrlName();
                int pos = accessUrlName.lastIndexOf(".xml");

                if (accessUrlName.equalsIgnoreCase("latest.xml") && !isLocalCatalog) {
                    String catBaseUriPath = "";
                    String catBaseUri = ds.getParentCatalog().getBaseURI().toString();
                    pos = catBaseUri.lastIndexOf("catalog.xml");
                    if (pos != -1) {
                        catBaseUriPath = catBaseUri.substring(0, pos);
                    }
                    accessUrlName = tdsContext.getContextPath() + "/remoteCatalogService?catalog=" + catBaseUriPath + accessUrlName;
                } else if (pos != -1) {
                    accessUrlName = accessUrlName.substring(0, pos) + ".html";
                }
                this.itemhref = accessUrlName;

            } else if (ds.findProperty(Dataset.NotAThreddsDataset) != null) { // Dataset can only be file served
                // Write link to HTML dataset page.
                this.itemhref = makeFileServerUrl(ds);

            } else if (ds.getID() != null) { // Dataset with an ID.    //URI catURI = cat.getBaseURI();
                Catalog cat = ds.getParentCatalog();
                String catHtml;
                if (!isLocalCatalog) {
                    // Setup HREF url to link to HTML dataset page (more below).
                    catHtml = tdsContext.getContextPath() + "/remoteCatalogService?command=subset&catalog=" + cat.getUriString() + "&";
                    // Can't be "/catalogServices?..." because subset decides on xml or html by trailing ".html" on URL path
                } else { // replace xml with html
                    URI catURI = cat.getBaseURI();
                    // Get the catalog name - we want a relative URL
                    catHtml = catURI.getPath();
                    if (catHtml == null) catHtml = cat.getUriString();  // if URI is a file
                    int pos = catHtml.lastIndexOf("/");
                    if (pos != -1) catHtml = catHtml.substring(pos + 1);
                    // change the ending to "catalog.html?"
                    pos = catHtml.lastIndexOf('.');
                    if (pos < 0)
                        catHtml = catHtml + "catalog.html?";
                    else
                        catHtml = catHtml.substring(0, pos) + ".html?";
                }

                // Write link to HTML dataset page.
                this.itemhref = catHtml + "dataset=" + StringUtil2.replace(ds.getID(), '+', "%2B");
            }else {
                this.usehref = false;
            }
        }

        if (this.iconSrc != null) this.iconSrc = htmlConfig.prepareUrlStringForHtml(this.iconSrc);

        // Get data size
        double size = ds.getDataSize();
        if ((size > 0) && !Double.isNaN(size))
            this.dataSize = (Format.formatByteSize(size));


        // Get last modified time.
        DateType lastModDateType = ds.getLastModifiedDate();
        if (lastModDateType != null)
            this.lastModified = lastModDateType.toDateTimeString();

        if (!(ds instanceof CatalogRef)) {

            List<CatalogItemContext> catalogItems = new ArrayList<>();
            for (Dataset nestedItem : ds.getDatasets()) {
                catalogItems.add(new CatalogItemContext(nestedItem, level+1, isLocalCatalog));
            }
        }
    }

    public String getDisplayName() { return this.displayName; }

    public String getIconSrc() { return  this.iconSrc; }

    public int getLevel() { return this.level; }

    public String getDataSize() { return this.dataSize; }

    public String getLastModified() { return this.lastModified; }

    public String getHref() {return this.itemhref; }

    public boolean useHref() {return this.usehref; }

    public List<CatalogItemContext> getCatalogItems() { return this.items; }

    private String makeFileServerUrl(Dataset ds) {
        Access acc = ds.getAccess(ServiceType.HTTPServer);
        assert acc != null;
        return acc.getStandardUrlName();
    }

}
