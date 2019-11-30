#!/usr/bin/env bash
set -eux

VERSION=${1:-v2.3.0-RC2}
INSTALL_LOCATION=${2:-/usr/local/bin/scalafmt-native}

CWD=$(pwd)

NAME=scalafmt-linux
if [ "$(uname)" == "Darwin" ]; then
  NAME=scalafmt-macos
fi

TMP=$(mktemp -d)
cd $TMP
ZIP=$NAME.zip
curl -Lo $ZIP https://github.com/scalameta/scalafmt/releases/download/$VERSION/$ZIP
unzip $ZIP
cp $NAME/scalafmt $INSTALL_LOCATION
chmod +x $INSTALL_LOCATION

cd $CWD
rm -rf $TMP

echo Installed $INSTALL_LOCATION

