package thredds.crawlabledataset.filter;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.mock.MockCrawlableDataset;
import thredds.crawlabledataset.mock.MockCrawlableDatasetTreeBuilderUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * _MORE_
 *
 * @author edavis
 */
public class MultiSelectorFilterTest {

  private static MockCrawlableDataset topCrDs;

  @BeforeClass
  public static void buildMockCrDsTree() {
    String basePath = "/some/cool/path";
    String[] descendants = {
        "C1/C1.1/A1.1.1", "C1/C1.1/A1.1.2", "C1/C1.1/A1.1.3",
        "C1/C1.2/A1.2.1", "C1/C1.2/A1.2.2",
        "C1/A1.3",
        "A2", "A3", "A4",
        "C5/C5.1/A5.1.1", "C5/C5.1/A5.1.2",
        "C5/C5.2/A5.2.1",
        "C5/C5.3/A5.3.1", "C5/C5.3/A5.3.2",
        "A6", "A7"
    };

    topCrDs = MockCrawlableDatasetTreeBuilderUtils.buildCrDsTree( basePath, descendants );
  }

  @Test
  public void checkNoSelectors() throws IOException {
    List<MultiSelectorFilter.Selector> selectorList = Collections.emptyList();
    MultiSelectorFilter filter = new MultiSelectorFilter( selectorList );

    List<CrawlableDataset> allCrDs = topCrDs.listDatasets();
    List<CrawlableDataset> filteredCrDs = topCrDs.listDatasets( filter );
    assertEquals( 7, allCrDs.size());
    assertEquals( 7, filteredCrDs.size());

    CrawlableDataset curCrDs = filteredCrDs.get( 0 );
    allCrDs = curCrDs.listDatasets();
    filteredCrDs = curCrDs.listDatasets( filter);
    assertEquals( 3, allCrDs.size() );
    assertEquals( 3, filteredCrDs.size() );

    curCrDs = filteredCrDs.get( 0 );
    allCrDs = curCrDs.listDatasets();
    filteredCrDs = curCrDs.listDatasets( filter);
    assertEquals( 3, allCrDs.size() );
    assertEquals( 3, filteredCrDs.size() );
  }

  @Test
  public void checkNoAtomicSelectors() throws IOException {
    List<MultiSelectorFilter.Selector> selectorList = new ArrayList<MultiSelectorFilter.Selector>();
    selectorList.add( new MultiSelectorFilter.Selector(
        new WildcardMatchOnNameFilter( "*1*" ), true, false, true));
    selectorList.add( new MultiSelectorFilter.Selector(
        new WildcardMatchOnNameFilter( "*2*" ), false, false, true));
    MultiSelectorFilter filter = new MultiSelectorFilter( selectorList );

    List<CrawlableDataset> filteredCrDs = topCrDs.listDatasets( filter );
    assertEquals( 6, filteredCrDs.size());

    CrawlableDataset curCrDs = filteredCrDs.get( 0 );
    filteredCrDs = curCrDs.listDatasets( filter);
    assertEquals( 2, filteredCrDs.size() );

    curCrDs = filteredCrDs.get( 0 );
    filteredCrDs = curCrDs.listDatasets( filter);
    assertEquals( 3, filteredCrDs.size() );
  }

  @Test
  public void checkNoCollectionSelectors() throws IOException {
    List<MultiSelectorFilter.Selector> selectorList = new ArrayList<MultiSelectorFilter.Selector>();
    selectorList.add( new MultiSelectorFilter.Selector(
        new WildcardMatchOnNameFilter( "*2*" ), true, true, false));
    selectorList.add( new MultiSelectorFilter.Selector(
        new WildcardMatchOnNameFilter( "*5*" ), false, true, false));
    MultiSelectorFilter filter = new MultiSelectorFilter( selectorList );

    List<CrawlableDataset> filteredCrDs = topCrDs.listDatasets( filter );
    assertEquals( 3, filteredCrDs.size());

    CrawlableDataset curCrDs = filteredCrDs.get( 0 );
    filteredCrDs = curCrDs.listDatasets( filter);
    assertEquals( 2, filteredCrDs.size() );

    curCrDs = filteredCrDs.get( 0 );
    filteredCrDs = curCrDs.listDatasets( filter);
    assertEquals( 1, filteredCrDs.size() );
  }

  @Test
  public void checkOnlyAtomicIncluderSelectors() throws IOException {
    List<MultiSelectorFilter.Selector> selectorList = new ArrayList<MultiSelectorFilter.Selector>();
    selectorList.add( new MultiSelectorFilter.Selector(
        new WildcardMatchOnNameFilter( "*3*" ), true, true, false));
    selectorList.add( new MultiSelectorFilter.Selector(
        new WildcardMatchOnNameFilter( "*5*" ), true, true, false));
    MultiSelectorFilter filter = new MultiSelectorFilter( selectorList );

    List<CrawlableDataset> filteredCrDs = topCrDs.listDatasets( filter );
    assertEquals( 3, filteredCrDs.size()); // C1, A3, C5

    CrawlableDataset curCrDs = filteredCrDs.get( 0 );
    filteredCrDs = curCrDs.listDatasets( filter);
    assertEquals( 3, filteredCrDs.size() ); // C1/C1.1, C1/C1.2, C1/A1.3

    curCrDs = filteredCrDs.get( 0 );
    filteredCrDs = curCrDs.listDatasets( filter);
    assertEquals( 1, filteredCrDs.size() ); // C1/C1.1/A1.1.3
  }

  // atomic: only includers
  // atomic: only excluders
  // atomic: both includers and excluders
  // collection: only includers
  // collection: only excluders
  // collection: both includers and excluders
  //
}
