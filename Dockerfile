FROM openjdk:8
ARG SCALAFMT_VERSION=2.4.2
ARG SCALAFMT_INSTALL_LOCATION=/usr/local/bin/scalafmt
RUN set -ex \
    && apt-get update -y \
    && rm -rf /var/lib/apt/lists/* \
    && echo insecure > "$HOME/.curlrc" \
    && curl -Lo coursier https://git.io/coursier-cli \
    && chmod u+x coursier \
    && ./coursier bootstrap "org.scalameta:scalafmt-cli_2.12:$SCALAFMT_VERSION" \
        -r sonatype:snapshots \
        -o "$SCALAFMT_INSTALL_LOCATION" \
        --main org.scalafmt.cli.Cli \
    && rm -f coursier \
    && scalafmt --version
CMD ["/usr/local/bin/scalafmt"]
