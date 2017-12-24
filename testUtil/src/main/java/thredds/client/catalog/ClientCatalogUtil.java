package thredds.client.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.builder.CatalogBuilder;
import ucar.unidata.util.StringUtil2;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * @author cwardgar
 * @since 2017-12-23
 */
public class ClientCatalogUtil {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static public String makeUrlFromFragment(String catFrag) {
        return "file:" + TestDir.cdmLocalTestDataDir + "thredds/catalog/" + catFrag;
    }

    static public Catalog open(String urlString) throws IOException {
        if (!urlString.startsWith("http:") && !urlString.startsWith("file:")) {
            urlString = makeUrlFromFragment(urlString);
        } else {
            urlString = StringUtil2.replace(urlString, "\\", "/");
        }
        logger.debug("Open {}", urlString);
        CatalogBuilder builder = new CatalogBuilder();
        Catalog cat = builder.buildFromLocation(urlString, null);
        if (builder.hasFatalError()) {
            logger.error(builder.getErrorMessage());
            assert false;
            return null;
        } else {
            String mess = builder.getErrorMessage();
            if (mess.length() > 0)
                logger.debug("Parse messages: {}", builder.getErrorMessage());
        }
        return cat;
    }

    public static String makeFilepath(String catalogName) {
        return makeFilepath() + catalogName;
    }

    public static String makeFilepath() {
        return "file:" + dataDir;
    }

    public static String dataDir = TestDir.cdmLocalTestDataDir + "thredds/catalog/";
}
