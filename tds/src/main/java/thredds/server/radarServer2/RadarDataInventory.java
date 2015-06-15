package thredds.server.radarServer2;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.time.*;
import ucar.nc2.units.DateRange;
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
    private String fileTimeRegex, fileTimeFmt, dataFormat;
    private boolean dirty;
    private CalendarDate lastUpdate;
    private int maxCrawlItems, maxCrawlDepth;
    private StationList stations;
    private CalendarPeriod nearestWindow, rangeAdjustment;
    private String name, description;
    private DateRange timeCoverage;
    private RadarServerConfig.RadarConfigEntry.GeoInfo geoCoverage;

    public RadarDataInventory(Path datasetRoot) {
        items = new EnumMap<>(DirType.class);
        collectionDir = datasetRoot;
        structure = new DirectoryStructure(collectionDir);
        dirty = true;
        maxCrawlItems = 5;
        maxCrawlDepth = 3;
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

    CalendarDate getLastUpdate() {
        return this.lastUpdate;
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

    public void setTimeCoverage(DateRange range) {
        this.timeCoverage = range;
    }

    public DateRange getTimeCoverage() {
        return this.timeCoverage;
    }

    public void setGeoCoverage(RadarServerConfig.RadarConfigEntry.GeoInfo info) {
        this.geoCoverage = info;
    }

    public RadarServerConfig.RadarConfigEntry.GeoInfo getGeoCoverage() {
        return this.geoCoverage;
    }

    public boolean needsVar() {
        return items.containsKey(DirType.Variable);
    }

    public StationList getStationList() {
        return stations;
    }

    public List<String> getVariableList() {
        return listItems(DirType.Variable);
    }

    public void setNearestWindow(CalendarPeriod pd) {
        nearestWindow = pd;
    }

    public static class DirectoryStructure {
        private static class DirEntry {
            public DirType type;
            public String fmt;
            public DirEntry(DirType type, String fmt) {
                this.type = type;
                this.fmt = fmt;
            }
        }

        private class DirectoryDateMatcher {
            // Map a directory level to a date format
            List<Integer> levels;
            String fmt;

            public DirectoryDateMatcher() {
                levels = new ArrayList<>(4);
                fmt = "";
            }

            public void add(int level, String value) {
                levels.add(level);
                fmt += value;
            }

            public SimpleDateFormat getFormat() {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                return sdf;
            }

            public Date getDate(Path path) {
                Path relPath = base.relativize(path);
                String[] parts = relPath.toString().split(sep);
                StringBuilder sb = new StringBuilder("");
                for (Integer l: levels) {
                    sb.append(parts[l]);
                }
                try {
                    SimpleDateFormat fmt = getFormat();
                    return fmt.parse(sb.toString());
                } catch (ParseException e) {
                    return null;
                }
            }
        }

        private Path base;
        private String sep;

        private List<DirEntry> order;
        private List<Integer> keyIndices;

        public DirectoryStructure(Path dir) {
            base = dir;
            sep = System.getProperty("file.separator");
            order = new ArrayList<>();
            keyIndices = new ArrayList<>();
        }

        public void addSubDir(DirType type, String fmt) {
            if (type == DirType.Station || type == DirType.Variable) {
                keyIndices.add(order.size());
            }
            order.add(new DirEntry(type, fmt));
        }

        // Get a key for a path based on station/var
        public String getKey(Path path) {
            Path relPath = base.relativize(path);
            String[] parts = relPath.toString().split(sep);
            StringBuilder sb = new StringBuilder("");
            for (int ind: keyIndices) {
                sb.append(parts[ind]);
            }
            return sb.toString();
        }

        public DirectoryDateMatcher matcher() { return new DirectoryDateMatcher(); }
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

    public void addFileTime(String regex, String fmt) {
        fileTimeRegex = regex;
        fileTimeFmt = fmt;
    }

    private void findItems(Path start, int level) {
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
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(start)) {
            for (Path p : dirStream) {
                if (p.toFile().isDirectory()) {
                    String item = p.getFileName().toString();
                    values.add(item);
                    // Try to grab station info from some file
                    // TODO: Fix or remove
//                    if (entry.type == DirType.Station)
//                        updateStations(item, p);
                    if (crawled < maxCrawlItems) {
                        findItems(p, level + 1);
                        ++crawled;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("findItems(): Error reading directory: " + start.toString());
        }
    }

    private class StationVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file,
                                         BasicFileAttributes attrs) {
            try (RadialDatasetSweep rds = (RadialDatasetSweep)
                    FeatureDatasetFactoryManager.open(FeatureType.RADIAL,
                            file.toString(), null, new Formatter())){
                if (rds == null) {
                    return FileVisitResult.CONTINUE;
                }

                EarthLocation loc = rds.getCommonOrigin();
                if (loc == null) {
                    return FileVisitResult.CONTINUE;
                }

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
        if (dirty || timeToUpdate()) {
            findItems(structure.base, 0);
            dirty = false;
            lastUpdate = CalendarDate.present();
        }
    }

    boolean timeToUpdate() {
        // See if it's been more than enough time since the last update
        CalendarDate now = CalendarDate.present();
        return now.getDifferenceInMsecs(lastUpdate) > updateIntervalMsec;
    }

    public List<String> listItems(DirType type) {
        update();
        Set<String> vals = items.get(type);
        if (vals == null) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(vals);
        }
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
            if (range == null) return null;
            CalendarDate newStart = reparseDate(fmt, range.getStart());
            CalendarDate newEnd = reparseDate(fmt, range.getEnd());
            return CalendarDateRange.of(newStart, newEnd.add(rangeAdjustment));
        }

        private CalendarDate reparseDate(SimpleDateFormat fmt, CalendarDate d) {
            try {
                Date newDate = fmt.parse(fmt.format(d.toDate()));
                return CalendarDate.of(newDate);
            } catch (ParseException e) {
                return d; // Better than just returning null
            }
        }

        private boolean checkDate(CalendarDateRange range, CalendarDate d) {
            // Range null indicates no time filter
            return range == null || range.includes(d);
        }

        public Collection<QueryResultItem> results() {
            List<Path> results = new ArrayList<>();
            DirectoryStructure.DirectoryDateMatcher matcher = structure.matcher();
            results.add(structure.base);
            boolean needDateFilter = false;
            for (int i = 0; i < structure.order.size(); ++i) {
                DirectoryStructure.DirEntry entry = structure.order.get(i);
                List<Path> newResults = new ArrayList<>();
                List<Object> queryItem = q.get(entry.type);
                switch (entry.type) {
                    // Loop over results and add subdirs
                    // Add date format to matcher string
                    case Date:
                        needDateFilter = queryItem.get(0) != null;
                        for (Path p : results)
                            try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(p)) {
                                for (Path sub : dirStream)
                                    newResults.add(sub);
                            } catch (IOException e) {
                                System.out.println("results(): Error reading dir: " + p.toString());
                            }
                        matcher.add(i, entry.fmt);
                        break;

                    // Add to results and prune non-existent
                    // Add station/var name to matcher string
                    case Station:
                    case Variable:
                    default:
                        for (Object next: queryItem) {
                            for (Path p : results) {
                                Path nextPath = p.resolve(next.toString());
                                if (nextPath.toFile().exists())
                                    newResults.add(nextPath);
                            }
                        }
                }
                results = newResults;
            }

            List<Object> dates = q.get(DirType.Date);
            CalendarDateRange range = (CalendarDateRange) dates.get(0);

            // If we're given a single point for time, signifying we are looking
            // for the file nearest, turn it into a window for query purposes.
            if (range != null && range.isPoint()) {
                range = CalendarDateRange.of(
                        range.getStart().subtract(nearestWindow),
                        range.getEnd().add(nearestWindow));
            }

            List<Path> filteredResults = new ArrayList<>();
            if (needDateFilter) {
                // Apply filtering of Date to results
                SimpleDateFormat fmt = matcher.getFormat();
                CalendarDateRange dirRange = rangeFromFormat(fmt, range);

                for (Path p : results) {
                    Date d = matcher.getDate(p);
                    if (d != null && checkDate(dirRange, CalendarDate.of(d)))
                        filteredResults.add(p);
                }
            } else {
                filteredResults = results;
            }

            // Now get the contents of the remaining directories
            Collection<QueryResultItem> filteredFiles = new ArrayList<>();
            for(Path p : filteredResults) {
                try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(p)) {
                    for (Path f: dirStream) {
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(fileTimeRegex);
                        java.util.regex.Matcher regexMatcher = pattern.matcher(f.toString());
                        if (!regexMatcher.find()) continue;

                        try {
                            SimpleDateFormat fmt = new SimpleDateFormat(fileTimeFmt);
                            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
                            Date d = fmt.parse(regexMatcher.group());
                            if (d != null) {
                                CalendarDate cd = CalendarDate.of(d);
                                if (checkDate(range, cd))
                                    filteredFiles.add(new QueryResultItem(f, cd));
                            }
                        } catch (ParseException e) {
                            // Ignore file
                        }
                    }
                } catch (IOException e) {
                    System.out.println("results(): Error getting files for: " + p.toString());
                }
            }

            // If only looking for nearest, perform that reduction now
            CalendarDateRange originalRange = (CalendarDateRange) dates.get(0);
            if (originalRange != null && originalRange.isPoint()) {
                Map<String, Long> offsets = new TreeMap<>();
                Map<String, QueryResultItem> best = new TreeMap<>();

                for (QueryResultItem it: filteredFiles) {
                    String key = structure.getKey(it.file);
                    Long offset = offsets.get(key);
                    if (offset == null) offset = Long.MAX_VALUE;

                    long check = Math.abs(it.time.getDifferenceInMsecs(
                            originalRange.getStart()));
                    if (check < offset) {
                        offsets.put(key, check);
                        best.put(key, it);
                    }
                }

                // Return the best matches in the map
                filteredFiles = best.values();
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
