/* Encoder.java
   A port of LAME for Android

   Copyright (c) 2010 Ethan Chen

   This library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public
   License as published by the Free Software Foundation; either
   version 2 of the License, or (at your option) any later version.
	
   This library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public
   License along with this library; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.gmail.radioserver2.utils;

import com.intervigil.wave.WaveReader;

import net.sourceforge.lame.Lame;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Mp3Encoder {

    private static final int OUTPUT_STREAM_BUFFER = 200000;

    private final File outFile;

    private BufferedOutputStream out;


    private final int sampleRate;

    private final int channels;

    byte[] mp3Buf = new byte[OUTPUT_STREAM_BUFFER];

    private boolean flag = false;

    private boolean isInit = false;

    private Lame lame;

    public boolean isInit() {
        return isInit;
    }

    public Mp3Encoder(int sampleRate, int channels, File out) {
        this.outFile = out;
        this.sampleRate = sampleRate;
        this.channels = channels;
        isInit = false;
        lame = new Lame();
    }

    public void initialize() throws FileNotFoundException, IOException {
        out = new BufferedOutputStream(new FileOutputStream(outFile), OUTPUT_STREAM_BUFFER);
        flag = false;
        lame.initializeEncoder(sampleRate, channels);
        isInit = true;
    }

    public void encode(byte[] buffer, int offset, int length) throws IOException {

        short[] left = new short[channels == 2 ? buffer.length / 4 : buffer.length / 2];
        short[] right = new short[channels == 2 ? buffer.length / 4 : buffer.length / 2];
        int samplesRead;
        int bytesEncoded;

        if (channels == 2) {
            int index = 0;
            for (int i = 0; i < length; i += 2) {
                short val = byteToShortLE(buffer[0], buffer[i + 1]);
                if (i % 4 == 0) {
                    left[index] = val;
                } else {
                    right[index] = val;
                    index++;
                }
            }
            samplesRead = index;
            if (samplesRead > 0) {
                if (!flag) {
                    flag = true;
                }
                if (!isInit) return;
                bytesEncoded = lame.encode(left, right, samplesRead, mp3Buf, OUTPUT_STREAM_BUFFER);
                if (bytesEncoded > 0 && mp3Buf.length > 0)
                    out.write(mp3Buf, 0, bytesEncoded);
                else
                    SimpleAppLog.error("bytesEncoded: " + bytesEncoded);
            }
        } else {
            int index = 0;
            for (int i = 0; i < length; i += 2) {
                left[index] = byteToShortLE(buffer[i], buffer[i + 1]);
                index++;
            }
            samplesRead = index;
            if (samplesRead > 0) {
                if (!flag) {
                    flag = true;
                }
                if (!isInit) return;
                bytesEncoded = lame.encode(left, left, samplesRead, mp3Buf, OUTPUT_STREAM_BUFFER);
                if (bytesEncoded > 0 && mp3Buf.length > 0) out.write(mp3Buf, 0, bytesEncoded);
                else
                    SimpleAppLog.error("bytesEncoded: " + bytesEncoded);
            }
        }
    }


    public void cleanup() {
        boolean doClose = isInit;
        isInit = false;
        if (flag) {
            int bytesEncoded = 0;
            try {
                SimpleAppLog.info("Call Lame.flushEncoder");
                //bytesEncoded = lame.flushEncoder(mp3Buf, mp3Buf.length);
                if (bytesEncoded > 0 && mp3Buf.length > 0) {
                    out.write(mp3Buf, 0, bytesEncoded);
                    // TODO: write Xing VBR/INFO tag to mp3 file here
                    out.flush();
                }
            } catch (Exception e) {
                SimpleAppLog.error("Could not flush encoder", e);
            }
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            SimpleAppLog.error("Could not close output stream", e);
        }
        if (doClose) {
            SimpleAppLog.info("Call Lame.closeEncoder");
            lame.closeEncoder();
        }
    }


    private static short byteToShortLE(byte b1, byte b2) {
        return (short) (b1 & 0xFF | ((b2 & 0xFF) << 8));
    }
}
