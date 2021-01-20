#!/bin/bash
set -o errexit
set -o nounset
set -o verbose

if [[ "$TRAVIS_PULL_REQUEST" == "false" ]]; then
    openssl aes-256-cbc -K $encrypted_aae76eb822dd_key -iv $encrypted_aae76eb822dd_iv \
        -in travis.gpg.enc -out travis.gpg -d

    gpg --import travis.gpg

    # Setup publishing
    cat <<-EOF > sonatype.sbt
	credentials in Global += Credentials(
	    "GnuPG Key ID",
	    "gpg",
	    "2DEEABE0DB8F8A2683DA4A4EB5C6B41853481DEE",
	    "ignored"
	)
	credentials in Global += Credentials(
	    "Sonatype Nexus Repository Manager",
	    "oss.sonatype.org",
	    "roman.iakovlev",
	    "$SONATYPE_PASSWORD"
	)
	EOF

    # Build and publish
    sbt test releaseTask
else
    sbt test
fi
