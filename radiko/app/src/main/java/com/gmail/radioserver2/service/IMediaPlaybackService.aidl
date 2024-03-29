/*
 * FFmpegMediaPlayer: A unified interface for playing audio files and streams.
 *
 * Copyright 2014 William Seemann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gmail.radioserver2.service;

import android.graphics.Bitmap;

interface IMediaPlaybackService
{
    // Added by Hai
    String updateRtmpSuck(String token, String playUrl);

    void startRecord(String token, String fileName);
    void stopRecord();
    boolean isRecording();
    void markAB();
    int getStateAB();
    void stopAB();
    void doBack(int length);
    void doFast(float level);
    void stopFast();
    void doSlow(float level);
    void stopSlow();

    long getAPos();
    long getBPos();
    boolean isFast();
    boolean isSlow();

    boolean isStreaming();
    void setStreaming(boolean isStreaming);

    void openStream(String token, String objChannel);
    String getChannelObject();
    // Default
    void openFile(String path);
    void open(in long [] list, int position);
    int getQueuePosition();
    boolean isPlaying();
    void stop();
    void pause();
    void play();
    void prev();
    void next();
    long duration();
    long position();
    long seek(long pos);
    String getTrackName();
    String getAlbumName();
    long getAlbumId();
    String getArtistName();
    long getArtistId();
    void enqueue(in long [] list, int action);
    long [] getQueue();
    void moveQueueItem(int from, int to);
    void setQueuePosition(int index);
    String getPath();
    long getAudioId();
    void setShuffleMode(int shufflemode);
    int getShuffleMode();
    int removeTracks(int first, int last);
    int removeTrack(long id);
    void setRepeatMode(int repeatmode);
    int getRepeatMode();
    int getMediaMountedCount();
    int getAudioSessionId();
    String getMediaUri();
}

