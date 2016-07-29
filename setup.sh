#!/bin/bash -ex

echo "Building vendor/imagej/source/ij.jar"
cd vendor/imagej
unzip -n ij151d-src.zip # -n means don't overwrite
cd source
ant build
cd ../../..

echo "Extracting JAI"
RELEASE=jai_imageio-1_1-lib-linux-amd64.tar.gz
# curl http://download.java.net/media/jai-imageio/builds/release/1.1/$RELEASE > vendor/jai/$RELEASE
cd vendor/jai
tar zxvf $RELEASE
