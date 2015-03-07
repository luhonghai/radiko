/*  RTMP Proxy Server
 *  Copyright (C) 2009 Andrej Stepanchuk
 *  Copyright (C) 2009 Howard Chu
 *
 *  This Program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2, or (at your option)
 *  any later version.
 *
 *  This Program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RTMPDump; see the file COPYING.  If not, write to
 *  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 *  Boston, MA  02110-1301, USA.
 *  http://www.gnu.org/copyleft/gpl.html
 *
 */

/* This is a Proxy Server that displays the connection parameters from a
 * client and then saves any data streamed to the client.
 */

#define _FILE_OFFSET_BITS	64

#include <jni.h>

#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <limits.h>

#include <signal.h>
#include <getopt.h>

#include <assert.h>

#include "rtmpdump/librtmp/rtmp_sys.h"
#include "rtmpdump/librtmp/log.h"

#include "thread.h"

#ifdef linux
#include <linux/netfilter_ipv4.h>
#endif

#define RTMPDUMP_VERSION=2.4
#define RD_SUCCESS		0
#define RD_FAILED		1
#define RD_INCOMPLETE		2

#define PACKET_SIZE 1024*1024

#define DEF_BUFTIME	(10 * 60 * 60 * 1000)	/* 10 hours default */

static const AVal av_conn = AVC("conn");

#ifdef WIN32
#define InitSockets()	{\
        WORD version;			\
        WSADATA wsaData;		\
					\
        version = MAKEWORD(1,1);	\
        WSAStartup(version, &wsaData);	}

#define	CleanupSockets()	WSACleanup()
#else
#define InitSockets()
#define	CleanupSockets()
#endif

enum
{
  STREAMING_ACCEPTING,
  STREAMING_IN_PROGRESS,
  STREAMING_STOPPING,
  STREAMING_STOPPED
};

typedef struct Flist
{
  struct Flist *f_next;
  FILE *f_file;
  AVal f_path;
} Flist;

typedef struct Plist
{
  struct Plist *p_next;
  RTMPPacket p_pkt;
} Plist;

typedef struct
{
  int socket;
  int state;
  uint32_t stamp;
  RTMP rs;
  RTMP rc;
  Plist *rs_pkt[2];	/* head, tail */
  Plist *rc_pkt[2];	/* head, tail */
  Flist *f_head, *f_tail;
  Flist *f_cur;

// Added by Hai to pass arguments
  int protocol;
  AVal *host;
  AVal *hostname;
  unsigned int port;
  AVal *sockshost;
  AVal *playpath;
  AVal *tcUrl;
  AVal *swfUrl;
  AVal *pageUrl;
  AVal *app;
  AVal *auth;
  AVal *swfSHA256Hash;
  uint32_t swfSize;
  AVal *flashVer;
  AVal *subscribepath;
  AVal *usherToken;
  int dStart;
  int dStop;
  int bLiveStream;
  long int timeout;
  char *sToken;

  int stream_id;
} STREAMING_SERVER;

STREAMING_SERVER *rtmpServer = 0;	// server structure pointer

STREAMING_SERVER * startStreaming(const char *address, int port,
                 int protocol,
        		 AVal *host,
        		 unsigned int remote_port,
        		 AVal *sockshost,
        		 AVal *playpath,
        		 AVal *tcUrl,
        		 AVal *swfUrl,
        		 AVal *pageUrl,
        		 AVal *app,
        		 AVal *auth,
        		 AVal *swfSHA256Hash,
        		 uint32_t swfSize,
        		 AVal *flashVer,
        		 AVal *subscribepath,
        		 AVal *usherToken,
        		 int dStart,
        		 int dStop, int bLiveStream, long int timeout,
        		 char *sToken);
void stopStreaming(STREAMING_SERVER * server);

#define STR2AVAL(av,str)	av.av_val = str; av.av_len = strlen(av.av_val)

#ifdef _DEBUG
uint32_t debugTS = 0;

int pnum = 0;

FILE *netstackdump = NULL;
FILE *netstackdump_read = NULL;
#endif

#define BUFFERTIME	(4*60*60*1000)	/* 4 hours */

#define SAVC(x) static const AVal av_##x = AVC(#x)

SAVC(app);
SAVC(connect);
SAVC(flashVer);
SAVC(swfUrl);
SAVC(pageUrl);
SAVC(tcUrl);
SAVC(fpad);
SAVC(capabilities);
SAVC(audioCodecs);
SAVC(videoCodecs);
SAVC(videoFunction);
SAVC(objectEncoding);
SAVC(_result);
SAVC(createStream);
SAVC(play);
SAVC(closeStream);
SAVC(fmsVer);
SAVC(mode);
SAVC(level);
SAVC(code);
SAVC(secureToken);
SAVC(onStatus);
SAVC(close);

#define HEX2BIN(a)      (((a)&0x40)?((a)&0xf)+9:((a)&0xf))
int hex2bin(char *str, char **hex)
{
  char *ptr;
  int i, l = strlen(str);

  if (l & 1)
  	return 0;

  *hex = malloc(l/2);
  ptr = *hex;
  if (!ptr)
    return 0;

  for (i=0; i<l; i+=2)
    *ptr++ = (HEX2BIN(str[i]) << 4) | HEX2BIN(str[i+1]);
  return l/2;
}


static const AVal av_NetStream_Failed = AVC("NetStream.Failed");
static const AVal av_NetStream_Play_Failed = AVC("NetStream.Play.Failed");
static const AVal av_NetStream_Play_StreamNotFound =
AVC("NetStream.Play.StreamNotFound");
static const AVal av_NetConnection_Connect_InvalidApp =
AVC("NetConnection.Connect.InvalidApp");
static const AVal av_NetStream_Play_Start = AVC("NetStream.Play.Start");
static const AVal av_NetStream_Play_Complete = AVC("NetStream.Play.Complete");
static const AVal av_NetStream_Play_Stop = AVC("NetStream.Play.Stop");

static const char *cst[] = { "client", "server" };

RTMP_Pass_Arg(STREAMING_SERVER *server, int includeRs) {
int i;
       #ifdef CRYPTO
       if (server->swfSHA256Hash != NULL && server->swfSize > 0)
       {
         memcpy(server->rc.Link.SWFHash, server->swfSHA256Hash->av_val, sizeof(server->rc.Link.SWFHash));
         server->rc.Link.SWFSize = server->swfSize;

         if (includeRs) {
            memcpy(server->rs.Link.SWFHash, server->swfSHA256Hash->av_val, sizeof(server->rs.Link.SWFHash));
            server->rs.Link.SWFSize = server->swfSize;
         }
       }
       else
       {
         server->rc.Link.SWFSize = 0;
         if (includeRs)
            server->rs.Link.SWFSize = 0;
       }
       #endif

        if (server->tcUrl && server->tcUrl->av_len) {
                server->rc.Link.tcUrl = *server->tcUrl;
                if (includeRs)
                    server->rs.Link.tcUrl = *server->tcUrl;
        }
        if (server->swfUrl && server->swfUrl->av_len) {
                server->rc.Link.swfUrl = *server->swfUrl;
                if (includeRs)
                    server->rs.Link.swfUrl = *server->swfUrl;
        }
        if (server->pageUrl && server->pageUrl->av_len) {
                server->rc.Link.pageUrl = *server->pageUrl;
                if (includeRs)
                    server->rs.Link.pageUrl = *server->pageUrl;
        }
        if (server->app && server->app->av_len) {
                server->rc.Link.app = *server->app;
                if (includeRs)
                    server->rs.Link.app = *server->app;
        }
        if (server->auth && server->auth->av_len)
         {
                  server->rc.Link.auth = *server->auth;
                  server->rc.Link.lFlags |= RTMP_LF_AUTH;
                  if (includeRs) {
                  server->rs.Link.auth = *server->auth;
                  server->rs.Link.lFlags |= RTMP_LF_AUTH;
                  }
          }
         if (server->flashVer && server->flashVer->av_len) {
                server->rc.Link.flashVer = *server->flashVer;
               if (includeRs)
                server->rs.Link.flashVer = *server->flashVer;
                }
         else {
                server->rc.Link.flashVer = RTMP_DefaultFlashVer;
                if (includeRs)
                    server->rs.Link.flashVer = RTMP_DefaultFlashVer;
        }
         if (server->subscribepath && server->subscribepath->av_len) {
                server->rc.Link.subscribepath = *server->subscribepath;
               if (includeRs)
                server->rs.Link.subscribepath = *server->subscribepath;
          }
         if (server->usherToken && server->usherToken->av_len) {
                 server->rc.Link.usherToken = *server->usherToken;
                if (includeRs)
                    server->rs.Link.usherToken = *server->usherToken;
                 }
                 server->rc.Link.seekTime = server->dStart;
                 server->rc.Link.stopTime = server->dStop;
                 if (includeRs) {
                 server->rs.Link.seekTime = server->dStart;
                 server->rs.Link.stopTime = server->dStop;
                 }
         if (server->bLiveStream) {
                server->rc.Link.lFlags |= RTMP_LF_LIVE;
                if (includeRs)
                server->rs.Link.lFlags |= RTMP_LF_LIVE;
                }
         server->rc.Link.timeout = server->timeout;
         if (includeRs)
         server->rs.Link.timeout = server->timeout;

         server->rc.Link.protocol = server->protocol;
              server->rc.Link.hostname = *server->host;
              server->rc.Link.port = server->port;
              server->rc.Link.playpath = *server->playpath;
if (includeRs) {
        server->rs.Link.protocol = server->protocol;
                      server->rs.Link.hostname = *server->host;
                      server->rs.Link.port = server->port;
                      server->rs.Link.playpath = *server->playpath;
}
              if (server->rc.Link.port == 0)
              {
                  if (server->protocol & RTMP_FEATURE_SSL)
            	        server->rc.Link.port = 443;
                  else if (server->protocol & RTMP_FEATURE_HTTP)
            	        server->rc.Link.port = 80;
                  else
            	        server->rc.Link.port = 1935;
              }
              if (includeRs) {
              if (server->rs.Link.port == 0)
                            {
                                if (server->protocol & RTMP_FEATURE_SSL)
                          	        server->rs.Link.port = 443;
                                else if (server->protocol & RTMP_FEATURE_HTTP)
                          	        server->rs.Link.port = 80;
                                else
                          	        server->rs.Link.port = 1935;
                            }
                }
              if (server->sockshost->av_len)
                  {
                    const char *socksport = strchr(server->sockshost->av_val, ':');
                    char *hostname = strdup(server->sockshost->av_val);

                    if (socksport)
              	  hostname[socksport - server->sockshost->av_val] = '\0';
                    server->rc.Link.sockshost.av_val = server->hostname;
                    server->rc.Link.sockshost.av_len = strlen(server->hostname);

                    server->rc.Link.socksport = socksport ? atoi(socksport + 1) : 1080;
                    if (includeRs) {
                    server->rs.Link.sockshost.av_val = server->hostname;
                     server->rs.Link.sockshost.av_len = strlen(server->hostname);

                    server->rs.Link.socksport = socksport ? atoi(socksport + 1) : 1080;
                }
                  }
                else
                  {
                    server->rc.Link.sockshost.av_val = NULL;
                    server->rc.Link.sockshost.av_len = 0;
                    server->rc.Link.socksport = 0;
                    if (includeRs) {
                    server->rs.Link.sockshost.av_val = NULL;
                                        server->rs.Link.sockshost.av_len = 0;
                                        server->rs.Link.socksport = 0;
                                        }
                  }

                AVal av;
                for (i=0; i<3;i++) {
                    STR2AVAL(av, "S:");
                    RTMP_SetOpt(&server->rc, &av_conn, &av);
                    if (includeRs)
                        RTMP_SetOpt(&server->rs, &av_conn, &av);
                }
                RTMP_LogPrintf("Use token %s <\n", server->sToken);
                AVal tav;
                STR2AVAL(tav, server->sToken);
                RTMP_SetOpt(&server->rc, &av_conn, &tav);
                if (includeRs)
                    RTMP_SetOpt(&server->rs, &av_conn, &tav);
}

// Returns 0 for OK/Failed/error, 1 for 'Stop or Complete'
int
ServeInvoke(STREAMING_SERVER *server, int which, RTMPPacket *pack, const char *body)
{
  int ret = 0, nRes;
  int nBodySize = pack->m_nBodySize;

  if (body > pack->m_body)
    nBodySize--;

  if (body[0] != 0x02)		// make sure it is a string method name we start with
    {
      RTMP_Log(RTMP_LOGWARNING, "%s, Sanity failed. no string method in invoke packet",
	  __FUNCTION__);
      return 0;
    }

  AMFObject obj;
  nRes = AMF_Decode(&obj, body, nBodySize, FALSE);
  if (nRes < 0)
    {
      RTMP_Log(RTMP_LOGERROR, "%s, error decoding invoke packet", __FUNCTION__);
      return 0;
    }

  AMF_Dump(&obj);
  AVal method;
  AMFProp_GetString(AMF_GetProp(&obj, NULL, 0), &method);
  RTMP_Log(RTMP_LOGDEBUG, "%s, %s invoking <%s>", __FUNCTION__, cst[which], method.av_val);

  if (AVMATCH(&method, &av_connect))
    {
      AMFObject cobj;
      AVal pname, pval;

      RTMP_LogPrintf("Processing connect\n");
       RTMP_SetBufferMS(&server->rc, DEF_BUFTIME);
      if (!RTMP_Connect(&server->rc, pack))
        {
          /* failed */
          return 1;
        }
      server->rc.m_bSendCounter = FALSE;
    }
  else if (AVMATCH(&method, &av_play))
    {
      Flist *fl;

      FILE *out;
      char *file, *p, *q;
      char flvHeader[] = { 'F', 'L', 'V', 0x01,
         0x05,                       // video + audio, we finalize later if the value is different
         0x00, 0x00, 0x00, 0x09,
         0x00, 0x00, 0x00, 0x00      // first prevTagSize=0
       };
      int count = 0, flen;
        AVal av;
      //server->rc.m_stream_id = server->stream_id;
       av = *server->playpath;
      //server->rc.Link.playpath = av;

      server->rc.m_stream_id = server->stream_id;
        //    AMFProp_GetString(AMF_GetProp(&obj, NULL, 3), &av);
            server->rc.Link.playpath = av;
        server->rc.rtmpName = "RC";
    RTMP_LogPrintf( "stream id %d", server->rc.m_stream_id);
    RTMP_LogPrintf( "play path %s", av.av_val);

      if (!av.av_val)
        goto out;

      /* check for duplicates */
      for (fl = server->f_head; fl; fl=fl->f_next)
        {
          if (AVMATCH(&av, &fl->f_path))
            count++;
        }
      /* strip trailing URL parameters */
      q = memchr(av.av_val, '?', av.av_len);
      if (q)
        {
	  if (q == av.av_val)
	    {
	      av.av_val++;
	      av.av_len--;
	    }
	  else
	    {
              av.av_len = q - av.av_val;
	    }
	}
      /* strip leading slash components */
      for (p=av.av_val+av.av_len-1; p>=av.av_val; p--)
        if (*p == '/')
          {
            p++;
            av.av_len -= p - av.av_val;
            av.av_val = p;
            break;
          }
      /* skip leading dot */
      if (av.av_val[0] == '.')
        {
          av.av_val++;
          av.av_len--;
        }
      flen = av.av_len;
      /* hope there aren't more than 255 dups */
      if (count)
        flen += 2;
      file = malloc(flen+1);

      memcpy(file, av.av_val, av.av_len);
      if (count)
        sprintf(file+av.av_len, "%02x", count);
      else
        file[av.av_len] = '\0';
      for (p=file; *p; p++)
        if (*p == ':')
          *p = '_';
      RTMP_LogPrintf("Playpath: %.*s\nSaving as: %s\n",
        server->rc.Link.playpath.av_len, server->rc.Link.playpath.av_val,
        file);
      out = fopen(file, "wb");
      free(file);
      if (!out)
        ret = 1;
      else
        {
          fwrite(flvHeader, 1, sizeof(flvHeader), out);
          av = server->rc.Link.playpath;
          fl = malloc(sizeof(Flist)+av.av_len+1);
          fl->f_file = out;
          fl->f_path.av_len = av.av_len;
          fl->f_path.av_val = (char *)(fl+1);
          memcpy(fl->f_path.av_val, av.av_val, av.av_len);
          fl->f_path.av_val[av.av_len] = '\0';
          fl->f_next = NULL;
          if (server->f_tail)
            server->f_tail->f_next = fl;
          else
            server->f_head = fl;
          server->f_tail = fl;
        }
    }
    else if (AVMATCH(&method, &av__result)) {
            AMFObjectProperty *prop = AMF_GetProp(&obj, NULL, 3);
            RTMP_LogPrintf("Fetch result");
            if (prop->p_type == AMF_OBJECT) {
                AMFObjectProperty *sidprop = AMF_GetProp(&prop->p_vu.p_object, NULL, 4);
                RTMP_LogPrintf("Found %s object", sidprop->p_name.av_val);
                int sid = (int) sidprop->p_vu.p_number;
                if (sid > 2) {
                    server->stream_id = sid;
                }
            }
        }
  else if (AVMATCH(&method, &av_onStatus))
    {
      AMFObject obj2;
      AVal code, level;
      AMFProp_GetObject(AMF_GetProp(&obj, NULL, 3), &obj2);
      AMFProp_GetString(AMF_GetProp(&obj2, &av_code, -1), &code);
      AMFProp_GetString(AMF_GetProp(&obj2, &av_level, -1), &level);

      RTMP_Log(RTMP_LOGDEBUG, "%s, onStatus: %s", __FUNCTION__, code.av_val);
      if (AVMATCH(&code, &av_NetStream_Failed)
	  || AVMATCH(&code, &av_NetStream_Play_Failed)
	  || AVMATCH(&code, &av_NetStream_Play_StreamNotFound)
	  || AVMATCH(&code, &av_NetConnection_Connect_InvalidApp))
	{
	  ret = 1;
	}
      if (AVMATCH(&code, &av_NetStream_Play_Start))
	{
          /* set up the next stream */
          if (server->f_cur)
		    {
		      if (server->f_cur->f_next)
                server->f_cur = server->f_cur->f_next;
			}
          else
            {
              for (server->f_cur = server->f_head; server->f_cur &&
                    !server->f_cur->f_file; server->f_cur = server->f_cur->f_next) ;
            }
	  server->rc.m_bPlaying = TRUE;
	}

      // Return 1 if this is a Play.Complete or Play.Stop
      if (AVMATCH(&code, &av_NetStream_Play_Complete)
	  || AVMATCH(&code, &av_NetStream_Play_Stop))
	{
	  ret = 1;
	}
    }

  else if (AVMATCH(&method, &av_closeStream))
    {
      ret = 1;
    }
  else if (AVMATCH(&method, &av_close))
    {
      RTMP_Log(RTMP_LOGDEBUG, "Close from av_close");
      RTMP_Close(&server->rc);
      ret = 1;
    }
out:
  AMF_Reset(&obj);
  return ret;
}

int
ServePacket(STREAMING_SERVER *server, int which, RTMPPacket *packet)
{
  int ret = 0;

  RTMP_Log(RTMP_LOGDEBUG, "%s, %s sent packet type %02X, size %u bytes", __FUNCTION__,
    cst[which], packet->m_packetType, packet->m_nBodySize);

  switch (packet->m_packetType)
    {
    case RTMP_PACKET_TYPE_CHUNK_SIZE:
      // chunk size
//      HandleChangeChunkSize(r, packet);
      break;

    case RTMP_PACKET_TYPE_BYTES_READ_REPORT:
      // bytes read report
      break;

    case RTMP_PACKET_TYPE_CONTROL:
      // ctrl
//      HandleCtrl(r, packet);
      break;

    case RTMP_PACKET_TYPE_SERVER_BW:
      // server bw
//      HandleServerBW(r, packet);
      break;

    case RTMP_PACKET_TYPE_CLIENT_BW:
      // client bw
 //     HandleClientBW(r, packet);
      break;

    case RTMP_PACKET_TYPE_AUDIO:
      // audio data
      //RTMP_Log(RTMP_LOGDEBUG, "%s, received: audio %lu bytes", __FUNCTION__, packet.m_nBodySize);
      break;

    case RTMP_PACKET_TYPE_VIDEO:
      // video data
      //RTMP_Log(RTMP_LOGDEBUG, "%s, received: video %lu bytes", __FUNCTION__, packet.m_nBodySize);
      break;

    case RTMP_PACKET_TYPE_FLEX_STREAM_SEND:
      // flex stream send
      break;

    case RTMP_PACKET_TYPE_FLEX_SHARED_OBJECT:
      // flex shared object
      break;

    case RTMP_PACKET_TYPE_FLEX_MESSAGE:
      // flex message
      {
	ret = ServeInvoke(server, which, packet, packet->m_body + 1);
	break;
      }
    case RTMP_PACKET_TYPE_INFO:
      // metadata (notify)
      break;

    case RTMP_PACKET_TYPE_SHARED_OBJECT:
      /* shared object */
      break;

    case RTMP_PACKET_TYPE_INVOKE:
      // invoke
      ret = ServeInvoke(server, which, packet, packet->m_body);
      break;

    case RTMP_PACKET_TYPE_FLASH_VIDEO:
      /* flv */
	break;

    default:
      RTMP_Log(RTMP_LOGDEBUG, "%s, unknown packet type received: 0x%02x", __FUNCTION__,
	  packet->m_packetType);
#ifdef _DEBUG
      RTMP_LogHex(RTMP_LOGDEBUG, packet->m_body, packet->m_nBodySize);
#endif
    }
  return ret;
}

int
WriteStream(char **buf,	// target pointer, maybe preallocated
	    unsigned int *plen,	// length of buffer if preallocated
            uint32_t *nTimeStamp,
            RTMPPacket *packet)
{
  uint32_t prevTagSize = 0;
  int ret = -1, len = *plen;

  while (1)
    {
      char *packetBody = packet->m_body;
      unsigned int nPacketLen = packet->m_nBodySize;

      // skip video info/command packets
      if (packet->m_packetType == RTMP_PACKET_TYPE_VIDEO &&
	  nPacketLen == 2 && ((*packetBody & 0xf0) == 0x50))
	{
	  ret = 0;
	  break;
	}

      if (packet->m_packetType == RTMP_PACKET_TYPE_VIDEO && nPacketLen <= 5)
	{
	  RTMP_Log(RTMP_LOGWARNING, "ignoring too small video packet: size: %d",
	      nPacketLen);
	  ret = 0;
	  break;
	}
      if (packet->m_packetType == RTMP_PACKET_TYPE_AUDIO && nPacketLen <= 1)
	{
	  RTMP_Log(RTMP_LOGWARNING, "ignoring too small audio packet: size: %d",
	      nPacketLen);
	  ret = 0;
	  break;
	}
#ifdef _DEBUG
      RTMP_Log(RTMP_LOGDEBUG, "type: %02X, size: %d, TS: %d ms", packet->m_packetType,
	  nPacketLen, packet->m_nTimeStamp);
      if (packet->m_packetType == RTMP_PACKET_TYPE_VIDEO)
	RTMP_Log(RTMP_LOGDEBUG, "frametype: %02X", (*packetBody & 0xf0));
#endif

      // calculate packet size and reallocate buffer if necessary
      unsigned int size = nPacketLen
	+
	((packet->m_packetType == RTMP_PACKET_TYPE_AUDIO
          || packet->m_packetType == RTMP_PACKET_TYPE_VIDEO
	  || packet->m_packetType == RTMP_PACKET_TYPE_INFO) ? 11 : 0)
        + (packet->m_packetType != 0x16 ? 4 : 0);

      if (size + 4 > len)
	{
          /* The extra 4 is for the case of an FLV stream without a last
           * prevTagSize (we need extra 4 bytes to append it).  */
	  *buf = (char *) realloc(*buf, size + 4);
	  if (*buf == 0)
	    {
	      RTMP_Log(RTMP_LOGERROR, "Couldn't reallocate memory!");
	      ret = -1;		// fatal error
	      break;
	    }
	}
      char *ptr = *buf, *pend = ptr + size+4;

      /* audio (RTMP_PACKET_TYPE_AUDIO), video (RTMP_PACKET_TYPE_VIDEO)
       * or metadata (RTMP_PACKET_TYPE_INFO) packets: construct 11 byte
       * header then add rtmp packet's data.  */
      if (packet->m_packetType == RTMP_PACKET_TYPE_AUDIO
          || packet->m_packetType == RTMP_PACKET_TYPE_VIDEO
	  || packet->m_packetType == RTMP_PACKET_TYPE_INFO)
	{
	  // set data type
	  //*dataType |= (((packet->m_packetType == RTMP_PACKET_TYPE_AUDIO)<<2)|(packet->m_packetType == RTMP_PACKET_TYPE_VIDEO));

	  (*nTimeStamp) = packet->m_nTimeStamp;
	  prevTagSize = 11 + nPacketLen;

	  *ptr++ = packet->m_packetType;
	  ptr = AMF_EncodeInt24(ptr, pend, nPacketLen);
	  ptr = AMF_EncodeInt24(ptr, pend, *nTimeStamp);
	  *ptr = (char) (((*nTimeStamp) & 0xFF000000) >> 24);
	  ptr++;

	  // stream id
	  ptr = AMF_EncodeInt24(ptr, pend, 0);
	}

      memcpy(ptr, packetBody, nPacketLen);
      unsigned int len = nPacketLen;

      // correct tagSize and obtain timestamp if we have an FLV stream
      if (packet->m_packetType == RTMP_PACKET_TYPE_FLASH_VIDEO)
	{
	  unsigned int pos = 0;

	  while (pos + 11 < nPacketLen)
	    {
	      uint32_t dataSize = AMF_DecodeInt24(packetBody + pos + 1);	// size without header (11) and without prevTagSize (4)
	      *nTimeStamp = AMF_DecodeInt24(packetBody + pos + 4);
	      *nTimeStamp |= (packetBody[pos + 7] << 24);

    #if 0
	      /* set data type */
	      *dataType |= (((*(packetBody+pos) == RTMP_PACKET_TYPE_AUDIO) << 2)
                            | (*(packetBody+pos) == RTMP_PACKET_TYPE_VIDEO));
    #endif

	      if (pos + 11 + dataSize + 4 > nPacketLen)
		{
		  if (pos + 11 + dataSize > nPacketLen)
		    {
		      RTMP_Log(RTMP_LOGERROR,
			  "Wrong data size (%u), stream corrupted, aborting!",
			  dataSize);
		      ret = -2;
		      break;
		    }
		  RTMP_Log(RTMP_LOGWARNING, "No tagSize found, appending!");

		  // we have to append a last tagSize!
		  prevTagSize = dataSize + 11;
		  AMF_EncodeInt32(ptr + pos + 11 + dataSize, pend, prevTagSize);
		  size += 4;
		  len += 4;
		}
	      else
		{
		  prevTagSize =
		    AMF_DecodeInt32(packetBody + pos + 11 + dataSize);

		  if (prevTagSize != (dataSize + 11))
		    {

		      prevTagSize = dataSize + 11;
		      AMF_EncodeInt32(ptr + pos + 11 + dataSize, pend, prevTagSize);
		    }
		}

	      pos += prevTagSize + 4;	//(11+dataSize+4);
	    }
	}
      ptr += len;

      if (packet->m_packetType != RTMP_PACKET_TYPE_FLASH_VIDEO)
	{			// FLV tag packets contain their own prevTagSize
	  AMF_EncodeInt32(ptr, pend, prevTagSize);
	  //ptr += 4;
	}

      ret = size;
      break;
    }

  if (len > *plen)
    *plen = len;

  return ret;			// no more media packets
}

TFTYPE
controlServerThread(void *unused)
{
  while (1) {
    if (RTMP_ctrlC) {
	  RTMP_LogPrintf("Exiting\n");
	  stopStreaming(rtmpServer);
          free(rtmpServer);
          break;
	}
   }
  TFRET();
}

TFTYPE doServe(void *arg)	// server socket and state (our listening socket)
{
  STREAMING_SERVER *server = arg;
  RTMPPacket pc = { 0 }, ps = { 0 };
  RTMPChunk rk = { 0 };
  char *buf = NULL;
  unsigned int buflen = 131072;
  int paused = FALSE;
  int sockfd = server->socket;

  // timeout for http requests
  fd_set rfds;
  struct timeval tv;

  server->state = STREAMING_IN_PROGRESS;

  memset(&tv, 0, sizeof(struct timeval));
  tv.tv_sec = 5;

  FD_ZERO(&rfds);
  FD_SET(sockfd, &rfds);

  if (select(sockfd + 1, &rfds, NULL, NULL, &tv) <= 0)
    {
      RTMP_Log(RTMP_LOGERROR, "Request timeout/select failed, ignoring request");
      goto quit;
    }
  else
    {
      server->rs.rtmpName = "RS";
      server->rc.rtmpName = "RC";

      RTMP_Init(&server->rs);
      RTMP_Init(&server->rc);
      server->rs.m_sb.sb_socket = sockfd;
      RTMP_Pass_Arg(server, 1);
      RTMP_SetBufferMS(&server->rc, DEF_BUFTIME);
      RTMP_SetBufferMS(&server->rs, DEF_BUFTIME);

      if (!RTMP_Serve(&server->rs))
        {
          RTMP_Log(RTMP_LOGERROR, "Handshake failed");
          goto cleanup;
        }
    }

  buf = malloc(buflen);

  /* Just process the Connect request */
  while (RTMP_IsConnected(&server->rs) && RTMP_ReadPacket(&server->rs, &ps))
    {
      if (!RTMPPacket_IsReady(&ps))
        continue;
      ServePacket(server, 0, &ps);
      RTMPPacket_Free(&ps);
      if (RTMP_IsConnected(&server->rc))
        break;
    }

  pc.m_chunk = &rk;

  /* We have our own timeout in select() */
  server->rc.Link.timeout = 10000;
  server->rs.Link.timeout = 10000;
  while (RTMP_IsConnected(&server->rs) || RTMP_IsConnected(&server->rc))
    {
      int n;
      int sr, cr;

      cr = server->rc.m_sb.sb_size;
      sr = server->rs.m_sb.sb_size;

      if (cr || sr)
        {
        }
      else
        {
          n = server->rs.m_sb.sb_socket;
	  if (server->rc.m_sb.sb_socket > n)
	    n = server->rc.m_sb.sb_socket;
	  FD_ZERO(&rfds);
	  if (RTMP_IsConnected(&server->rs))
	    FD_SET(sockfd, &rfds);
	  if (RTMP_IsConnected(&server->rc))
	    FD_SET(server->rc.m_sb.sb_socket, &rfds);

          /* give more time to start up if we're not playing yet */
	  tv.tv_sec = server->f_cur ? 30 : 60;
	  tv.tv_usec = 0;

	  if (select(n + 1, &rfds, NULL, NULL, &tv) <= 0)
	    {
              if (server->f_cur && server->rc.m_mediaChannel && !paused)
                {
                  server->rc.m_pauseStamp = server->rc.m_channelTimestamp[server->rc.m_mediaChannel];
                  if (RTMP_ToggleStream(&server->rc))
                    {
                      paused = TRUE;
                      continue;
                    }
                }
	      RTMP_Log(RTMP_LOGERROR, "Request timeout/select failed, ignoring request");
	      goto cleanup;
	    }
          if (server->rs.m_sb.sb_socket > 0 &&
	    FD_ISSET(server->rs.m_sb.sb_socket, &rfds))
            sr = 1;
          if (server->rc.m_sb.sb_socket > 0 &&
	    FD_ISSET(server->rc.m_sb.sb_socket, &rfds))
            cr = 1;
        }
      if (sr)
        {
          while (RTMP_ReadPacket(&server->rs, &ps)) {
           if (server->rc.m_stream_id > 2) {
                char *ptrs = ps.m_body+2;
                int streamId = AMF_DecodeInt32(ptrs);
                RTMP_Log(RTMP_LOGDEBUG, "Change request client id %d to fixed client id %d", streamId, server->rc.m_stream_id);
                AMF_EncodeInt32(ptrs, ptrs+4, server->rc.m_stream_id);
            }

            if (RTMPPacket_IsReady(&ps))
              {
                /* change chunk size */
                if (ps.m_packetType == RTMP_PACKET_TYPE_CHUNK_SIZE)
                  {
                    if (ps.m_nBodySize >= 4)
                      {
                        server->rs.m_inChunkSize = AMF_DecodeInt32(ps.m_body);
                        RTMP_Log(RTMP_LOGDEBUG, "%s, client: chunk size change to %d", __FUNCTION__,
                            server->rs.m_inChunkSize);
                        server->rc.m_outChunkSize = server->rs.m_inChunkSize;
                      }
                  }
                /* bytes received */
                else if (ps.m_packetType == RTMP_PACKET_TYPE_BYTES_READ_REPORT)
                  {
                    if (ps.m_nBodySize >= 4)
                      {
                        int count = AMF_DecodeInt32(ps.m_body);
                        RTMP_Log(RTMP_LOGDEBUG, "%s, client: bytes received = %d", __FUNCTION__,
                            count);
                      }
                  }
                /* ctrl */
                else if (ps.m_packetType == RTMP_PACKET_TYPE_CONTROL)
                  {
                    short nType = AMF_DecodeInt16(ps.m_body);
                    /* UpdateBufferMS */
                    if (nType == 0x03)
                      {
                        char *ptr = ps.m_body+2;
                        int id;
                        int len;
                        id = AMF_DecodeInt32(ptr);
                       // id = server->rc.m_stream_id;

                        /* Assume the interesting media is on a non-zero stream */
                        RTMP_LogPrintf( "Client id %d request new buffer time", id);
                        if (id)
                          {
                            len = AMF_DecodeInt32(ptr+4);
                            /* request a big buffer */
                            if (len < BUFFERTIME)
                              {
                                AMF_EncodeInt32(ptr+4, ptr+8, BUFFERTIME);
                              }
                            RTMP_LogPrintf("%s, client: BufferTime change in stream %d from %d to %d", __FUNCTION__,
                                id, len, BUFFERTIME);
                          }
                      }
                  }
                else if (ps.m_packetType == RTMP_PACKET_TYPE_FLEX_MESSAGE
                         || ps.m_packetType == RTMP_PACKET_TYPE_INVOKE)
                  {
                    if (ServePacket(server, 0, &ps) && server->f_cur)
                      {
                        fclose(server->f_cur->f_file);
                        server->f_cur->f_file = NULL;
                        server->f_cur = NULL;
                      }
                  }
                RTMP_SendPacket(&server->rc, &ps, FALSE);
                RTMPPacket_Free(&ps);
                break;
              }
              }
        }
      if (cr)
        {
          while (RTMP_ReadPacket(&server->rc, &pc))
            {
              int sendit = 1;
              if (RTMPPacket_IsReady(&pc))
                {
                  if (paused)
                    {
                      if (pc.m_nTimeStamp <= server->rc.m_mediaStamp)
                        continue;
                      paused = 0;
                      server->rc.m_pausing = 0;
                    }
                  /* change chunk size */
                  if (pc.m_packetType == RTMP_PACKET_TYPE_CHUNK_SIZE)
                    {
                      if (pc.m_nBodySize >= 4)
                        {
                          server->rc.m_inChunkSize = AMF_DecodeInt32(pc.m_body);
                          RTMP_Log(RTMP_LOGDEBUG, "%s, server: chunk size change to %d", __FUNCTION__,
                              server->rc.m_inChunkSize);
                          server->rs.m_outChunkSize = server->rc.m_inChunkSize;
                        }
                    }
                  else if (pc.m_packetType == RTMP_PACKET_TYPE_CONTROL)
                    {
                          short nType = AMF_DecodeInt16(pc.m_body);
                          /* SWFverification */
                          if (nType == 0x1a)
                        #ifdef CRYPTO
                                                if (server->rc.Link.SWFSize)
                                                {
                                                  RTMP_SendCtrl(&server->rc, 0x1b, 0, 0);
                                                  sendit = 0;
                                                }
                        #else
                                                /* The session will certainly fail right after this */
                                                RTMP_Log(RTMP_LOGERROR, "%s, server requested SWF verification, need CRYPTO support! ", __FUNCTION__);
                        #endif
                    }
                  else if (server->f_cur && (
                       pc.m_packetType == RTMP_PACKET_TYPE_AUDIO ||
                       pc.m_packetType == RTMP_PACKET_TYPE_VIDEO ||
                       pc.m_packetType == RTMP_PACKET_TYPE_INFO ||
                       pc.m_packetType == RTMP_PACKET_TYPE_FLASH_VIDEO) &&
                       RTMP_ClientPacket(&server->rc, &pc))
                    {
                      int len = WriteStream(&buf, &buflen, &server->stamp, &pc);
                      if (len > 0 && fwrite(buf, 1, len, server->f_cur->f_file) != len)
                        goto cleanup;
                    }
                  else if (pc.m_packetType == RTMP_PACKET_TYPE_FLEX_MESSAGE ||
                           pc.m_packetType == RTMP_PACKET_TYPE_INVOKE)
                    {
                      if (ServePacket(server, 1, &pc) && server->f_cur)
                        {
                          fclose(server->f_cur->f_file);
                          server->f_cur->f_file = NULL;
                          server->f_cur = NULL;
                        }
                    }
                }
              if (sendit && RTMP_IsConnected(&server->rs))
                RTMP_SendChunk(&server->rs, &rk);
              if (RTMPPacket_IsReady(&pc))
                  RTMPPacket_Free(&pc);
              break;
            }
        }
      if (!RTMP_IsConnected(&server->rs) && RTMP_IsConnected(&server->rc)
        && !server->f_cur) {
        RTMP_Log(RTMP_LOGDEBUG, "Close from end of stream");
        RTMP_Close(&server->rc);
        }
    }

cleanup:
  RTMP_LogPrintf("Closing connection... ");
  RTMP_Close(&server->rs);
  RTMP_Close(&server->rc);
  while (server->f_head)
    {
      Flist *fl = server->f_head;
      server->f_head = fl->f_next;
      if (fl->f_file)
        fclose(fl->f_file);
      free(fl);
    }
  server->f_tail = NULL;
  server->f_cur = NULL;
  free(buf);
  /* Should probably be done by RTMP_Close() ... */
  server->rc.Link.hostname.av_val = NULL;
  server->rc.Link.tcUrl.av_val = NULL;
  server->rc.Link.swfUrl.av_val = NULL;
  server->rc.Link.pageUrl.av_val = NULL;
  server->rc.Link.app.av_val = NULL;
  server->rc.Link.auth.av_val = NULL;
  server->rc.Link.flashVer.av_val = NULL;
  RTMP_LogPrintf("done!\n\n");

quit:
  if (server->state == STREAMING_IN_PROGRESS)
    server->state = STREAMING_ACCEPTING;

  TFRET();
}

TFTYPE
serverThread(void *arg)
{
  STREAMING_SERVER *server = arg;
  server->state = STREAMING_ACCEPTING;

  while (server->state == STREAMING_ACCEPTING)
    {
      struct sockaddr_in addr;
      socklen_t addrlen = sizeof(struct sockaddr_in);
      STREAMING_SERVER *srv2 = malloc(sizeof(STREAMING_SERVER));
      int sockfd =
	accept(server->socket, (struct sockaddr *) &addr, &addrlen);

      if (sockfd > 0)
	{
#ifdef linux
          struct sockaddr_in dest;
	  char destch[16];
          socklen_t destlen = sizeof(struct sockaddr_in);
	  getsockopt(sockfd, SOL_IP, SO_ORIGINAL_DST, &dest, &destlen);
          strcpy(destch, inet_ntoa(dest.sin_addr));
	  RTMP_Log(RTMP_LOGDEBUG, "%s: accepted connection from %s to %s\n", __FUNCTION__,
	      inet_ntoa(addr.sin_addr), destch);
#else
	  RTMP_Log(RTMP_LOGDEBUG, "%s: accepted connection from %s\n", __FUNCTION__,
	      inet_ntoa(addr.sin_addr));
#endif
	  *srv2 = *server;
	  srv2->socket = sockfd;
	  /* Create a new thread and transfer the control to that */
	  ThreadCreate(doServe, srv2);
	  RTMP_Log(RTMP_LOGDEBUG, "%s: processed request\n", __FUNCTION__);
	}
      else
	{
	  RTMP_Log(RTMP_LOGERROR, "%s: accept failed", __FUNCTION__);
	}
    }
  server->state = STREAMING_STOPPED;
  TFRET();
}

STREAMING_SERVER *
startStreaming(const char *address, int port,
                 int protocol,
        		 AVal *host,
        		 unsigned int remote_port,
        		 AVal *sockshost,
        		 AVal *playpath,
        		 AVal *tcUrl,
        		 AVal *swfUrl,
        		 AVal *pageUrl,
        		 AVal *app,
        		 AVal *auth,
        		 AVal *swfSHA256Hash,
        		 uint32_t swfSize,
        		 AVal *flashVer,
        		 AVal *subscribepath,
        		 AVal *usherToken,
        		 int dStart,
        		 int dStop, int bLiveStream, long int timeout,char *sToken)
{
  struct sockaddr_in addr;
  int sockfd, tmp;
  STREAMING_SERVER *server;

  sockfd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
  if (sockfd == -1)
    {
      RTMP_Log(RTMP_LOGERROR, "%s, couldn't create socket", __FUNCTION__);
      return 0;
    }

  tmp = 1;
  setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR,
				(char *) &tmp, sizeof(tmp) );

  addr.sin_family = AF_INET;
  addr.sin_addr.s_addr = inet_addr(address);	//htonl(INADDR_ANY);
  addr.sin_port = htons(port);

  if (bind(sockfd, (struct sockaddr *) &addr, sizeof(struct sockaddr_in)) ==
      -1)
    {
      RTMP_Log(RTMP_LOGERROR, "%s, TCP bind failed for port number: %d", __FUNCTION__,
	  port);
      return 0;
    }

  if (listen(sockfd, 10) == -1)
    {
      RTMP_Log(RTMP_LOGERROR, "%s, listen failed", __FUNCTION__);
      closesocket(sockfd);
      return 0;
    }

  server = (STREAMING_SERVER *) calloc(1, sizeof(STREAMING_SERVER));
  // Added by Hai, pass all argument
  server->protocol = protocol;
  server->host = host;
  server->hostname = host;
  server->port = remote_port;
  server->sockshost = sockshost;
  server->playpath = playpath;
  server->tcUrl = tcUrl;
  server->swfUrl = swfUrl;
  server->pageUrl = pageUrl;
  server->app = app;
  server->auth = auth;
  server->swfSHA256Hash = swfSHA256Hash;
  server->swfSize = swfSize;
  server->flashVer = flashVer;
  server->subscribepath = subscribepath;
  server->usherToken = usherToken;
  server->dStart = dStart;
  server->dStop = dStop;
  server->bLiveStream = bLiveStream;
  server->timeout = timeout;
  server->sToken = sToken;

  server->socket = sockfd;

  ThreadCreate(serverThread, server);

  return server;
}

void
stopStreaming(STREAMING_SERVER * server)
{
  assert(server);

  if (server->state != STREAMING_STOPPED)
    {
      int fd = server->socket;
      server->socket = 0;
      if (server->state == STREAMING_IN_PROGRESS)
	{
	  server->state = STREAMING_STOPPING;

	  // wait for streaming threads to exit
	  while (server->state != STREAMING_STOPPED)
	    msleep(1);
	}

      if (fd && closesocket(fd))
	RTMP_Log(RTMP_LOGERROR, "%s: Failed to close listening socket, error %d",
	    __FUNCTION__, GetSockError());

      server->state = STREAMING_STOPPED;
    }
}


void
sigIntHandler(int sig)
{
  RTMP_ctrlC = TRUE;
  RTMP_LogPrintf("Caught signal: %d, cleaning up, just a second...\n", sig);
  if (rtmpServer)
    stopStreaming(rtmpServer);
  signal(SIGINT, SIG_DFL);
}

int
main_rtmpsuck(int argc, char **argv, char *sToken)
{
  // Parsing arguments
  extern char *optarg;

 int bOverrideBufferTime = FALSE;	// if the user specifies a buffer time override this is true
 int bStdoutMode = FALSE;	// if true print the stream directly to stdout, messages go to stderr
 int bResume = FALSE;		// true in resume mode

    // meta header and initial frame for the resume mode (they are read from the file and compared with
    // the stream we are trying to continue
 char *metaHeader = 0;
 uint32_t nMetaHeaderSize = 0;
    // video keyframe for matching
 char *initialFrame = 0;
 uint32_t nInitialFrameSize = 0;
 int initialFrameType = 0;	// tye: audio or video
 AVal hostname = { 0, 0 };
 AVal playpath = { 0, 0 };
 AVal subscribepath = { 0, 0 };
 AVal usherToken = { 0, 0 }; //Justin.tv auth token
 int port = -1;
 int protocol = RTMP_PROTOCOL_UNDEFINED;
 int retries = 0;
 int bLiveStream = FALSE;	// is it a live stream? then we can't seek/resume
 int bHashes = FALSE;		// display byte counters not hashes by default
 uint32_t dSeek = 0;		// seek position in resume mode, 0 otherwise
 uint32_t dStartOffset = 0;	// seek position in non-live mode
 uint32_t dStopOffset = 0;
 int timeout = 30000;
    AVal swfUrl = { 0, 0 };
    AVal tcUrl = { 0, 0 };
    AVal pageUrl = { 0, 0 };
    AVal app = { 0, 0 };
    AVal auth = { 0, 0 };
    AVal swfHash = { 0, 0 };
    uint32_t swfSize = 0;
    AVal flashVer = { 0, 0 };
    AVal sockshost = { 0, 0 };
  #ifdef CRYPTO
    int swfAge = 30;	/* 30 days for SWF cache by default */
    int swfVfy = 0;
    unsigned char hash[RTMP_SWF_HASHLEN];
  #endif

    char *flvFile = 0;

 RTMP_debuglevel = RTMP_LOGALL;
 // RTMP_debuglevel = RTMP_LOGINFO;

 RTMP_LogPrintf("Check all arguments\n");
    int opt;
    struct option longopts[] = {
      {"help", 0, NULL, 'h'},
      {"host", 1, NULL, 'n'},
      {"port", 1, NULL, 'c'},
      {"socks", 1, NULL, 'S'},
      {"protocol", 1, NULL, 'l'},
      {"playpath", 1, NULL, 'y'},
      {"playlist", 0, NULL, 'Y'},
      {"rtmp", 1, NULL, 'r'},
      {"swfUrl", 1, NULL, 's'},
      {"tcUrl", 1, NULL, 't'},
      {"pageUrl", 1, NULL, 'p'},
      {"app", 1, NULL, 'a'},
      {"auth", 1, NULL, 'u'},
      {"conn", 1, NULL, 'C'},
  #ifdef CRYPTO
      {"swfhash", 1, NULL, 'w'},
      {"swfsize", 1, NULL, 'x'},
      {"swfVfy", 1, NULL, 'W'},
      {"swfAge", 1, NULL, 'X'},
  #endif
      {"flashVer", 1, NULL, 'f'},
      {"live", 0, NULL, 'v'},
      {"flv", 1, NULL, 'o'},
      {"resume", 0, NULL, 'e'},
      {"timeout", 1, NULL, 'm'},
      {"buffer", 1, NULL, 'b'},
      {"skip", 1, NULL, 'k'},
      {"subscribe", 1, NULL, 'd'},
      {"start", 1, NULL, 'A'},
      {"stop", 1, NULL, 'B'},
      {"token", 1, NULL, 'T'},
      {"hashes", 0, NULL, '#'},
      {"debug", 0, NULL, 'z'},
      {"quiet", 0, NULL, 'q'},
      {"verbose", 0, NULL, 'V'},
      {"jtv", 1, NULL, 'j'},
      {0, 0, 0, 0}
    };
      RTMP_LogPrintf("Passing arguments ...\n");
    optind = 1;
    while ((opt =
  	  getopt_long(argc, argv,
  		      "hVveqzr:s:t:p:a:b:f:o:u:C:n:c:l:y:Ym:k:d:A:B:T:w:x:W:X:S:#j:",
  		      longopts, NULL)) != -1)
      {
        switch (opt)
  	{

  #ifdef CRYPTO
  	case 'w':
  	  {
  	    int res = hex2bin(optarg, &swfHash.av_val);
  	    if (res != RTMP_SWF_HASHLEN)
  	      {
  		swfHash.av_val = NULL;
  		RTMP_Log(RTMP_LOGWARNING,
  		    "Couldn't parse swf hash hex string, not hexstring or not %d bytes, ignoring!", RTMP_SWF_HASHLEN);
  	      }
  	    swfHash.av_len = RTMP_SWF_HASHLEN;
  	    break;
  	  }
  	case 'x':
  	  {
  	    int size = atoi(optarg);
  	    if (size <= 0)
  	      {
  		RTMP_Log(RTMP_LOGERROR, "SWF Size must be at least 1, ignoring\n");
  	      }
  	    else
  	      {
  		swfSize = size;
  	      }
  	    break;
  	  }
          case 'W':
  	  STR2AVAL(swfUrl, optarg);
  	  swfVfy = 1;
            break;
          case 'X':
  	  {
  	    int num = atoi(optarg);
  	    if (num < 0)
  	      {
  		RTMP_Log(RTMP_LOGERROR, "SWF Age must be non-negative, ignoring\n");
  	      }
  	    else
  	      {
  		swfAge = num;
  	      }
  	  }
            break;
  #endif
  	case 'k':
  	  // Removed
  	  break;
  	case 'b':
  	  // Removed
  	  break;
  	case 'v':
  	  bLiveStream = TRUE;	// no seeking or resuming possible!
  	  break;
  	case 'd':
  	  STR2AVAL(subscribepath, optarg);
  	  break;
  	case 'n':
  	  STR2AVAL(hostname, optarg);
  	  break;
  	case 'c':
  	  port = atoi(optarg);
  	  break;
  	case 'l':
  	  protocol = atoi(optarg);
  	  if (protocol < RTMP_PROTOCOL_RTMP || protocol > RTMP_PROTOCOL_RTMPTS)
  	    {
  	      RTMP_Log(RTMP_LOGERROR, "Unknown protocol specified: %d", protocol);
  	    }
  	  break;
  	case 'y':
  	  STR2AVAL(playpath, optarg);
  	  break;
  	case 'Y':
  	 // Skip
  	 //  RTMP_SetOpt(&rtmp, &av_playlist, (AVal *)&av_true);
  	  break;
  	case 'r':
  	  {
  	    RTMP_LogPrintf("Step parse host %s\n", optarg);
  	    AVal parsedHost, parsedApp, parsedPlaypath;
  	    unsigned int parsedPort = 0;
  	    int parsedProtocol = RTMP_PROTOCOL_UNDEFINED;

  	    if (!RTMP_ParseURL
  		(optarg, &parsedProtocol, &parsedHost, &parsedPort,
  		 &parsedPlaypath, &parsedApp))
  	      {
  		RTMP_Log(RTMP_LOGWARNING, "Couldn't parse the specified url (%s)!",
  		    optarg);
  	      }
  	    else
  	      {
  		if (!hostname.av_len)
  		  hostname = parsedHost;
  		  RTMP_LogPrintf("Hostname =  %s\n", hostname);
  		if (port == -1)
  		  port = parsedPort;
  		if (playpath.av_len == 0 && parsedPlaypath.av_len)
  		  {
  		    playpath = parsedPlaypath;
  		  }
  		if (protocol == RTMP_PROTOCOL_UNDEFINED)
  		  protocol = parsedProtocol;
  		if (app.av_len == 0 && parsedApp.av_len)
  		  {
  		    app = parsedApp;
  		  }
  	      }
  	    break;
  	  }
  	case 's':
  	  STR2AVAL(swfUrl, optarg);
  	  break;
  	case 't':
  	  STR2AVAL(tcUrl, optarg);
  	  break;
  	case 'p':
  	  STR2AVAL(pageUrl, optarg);
  	  break;
  	case 'a':
  	  STR2AVAL(app, optarg);
  	  break;
  	case 'f':
  	  STR2AVAL(flashVer, optarg);
  	  break;
  	case 'o':
  	  flvFile = optarg;
  	  if (strcmp(flvFile, "-"))
  	    bStdoutMode = FALSE;
  	  break;
  	case 'e':
  	  bResume = TRUE;
  	  break;
  	case 'u':
  	  STR2AVAL(auth, optarg);
  	  break;
  	case 'm':
  	 // timeout = atoi(optarg);
  	  break;
  	case 'A':
  	  dStartOffset = (int) (atof(optarg) * 1000.0);
  	  break;
  	case 'B':
  	  dStopOffset = (int) (atof(optarg) * 1000.0);
  	  break;
  	case 'T': {
  	  AVal token;
  	  STR2AVAL(token, optarg);
  	  }
  	  break;
  	case '#':
  	  bHashes = TRUE;
  	  break;
  	case 'q':
  	  RTMP_debuglevel = RTMP_LOGCRIT;
  	  break;
  	case 'V':
  	  RTMP_debuglevel = RTMP_LOGDEBUG;
  	  break;
  	case 'z':
  	  RTMP_debuglevel = RTMP_LOGALL;
  	  break;
  	case 'S':
  	  STR2AVAL(sockshost, optarg);
  	  break;
  	case 'j':
  	  STR2AVAL(usherToken, optarg);
  	  break;
  	default:
  	 // RTMP_LogPrintf("unknown option: %c\n", opt);
  	 // usage(argv[0]);
  	 // return RD_FAILED;
  	  break;
  	}
      }
    RTMP_LogPrintf("Check Hostname =  %s\n", hostname);
    if (!hostname.av_len)
     {
        RTMP_Log(RTMP_LOGERROR,
  	  "You must specify a hostname (--host) or url (-r \"rtmp://host[:port]/playpath\") containing a hostname");
        return RD_FAILED;
     }
    if (playpath.av_len == 0)
      {
        RTMP_Log(RTMP_LOGERROR,
  	  "You must specify a playpath (--playpath) or url (-r \"rtmp://host[:port]/playpath\") containing a playpath");
        return RD_FAILED;
      }

    if (protocol == RTMP_PROTOCOL_UNDEFINED)
      {
        RTMP_Log(RTMP_LOGWARNING,
  	  "You haven't specified a protocol (--protocol) or rtmp url (-r), using default protocol RTMP");
        protocol = RTMP_PROTOCOL_RTMP;
      }
    if (port == -1)
      {
        RTMP_Log(RTMP_LOGWARNING,
  	  "You haven't specified a port (--port) or rtmp url (-r), using default port 1935");
        port = 0;
      }
    if (port == 0)
      {
        if (protocol & RTMP_FEATURE_SSL)
  	port = 443;
        else if (protocol & RTMP_FEATURE_HTTP)
  	port = 80;
        else
  	port = 1935;
      }

    if (flvFile == 0)
      {
        RTMP_Log(RTMP_LOGWARNING,
  	  "You haven't specified an output file (-o filename), using stdout");
        //bStdoutMode = TRUE;
      }

    if (bStdoutMode && bResume)
      {
        RTMP_Log(RTMP_LOGWARNING,
  	  "Can't resume in stdout mode, ignoring --resume option");
        bResume = FALSE;
      }

    if (bLiveStream && bResume)
      {
        RTMP_Log(RTMP_LOGWARNING, "Can't resume live stream, ignoring --resume option");
        bResume = FALSE;
      }

  #ifdef CRYPTO
    if (swfVfy)
      {
        if (RTMP_HashSWF(swfUrl.av_val, &swfSize, hash, swfAge) == 0)
          {
            swfHash.av_val = (char *)hash;
            swfHash.av_len = RTMP_SWF_HASHLEN;
          }
      }

    if (swfHash.av_len == 0 && swfSize > 0)
      {
        RTMP_Log(RTMP_LOGWARNING,
  	  "Ignoring SWF size, supply also the hash with --swfhash");
        swfSize = 0;
      }

    if (swfHash.av_len != 0 && swfSize == 0)
      {
        RTMP_Log(RTMP_LOGWARNING,
  	  "Ignoring SWF hash, supply also the swf size  with --swfsize");
        swfHash.av_len = 0;
        swfHash.av_val = NULL;
      }
  #endif

    if (tcUrl.av_len == 0)
      {
  	  tcUrl.av_len = strlen(RTMPProtocolStringsLower[protocol]) +
  	  	hostname.av_len + app.av_len + sizeof("://:65535/");
        tcUrl.av_val = (char *) malloc(tcUrl.av_len);
  	  if (!tcUrl.av_val)
  	    return RD_FAILED;
        tcUrl.av_len = snprintf(tcUrl.av_val, tcUrl.av_len, "%s://%.*s:%d/%.*s",
  	  	   RTMPProtocolStringsLower[protocol], hostname.av_len,
  		   hostname.av_val, port, app.av_len, app.av_val);
      }

    int first = 1;

    // User defined seek offset
    if (dStartOffset > 0)
      {
        // Live stream
        if (bLiveStream)
  	{
  	  RTMP_Log(RTMP_LOGWARNING,
  	      "Can't seek in a live stream, ignoring --start option");
  	  dStartOffset = 0;
  	}
   }

  RTMP_LogPrintf("Complete passing arguments\n");
  // RTMPSuck section
  int nStatus = RD_SUCCESS;

  // rtmp streaming server
  char DEFAULT_RTMP_STREAMING_DEVICE[] = "0.0.0.0";	// 0.0.0.0 is any device

  char *rtmpStreamingDevice = DEFAULT_RTMP_STREAMING_DEVICE;	// streaming device, default 0.0.0.0
  int nRtmpStreamingPort = 1935;	// port

  RTMP_LogPrintf("(c) 2010 Andrej Stepanchuk, Howard Chu; license: GPL\n\n");

  signal(SIGINT, sigIntHandler);
#ifndef WIN32
  signal(SIGPIPE, SIG_IGN);
#endif

  InitSockets();

  // start text UI
 // ThreadCreate(controlServerThread, 0);

  // start http streaming
  if ((rtmpServer =
       startStreaming(rtmpStreamingDevice, nRtmpStreamingPort,
        protocol, &hostname, port, &sockshost, &playpath,
        		   &tcUrl, &swfUrl, &pageUrl, &app, &auth, &swfHash, swfSize,
        		   &flashVer, &subscribepath, &usherToken, dSeek, dStopOffset, bLiveStream, timeout,sToken)) == 0)
    {
      RTMP_Log(RTMP_LOGERROR, "Failed to start RTMP server, exiting!");
      return RD_FAILED;
    }
  RTMP_LogPrintf("Streaming on rtmp://%s:%d\n", rtmpStreamingDevice,
	    nRtmpStreamingPort);

  while (rtmpServer->state != STREAMING_STOPPED && !RTMP_ctrlC)
    {
      sleep(1);
    }
  RTMP_Log(RTMP_LOGDEBUG, "Done, exiting...");

  free(rtmpServer);

  CleanupSockets();

  return nStatus;
}


JNIEXPORT void JNICALL Java_com_dotohsoft_rtmpdump_RTMPSuck_init(JNIEnv * env, jobject obj, jstring token, jstring dest)
{
    RTMP_ctrlC = FALSE;
    const char *nativeToken = (*env)->GetStringUTFChars(env, token, 0);
    const char *nativeDest = (*env)->GetStringUTFChars(env, dest, 0);
    RTMP_LogPrintf("Start rtmp server. Token: %s, Save to file %s.\n", nativeToken,
    	    nativeDest);
    char *v[] = {
            "-v",
            "-l", "2",
            "-t", "rtmpe://f-radiko.smartstream.ne.jp/TBS/_definst_",
            "-n", "f-radiko.smartstream.ne.jp",
            "--app", "TBS/_definst_",
            "--playpath", "simul-stream.stream"
            };
    char **argv = v;
    char *sToken = strdup(nativeToken);
    int argc = 11;
    main_rtmpsuck(argc, argv, sToken);
}

JNIEXPORT void JNICALL Java_com_dotohsoft_rtmpdump_RTMPSuck_stop(JNIEnv * env, jobject obj)
{
    RTMP_LogPrintf("Force stop server\n");
         RTMP_LogPrintf("Stop streaming... socket %d", rtmpServer->socket);
        int fd = rtmpServer->socket;
        //if (fd) continue;
                rtmpServer->socket = 0;
                if (rtmpServer->state == STREAMING_IN_PROGRESS)
                {
                    rtmpServer->state = STREAMING_STOPPING;
                    // wait for streaming threads to exit
                    while (rtmpServer->state != STREAMING_STOPPED)
                        msleep(1);
                }
                RTMP_LogPrintf("Try to close socket %d\n", fd);
                shutdown(fd,SHUT_RDWR);
                closesocket(fd);
    // break;
        rtmpServer->state = STREAMING_STOPPED;
        RTMP_LogPrintf("Close RTMP Object ... ");
        if (&rtmpServer->rs)
           RTMP_Close(&rtmpServer->rs);
        if (&rtmpServer->rc)
           RTMP_Close(&rtmpServer->rc);
        while (rtmpServer->f_head)
        {
          Flist *fl = rtmpServer->f_head;
          rtmpServer->f_head = fl->f_next;
          if (fl->f_file)
             fclose(fl->f_file);
           free(fl);
        }
        rtmpServer->f_tail = NULL;
        rtmpServer->f_cur = NULL;
                    /* Should probably be done by RTMP_Close() ... */
        rtmpServer->rc.Link.hostname.av_val = NULL;
        rtmpServer->rc.Link.tcUrl.av_val = NULL;
        rtmpServer->rc.Link.swfUrl.av_val = NULL;
        rtmpServer->rc.Link.pageUrl.av_val = NULL;
        rtmpServer->rc.Link.app.av_val = NULL;
        rtmpServer->rc.Link.auth.av_val = NULL;
        rtmpServer->rc.Link.flashVer.av_val = NULL;

       // RTMP_LogPrintf("Free object...");
       // free(rtmpServer);
        RTMP_LogPrintf("Cleanup socket...");
        CleanupSockets();
        RTMP_LogPrintf("Done!");
}