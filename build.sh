#!/bin/sh
cd /Volumes/DATA/Development/radiko/rtmpdump-android/app/src/main
ndk-build
cp -R /Volumes/DATA/Development/radiko/rtmpdump-android/app/src/main/libs/. /Volumes/DATA/Development/radiko/radiko/app/src/main/jniLibs