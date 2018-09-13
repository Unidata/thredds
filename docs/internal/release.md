## Making a release for CDM/TDS using Gradle

`releaseMinor` refers to the full, 3-part version, e.g. `4.6.6`

`releaseMajor` refers to the truncated, 2-part version, e.g. `4.6`

1. Prepare ncWMS and threddsIso
   - Detailed instructions can be found in `ncwms/docs/internal/release.txt and threddsIso/docs/internal/release.txt`.
   - The instructions for the two are very similar.

2. Ensure that there are no uncommitted changes.
   - `git checkout master`
   - `git status`

3. Pull all of the latest changes from upstream.
   - `git pull`

4. Create a new branch for the release and switch to it.
   - `git checkout -b ${releaseMinor}`

5. In `/build.gradle`, update the project's version for the release.
   Likely, this means removing the `-SNAPSHOT` prefix, e.g. `4.6.6-SNAPSHOT` to `4.6.6`.

6. In `/gradle/dependencies.gradle`, update the `uk.ac.rdg.resc:ncwms` and `EDS:threddsIso` dependencies to the
   versions deployed in step 1. Also, remove any dependencies on SNAPSHOT versions of libraries.

7. Publish the artifacts to Nexus.
   - You need the correct `nexus.username` and `nexus.password` properties defined in your
     `~/.gradle/gradle.properties` file. Ask Christian for those.
   - `./gradlew clean publish`
   - Check artifacts at http://artifacts.unidata.ucar.edu/repository/unidata-releases/

8. On `www`, prepare environment variables for scripts that follow:
    ```bash
    ssh www
    bash
    export releaseMajor="4.6"    # Replace with appropriate value
    export releaseMinor="4.6.8"  # Replace with appropriate value
    ```

9. Prepare the FTP directory for the new version of TDS and TDM (best to do from SSH)
    ```bash
    cd /web/ftp/pub/thredds/${releaseMajor}
    mkdir ${releaseMinor}

    # move /web/ftp/pub/thredds/${releaseMajor}/current to point to
    # /web/ftp/pub/thredds/${releaseMajor}/${releaseMinor}
    rm current   (If it exists)
    ln -s ${releaseMinor} current
    ```

10. Copy over the TDS war and its security hashes from Nexus, renaming them in the process.
    ```bash
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/tds/${releaseMinor}/tds-${releaseMinor}.war -O ${releaseMinor}/thredds.war
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/tds/${releaseMinor}/tds-${releaseMinor}.war.md5 -O ${releaseMinor}/thredds.war.md5
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/tds/${releaseMinor}/tds-${releaseMinor}.war.sha1 -O ${releaseMinor}/thredds.war.sha1
    ```

11. Copy over the TDM fat jar and its security hashes from Nexus, renaming them in the process.
   When renaming, "tdmFat" should become "tdm".
    ```bash
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/tdmFat/${releaseMinor}/tdmFat-${releaseMinor}.jar -O ${releaseMinor}/tdm-${releaseMajor}.jar
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/tdmFat/${releaseMinor}/tdmFat-${releaseMinor}.jar.sha1 -O ${releaseMinor}/tdm-${releaseMajor}.jar.sha1
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/tdmFat/${releaseMinor}/tdmFat-${releaseMinor}.jar.md5 -O ${releaseMinor}/tdm-${releaseMajor}.jar.md5
    ```

12. Change permissions of the files you just copied.
    ```bash
    cd /web/ftp/pub/thredds/${releaseMajor}/${releaseMinor}
    chmod 775 .
    chmod 664 *
    ```


13. Copy over ncIdv, netcdfAll, toolsUI and their security hashes from Nexus
    ```bash
    cd /web/ftp/pub/netcdf-java/v${releaseMajor}
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/ncIdv/${releaseMinor}/ncIdv-${releaseMinor}.jar
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/ncIdv/${releaseMinor}/ncIdv-${releaseMinor}.jar.md5
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/ncIdv/${releaseMinor}/ncIdv-${releaseMinor}.jar.sha1
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/netcdfAll/${releaseMinor}/netcdfAll-${releaseMinor}.jar
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/netcdfAll/${releaseMinor}/netcdfAll-${releaseMinor}.jar.md5
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/netcdfAll/${releaseMinor}/netcdfAll-${releaseMinor}.jar.sha1
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/toolsUI/${releaseMinor}/toolsUI-${releaseMinor}.jar
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/toolsUI/${releaseMinor}/toolsUI-${releaseMinor}.jar.md5
    wget https://artifacts.unidata.ucar.edu/repository/unidata-releases/edu/ucar/toolsUI/${releaseMinor}/toolsUI-${releaseMinor}.jar.sha1
    ```

14. Remove symlinks to old versions and create ones to new versions
    ```bash
    cd /web/ftp/pub/netcdf-java/v${releaseMajor}
    rm toolsUI-${releaseMajor}.jar netcdfAll-${releaseMajor}.jar ncIdv-${releaseMajor}.jar
    ln -s toolsUI-${releaseMinor}.jar toolsUI-${releaseMajor}.jar
    ln -s netcdfAll-${releaseMinor}.jar netcdfAll-${releaseMajor}.jar
    ln -s ncIdv-${releaseMinor}.jar ncIdv-${releaseMajor}.jar
    ```

15. Change permissions of the files you just copied.
    ```bash
    cd /web/ftp/pub/netcdf-java/v${releaseMajor}
    chmod 664 *
    ```

16. Mount the `www.unidata.ucar.edu:/web/` Samba share to a local directory, if it's not mounted already.
    Let's call that directory `webMountDir`. The details of this will vary based on your OS.
    - On OS X do: Finder->Go->Connect to Server...  Then connect to `smb://www/web`.

17. Set the `webdir` and `ftpdir` Gradle properties
    - Open `~/.gradle/gradle.properties`
    - Set `webdir` to `${webMountDir}/content/software/thredds/v${releaseMajor}/netcdf-java`
    - Set `ftpdir` to `${webMountDir}/ftp/pub/netcdf-java/v${releaseMajor}`
    - The value of `webMountDir` will likely differ, but mine is `/Volumes/web`.
    - So for example, on OS X, my (Christian's) Gradle properties for `webdir` and `ftpdir` are:
      ```properties
      webdir=/Volumes/web/content/software/thredds/v4.6/netcdf-java
      ftpdir=/Volumes/web/ftp/pub/netcdf-java/v4.6
      ```

18. Release Web Start to `www:/content/software/thredds/v${releaseMajor}/netcdf-java/webstart`
    - Make sure that you have the correct gradle.properties (see Christian for info). In particular, you'll need the
      `keystore`, `keystoreAlias`, `keystorePassword`, `webdir`, and `ftpdir` properties defined.
    - Rename old directories
      * `cd /content/software/thredds/v${releaseMajor}/netcdf-java/`
      * `mv webstart webstartOld`
    - Perform release
      * `./gradlew :ui:clean :ui:releaseWebstart`
    - Test the new Web Start. If there were no errors, delete the old stuff.
      * `rm -r webstartOld`

19. Release Javadoc to `www:/content/software/thredds/v${releaseMajor}/netcdf-java/javadoc` and `javadocAll`
    - Rename old directories
      * `cd /content/software/thredds/v${releaseMajor}/netcdf-java/`
      * `mv javadoc javadocOld`
      * `mv javadocAll javadocAllOld`
    - Perform release
      * `./gradlew :cdm:clean :cdm:releaseDocs`
      * `./gradlew :ui:clean :ui:releaseDocs`
    - If there were no errors and the new Javadoc looks good, delete the old stuff.
      * `rm -r javadocOld`
      * `rm -r javadocAllOld`

20. Change permissions of the files you just copied.
    ```bash
    cd /content/software/thredds/v${releaseMajor}/netcdf-java/
    find webstart -type d -exec chmod 775 {} \;
    find webstart -type f -exec chmod 664 {} \;
    find javadoc -type d -exec chmod 775 {} \;
    find javadoc -type f -exec chmod 664 {} \;
    find javadocAll -type d -exec chmod 775 {} \;
    find javadocAll -type f -exec chmod 664 {} \;
    ```

21. Update Unidata download page(s)
    - check http://www.unidata.ucar.edu/downloads/thredds/index.jsp
      * modify `www:/content/downloads/thredds/toc.xml` as needed
    - check http://www.unidata.ucar.edu/downloads/netcdf/netcdf-java-4/index.jsp
      * modify `www:/content/downloads/netcdf/netcdf-java-4/toc.xml` as needed

22. Edit `www:/content/software/thredds/latest.xml` to reflect the correct
    releaseMinor version for stable and development. This file is read by all
    TDS > v4.6 to make log entries regarding current stable and development versions
    to give users a heads-up of the need to update.

23. Commit the changes you've made.
    - At the very least, `project.version` in the root build script should have been modified.
    - `git add ...`
    - `git commit -m "Release ${releaseMinor}"`

24. Prepare for next round of development.
    - Update the project version. Increment it and add the "-SNAPSHOT" suffix.
      * For example, `if ${releaseMinor} == "4.6.6"`, the next version will be "4.6.7-SNAPSHOT".
    - Commit the change.
      * `git add ...`
      * `git commit -m "Begin work on 4.6.7-SNAPSHOT"`

25. Push the commits upstream.
    - `git push --set-upstream <your-repo> ${releaseMinor}`

26. Create a pull request on GitHub and wait for it to be merged.
    - It should pull your changes on `<your-repo>/${releaseMinor}` into `Unidata/master`.
    - Alternatively, merge it yourself. As long as the changeset is small and non-controversial, nobody will care.

27. Once merged, pull down the latest changes from master. You can also delete the release branch.
    - `git checkout master`
    - `git pull`
    - `git branch -d ${releaseMinor}`

28. In the git log, find the "Release ${releaseMinor}" commit and tag it with the version number.
    - `git log`
    - `git tag v${releaseMinor} <commit-id>`
        * `HEAD~1` is usually the right commit, so you can probably do `git tag v${releaseMinor} HEAD~1`
    - You can't create this tag earlier because when our PR was merged above, GitHub rebased our original
      commits, creating brand new commits in the process. We want to apply the tag to the new commit,
      because it will actually be part of `master`'s history.

29. Push the release tag upstream.
    -  `git push origin v${releaseMinor}`

30. Create a release on GitHub using the tag you just pushed.
    - Example: https://github.com/Unidata/thredds/releases/tag/v4.6.7
    - To help create the changelog, examine the pull requests on GitHub. For example, this URL shows all PRs that
      have been merged into `master` since 2016-02-12:
      https://github.com/Unidata/thredds/pulls?q=base%3Amaster+merged%3A%3E%3D2016-02-12

31. Make blog post for the release.
    - Example: http://www.unidata.ucar.edu/blogs/news/entry/netcdf-java-library-and-tds4
    - Best to leave it relatively short and just link to the GitHub release.

32. Make a release announcement to the mailing lists: netcdf-java@unidata.ucar.edu and thredds@unidata.ucar.edu
    - Example: http://www.unidata.ucar.edu/mailing_lists/archives/netcdf-java/2017/msg00000.html
    - Best to leave it relatively short and just link to the GitHub release.

**Note 1**: In the Maven build, the maven-release-plugin roughly handled steps 2-6 and 23-25 for us. In the future, we
should investigate similar Gradle plugins that offer the same functionality.

**Note 2**: In the future, we should be performing many (all?) of these steps from Jenkins, not our local machine.

**Note 3**: The latest.xml doc in step 21 is very simple and could probably be updated
        automatically during the release process.
