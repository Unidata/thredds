package thredds.util;

import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Remap the dataset UrlPath's from TDS 4.2 to
 * TDS 4.3 for the Unidata TDS server. This is only
 * valid for the initial move from 4.2 to 4.3.
 *
 * This code is heavily based on
 * ucar.nc2.grib.GribVariableRenamer.
 *
 * @author arms
 * @since 12/21/2012
 */
public class UnidataTdsDataPathRemapper {

    private static HashMap<String, Remapper> map;

    private void initMap() {
        List<UrlRemapperBean> beans = readUrlRemapFile("resources/thredds/4p2to4p3Remap.xml");
        map = makeMapBeans(beans);
    }

    /**
     *
     * Get the new datasetUrlPath given the old dataset UrlPath
     *
     * @param oldUrlPath Old dataset UrlPath
     * @return result List<String> containing possible remaps
     */
    public List<String> getMappedUrlPaths(String oldUrlPath) {
        return getMappedUrlPaths(oldUrlPath,null);
    }

    /**
     *
     * Get the new datasetUrlPath given the old dataset UrlPath
     * and the dataset "type" (a "file" or "best time series")
     *
     * A "file" urlPath looks something like:
     *   fmrc/NCEP/GFS/Alaska_191km/files/
     *
     * while a "best" urlPath looks like:
     *   fmrc/NCEP/GFS/Alaska_191km/NCEP-GFS-Alaska_191km_best.ncd
     *
     * @param oldUrlPath Old dataset UrlPath
     * @param urlType  either file (which includes latest) or best (for best time series)
     * @return result List<String> containing possible remaps
     */
    public List<String> getMappedUrlPaths(String oldUrlPath,String urlType) {
        List<String> result = new ArrayList<>();
        // look in our Url Remapper map
        if (map == null) {
            initMap();
        }

        Remapper mbean = map.get(oldUrlPath);
        if (mbean != null && mbean.newUrl != null)  {
            result.add(mbean.newUrl); // if its unique, then we are done
            return result;
        }

        // not unique - match against urlType (best, latest, or files)
        if (urlType != null) {
            if (mbean != null && mbean.newUrls.size() > 0) {
                for (UrlRemapperBean r : mbean.newUrls) {
                    if (r.getUrlType().equals(urlType)) result.add(r.newUrlPath);
                }
            }
        }
        return result;
    }

    private List<UrlRemapperBean> readUrlRemapFile(String path) {
        java.util.List<UrlRemapperBean> beans = new ArrayList<>(1000);

        ClassLoader cl = this.getClass().getClassLoader();

        try (InputStream is = cl.getResourceAsStream(path)) {

            if (is == null) {
                System.out.println("Cant read file " + path);
                return null;
            }

            SAXBuilder builder = new SAXBuilder();
            org.jdom2.Document doc = builder.build(is);
            org.jdom2.Element root = doc.getRootElement();
            List<org.jdom2.Element> dsElems = root.getChildren("urlMap");
            for (org.jdom2.Element dsElem : dsElems) {
                String dsType = dsElem.getAttributeValue("type");
                List<org.jdom2.Element> params = dsElem.getChildren("urlPath");
                for (org.jdom2.Element elem : params) {
                    String oldUrlPath = elem.getAttributeValue("oldUrlPath");
                    String newUrlPath = elem.getAttributeValue("newUrlPath");
                    beans.add(new UrlRemapperBean(dsType,oldUrlPath,newUrlPath));
                }
            }
            return beans;

        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;

        } catch (JDOMException e) {
            e.printStackTrace();
            return null;

        }
    }

    public static class UrlRemapperBean implements Comparable<UrlRemapperBean> {
        String urlType, oldUrlPath, newUrlPath;

        // used in IDV
        @SuppressWarnings("unused")
        public UrlRemapperBean() {}

        public UrlRemapperBean(String dsType, String oldUrlPath, String newUrlPath) {
            this.urlType = dsType;
            this.oldUrlPath = oldUrlPath;
            this.newUrlPath = newUrlPath;
        }

        public String getUrlType() {
            return urlType;
        }

        public String getOldUrlPath() {
            return oldUrlPath;
        }

        public String getNewUrlPath() {
            return newUrlPath;
        }

        @Override
        public int compareTo(UrlRemapperBean o) {
            return newUrlPath.compareTo(o.getNewUrlPath());
        }
    }

    private HashMap<String, Remapper> makeMapBeans(List<UrlRemapperBean> vbeans) {
        HashMap<String, Remapper> map = new HashMap<>(200);
        if (vbeans != null) {
            for (UrlRemapperBean vbean : vbeans) {

                // construct the old -> new mapping
                Remapper mbean = map.get(vbean.getOldUrlPath());
                if (mbean == null) {
                    mbean = new Remapper(vbean.getOldUrlPath());
                    map.put(vbean.getOldUrlPath(), mbean);
                }
                mbean.add(vbean);
            }

            for (Remapper rmap : map.values()) {
                rmap.finish();
            }
        }
        return map;
    }

    private static class Remapper {
        String oldUrl, newUrl; // newName exists when theres only one
        List<UrlRemapperBean> newUrls = new ArrayList<>();
        HashMap<String, UrlRemapperBean> newUrlMap = new HashMap<>();

        // no-arg constructor, used by IDV
        @SuppressWarnings("unused")
        public Remapper() {}

        public Remapper(String oldUrl) {
            this.oldUrl = oldUrl;
        }

        void add(UrlRemapperBean vbean) {
            newUrlMap.put(vbean.getNewUrlPath(), vbean);
            newUrls.add(vbean);
        }

        void finish() {
            if (newUrlMap.values().size() == 1) {
                newUrl = newUrls.get(0).getNewUrlPath();
            }
        }

        public int getCount() {
            return newUrls.size();
        }

        // used by IDV
        @SuppressWarnings("unused")
        public String getOldUrl() {
            return oldUrl;
        }
    }
}
