#!/usr/bin/env bash

# Display the S3 URLs of the artifacts that Travis uploaded.
#
# Only do so for the "build" task, as that is where the artifacts are actually created.
#
# Also, only do so if the ARTIFACTS_KEY variable is set. It--along with ARTIFACTS_SECRET and ARTIFACTS_BUCKET--must be
# defined in order for Travis to be able to upload artifacts. Because they contain sensitive information, we've defined
# those values in the Travis repository settings of the Unidata/thredds repo.
# See https://docs.travis-ci.com/user/uploading-artifacts/
# See https://docs.travis-ci.com/user/environment-variables/#Defining-Variables-in-Repository-Settings
#
# A consequence of defining those variables in the Travis repositories settings is that they are only
# available to builds of branches on the Unidata/thredds GitHub repo. They are NOT available to forks, such as
# cwardgar/thredds. This was a deliberate design decision made by the Travis devs to protect sensitive data.
# But for us, it means that artifacts won't be uploaded for pull requests against our personal thredds forks.
#
# An alternative mechanism for protecting sensitive information is encrypted variables. But once again, those
# variables are not made available to untrusted builds such as pull requests coming from another repository.
# See https://docs.travis-ci.com/user/environment-variables/#Encrypted-Variables
if [ $TASK == "build" ] && [ -n "$ARTIFACTS_KEY" ]; then
    S3_URL="http://unidata-tds.s3.amazonaws.com/Travis/${TRAVIS_BRANCH}-${TRAVIS_BUILD_NUMBER}/${TRAVIS_JOB_NUMBER}"
    echo "The test report is available at:"
    echo "    $S3_URL/index.html"

    echo "Log files from the embedded TDS launched by the 'it' module are available at:"
    for LOG_FILE in $TDS_LOGS_DIR/*.log; do
        echo "    $S3_URL/$(basename $LOG_FILE)"
    done
fi
