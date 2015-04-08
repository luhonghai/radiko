/*
 * FFmpegMediaPlayer: A unified interface for playing audio files and streams.
 *
 * Copyright 2014 William Seemann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifdef __cplusplus
extern "C" {
#endif
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#ifdef __cplusplus
}
#endif
#include <libavutil/opt.h>
#include <libswresample/swresample.h>
#include <libswscale/swscale.h>
#include <ffmpeg_mediaplayer.h>
#include <android/log.h>


#include <stdio.h>

const char *DURATION = "duration";
const char *AUDIO_CODEC = "audio_codec";
const char *VIDEO_CODEC = "video_codec";
const char *ICY_METADATA = "icy_metadata";
//const char *ICY_ARTIST = "icy_artist";
//const char *ICY_TITLE = "icy_title";
const char *ROTATE = "rotate";
const char *FRAMERATE = "framerate";

const int SUCCESS = 0;
const int FAILURE = -1;

static pthread_mutex_t *lock;
static pthread_mutex_t *lock_prepare;

int decode_interrupt_cb(void *opaque) {
	State *state = (State *) opaque;
	 
    return (state && state->abort_request);
}

void clear_l(State **ps)
{

	State *state = *ps;
	        
	if (state && state->pFormatCtx) {
		avformat_close_input(&state->pFormatCtx);
	}
	
	if (state && state->fd != -1) {
		close(state->fd);
	}
	
    if (!state) {
        state = av_mallocz(sizeof(State));
    }
    if (state) {
        state->pFormatCtx = NULL;
        state->audio_stream = -1;
        state->video_stream = -1;
        state->audio_st = NULL;
        state->video_st = NULL;
        state->buffer_size = -1;
        state->abort_request = 0;
        state->paused = 0;
        state->last_paused = -1;
        state->filename[0] = '\0';
        state->headers[0] = '\0';
        state->fd = -1;
        state->offset = 0;
        state->player_started = 0;
        *ps = state;
	}
}

void disconnect(State **ps)
{
	State *state = *ps;
	        
	if (state) {
		if (state->pFormatCtx) {
			avformat_close_input(&state->pFormatCtx);
	    }
	           
		if (state && state->fd != -1) {
			close(state->fd);
		}
		if (state)
	        av_freep(&state);
	    *ps = NULL;
	}
}

int setNotifyListener(State **ps, void* clazz, void (*listener) (void*, int, int, int, int))
{
    State *state = *ps;
    state->clazz = clazz;
    state->notify_callback = listener;
    return SUCCESS;
}

int setInitAudioTrackListener(State **ps, void* clazz, int (*listener) (void*, int, int, int))
{
    State *state = *ps;
    state->clazz = clazz;
    state->init_audio_track_callback = listener;
    return SUCCESS;
}

int setWriteAudioListener(State **ps, void* clazz, void (*listener) (void*, int16_t *, int, int))
{
    State *state = *ps;
    state->clazz = clazz;
    state->write_audio_callback = listener;
    return SUCCESS;
}

int setDataSourceURI(State **ps, const char *url, const char *headers)
{
    printf("setDataSource\n");

    if (!url) {
    	return FAILURE;
    }

    State *state = *ps;

    // Workaround for FFmpeg ticket #998
    // "must convert mms://... streams to mmsh://... for FFmpeg to work"
    char *restrict_to = strstr(url, "mms://");
    if (restrict_to) {
    	strncpy(restrict_to, "mmsh://", 6);
    	puts(url);
    }

    strncpy(state->filename, url, sizeof(state->filename));

    if (headers) {
        strncpy(state->headers, headers, sizeof(state->headers));
    }
    
    return SUCCESS;
}

int setDataSourceFD(State **ps, int fd, int64_t offset, int64_t length) {
	printf("setDataSource\n");
	
	State *state = *ps;
	
    int myfd = dup(fd);

    char str[20];
    sprintf(str, "pipe:%d", myfd);
    strncpy(state->filename, str, sizeof(state->filename));
    
    state->fd = myfd;
    state->offset = offset;
    
	*ps = state;

	return SUCCESS;
}

int setMetadataFilter(State **ps, char *allow[], char *block[])
{
    State *state = *ps;
	
    __android_log_print(ANDROID_LOG_INFO, "TAG", "Allow %s \n", allow[0]);
    __android_log_print(ANDROID_LOG_INFO, "TAG", "Block %s \n", block[0]);
    
    //state->allow = allow;
    //state->block = block;
    
	return SUCCESS;
}

void get_shoutcast_metadata(AVFormatContext *ic) {
    char *value = NULL;
    
    if (av_opt_get(ic, "icy_metadata_packet", 1, (uint8_t **) &value) < 0) {
        value = NULL;
    }
	
    if (value && value[0]) {
    	av_dict_set(&ic->metadata, ICY_METADATA, value, 0);
    	
    	/*int first_pos = first_char_pos(value, '\'');
        int last_pos = first_char_pos(value, ';') - 2;
        int pos = last_pos - first_pos;
        
        if (pos == 0) {
        	return;
        }
        
        char temp[pos];
        memcpy(temp, value + first_pos + 1 , pos);
        temp[pos] = '\0';
        value = temp;

    	first_pos = first_char_pos(value, '-') - 1;
        
        char artist[first_pos];
        memcpy(artist, value, first_pos);
        artist[first_pos] = '\0';
        
		av_dict_set(&ic->metadata, ICY_ARTIST, artist, 0);
        
        pos = strlen(value) - first_char_pos(value, '-') + 2;
         
        char album[pos];
        memcpy(album, value + first_char_pos(value, '-') + 2, pos);
        album[pos] = '\0';
        
		av_dict_set(&ic->metadata, ICY_TITLE, album, 0);*/
    }
}

void get_duration(AVFormatContext *ic, char * value) {
	int duration = 0;

	if (ic) {
		if (ic->duration != AV_NOPTS_VALUE) {
			duration = ((ic->duration / AV_TIME_BASE) * 1000);
		}
	}

	sprintf(value, "%d", duration); // %i
}

void set_codec(AVFormatContext *ic, int i) {
    const char *codec_type = av_get_media_type_string(ic->streams[i]->codec->codec_type);

	if (!codec_type) {
		return;
	}

    const char *codec_name = avcodec_get_name(ic->streams[i]->codec->codec_id);

	if (strcmp(codec_type, "audio") == 0) {
		av_dict_set(&ic->metadata, AUDIO_CODEC, codec_name, 0);
    } else if (codec_type && strcmp(codec_type, "video") == 0) {
	   	av_dict_set(&ic->metadata, VIDEO_CODEC, codec_name, 0);
	}
}

void set_rotation(State *s) {
    
	if (!extract_metadata(&s, ROTATE) && s->video_st && s->video_st->metadata) {
		AVDictionaryEntry *entry = av_dict_get(s->video_st->metadata, ROTATE, NULL, AV_DICT_IGNORE_SUFFIX);
        
        if (entry && entry->value) {
            av_dict_set(&s->pFormatCtx->metadata, ROTATE, entry->value, 0);
        }
	}
}

void set_framerate(State *s) {
	char value[30] = "0";
	
	if (s->video_st && s->video_st->avg_frame_rate.den && s->video_st->avg_frame_rate.num) {
		double d = av_q2d(s->video_st->avg_frame_rate);
		uint64_t v = lrintf(d * 100);
		if (v % 100) {
			sprintf(value, "%3.2f", d);
		} else if (v % (100 * 1000)) {
			sprintf(value,  "%1.0f", d);
		} else {
			sprintf(value, "%1.0fk", d / 1000);
		}
		
	    av_dict_set(&s->pFormatCtx->metadata, FRAMERATE, value, 0);
	}
}

const char* extract_metadata(State **ps, const char* key) {
	printf("extract_metadata\n");
    char* value = NULL;
	
	State *state = *ps;
    
	if (!state || !state->pFormatCtx) {
		return value;
	}

	if (key) {
		if (av_dict_get(state->pFormatCtx->metadata, key, NULL, AV_DICT_IGNORE_SUFFIX)) {
			value = av_dict_get(state->pFormatCtx->metadata, key, NULL, AV_DICT_IGNORE_SUFFIX)->value;
		} else if (state->audio_st && av_dict_get(state->audio_st->metadata, key, NULL, AV_DICT_IGNORE_SUFFIX)) {
			value = av_dict_get(state->audio_st->metadata, key, NULL, AV_DICT_IGNORE_SUFFIX)->value;
		} else if (state->video_st && av_dict_get(state->video_st->metadata, key, NULL, AV_DICT_IGNORE_SUFFIX)) {
			value = av_dict_get(state->video_st->metadata, key, NULL, AV_DICT_IGNORE_SUFFIX)->value;
		}
	}

	return value;
}

int getMetadata(State **ps, AVDictionary **metadata) {
    State *state = *ps;
    
    if (!state || !state->pFormatCtx) {
        return FAILURE;
    }
    
    get_shoutcast_metadata(state->pFormatCtx);
    av_dict_copy(metadata, state->pFormatCtx->metadata, 0);
    
    return SUCCESS;
}

/*int setVideoSurface(const sp<Surface>& surface)
{
    return 0;
}*/

static int check_sample_fmt(AVCodec *codec, enum AVSampleFormat sample_fmt)
 {
     const enum AVSampleFormat *p = codec->sample_fmts;

     while (*p != AV_SAMPLE_FMT_NONE) {
         if (*p == sample_fmt)
             return 1;
         p++;
     }
     return 0;
}

/* just pick the highest supported samplerate */
static int select_sample_rate(AVCodec *codec)
 {
     const int *p;
     int best_samplerate = 0;

     if (!codec->supported_samplerates)
         return 44100;

     p = codec->supported_samplerates;
     while (*p) {
         best_samplerate = FFMAX(*p, best_samplerate);
         p++;
     }
     return best_samplerate;
 }

 /* select layout with the highest channel count */
 static int select_channel_layout(AVCodec *codec)
 {
     const uint64_t *p;
     uint64_t best_ch_layout = 0;
     int best_nb_channells   = 0;

     if (!codec->channel_layouts)
         return AV_CH_LAYOUT_STEREO;

     p = codec->channel_layouts;
     while (*p) {
         int nb_channels = av_get_channel_layout_nb_channels(*p);

         if (nb_channels > best_nb_channells) {
             best_ch_layout    = *p;
             best_nb_channells = nb_channels;
         }
         p++;
     }
     return best_ch_layout;
}

int decode_frame_from_packet(State *state, AVPacket *aPacket, int *frame_size_ptr, int from_thread)
{
	int n;
	int16_t *samples;
	AVPacket *pkt = aPacket;
    AVFrame *decoded_frame = NULL;
    AVFrame *encoded_frame = NULL;
    int got_frame = 0;
    
	int64_t src_ch_layout, dst_ch_layout;
	int src_rate, dst_rate;
	uint8_t **src_data = NULL, **dst_data = NULL;
	int src_nb_channels = 0, dst_nb_channels = 0;
	int src_linesize, dst_linesize;
	int src_nb_samples, dst_nb_samples, max_dst_nb_samples;
	enum AVSampleFormat src_sample_fmt, dst_sample_fmt;
	int dst_bufsize;
	const char *fmt;
	struct SwrContext *swr_ctx;
	double t;
    int ret;
    int data_size = 0;

    // MP3 Encodeer
    AVCodec *codec;
    AVCodecContext *c= NULL;

    if (state && !state->abort_request && aPacket->stream_index == state->audio_stream) {

    	if (!decoded_frame) {
    		if (!(decoded_frame = avcodec_alloc_frame())) {
    			__android_log_print(ANDROID_LOG_INFO, "TAG", "Could not allocate audio frame\n");
    	        return -2;
    		}
    	}

    	if (!encoded_frame) {
            if (!(encoded_frame = av_frame_alloc())) {
                __android_log_print(ANDROID_LOG_INFO, "TAG", "Could not allocate encoder audio frame\n");
            	return -2;
            }
         }
    	
    	if (avcodec_decode_audio4(state->audio_st->codec, decoded_frame, &got_frame, aPacket) < 0) {
    		__android_log_print(ANDROID_LOG_ERROR, "TAG", "avcodec_decode_audio4() decoded no frame");
    		return -2;
    	}
    	
    	if (got_frame) {
    		/* if a frame has been decoded, output it */
    		data_size = av_samples_get_buffer_size(NULL, state->audio_st->codec->channels,
    	    		decoded_frame->nb_samples,
    	    		state->audio_st->codec->sample_fmt, 1);
    	} else {
    		*frame_size_ptr = 0;
    	    return 0;
    	}
    	if (decoded_frame->format != AV_SAMPLE_FMT_S16) {
            src_nb_samples = decoded_frame->nb_samples;
            src_linesize = (int) decoded_frame->linesize;
            src_data = decoded_frame->data;
            
            if (decoded_frame->channel_layout == 0) {
            	decoded_frame->channel_layout = av_get_default_channel_layout(decoded_frame->channels);
            }

            /* create resampler context */
            swr_ctx = swr_alloc();
            if (!swr_ctx) {
            	//fprintf(stderr, "Could not allocate resampler context\n");
                //ret = AVERROR(ENOMEM);
                //goto end;
            }

            
            src_rate = decoded_frame->sample_rate;
            dst_rate = decoded_frame->sample_rate;
            src_ch_layout = decoded_frame->channel_layout;
            dst_ch_layout = decoded_frame->channel_layout;
            src_sample_fmt = decoded_frame->format;
            dst_sample_fmt = AV_SAMPLE_FMT_S16;
            
            av_opt_set_int(swr_ctx, "in_channel_layout", src_ch_layout, 0);
            av_opt_set_int(swr_ctx, "out_channel_layout", dst_ch_layout,  0);
            av_opt_set_int(swr_ctx, "in_sample_rate", src_rate, 0);
            av_opt_set_int(swr_ctx, "out_sample_rate", dst_rate, 0);
            av_opt_set_sample_fmt(swr_ctx, "in_sample_fmt", src_sample_fmt, 0);
            av_opt_set_sample_fmt(swr_ctx, "out_sample_fmt", dst_sample_fmt,  0);
            
            /* initialize the resampling context */
            if ((ret = swr_init(swr_ctx)) < 0) {
            	__android_log_print(ANDROID_LOG_INFO, "TAG", "Failed to initialize the resampling context\n");
            	//goto end;
            }

            /* allocate source and destination samples buffers */

            src_nb_channels = av_get_channel_layout_nb_channels(src_ch_layout);
            ret = av_samples_alloc_array_and_samples(&src_data, &src_linesize, src_nb_channels,
            		src_nb_samples, src_sample_fmt, 0);
            if (ret < 0) {
            	__android_log_print(ANDROID_LOG_INFO, "TAG", "Could not allocate source samples\n");
            	//goto end;
            }

            /* compute the number of converted samples: buffering is avoided
             * ensuring that the output buffer will contain at least all the
             * converted input samples */
            max_dst_nb_samples = dst_nb_samples =
            	av_rescale_rnd(src_nb_samples, dst_rate, src_rate, AV_ROUND_UP);

            /* buffer is going to be directly written to a rawaudio file, no alignment */
            dst_nb_channels = av_get_channel_layout_nb_channels(dst_ch_layout);
            ret = av_samples_alloc_array_and_samples(&dst_data, &dst_linesize, dst_nb_channels,
            		dst_nb_samples, dst_sample_fmt, 0);
            if (ret < 0) {
            	__android_log_print(ANDROID_LOG_INFO, "TAG", "Could not allocate destination samples\n");
            	//goto end;
            }
            
            /* compute destination number of samples */
            dst_nb_samples = av_rescale_rnd(swr_get_delay(swr_ctx, src_rate) +
                                            src_nb_samples, dst_rate, src_rate, AV_ROUND_UP);
            encoded_frame->nb_samples = dst_nb_samples;
            /* convert to destination format */
            ret = swr_convert(swr_ctx, dst_data, dst_nb_samples, (const uint8_t **)decoded_frame->data, src_nb_samples);
            if (ret < 0) {
            	__android_log_print(ANDROID_LOG_INFO, "TAG", "Error while converting\n");
                //goto end;
            }

            dst_bufsize = av_samples_get_buffer_size(&dst_linesize, dst_nb_channels,
                                                     ret, dst_sample_fmt, 1);
            if (dst_bufsize < 0) {
                fprintf(stderr, "Could not get sample buffer size\n");
                //goto end;
            }
            
            samples = malloc(dst_bufsize);
    		memcpy(samples, dst_data[0], dst_bufsize);
    		data_size = dst_bufsize;

    		if (src_data) {
    			av_freep(&src_data[0]);
    		}
    		av_freep(&src_data);

    		if (dst_data) {
    			av_freep(&dst_data[0]);
    		}
    		av_freep(&dst_data);

    		swr_free(&swr_ctx);

    	} else {
    	    encoded_frame->nb_samples = decoded_frame->nb_samples;
    		/* if a frame has been decoded, output it */
    	    data_size = av_samples_get_buffer_size(NULL, state->audio_st->codec->channels,
    	    		decoded_frame->nb_samples, state->audio_st->codec->sample_fmt, 1);
    		samples = malloc(data_size);
    		memcpy(samples, decoded_frame->data[0], data_size);
    	}
        
        *frame_size_ptr = data_size;
        
        // TODO add this call back!
        //*pts_ptr = pts;
        n = 2 * state->audio_st->codec->channels;
        state->audio_clock += (double)*frame_size_ptr /
        		(double)(n * state->audio_st->codec->sample_rate);

        /* if update, update the audio clock w/pts */
        if(pkt->pts != AV_NOPTS_VALUE) {
        	state->audio_clock = av_q2d(state->audio_st->time_base) * pkt->pts;
        }

        // Init MP3 encoder codec
        //codec = avcodec_find_encoder(AV_CODEC_ID_MP3);
        codec = avcodec_find_encoder(AV_CODEC_ID_MP2);

        if (codec == NULL) {
            __android_log_print(ANDROID_LOG_ERROR, "TAG", "Could not init avcodec encoder\n");
        }
        c = avcodec_alloc_context3(codec);

        if (c == NULL) {
            __android_log_print(ANDROID_LOG_ERROR, "TAG", "Could not init avcodec context\n");
        }

        c->sample_fmt     = AV_SAMPLE_FMT_S16;
        if (!check_sample_fmt(codec, c->sample_fmt)) {
            __android_log_print(ANDROID_LOG_ERROR, "TAG", "Encoder does not support sample format %s",
                    av_get_sample_fmt_name(c->sample_fmt));
        }
        c->bit_rate       = 64000;
        c->sample_rate    = decoded_frame->sample_rate;
        c->channel_layout = decoded_frame->channel_layout;
        c->channels       = decoded_frame->channels;


        /* open it */
        ret = avcodec_open2(c, codec, NULL);
        if (ret < 0) {
            __android_log_print(ANDROID_LOG_ERROR, "TAG", "Could not open avcodec.  Error code: %s\n", av_err2str(ret));
        } else {
            encoded_frame->format = AV_SAMPLE_FMT_S16;
            encoded_frame->channel_layout = decoded_frame->channel_layout;
            encoded_frame->channels = decoded_frame->channels;
            encoded_frame->sample_rate = decoded_frame->sample_rate;
            encoded_frame->pts = decoded_frame->pts;

            ret = avcodec_fill_audio_frame(encoded_frame,
                    encoded_frame->channels,
                    encoded_frame->format,
                    (const uint8_t *) samples,
                    data_size,0);

            if (ret < 0) {
                __android_log_print(ANDROID_LOG_ERROR, "TAG", "Unable to fill the audio frame captured audio data. Error code: %s\n", av_err2str(ret));
            }

            int got_packet;
            AVPacket output_packet;
            av_init_packet(&output_packet);
            output_packet.data = NULL;
            output_packet.size = 0;
            ret = avcodec_encode_audio2(c, &output_packet, encoded_frame, &got_packet);
            if (ret < 0) {
                __android_log_print(ANDROID_LOG_ERROR, "TAG", "Error while encoding audio. Error code: %s\n", av_err2str(ret));
            }
            if (output_packet.size > 0)
                av_free_packet(&output_packet);
        }
        //*frame_size_ptr = data_size;
        if (state) {
            state->write_audio_callback(state->clazz, samples, data_size, from_thread);
        }
        avcodec_free_frame(&decoded_frame);

        avcodec_free_frame(&encoded_frame);

        free(samples);

        if (c) {
            avcodec_close(c);
            av_free(c);
        }


    	return AUDIO_DATA_ID;
    }

    return 0;
}

void player_decode(void *data)
{
	State *state = (State *) data;
	
	int ret;
	int eof = 0;

	for (;;) {

		if (state->abort_request) {
			break;
		}

        if (state->paused != state->last_paused) {
        	state->last_paused = state->paused;
            if (state->paused) {
            	state->read_pause_return = av_read_pause(state->pFormatCtx);
            } else {
                av_read_play(state->pFormatCtx);
            }
        }

        if (state->seek_req) {
            int64_t seek_target = state->seek_pos;
            int64_t seek_min = state->seek_rel > 0 ? seek_target - state->seek_rel + 2: INT64_MIN;
            int64_t seek_max = state->seek_rel < 0 ? seek_target - state->seek_rel - 2: INT64_MAX;

            ret = avformat_seek_file(state->pFormatCtx, -1, seek_min, seek_target, seek_max, state->seek_flags);
            if (ret < 0) {
                fprintf(stderr, "%s: error while seeking\n", state->pFormatCtx->filename);
            } else {
                if (state->audio_stream >= 0) {
                	avcodec_flush_buffers(state->audio_st->codec);
                }
                state->notify_callback(state->clazz, MEDIA_SEEK_COMPLETE, 0, 0, FROM_THREAD);
            }
            state->seek_req = 0;
            eof = 0;
        }

        if (state->paused) {
        	goto sleep;
        }

		AVPacket packet;
		memset(&packet, 0, sizeof(packet)); //make sure we can safely free it

		int i;
			
		for (i = 0; i < state->pFormatCtx->nb_streams; ++i) {
			//av_init_packet(&packet);
			ret = av_read_frame(state->pFormatCtx, &packet);

	        if (ret < 0) {
	            if (ret == AVERROR_EOF || url_feof(state->pFormatCtx->pb)) {
	                eof = 1;
	                break;
	            }
	        }

	        int frame_size_ptr;
			ret = decode_frame_from_packet(state, &packet, &frame_size_ptr, FROM_THREAD);
			av_free_packet(&packet);

			if (ret != 0) { //an error or a frame decoded
				// TODO add this bacl=k
			}
		}

		if (eof) {
			break;
		}

		sleep:
		    usleep(100);
	}

	if (eof) {
		state->notify_callback(state->clazz, MEDIA_PLAYBACK_COMPLETE, 0, 0, FROM_THREAD);
	}
}

int stream_component_open(State *s, int stream_index, int from_thread)
{
	AVFormatContext *pFormatCtx = s->pFormatCtx;
	AVCodecContext *codecCtx;
	AVCodec *codec;

	if (stream_index < 0 || stream_index >= pFormatCtx->nb_streams) {
		return FAILURE;
	}

	// Get a pointer to the codec context for the stream
	codecCtx = pFormatCtx->streams[stream_index]->codec;

	printf("avcodec_find_decoder %s\n", codecCtx->codec_name);

	// Find the decoder for the audio stream
	codec = avcodec_find_decoder(codecCtx->codec_id);

	if(codec == NULL) {
	    printf("avcodec_find_decoder() failed to find audio decoder\n");
	    return FAILURE;
	}

	// Open the codec
    if (!codec || (avcodec_open2(codecCtx, codec, NULL) < 0)) {
	  	printf("avcodec_open2() failed\n");
		return FAILURE;
	}

	switch(codecCtx->codec_type) {
		case AVMEDIA_TYPE_AUDIO:
			s->audio_stream = stream_index;
		    s->audio_st = pFormatCtx->streams[stream_index];
	        s->buffer_size = (s->init_audio_track_callback(s->clazz, codecCtx->sample_rate, codecCtx->channels, from_thread));
			break;
		case AVMEDIA_TYPE_VIDEO:
			s->video_stream = stream_index;
		    s->video_st = pFormatCtx->streams[stream_index];
			break;
		default:
			break;
	}

	return SUCCESS;
}

int player_prepare(State **ps, int from_thread)
{
	int audio_index = -1;
	int video_index = -1;
	int i;

	State *state = *ps;
	if (state && state->abort_request) {
	    return SUCCESS;
    }

        if (state && state->pFormatCtx) {
            avformat_close_input(&state->pFormatCtx);
        }
        if (state) {
            state->pFormatCtx = NULL;
            state->audio_stream = -1;
            state->video_stream = -1;
            state->audio_st = NULL;
            state->video_st = NULL;
        }

    int rst = 0;

        AVDictionary *options = NULL;
        av_dict_set(&options, "icy", "1", 0);
        av_dict_set(&options, "user-agent", "NSPlayer/7.10.0.3059", 0);

        if (state->headers) {
            av_dict_set(&options, "headers", state->headers, 0);
        }

        if (state->offset > 0) {
            state->pFormatCtx = avformat_alloc_context();
            state->pFormatCtx->skip_initial_bytes = state->offset;
        }

        rst = avformat_open_input(&state->pFormatCtx, state->filename, NULL, &options);

    if (rst != 0 && state) {
        printf("Input file could not be opened\n");
        if (state && !state->abort_request) {
    	    state->notify_callback(state->clazz, MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, 0, from_thread);
    	}
    	*ps = NULL;
        return MEDIA_ERROR;
    }
    if (state->abort_request) {
        return SUCCESS;
    }
    rst = -1;

        rst = avformat_find_stream_info(state->pFormatCtx, NULL);

	if (rst < 0) {
	    printf("Stream information could not be retrieved\n");
	    if (state && state->pFormatCtx) {
            avformat_close_input(&state->pFormatCtx);
        }
        if (state && !state->abort_request) {
		    state->notify_callback(state->clazz, MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, 0, from_thread);
		}
		*ps = NULL;
    	return MEDIA_ERROR;
	}
	char duration[30] = "0";

        if (state && !state->abort_request) {
            get_duration(state->pFormatCtx, duration);
            av_dict_set(&state->pFormatCtx->metadata, DURATION, duration, 0);
            get_shoutcast_metadata(state->pFormatCtx);

            // Find the first audio and video stream
            for (i = 0; i < state->pFormatCtx->nb_streams; i++) {
                if (state->pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO && video_index < 0) {
                    video_index = i;
                }

                if (state->pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO && audio_index < 0) {
                    audio_index = i;
                }

                set_codec(state->pFormatCtx, i);
            }
            if (audio_index >= 0) {
                stream_component_open(state, audio_index, from_thread);
            }

            if (video_index >= 0) {
                stream_component_open(state, video_index, from_thread);
            }
        }

	if (state && state->video_stream < 0 && state->audio_stream < 0) {
	    avformat_close_input(&state->pFormatCtx);
		state->notify_callback(state->clazz, MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, 0, from_thread);
		*ps = NULL;
		return MEDIA_ERROR;
	}

        if (state && !state->abort_request) {
            set_rotation(state);
            set_framerate(state);
            state->pFormatCtx->interrupt_callback.callback = decode_interrupt_cb;
            state->pFormatCtx->interrupt_callback.opaque = state;
        }

    // fill the inital buffer
    AVPacket packet;
    memset(&packet, 0, sizeof(packet)); //make sure we can safely free it
    int j, ret;
    int bytes_written = 0;
    while (state && !state->abort_request && bytes_written < state->buffer_size) {
            for (j = 0; j < state->pFormatCtx->nb_streams; j++) {
                if (!state || state->abort_request) {
                    return SUCCESS;
                }

                    ret = av_read_frame(state->pFormatCtx, &packet);
                    if (ret < 0) {
                        if (ret == AVERROR_EOF || url_feof(state->pFormatCtx->pb)) {
                            //break;
                        }
                    }

                int frame_size_ptr = 0;
                if (state && !state->abort_request) {
                    ret = decode_frame_from_packet(state, &packet, &frame_size_ptr, from_thread);
                    __android_log_print(ANDROID_LOG_ERROR, "TAG", "Fill buffer: %d -> %d", frame_size_ptr, state->buffer_size);
                    bytes_written = bytes_written + frame_size_ptr;
                }
                av_free_packet(&packet);
            }
    }
    if (state && !state->abort_request) {
        *ps = state;
        state->notify_callback(state->clazz, MEDIA_PREPARED, 0, 0, from_thread);
	}
	return SUCCESS;
}

int prepareAsync_l(State **ps)
{
     return SUCCESS;
}

int prepare(State **ps)
{
    printf("prepare\n");
    return player_prepare(ps, NOT_FROM_THREAD);
    //return player_prepare(ps, FROM_THREAD);
}

void player_prepare_thread(void *data)
{
	State ** ps = (State **) data;
	player_prepare(ps, FROM_THREAD);
}

int prepareAsync(State **ps)
{
    State *state = *ps;
    pthread_mutex_lock(lock_prepare);
    pthread_create(&state->prepare_thread, NULL, (void *) &player_prepare_thread, ps);
    pthread_mutex_unlock(lock_prepare);
    return SUCCESS;
}

int start(State **ps)
{
	State *state = *ps;
    
	if (!state->paused) {
		pthread_mutex_lock(lock);
		pthread_create(&state->decoder_thread, NULL, (void *) &player_decode, state);
	}
    
    state->paused = 0;
    state->player_started = 1;
    
    pthread_mutex_unlock(lock);
    
    return 0;
}

int stop(State **ps)
{
	State *state = *ps;
	
    pthread_mutex_lock(lock);
    state->abort_request = 1;
    pthread_mutex_unlock(lock);
	
    return 0;
}

int pause(State **ps)
{
	State *state = *ps;
	
    pthread_mutex_lock(lock);
	state->paused = !state->paused;
    pthread_mutex_lock(lock);
    
    return 0;
}

int isPlaying(State **ps)
{
	State *state = *ps;

	if (!state->player_started) {
		return 0;
	} else {
	    return !state->paused;
	}
}

int getVideoWidth(int *w)
{
    return 0;
}

int getVideoHeight(int *h)
{
    return 0;
}

int getCurrentPosition(State **ps, int *msec)
{
	State *state = *ps;
	*msec = state->audio_clock * 1000;
    return 0;
}

int getDuration(State **ps, int *msec)
{
	State *state = *ps;
	if (state->pFormatCtx && (state->pFormatCtx->duration != AV_NOPTS_VALUE)) {
		*msec = (state->pFormatCtx->duration / AV_TIME_BASE) * 1000;
	} else {
		*msec = 0;
	}
	
    return 0;
}

int seekTo(State **ps, int msec)
{
	State *state = *ps;
	
    pthread_mutex_lock(lock);

    if (!state->seek_req) {
    	state->seek_pos = msec * 1000;
    	state->seek_rel = msec * 1000;
    	state->seek_flags = AVSEEK_FLAG_FRAME;
        state->seek_req = 1;
    }

    pthread_mutex_unlock(lock);
	
    return 0;
}

int reset(State **ps)
{
        State *state = *ps;

        pthread_mutex_lock(lock_prepare);

        if (state) {
            state->abort_request = 1;
            pthread_join(state->prepare_thread, NULL);
        }

        pthread_mutex_unlock(lock_prepare);

        pthread_mutex_lock(lock);

        if (state) {
            state->abort_request = 1;
            pthread_join(state->decoder_thread, NULL);
        }

        pthread_mutex_unlock(lock);

        clear_l(ps);

    return 0;
}

int setLooping(State **ps, int loop)
{
	State *state = *ps;
	
	state->loop = loop;
	
    return 0;
}

/*void notify(int msg, int ext1, int ext2)
{

}*/

/*sp<IMemory> decode(const char* url, uint32_t *pSampleRate, int* pNumChannels, int* pFormat)
{
    return p;
}

sp<IMemory> decode(int fd, int64_t offset, int64_t length, uint32_t *pSampleRate, int* pNumChannels, int* pFormat)
{
    return p;
}*/
