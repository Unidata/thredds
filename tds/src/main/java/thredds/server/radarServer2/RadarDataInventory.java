package thredds.server.radarServer2;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.time.*;
import ucar.nc2.units.DateFromString;
import ucar.unidata.geoloc.EarthLocation;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class to manage generating an inventory of radar data and providing a way
 * to query what data are available.
 *
 * @author rmay
 * @since 01/15/2015
 */

public class RadarDataInventory {
    public enum DirType {
        Station, Variable, Date
    }

    private static final long updateIntervalMsec = 600000;
    private EnumMap<DirType, Set<String>> items;
    private Path collectionDir;
    private DirectoryStructure structure;
    private String fileTimeFmt, dataFormat;
    private boolean dirty;
    private CalendarDate lastUpdate;
    private int maxCrawlItems, maxCrawlDepth;
    private StationList stations;
    private CalendarPeriod nearestWindow, rangeAdjustment;
    private String name, description;

    public RadarDataInventory(Path datasetRoot) {
        items = new EnumMap<>(DirType.class);
        collectionDir = datasetRoot;
        structure = new DirectoryStructure(collectionDir);
        dirty = true;
        maxCrawlItems = 10;
        maxCrawlDepth = 2;
        stations = new StationList();
        nearestWindow = CalendarPeriod.of(1, CalendarPeriod.Field.Hour);
    }

    public Path getCollectionDir()
    {
        return collectionDir;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // TODO: Can we pull this from data?
    public FeatureType getFeatureType() {
        return FeatureType.RADIAL;
    }

    public void setDataFormat(String format) {
        this.dataFormat = format;
    }

    public String getDataFormat() {
        return dataFormat;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean needsVar() {
        return items.containsKey(DirType.Variable);
    }

    public StationList getStationList() {
        return stations;
    }

    public List<String> getVariableList() { return listItems(DirType.Variable); }

    public void setNearestWindow(CalendarPeriod pd) {
        nearestWindow = pd;
    }

    public class DirectoryStructure {
        private class DirEntry {
            public DirType type;
            public String fmt;
            public DirEntry(DirType type, String fmt) {
                this.type = type;
                this.fmt = fmt;
            }
        }

        private class DirectoryMatcher {
            private class Item {
                String path;
                boolean inString;
                Item(String path, boolean inString)
                {
                    this.path = path;
                    this.inString = inString;
                }
            }

            Stack<Item> stack;
            private boolean inString;
            String matcher;

            public DirectoryMatcher() {
                stack = new Stack<>();
                inString = true;
                matcher = "'" + base.toString();
            }

            public void push(DirType type, String value) {
                stack.push(new Item(matcher, inString));
                String newSegment;
                if (inString)
                    newSegment = structure.sep;
                else
                    newSegment = "'" + structure.sep;

                if (type == DirType.Date) {
                    newSegment += "'";
                    inString = false;
                } else {
                    inString = true;
                }
                matcher += newSegment + value;
            }

            public String matchString()
            {
                return matcher;
            }

            public void pop() {
                Item it = stack.pop();
                inString = it.inString;
                matcher = it.path;
            }
        }

        private Path base;
        private String sep;

        private List<DirEntry> order;

        public DirectoryStructure(Path dir) {
            base = dir;
            sep = System.getProperty("file.separator");
            order = new ArrayList<>();
        }

        public void addSubDir(DirType type, String fmt) {
            order.add(new DirEntry(type, fmt));
        }

        public DirectoryMatcher matcher() { return new DirectoryMatcher(); }
    }

    public void addStationDir() {
        structure.addSubDir(DirType.Station, null);
        dirty = true;
    }

    public void addVariableDir() {
        structure.addSubDir(DirType.Variable, null);
        dirty = true;
    }

    public void addDateDir(String fmt) {
        structure.addSubDir(DirType.Date, fmt);
        CalendarPeriod adjust = findRangeAdjustment(fmt);
        if (rangeAdjustment == null) {
            rangeAdjustment = adjust;
        } else if (adjust.getConvertFactor(rangeAdjustment) > 1.0) {
            rangeAdjustment = adjust;
        }
        dirty = true;
    }

    // Trying to figure out how much to fudge the date range to compensate for
    // the fact that when parsing a path for a date, such as YYYYMMDD, we
    // have essentially truncated a portion of the date. While parsing the
    // start time the same way will give a wider start point, the end
    // ends up going the wrong way. Adding this adjustment should correct for
    // this.
    private CalendarPeriod findRangeAdjustment(String fmtString) {
        if (fmtString.contains("H") || fmtString.contains("k"))
            return CalendarPeriod.of(1, CalendarPeriod.Field.Hour);
        else if (fmtString.contains("d"))
            return CalendarPeriod.of(1, CalendarPeriod.Field.Day);
        else if (fmtString.contains("M"))
            return CalendarPeriod.of(31, CalendarPeriod.Field.Day);
        else
            return CalendarPeriod.of(366, CalendarPeriod.Field.Day);
    }

    public void addFileTime(String fmt) {
        fileTimeFmt = fmt;
    }

    private void findItems(Path start, int level) throws IOException {
        // Add each entry from this level to the appropriate item box
        // and recurse
        if (level >= structure.order.size() || level >= maxCrawlDepth)
            return;

        DirectoryStructure.DirEntry entry = structure.order.get(level);
        Set<String> values;

        if (!items.containsKey(entry.type)) {
            values = new TreeSet<>();
            items.put(entry.type, values);
        } else {
            values = items.get(entry.type);
        }

        int crawled = 0;
        for (Path p : Files.newDirectoryStream(start)) {
            if (p.toFile().isDirectory()) {
//                System.out.println(p.toString());
                String item = p.getFileName().toString();
                values.add(item);
                if (entry.type == DirType.Station)
                    updateStations(item, p);
                if (crawled < maxCrawlItems) {
                    findItems(p, level + 1);
                    ++crawled;
                }
            }
        }
    }

    private class StationVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attrs) {
            try {
                RadialDatasetSweep rds = (RadialDatasetSweep)
                        FeatureDatasetFactoryManager.open(FeatureType.RADIAL,
                                file.toString(), null, new Formatter());
                EarthLocation loc = rds.getCommonOrigin();
                StationList.Station added = stations.addStation(
                        rds.getRadarID(), loc.getLatLon());
                added.setElevation(loc.getAltitude());
                added.setName(rds.getRadarName());
                return FileVisitResult.TERMINATE;
            } catch (IOException e) {
                return FileVisitResult.CONTINUE;
            }
        }
    }

    private void updateStations(String station, Path path) {
        StationList.Station match = stations.get(station);
        if (match == null) {
            try {
                Files.walkFileTree(path, new StationVisitor());
            } catch (IOException e) {
                System.out.println("Error walking to find station info: " +
                        station);
            }
        }
    }

    private void update() {
        try {
            if (dirty || timeToUpdate()) {
                findItems(structure.base, 0);
                dirty = false;
                lastUpdate = CalendarDate.present();
            }
        } catch (IOException e) {
            System.out.println("Error updating data inventory.");
        }
    }

    private boolean timeToUpdate() {
        // See if it's been more than enough time since the last update
        CalendarDate now = CalendarDate.present();
        return now.getDifferenceInMsecs(lastUpdate) > updateIntervalMsec;
    }

    public List<String> listItems(DirType type) {
        update();
        Set<String> vals = items.get(type);
        if (vals == null)
            return new ArrayList<>();
        else
            return new ArrayList<>(items.get(type));
    }

    public Query newQuery() {
        update();
        return new Query();
    }

    public class Query {
        public class QueryResultItem {
            private QueryResultItem(Path f, CalendarDate cd)
            {
                file = f;
                time = cd;
            }
            public Path file;
            public CalendarDate time;
        }

        private EnumMap<DirType, List<Object>> q;

        public Query()
        {
            q = new EnumMap<>(DirType.class);
        }

        public void addCriteria(DirType type, Object val)
        {
            List<Object> curVals = q.get(type);
            if (curVals == null)
            {
                curVals = new ArrayList<>();
                q.put(type, curVals);
            }
            curVals.add(val);
        }

        public void addStation(String stID) {
            addCriteria(DirType.Station, stID);
        }

        public void addVariable(String varName) {
            addCriteria(DirType.Variable, varName);
        }

        public void addDateRange(CalendarDateRange range) {
            addCriteria(DirType.Date, range);
        }

        private CalendarDateRange rangeFromFormat(SimpleDateFormat fmt,
                                                  CalendarDateRange range) {
            CalendarDate newStart = reparseDate(fmt, range.getStart());
            CalendarDate newEnd = reparseDate(fmt, range.getEnd());
            return CalendarDateRange.of(newStart, newEnd.add(rangeAdjustment));
        }

        private CalendarDate reparseDate(SimpleDateFormat fmt, CalendarDate d) {
            try {
                Date newDate = fmt.parse(fmt.format(d.toDate()));
                return CalendarDate.of(newDate);
            } catch (ParseException e) {
                return null;
            }
        }

        private boolean checkDate(CalendarDateRange range, CalendarDate d) {
            // Range null indicates no time filter
            return range == null || range.includes(d);
        }

        public List<QueryResultItem> results() throws IOException {
            List<Path> results = new ArrayList<>();
            DirectoryStructure.DirectoryMatcher matcher = structure.matcher();
            results.add(structure.base);
            for (int i = 0; i < structure.order.size(); ++i) {
                DirectoryStructure.DirEntry entry = structure.order.get(i);
                List<Path> newResults = new ArrayList<>();
                List<Object> queryItem = q.get(entry.type);
                switch (entry.type) {
                    // Loop over results and add subdirs
                    // Add date format to matcher string
                    case Date:
                        for (Path p : results)
                            for (Path sub : Files.newDirectoryStream(p))
                                newResults.add(sub);
                        matcher.push(DirType.Date, entry.fmt);
                        break;

                    // Add to results and prune non-existent
                    // Add station/var name to matcher string
                    case Station:
                    case Variable:
                    default:
                        String next = queryItem.get(0).toString();
                        for (Path p : results) {
                            Path nextPath = p.resolve(next);
                            if (nextPath.toFile().exists())
                                newResults.add(nextPath);
                        }
                        matcher.push(entry.type, next);
                }
                results = newResults;
            }

            // Apply filtering of Date to results
            SimpleDateFormat fmt = new SimpleDateFormat(matcher.matchString());
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

            List<Path> filteredResults = new ArrayList<>();
            List<Object> dates = q.get(DirType.Date);
            CalendarDateRange range = (CalendarDateRange) dates.get(0);

            // If we're given a single point for time, signifying we are looking
            // for the file nearest, turn it into a window for query purposes.
            if (range.isPoint()) {
                range = CalendarDateRange.of(
                        range.getStart().subtract(nearestWindow),
                        range.getEnd().add(nearestWindow));
            }

            CalendarDateRange dirRange = rangeFromFormat(fmt, range);

            for(Path p: results) {
                try {
                    Date d = fmt.parse(p.toString());
                    if (checkDate(dirRange, CalendarDate.of(d)))
                        filteredResults.add(p);
                } catch (ParseException e) {
                    // Ignore directory
                }
            }

            // Now get the contents of the remaining directories
            List<QueryResultItem> filteredFiles = new ArrayList<>();
            for(Path p : filteredResults) {
                for (Path f : Files.newDirectoryStream(p)) {
                    Date d = DateFromString.getDateUsingDemarkatedMatch(
                            f.toString(), fileTimeFmt, '#');
                    if (d != null) {
                        CalendarDate cd = CalendarDate.of(d);
                        if (checkDate(range, cd))
                            filteredFiles.add(new QueryResultItem(f, cd));
                    }
                }
            }

            // If only looking for nearest, perform that reduction now
            CalendarDateRange originalRange = (CalendarDateRange) dates.get(0);
            if (originalRange.isPoint()) {
                long offset = Long.MAX_VALUE;
                QueryResultItem bestMatch = null;
                for (QueryResultItem it: filteredFiles) {
                    long check = Math.abs(it.time.getDifferenceInMsecs(
                            originalRange.getStart()));
                    if (check < offset) {
                        offset = check;
                        bestMatch = it;
                    }
                }
                filteredFiles.clear();
                filteredFiles.add(bestMatch);
            }

            return filteredFiles;
        }
    }

    public static void main(String[] args) throws IOException {
        for (String name : args) {
            File baseDir = new File(name);
            if (baseDir.isDirectory()) {
                RadarDataInventory dw = new RadarDataInventory(baseDir.toPath());
                dw.addVariableDir();
                dw.addStationDir();
                dw.addDateDir("yyyyMMdd");

                System.out.println("Stations:");
                for (Object res : dw.listItems(DirType.Station))
                    System.out.println("\t" + res);

                System.out.println("Variables:");
                for (Object res : dw.listItems(DirType.Variable))
                    System.out.println("\t" + res);

                System.out.println("Dates:");
                for (Object res : dw.listItems(DirType.Date))
                    System.out.println("\t" + res);

                Query q = dw.newQuery();
                q.addVariable("N0Q");
                q.addStation("TLX");
                q.addDateRange(CalendarDateRange.of(CalendarDate.of(null, 2014,
                                6, 24, 0, 0, 0),
                        CalendarDate.of(null, 2014, 6, 25, 0, 0, 0)));

                System.out.println("Results of query:");
                for (Query.QueryResultItem i : q.results())
                    System.out.println("File: " + i.file.toString());
            }
        }
    }
}
