#!/bin/sh
WORKING_DIR=`pwd`
# build RTMPDump
cd $WORKING_DIR/rtmpdump-android/app/src/main
ndk-build
cp -R $WORKING_DIR/rtmpdump-android/app/src/main/libs/. $WORKING_DIR/radiko/app/src/main/jniLibs

# build FFmpeg MediaPlayer
# If it the first time, need to compile openssl & ffmpeg first.
# Read fmp-library/README.md

cd $WORKING_DIR/fmp-library
ndk-build
cp -R $WORKING_DIR/fmp-library/libs/. $WORKING_DIR/radiko/app/src/main/jniLibs

# build liblame
cd $WORKING_DIR/liblame
ndk-build
cp -R $WORKING_DIR/liblame/libs/. $WORKING_DIR/radiko/app/src/main/jniLibs

#build libautotalent
cd $WORKING_DIR/libautotalent
ndk-build
cp -R $WORKING_DIR/libautotalent/libs/. $WORKING_DIR/radiko/app/src/main/jniLibs

#build OSLESMediaPlayer
cd $WORKING_DIR/OSLESMediaPlayer
ndk-build TARGET_PLATFORM=android-9
cp -R $WORKING_DIR/OSLESMediaPlayer/libs/. $WORKING_DIR/radiko/app/src/main/jniLibs
