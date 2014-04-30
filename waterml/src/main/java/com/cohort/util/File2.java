/* This file is Copyright (c) 2005 Robert Alten Simons (info@cohort.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact info@cohort.com.
 */
package com.cohort.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * File2 has useful static methods for working with files.
 *
 */
public class File2 {

    /**
     * Set this to true (by calling verbose=true in your program, not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    private static String tempDirectory; //lazy creation by getSystemTempDirectory

    /**
     * This indicates if the named file is indeed an existing file.
     * If dir="", it just says it isn't a file.
     *
     * @param dirName the full name of the file
     * @return true if the file exists
     */
    public static boolean isFile(String dirName) {
        try {
            //String2.log("File2.isFile: " + dirName);
            File file = new File(dirName);
            return file.isFile();
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.isFile(" + dirName + ")", e));
            return false;
        }
    }

    /**
     * For newly created files, this tries a few times to wait for the file
     * to be accessible via the operating system.
     *
     * @param dirName the full name of the file
     * @param nTimes the number of times to try (e.g., 5) with 200ms sleep between tries
     * @return true if the file exists
     */
    public static boolean isFile(String dirName, int nTimes) {
        try {
            //String2.log("File2.isFile: " + dirName);
            File file = new File(dirName);
            boolean b = false;
            nTimes = Math.max(1, nTimes);

            for (int i = 0; i < nTimes; i++) {
                b = file.isFile();
                if (b) {
                    return true;
                } else if (i < nTimes - 1) {
                    Math2.sleep(200);
                }
            }
            return b;
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.isFile(" + dirName + ")", e));
            return false;
        }
    }

    /**
     * This indicates if the named directory is indeed an existing directory.
     *
     * @param dir the full name of the directory
     * @return true if the file exists
     */
    public static boolean isDirectory(String dir) {
        try {
            //String2.log("File2.isFile: " + dirName);
            File d = new File(dir);
            return d.isDirectory();
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.isDirectory(" + dir + ")", e));
            return false;
        }
    }

    /**
     * This deletes the specified file or directory (must be empty).
     *
     * @param dirName the full name of the file
     * @return true if the file existed and was successfully deleted; 
     *    otherwise returns false.
     */
    public static boolean delete(String dirName) {
        //This can have problems if another thread is reading the file, so try repeatedly.
        //Unlike other places, this is often part of delete/rename, 
        //  so we want to know when it is done ASAP.
        int maxAttempts = String2.OSIsWindows? 11 : 4;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                File file = new File(dirName);
                if (!file.exists())
                    return attempt > 1; //if attempt=1, nothing to delete; if attempt>1, there was something
                //I think Linux deletes no matter what.
                //I think Windows won't delete file if in use by another thread or pending action(?).
                boolean result = file.delete();  
                file = null; //don't hang on to it
                if (result) {
                    //take it at its word (file has been or will soon be deleted)
                    //In Windows, file may continue to be isFile() for a short time.
                    return true;
                } else {
                    //2009-02-16 I had wierd problems on Windows with File2.delete not deleting when it should be able to.
                    //2011-02-18 problem comes and goes (varies from run to run), but still around.
                    //Windows often sets result=false on first attempt in some of my unit tests.
                    //Notably, when creating cwwcNDBCMet on local erddap.
                    //I note (from http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4045014)
                    //  that Win (95?) won't delete an open file, but solaris will.
                    //  But I think the files I'm working with have been closed.
                    //Solution? call Math2.gc instead of Math2.sleep
                    if (attempt == maxAttempts) {
                        String2.log(String2.ERROR + ": File2.delete was unable to delete " + dirName);
                        return result;
                    }
                    String2.log("WARNING #" + attempt + 
                        ": File2.delete is having trouble. It will try again to delete " + dirName);
                    if (attempt % 4 == 1)
                        Math2.gc(1000); //by experiment: gc works better than sleep
                    else Math2.sleep(1000);
                }

            } catch (Exception e) {
                if (verbose) String2.log(MustBe.throwable("File2.delete(" + dirName + ")", e));
                return false;
            }
        }
        return false; //won't get here
    }

    /**
     * This just tries once to delete the file or directory (must be empty).
     *
     * @param dirName the full name of the file
     * @return true if the file existed and was successfully deleted; 
     *    otherwise returns false.
     */
    public static boolean simpleDelete(String dirName) {
        //This can have problems if another thread is reading the file, so try repeatedly.
        //Unlike other places, this is often part of delete/rename, 
        //  so we want to know when it is done ASAP.
        try {
            File file = new File(dirName);
            if (!file.exists())
                return false; //it didn't exist
            //I think Linux deletes no matter what.
            //I think Windows won't delete file if in use by another thread or pending action(?).
            return file.delete();  
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.simpleDelete(" + dirName + ")", e));
            return false;
        }
    }

    /**
     * This deletes all files in the specified directory (not subdirectories).
     * If the dir isn't a directory, nothing happens.
     *
     * @param dir the full name of the directory
     */
    public static void deleteAllFiles(String dir) {
        deleteIfOld(dir, Long.MAX_VALUE, false, false);
    }

    /**
     * This deletes all files in the specified directory.
     * See also gov.noaa.pfel.coastwatch.util.RegexFilenameFilter.recursiveDelete().
     * If the dir isn't a directory, nothing happens.
     *
     * @param dir the full name of the directory
     * @param recursive if true, subdirectories are searched, too
     * @param deleteEmptySubdirectories  this is only used if recursive is true
     */
    public static void deleteAllFiles(String dir, 
            boolean recursive, boolean deleteEmptySubdirectories) {
        deleteIfOld(dir, Long.MAX_VALUE, recursive, deleteEmptySubdirectories);
    }

    /**
     * This deletes the files in the specified directory if the
     * last modified time is older than the specified time.
     *
     * @param dir the full name of the main directory
     * @param time System.currentTimeMillis of oldest file to be kept.
     *     Files will a smaller lastModified will be deleted.
     * @param recursive if true, subdirectories are searched, too
     * @param deleteEmptySubdirectories  this is only used if recursive is true
     * @return number of files that remain (or -1 if trouble)
     */
    public static int deleteIfOld(String dir, long time, 
            boolean recursive, boolean deleteEmptySubdirectories) {
        try {
            File file = new File(dir);

            //make sure it is an existing directory
            if (!file.isDirectory())
                return -1;

            //go through the files and delete old ones
            File files[] = file.listFiles();
            int nRemain = 0;
            int nDir = 0;
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if (files[i].lastModified() < time) 
                        files[i].delete();
                    else if (nRemain != -1)   //once nRemain is -1, it isn't changed
                        nRemain++;
                } else if (recursive && files[i].isDirectory()) {
                    nDir++;
                    int tnRemain = deleteIfOld(files[i].getAbsolutePath(), time, 
                        recursive, deleteEmptySubdirectories);
                    if (tnRemain == -1)
                        nRemain = -1;
                    else {
                        if (nRemain != -1)  //once nRemain is -1, it isn't changed
                            nRemain += tnRemain;
                        if (tnRemain == 0 && deleteEmptySubdirectories) {
                            //String2.log("File2.deleteIfOld is deleting dir=" + files[i].getAbsolutePath() + 
                            //"\n" + MustBe.stackTrace());
                            files[i].delete();
                        }
                    }
                }
            }
            if (nRemain > 0) String2.log("File2.deleteIfOld(" + dir + 
                (time == Long.MAX_VALUE? "" : 
                    ", " + Calendar2.safeEpochSecondsToIsoStringTZ(time / 1000.0, "" + time)) +
                ") nDir=" + nDir + 
                " nDeleted=" + (files.length - nDir + nRemain) + 
                " nRemain=" + nRemain);
            return nRemain;
        } catch (Exception e) {
            String2.log(MustBe.throwable("File2.deleteIfOld(" + dir + ", " + time + ")", e));
            return -1;
        }
    }

    /**
     * This renames the specified file.
     * If the newDirName file already exists, it will be deleted.
     *
     * @param dir the directory containing the file (with a trailing slash)
     * @param oldName the old name of the file
     * @param newName the new name of the file
     * @throws Exception if trouble
     */
    public static void rename(String dir, String oldName, String newName) 
            throws RuntimeException {
        rename(dir + oldName, dir + newName);
    }

    /**
     * This renames the specified file.
     * If the newDirName file already exists, it will be deleted before the renaming.
     * The files must be in the same directory.
     *
     * @param fullOldName the complete old name of the file
     * @param fullNewName the complete new name of the file
     * @throws Exception if trouble
     */
    public static void rename(String fullOldName, String fullNewName) 
            throws RuntimeException {
        File oldFile = new File(fullOldName);
        if (!oldFile.isFile())
            throw new RuntimeException(
                "Unable to rename\n" + fullOldName + " to\n" + fullNewName + 
                "\nbecause source file doesn't exist.");

        //delete any existing file with destination name
        File newFile = new File(fullNewName);
        if (newFile.isFile()) {
            //It may try a few times.
            //Since we know file exists, !delete really means it couldn't be deleted; take result at its word.
            if (!delete(fullNewName))  
                throw new RuntimeException(
                    "Unable to rename\n" + fullOldName + " to\n" + fullNewName +
                    "\nbecause unable to delete an existing file with destinationName.");

            //In Windows, file may be isFile() for a short time. Give it time (and gc encourage it) to delete.
            if (String2.OSIsWindows)
                Math2.gc(100);
        }

        //rename
        if (oldFile.renameTo(newFile))  
            return;

        throw new RuntimeException(
            "Unable to rename\n" + fullOldName + " to\n" + fullNewName);
    }

    /**
     * This renames fullOldName to fullNewName if fullNewName doesn't exist.
     * <br>If fullNewName does exist, this just deletes fullOldName (and doesn't touch() fullNewName.
     * <br>The files must be in the same directory.
     * <br>This is used when more than one thread may be creating the same fullNewName 
     *   with same content (and where last step is rename tempFile to newName).
     *
     * @param fullOldName the complete old name of the file
     * @param fullNewName the complete new name of the file
     * @throws Exception if trouble
     */
    public static void renameIfNewDoesntExist(String fullOldName, String fullNewName) 
            throws RuntimeException {
        if (isFile(fullNewName)) 
            //in these cases, fullNewName may be in use, so rename will fail because can't delete
            delete(fullOldName);  
        else rename(fullOldName, fullNewName);
    } 

    
    /**
     * If the directory or file exists, 
     * this changes its lastModification date/time to the current
     * date and time.  
     * (The name comes from the Unix "touch" program.)
     *
     * @param dirName the full name of the file
     * @return true if the directory or file exists 
     *    and if the modification was successful
     */
    public static boolean touch(String dirName) {
        return touch(dirName, 0);
    }

    /**
     * If the directory or file exists, 
     * this changes its lastModification date/time to the current
     * date and time minus millisInPast.  
     * (The name comes from the Unix "touch" program.)
     *
     * @param dirName the full name of the file
     * @param millisInPast
     * @return true if the directory or file exists 
     *    and if the modification was successful
     */
    public static boolean touch(String dirName, long millisInPast) {
        try {
            File file = new File(dirName);
            //The Java documentation for setLastModified doesn't state
            //if the method returns false if the file doesn't exist 
            //or if the method creates a 0 byte file (as does Unix's touch).
            //But tests show that it returns false if !exists, so no need to test. 
            //if (!file.exists()) return false;
            return file.setLastModified(System.currentTimeMillis() - millisInPast);
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.touch(" + dirName + ")", e));
            return false;
        }
    }

    /**
     * If the directory or file exists, 
     * this changes its lastModification date/time to millis  
     * (The name comes from the Unix "touch" program.)
     *
     * @param dirName the full name of the file
     * @param millis
     * @return true if the directory or file exists 
     *    and if the modification was successful
     */
    public static boolean setLastModified(String dirName, long millis) {
        try {
            File file = new File(dirName);
            //The Java documentation for setLastModified doesn't state
            //if the method returns false if the file doesn't exist 
            //or if the method creates a 0 byte file (as does Unix's touch).
            //But tests show that it returns false if !exists, so no need to test. 
            //if (!file.exists()) return false;
            return file.setLastModified(millis);
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.setLastModified(" + dirName + ")", e));
            return false;
        }
    }

    /**
     * This returns the length of the named file (or -1 if trouble).
     *
     * @param dirName the full name of the file
     * @return true if the file exists
     */
    public static long length(String dirName) {
        try {
            //String2.log("File2.isFile: " + dirName);
            File file = new File(dirName);
            if (!file.isFile()) return -1;
            return file.length();
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.length(" + dirName + ")", e));
            return -1;
        }
    }

    /**
     * Get the number of millis since the start of the Unix epoch
     * when the file was last modified.
     *
     * @param dirName the full name of the file
     * @return the time (millis since the start of the Unix epoch) 
     *    the file was last modified 
     *    (or 0 if trouble)
     */
    public static long getLastModified(String dirName) {
        try {
            File file = new File(dirName);
            return file.lastModified();
        } catch (Exception e) {
            //pause and try again
            try {
                Math2.gc(100);
                File file = new File(dirName);
                return file.lastModified();
            } catch (Exception e2) {
                if (verbose) String2.log(MustBe.throwable("File2.getLastModified(" + dirName + ")", e2));
                return 0;
            }
        }
    }

    /** This returns the name of the oldest file in the list. 
     * Error if dirNames.length == 0.
     */
    public static String getOldest(String dirNames[]) {
        int ti = 0;
        long tm = getLastModified(dirNames[0]);
        for (int i = 1; i < dirNames.length; i++) {
            long ttm = getLastModified(dirNames[i]);
            if (ttm != 0 && ttm < tm) {
                ti = i;
                tm = ttm;
            }
        }
        return dirNames[ti];
    }

    /** This returns the name of the youngest file in the list. 
     * Error if dirNames.length == 0.
     */
    public static String getYoungest(String dirNames[]) {
        int ti = 0;
        long tm = getLastModified(dirNames[0]);
        for (int i = 1; i < dirNames.length; i++) {
            long ttm = getLastModified(dirNames[i]);
            if (ttm != 0 && ttm > tm) {
                ti = i;
                tm = ttm;
            }
        }
        return dirNames[ti];
    }

    /**
     * This returns the protocol+domain (without a trailing slash) from a URL.
     *
     * @param url a full or partial URL.
     * @return the protocol+domain (without a trailing slash).
     *    If no protocol in URL, that's okay.
     *    null returns null.  "" returns "".
     */
    public static String getProtocolDomain(String url) {
        if (url == null)
            return url;

        int urlLength = url.length();
        int po = url.indexOf('/');
        if (po < 0)
            return url;
        if (po == urlLength - 1 || po > 7) 
            //protocol names are short e.g., http:// .  Perhaps it's coastwatch.pfeg.noaa.gov/
            return url.substring(0, po);
        if (url.charAt(po + 1) == '/') { 
            if (po == urlLength - 2)
                //e.g., http://
                return url;
            //e.g., http://a.com/
            po = url.indexOf('/', po + 2);
            return po >= 0? url.substring(0, po) : url;
        } else {
            //e.g., www.a.com/...
            return url.substring(0, po);            
        }
    }

    /**
     * This returns the directory info (with a trailing slash) from the dirName).
     *
     * @param dirName the full name of the file.
     *   It can have forward or backslashes.
     * @return the directory (or currentDirectory if none)
     */
    public static String getDirectory(String dirName) {
        int po = dirName.lastIndexOf('/');
        if (po < 0)
            po = dirName.lastIndexOf('\\');
        return po > 0? dirName.substring(0, po + 1) : getCurrentDirectory();
    }

    /**
     * This returns the current directory (with the proper separator at
     *   the end).
     *
     * @return the current directory (with the proper separator at
     *   the end)
     */
    public static String getCurrentDirectory() {
        String dir = System.getProperty("user.dir");

        if (!dir.endsWith(File.separator))
            dir += File.separator;

        return dir;
    }

    /**
     * This removes the directory info (if any) from the dirName,
     * and so returns just the name and extension.
     *
     * @param dirName the full name of the file.
     *   It can have forward or backslashes.
     * @return the name and extension of the file  (may be "")
     */
    public static String getNameAndExtension(String dirName) {
        int po = dirName.lastIndexOf('/');
        if (po >= 0)
            return dirName.substring(po + 1);

        po = dirName.lastIndexOf('\\');
        if (po >= 0)
            return dirName.substring(po + 1);

        return dirName;
    }

    /**
     * This returns just the extension from the file's name 
     * (the last "." and anything after, e.g., ".asc").
     *
     * @param dirName the full name or just name of the file.
     *   It can have forward or backslashes.
     * @return the name and extension of the file
     */
    public static String getExtension(String dirName) {
        String name = getNameAndExtension(dirName);
        int po = name.lastIndexOf('.');
        if (po >= 0)
            return name.substring(po);
        else return "";
    }

    /**
     * This replaces the existing extension (if any) with ext.
     *
     * @param dirName the full name or just name of the file.
     *   It can have forward or backslashes.
     * @param ext the new extension (e.g., ".das")
     * @return the dirName with the new ext
     */
    public static String forceExtension(String dirName, String ext) {
        String oldExt = getExtension(dirName);
        return dirName.substring(0, dirName.length() - oldExt.length()) + ext;
    }

    /**
     * This removes the directory info (if any) and extension (after the last ".", if any) 
     * from the dirName, and so returns just the name.
     *
     * @param dirName the full name of the file.
     *   It can have forward or backslashes.
     * @return the name of the file
     */
    public static String getNameNoExtension(String dirName) {
        String name = getNameAndExtension(dirName);
        String extension = getExtension(dirName);
        return name.substring(0, name.length() - extension.length());
    }

    /**
     * This generates a hex dump of the first nBytes of the file.
     * 
     * @param fullFileName
     * @param nBytes 
     * @return a String with a hex dump of the first nBytes of the file.
     * @throws Exception if trouble
     */
    public static String hexDump(String fullFileName, int nBytes) throws Exception {
        FileInputStream fis = new FileInputStream(fullFileName);
        nBytes = Math.min(nBytes, fis.available());
        byte ba[] = new byte[nBytes];
        int bytesRead = 0;
        while (bytesRead < nBytes)
            bytesRead += fis.read(ba, bytesRead, nBytes - bytesRead);
        fis.close();
        return String2.hexDump(ba);
    }

    /**
     * This returns the byte# at which the two files are different
     * (or -1 if same).
     *
     * @param fullFileName1
     * @param fullFileName2
     * @return byte# at which the two files are different (or -1 if same).
     */
    public static long whereDifferent(String fullFileName1, String fullFileName2) {

        long length1 = length(fullFileName1);
        long length2 = length(fullFileName2);
        long length = Math.min(length1, length2);
        BufferedInputStream bis1 = null, bis2 = null;
        long po = 0; 
        try {
            bis1 = new BufferedInputStream(new FileInputStream(fullFileName1));
            bis2 = new BufferedInputStream(new FileInputStream(fullFileName2));
            for (po = 0; po < length; po++) {
                if (bis1.read() != bis2.read())
                    break;
            }
        } catch (Exception e) {}
        try { 
            if (bis1 != null) bis1.close();
        } catch (Exception e) {}
        try { 
            if (bis2 != null) bis2.close();
        } catch (Exception e) {}

        if (po < length) return po;
        if (length1 != length2) return length;
        return -1;
    }

    /**
     * This makes a directory (and any necessary parent directories) (if it doesn't already exist).
     *
     * @param name
     * @throws RuntimeException if unable to comply.
     */
    public static void makeDirectory(String name) throws RuntimeException {
        File dir = new File(name);
        if (dir.isFile()) {
            throw new RuntimeException("Unable to make directory=" + name + ". There is a file by that name!");
        } else if (!dir.isDirectory()) {
            if (!dir.mkdirs())
                throw new RuntimeException("Unable to make directory=" + name + ".");
        }
    }
    
    /**
     * This makes a copy of a file.
     *
     * @param source the full file name of the source file
     * @param destination the full file name of the destination file.
     *   If the directory doesn't exist, it will be created.
     * @return true if successful. If not successful, the destination file
     *   won't exist.
     */
    public static boolean copy(String source, String destination) {

        if (source.equals(destination)) return false;
        FileOutputStream out = null;
        boolean success = false;
        try {
            File dir = new File(getDirectory(destination));
            if (!dir.isDirectory())
                 dir.mkdirs();
            out = new FileOutputStream(destination);
            success = copy(source, out);
        } catch (Exception e) {
            String2.log(MustBe.throwable(String2.ERROR + " in File2.copy", e));
        }
        try { 
            if (out != null) out.close();
        } catch (Exception e) {}

        if (!success) 
            delete(destination);

        return success;

    }

    /**
     * This makes a copy of a file to an outputStream.

     *
     * @param source the full file name of the source file
     * @param out  which is flushed, but not closed, at the end
     * @return true if successful. If not successful, the destination file
     *   won't exist.
     */
    public static boolean copy(String source, OutputStream out) {

        FileInputStream in = null;
        int bufferSize = 8192;
        byte buffer[] = new byte[bufferSize];
        boolean success = false;
        try {
            File file = new File(source);
            if (!file.isFile())
                return false;
            long remain = file.length();
            in  = new FileInputStream(file);
            while (remain > 0) {
                int read = in.read(buffer);
                out.write(buffer, 0, read);
                remain -= read;
            }
            out.flush();
            success = true;
        } catch (Exception e) {
            String2.log(MustBe.throwable(String2.ERROR + " in File2.copy: ", e));
        }
        try { 
            if (in != null) in.close();
        } catch (Exception e) {
        }

        return success;
    }

    /**
     * This reads the specified number of bytes from the inputstream
     * (unlike InputStream.read, which may not read all of the bytes).
     *
     * @param inputStream 
     * @param byteArray
     * @param offset the first position of byteArray to be written to
     * @param length the number of bytes to be read
     * @throws Exception if trouble
     */
    public static void read(InputStream inputStream, byte[] byteArray,
        int offset, int length) throws Exception {

        int po = offset;
        int remain = length;
        while (remain > 0) {
            int read = inputStream.read(byteArray, po, remain);
            po += read;
            remain -= read;
        }
    }

    /**
     * This creates, reads, and returns a byte array of the specified length.
     *
     * @param inputStream 
     * @param length the number of bytes to be read
     * @throws Exception if trouble
     */
    public static byte[] read(InputStream inputStream, int length) throws Exception {

        byte[] byteArray = new byte[length];
        read(inputStream, byteArray, 0, length);
        return byteArray;
    }

    /**
     * This returns a temporary directory 
     * (with forward slashes and a trailing slash, 
     * e.g., c:/Documents and Settings/Bob.Simons/ ... /).
     *
     * @return the temporary directory 
     * @throws Exception if trouble
     */
    public static String getSystemTempDirectory() throws Exception {
        if (tempDirectory == null) {
            File file = File.createTempFile("File2.getSystemTempDirectory", ".tmp");
            tempDirectory = file.getCanonicalPath();
            tempDirectory = String2.replaceAll(tempDirectory, "\\", "/");
            //String2.log("tempDir=" + tempDirectory);
            int po = tempDirectory.lastIndexOf('/');
            tempDirectory = tempDirectory.substring(0, po + 1);
            file.delete();
        }

        return tempDirectory;
    }

    /**
     * This adds a slash (matching the other slashes in the dir) 
     * to the end of the dir (if one isn't there already).
     *
     * @param dir with or without a slash at the end
     * @return dir with a slash (matching the other slashes) at the end
     */
    public static String addSlash(String dir) {
        if ("\\/".indexOf(dir.charAt(dir.length() - 1)) >= 0)
            return dir;
        int po = dir.indexOf('\\');
        if (po < 0) 
            po = dir.indexOf('/');
        char slash = po < 0? '/' : dir.charAt(po);
        return dir + slash;
    }

}

