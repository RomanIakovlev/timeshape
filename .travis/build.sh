#!/bin/bash
set -o errexit
set -o nounset
set -o verbose

if [[ "$TRAVIS_PULL_REQUEST" == "false" ]]; then
    openssl aes-256-cbc -K $encrypted_aae76eb822dd_key -iv $encrypted_aae76eb822dd_iv \
        -in travis.gpg.enc -out travis.gpg -d

    gpg --import travis.gpg

    # Build and publish
    mvn -B -e --settings sonatype-settings.xml deploy
else
    mvn -B -e verify
fi