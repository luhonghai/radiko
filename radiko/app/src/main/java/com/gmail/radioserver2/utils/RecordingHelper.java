package com.gmail.radioserver2.utils;

import android.media.AudioFormat;

import com.intervigil.lame.Encoder;
import com.intervigil.wave.WaveReader;
import com.intervigil.wave.WaveWriter;

import net.sourceforge.autotalent.Autotalent;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by luhonghai on 02/03/2015.
 */
public class RecordingHelper {

    private static final int AUTOTALENT_CHUNK_SIZE = 8192;

    private static final int RECORDER_BPP = 16;
    private final int recordedSampleRate;
    private final int recordedChannel;
    private final int recordedAudioEncoding;
    private final int bufferSize;

    public RecordingHelper(long recordedSampleRate, int recordedChannel, int recordedAudioEncoding, int bufferSize) {
        this.recordedAudioEncoding = recordedAudioEncoding;
        this.recordedChannel = recordedChannel;
        this.bufferSize = bufferSize;
        this.recordedSampleRate = (int) recordedSampleRate;
    }

    private void processCorrection(File source, File dest) throws IOException {
        WaveReader reader = null;
        WaveWriter writer = null;
        short[] buf = new short[AUTOTALENT_CHUNK_SIZE];
        try {
            reader = new WaveReader(source);
            reader.openWave();
            writer = new WaveWriter(
                    dest,
                    reader.getSampleRate(),
                    reader.getChannels(),
                    reader.getPcmFormat());
            writer.createWaveFile();
            while (true) {
                int samplesRead = reader.read(buf, AUTOTALENT_CHUNK_SIZE);
                if (samplesRead > 0) {
                    //  Autotalent.processSamples(buf, samplesRead);
                    writer.write(buf, 0, samplesRead);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (reader != null) {
                    reader.closeWaveFile();
                }
                if (writer != null) {
                    writer.closeWaveFile();
                }
            } catch (IOException e) {
                // I hate you sometimes java
                e.printStackTrace();
            }
        }
    }

    public void copyWaveFile(String inFilename, String outFilename) throws IOException {
        SimpleAppLog.info("Start create WAV header");
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen;
        //long totalDataLen;
        int longSampleRate = recordedSampleRate;
        int channels;
        if (recordedChannel == AudioFormat.CHANNEL_OUT_MONO) {
            SimpleAppLog.info("Channel mono");
            channels = 1;
        } else {
            SimpleAppLog.info("Channel stereo");
            channels = 2;
        }

        SimpleAppLog.info("Sample rate: " + longSampleRate);

        byte[] data = new byte[bufferSize];
        File tmp = new File(FileUtils.getTempDirectory(), UUID.randomUUID().toString() + ".wav");
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(tmp);
            totalAudioLen = in.getChannel().size();

            //long myChunk2Size =  totalAudioLen * channels * RECORDER_BPP /8;
            //long myChunkSize = 36 + myChunk2Size;
            long myChunkSize = 36 + totalAudioLen;

            SimpleAppLog.info("Found audio length: " + totalAudioLen);
            SimpleAppLog.info("Chunk size: " + myChunkSize);
            writeWaveFileHeader(out, totalAudioLen, myChunkSize,
                    longSampleRate, channels);
            int len;
            while ((len = in.read(data)) != -1) {
                out.write(data);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (Exception ex) {

            }
            try {
                if (in != null)
                    in.close();
            } catch (Exception ex) {

            }

        }
//        try {
//            if (tmp.exists());
//            parseWave(tmp);
//        } catch (Exception ex) {
//            SimpleAppLog.error("Could not parse WAVE file", ex);
//        }
        if (tmp.exists()) {
            //File newTmp = new File(FileUtils.getTempDirectory(), UUID.randomUUID().toString() + ".wav");
            //processCorrection(tmp, newTmp);
            Encoder encoder = null;
            try {
                encoder = new Encoder(tmp, new File(outFilename));
                encoder.initialize();
                encoder.encode();
            } catch (Exception ex) {
                SimpleAppLog.error("Could not encode MP3 file " + outFilename, ex);
            } finally {
                if (encoder != null)
                    encoder.cleanup();
            }
            try {
                FileUtils.forceDelete(tmp);
            } catch (Exception ex) {

            }
        }
    }

    private void writeWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels) throws IOException {

        byte[] header = new byte[44];

        long bitrate = longSampleRate * channels * RECORDER_BPP;

        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = (byte) RECORDER_BPP;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) ((bitrate / 8) & 0xff);
        header[29] = (byte) (((bitrate / 8) >> 8) & 0xff);
        header[30] = (byte) (((bitrate / 8) >> 16) & 0xff);
        header[31] = (byte) (((bitrate / 8) >> 24) & 0xff);
        header[32] = (byte) ((channels * RECORDER_BPP) / 8);
        header[33] = 0;
        header[34] = RECORDER_BPP;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) ((totalAudioLen) & 0xff);
        header[41] = (byte) (((totalAudioLen) >> 8) & 0xff);
        header[42] = (byte) (((totalAudioLen) >> 16) & 0xff);
        header[43] = (byte) (((totalAudioLen) >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    private void parseWave(File file)
            throws IOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] bytes = new byte[4];

            // read first 4 bytes
            // should be RIFF descriptor
            if (in.read(bytes) < 0) {
                return;
            }

            printDescriptor(bytes);

            // first subchunk will always be at byte 12
            // there is no other dependable constant
            in.skip(8);

            for (; ; ) {
                // read each chunk descriptor
                if (in.read(bytes) < 0) {
                    break;
                }

                printDescriptor(bytes);

                // read chunk length
                if (in.read(bytes) < 0) {
                    break;
                }

                // skip the length of this chunk
                // next bytes should be another descriptor or EOF
                in.skip(
                        (bytes[0] & 0xFF)
                                | (bytes[1] & 0xFF) << 8
                                | (bytes[2] & 0xFF) << 16
                                | (bytes[3] & 0xFF) << 24
                );
            }

            System.out.println("end of file");

        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private void printDescriptor(byte[] bytes)
            throws IOException {
        SimpleAppLog.info(
                "found '" + new String(bytes, "US-ASCII") + "' descriptor"
        );
    }

}
