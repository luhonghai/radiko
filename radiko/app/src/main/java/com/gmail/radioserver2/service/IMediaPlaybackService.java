/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Volumes/DATA/Development/radiko/radiko/app/src/main/aidl/wseemann/media/fmpdemo/service/IMediaPlaybackService.aidl
 */
package com.gmail.radioserver2.service;

import android.os.RemoteException;

public interface IMediaPlaybackService extends android.os.IInterface
{
    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends android.os.Binder implements com.gmail.radioserver2.service.IMediaPlaybackService
    {
        private static final java.lang.String DESCRIPTOR = "com.gmail.radioserver2.service.IMediaPlaybackService";
        /** Construct the stub at attach it to the interface. */
        public Stub()
        {
            this.attachInterface(this, DESCRIPTOR);
        }
        /**
         * Cast an IBinder object into an com.gmail.radioserver2.service.IMediaPlaybackService interface,
         * generating a proxy if needed.
         */
        public static com.gmail.radioserver2.service.IMediaPlaybackService asInterface(android.os.IBinder obj)
        {
            if ((obj==null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin!=null)&&(iin instanceof com.gmail.radioserver2.service.IMediaPlaybackService))) {
                return ((com.gmail.radioserver2.service.IMediaPlaybackService)iin);
            }
            return new com.gmail.radioserver2.service.IMediaPlaybackService.Stub.Proxy(obj);
        }
        @Override public android.os.IBinder asBinder()
        {
            return this;
        }
        @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
        {
            switch (code)
            {
                case INTERFACE_TRANSACTION:
                {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                // Added by Hai

                case TRANSACTION_setChannelObject: {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String _arg0;
                    _arg0 = data.readString();
                    this.setChannelObject(_arg0);
                    reply.writeNoException();
                    return true;
                }

                case TRANSACTION_updateRtmpSuck: {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String _arg0;
                    _arg0 = data.readString();
                    java.lang.String _arg1;
                    _arg1 = data.readString();
                    String _result = this.updateRtmpSuck(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }

                case TRANSACTION_startRecord: {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String _arg0;
                    _arg0 = data.readString();
                    java.lang.String _arg1;
                    _arg1 = data.readString();
                    this.startRecord(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_stopRecord: {
                    data.enforceInterface(DESCRIPTOR);
                    this.stopRecord();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_isRecording: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = this.isRecording();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_markAB: {
                    data.enforceInterface(DESCRIPTOR);
                    this.markAB();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getStateAB: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = this.getStateAB();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_stopAB: {
                    data.enforceInterface(DESCRIPTOR);
                    this.stopAB();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getAPos:
                {
                    data.enforceInterface(DESCRIPTOR);
                    long _result = this.getAPos();
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                }
                case TRANSACTION_getBPos:
                {
                    data.enforceInterface(DESCRIPTOR);
                    long _result = this.getBPos();
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                }
                case TRANSACTION_doBack: {
                    data.enforceInterface(DESCRIPTOR);
                    int length = data.readInt();
                    this.doBack(length);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_doFast: {
                    data.enforceInterface(DESCRIPTOR);
                    float level = data.readFloat();
                    this.doFast(level);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_stopFast: {
                    data.enforceInterface(DESCRIPTOR);
                    this.stopFast();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_doSlow: {
                    data.enforceInterface(DESCRIPTOR);
                    float level = data.readFloat();
                    this.doSlow(level);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_stopSlow: {
                    data.enforceInterface(DESCRIPTOR);
                    this.stopSlow();
                    reply.writeNoException();
                    return true;
                }

                case TRANSACTION_isSlow: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean r = this.isSlow();
                    reply.writeInt(r ? 1 : 0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_isFast: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean r = this.isFast();
                    reply.writeInt(r ? 1 : 0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_setStreaming: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean arg = data.readInt() == 1;
                    this.setStreaming(arg);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_isStreaming: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean r = this.isStreaming();
                    reply.writeInt(r ? 1 : 0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getChannelObject: {
                    data.enforceInterface(DESCRIPTOR);
                    String r = this.getChannelObject();
                    reply.writeString(r);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_openStream: {
                    data.enforceInterface(DESCRIPTOR);
                    String token = data.readString();
                    String channelObj = data.readString();
                    this.openStream(token, channelObj);
                    reply.writeNoException();
                    return true;
                }
                // Default
                case TRANSACTION_openFile:
                {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String _arg0;
                    _arg0 = data.readString();
                    this.openFile(_arg0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_open:
                {
                    data.enforceInterface(DESCRIPTOR);
                    long[] _arg0;
                    _arg0 = data.createLongArray();
                    int _arg1;
                    _arg1 = data.readInt();
                    this.open(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getQueuePosition:
                {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = this.getQueuePosition();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_isPlaying:
                {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = this.isPlaying();
                    reply.writeNoException();
                    reply.writeInt(((_result)?(1):(0)));
                    return true;
                }
                case TRANSACTION_stop:
                {
                    data.enforceInterface(DESCRIPTOR);
                    this.stop();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_pause:
                {
                    data.enforceInterface(DESCRIPTOR);
                    this.pause();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_play:
                {
                    data.enforceInterface(DESCRIPTOR);
                    this.play();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_prev:
                {
                    data.enforceInterface(DESCRIPTOR);
                    this.prev();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_next:
                {
                    data.enforceInterface(DESCRIPTOR);
                    this.next();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_duration:
                {
                    data.enforceInterface(DESCRIPTOR);
                    long _result = this.duration();
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                }
                case TRANSACTION_position:
                {
                    data.enforceInterface(DESCRIPTOR);
                    long _result = this.position();
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                }
                case TRANSACTION_seek:
                {
                    data.enforceInterface(DESCRIPTOR);
                    long _arg0;
                    _arg0 = data.readLong();
                    long _result = this.seek(_arg0);
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                }
                case TRANSACTION_getTrackName:
                {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String _result = this.getTrackName();
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }
                case TRANSACTION_getAlbumName:
                {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String _result = this.getAlbumName();
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }
                case TRANSACTION_getAlbumId:
                {
                    data.enforceInterface(DESCRIPTOR);
                    long _result = this.getAlbumId();
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                }
                case TRANSACTION_getArtistName:
                {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String _result = this.getArtistName();
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }
                case TRANSACTION_getArtistId:
                {
                    data.enforceInterface(DESCRIPTOR);
                    long _result = this.getArtistId();
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                }
                case TRANSACTION_enqueue:
                {
                    data.enforceInterface(DESCRIPTOR);
                    long[] _arg0;
                    _arg0 = data.createLongArray();
                    int _arg1;
                    _arg1 = data.readInt();
                    this.enqueue(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getQueue:
                {
                    data.enforceInterface(DESCRIPTOR);
                    long[] _result = this.getQueue();
                    reply.writeNoException();
                    reply.writeLongArray(_result);
                    return true;
                }
                case TRANSACTION_moveQueueItem:
                {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0;
                    _arg0 = data.readInt();
                    int _arg1;
                    _arg1 = data.readInt();
                    this.moveQueueItem(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_setQueuePosition:
                {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0;
                    _arg0 = data.readInt();
                    this.setQueuePosition(_arg0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getPath:
                {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String _result = this.getPath();
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }
                case TRANSACTION_getAudioId:
                {
                    data.enforceInterface(DESCRIPTOR);
                    long _result = this.getAudioId();
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                }
                case TRANSACTION_setShuffleMode:
                {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0;
                    _arg0 = data.readInt();
                    this.setShuffleMode(_arg0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getShuffleMode:
                {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = this.getShuffleMode();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_removeTracks:
                {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0;
                    _arg0 = data.readInt();
                    int _arg1;
                    _arg1 = data.readInt();
                    int _result = this.removeTracks(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_removeTrack:
                {
                    data.enforceInterface(DESCRIPTOR);
                    long _arg0;
                    _arg0 = data.readLong();
                    int _result = this.removeTrack(_arg0);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_setRepeatMode:
                {
                    data.enforceInterface(DESCRIPTOR);
                    int _arg0;
                    _arg0 = data.readInt();
                    this.setRepeatMode(_arg0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getRepeatMode:
                {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = this.getRepeatMode();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_getMediaMountedCount:
                {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = this.getMediaMountedCount();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_getAudioSessionId:
                {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = this.getAudioSessionId();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_getMediaUri:
                {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String _result = this.getMediaUri();
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }
        private static class Proxy implements com.gmail.radioserver2.service.IMediaPlaybackService
        {
            private android.os.IBinder mRemote;
            Proxy(android.os.IBinder remote)
            {
                mRemote = remote;
            }
            @Override public android.os.IBinder asBinder()
            {
                return mRemote;
            }
            public java.lang.String getInterfaceDescriptor()
            {
                return DESCRIPTOR;
            }
            @Override public void openFile(java.lang.String path) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(path);
                    mRemote.transact(Stub.TRANSACTION_openFile, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void open(long[] list, int position) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLongArray(list);
                    _data.writeInt(position);
                    mRemote.transact(Stub.TRANSACTION_open, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public int getQueuePosition() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getQueuePosition, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public boolean isPlaying() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_isPlaying, _data, _reply, 0);
                    _reply.readException();
                    _result = (0!=_reply.readInt());
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public void stop() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_stop, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void pause() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_pause, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void play() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_play, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void prev() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_prev, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void next() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_next, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public long duration() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                long _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_duration, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readLong();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public long position() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                long _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_position, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readLong();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public long seek(long pos) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                long _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLong(pos);
                    mRemote.transact(Stub.TRANSACTION_seek, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readLong();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public java.lang.String getTrackName() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.lang.String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getTrackName, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public java.lang.String getAlbumName() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.lang.String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getAlbumName, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public long getAlbumId() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                long _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getAlbumId, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readLong();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public java.lang.String getArtistName() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.lang.String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getArtistName, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public long getArtistId() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                long _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getArtistId, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readLong();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public void enqueue(long[] list, int action) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLongArray(list);
                    _data.writeInt(action);
                    mRemote.transact(Stub.TRANSACTION_enqueue, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public long[] getQueue() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                long[] _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getQueue, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.createLongArray();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public void moveQueueItem(int from, int to) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(from);
                    _data.writeInt(to);
                    mRemote.transact(Stub.TRANSACTION_moveQueueItem, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void setQueuePosition(int index) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(index);
                    mRemote.transact(Stub.TRANSACTION_setQueuePosition, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public java.lang.String getPath() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.lang.String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getPath, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public long getAudioId() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                long _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getAudioId, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readLong();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public void setShuffleMode(int shufflemode) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(shufflemode);
                    mRemote.transact(Stub.TRANSACTION_setShuffleMode, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public int getShuffleMode() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getShuffleMode, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public int removeTracks(int first, int last) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(first);
                    _data.writeInt(last);
                    mRemote.transact(Stub.TRANSACTION_removeTracks, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public int removeTrack(long id) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLong(id);
                    mRemote.transact(Stub.TRANSACTION_removeTrack, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public void setRepeatMode(int repeatmode) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(repeatmode);
                    mRemote.transact(Stub.TRANSACTION_setRepeatMode, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public int getRepeatMode() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getRepeatMode, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public int getMediaMountedCount() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getMediaMountedCount, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public int getAudioSessionId() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getAudioSessionId, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public java.lang.String getMediaUri() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.lang.String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getMediaUri, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override public void startRecord(java.lang.String token, java.lang.String filePath) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(token);
                    _data.writeString(filePath);
                    mRemote.transact(Stub.TRANSACTION_startRecord, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void stopRecord() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_stopRecord, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public boolean isRecording() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                boolean _result = false;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_isRecording, _data, _reply, 0);
                    _reply.readException();
                    _result = (_reply.readInt() != 0);
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public void markAB() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_markAB, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public int getStateAB() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int _result = -1;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getStateAB, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            @Override public void stopAB() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_stopAB, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void doBack(int length) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(length);
                    mRemote.transact(Stub.TRANSACTION_doBack, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void doFast(float level) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeFloat(level);
                    mRemote.transact(Stub.TRANSACTION_doFast, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void stopFast() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_stopFast, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void doSlow(float level) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeFloat(level);
                    mRemote.transact(Stub.TRANSACTION_doSlow, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public void stopSlow() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_stopSlow, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override public void setStreaming(boolean isStreaming) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(isStreaming ? 1 : 0);
                    mRemote.transact(Stub.TRANSACTION_setStreaming, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
            @Override public boolean isStreaming() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_isStreaming, _data, _reply, 0);
                    _reply.readException();
                    _result = (_reply.readInt() != 0);
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override public void openStream(String token, String channelObject) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(token);
                    _data.writeString(channelObject);
                    mRemote.transact(Stub.TRANSACTION_openStream, _data, _reply, 0);
                    _reply.readException();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override public String getChannelObject() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getChannelObject, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override public String updateRtmpSuck(String token, String playUrl) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(token);
                    _data.writeString(playUrl);
                    mRemote.transact(Stub.TRANSACTION_updateRtmpSuck, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override public void setChannelObject(String channelObject) throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(channelObject);
                    mRemote.transact(Stub.TRANSACTION_setChannelObject, _data, _reply, 0);
                    _reply.readException();

                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override public boolean isFast() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_isFast, _data, _reply, 0);
                    _reply.readException();
                    _result = (_reply.readInt() != 0);
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override public boolean isSlow() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_isSlow, _data, _reply, 0);
                    _reply.readException();
                    _result = (_reply.readInt() != 0);
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override public long getAPos() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                long _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getAPos, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readLong();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override public long getBPos() throws android.os.RemoteException
            {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                long _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getBPos, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readLong();
                }
                finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
        }
        // Added by Hai
        static final int TRANSACTION_startRecord = (android.os.IBinder.FIRST_CALL_TRANSACTION + 32);
        static final int TRANSACTION_stopRecord = (android.os.IBinder.FIRST_CALL_TRANSACTION + 33);
        static final int TRANSACTION_markAB = (android.os.IBinder.FIRST_CALL_TRANSACTION + 34);
        static final int TRANSACTION_getStateAB = (android.os.IBinder.FIRST_CALL_TRANSACTION + 35);
        static final int TRANSACTION_stopAB = (android.os.IBinder.FIRST_CALL_TRANSACTION + 36);
        static final int TRANSACTION_doBack = (android.os.IBinder.FIRST_CALL_TRANSACTION + 37);
        static final int TRANSACTION_doFast = (android.os.IBinder.FIRST_CALL_TRANSACTION + 38);
        static final int TRANSACTION_stopFast = (android.os.IBinder.FIRST_CALL_TRANSACTION + 39);
        static final int TRANSACTION_doSlow = (android.os.IBinder.FIRST_CALL_TRANSACTION + 40);
        static final int TRANSACTION_stopSlow = (android.os.IBinder.FIRST_CALL_TRANSACTION + 41);
        static final int TRANSACTION_isRecording = (android.os.IBinder.FIRST_CALL_TRANSACTION + 42);
        static final int TRANSACTION_setStreaming = (android.os.IBinder.FIRST_CALL_TRANSACTION + 43);
        static final int TRANSACTION_isStreaming = (android.os.IBinder.FIRST_CALL_TRANSACTION + 44);
        static final int TRANSACTION_openStream = (android.os.IBinder.FIRST_CALL_TRANSACTION + 45);
        static final int TRANSACTION_getChannelObject = (android.os.IBinder.FIRST_CALL_TRANSACTION + 46);
        static final int TRANSACTION_updateRtmpSuck = (android.os.IBinder.FIRST_CALL_TRANSACTION + 47);
        static final int TRANSACTION_setChannelObject = (android.os.IBinder.FIRST_CALL_TRANSACTION + 48);
        static final int TRANSACTION_isFast = (android.os.IBinder.FIRST_CALL_TRANSACTION + 49);
        static final int TRANSACTION_isSlow = (android.os.IBinder.FIRST_CALL_TRANSACTION + 50);
        static final int TRANSACTION_getAPos = (android.os.IBinder.FIRST_CALL_TRANSACTION + 51);
        static final int TRANSACTION_getBPos= (android.os.IBinder.FIRST_CALL_TRANSACTION + 52);


        // Default
        static final int TRANSACTION_openFile = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_open = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
        static final int TRANSACTION_getQueuePosition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
        static final int TRANSACTION_isPlaying = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
        static final int TRANSACTION_stop = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
        static final int TRANSACTION_pause = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
        static final int TRANSACTION_play = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
        static final int TRANSACTION_prev = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
        static final int TRANSACTION_next = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
        static final int TRANSACTION_duration = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
        static final int TRANSACTION_position = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
        static final int TRANSACTION_seek = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
        static final int TRANSACTION_getTrackName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
        static final int TRANSACTION_getAlbumName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
        static final int TRANSACTION_getAlbumId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
        static final int TRANSACTION_getArtistName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
        static final int TRANSACTION_getArtistId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
        static final int TRANSACTION_enqueue = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
        static final int TRANSACTION_getQueue = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
        static final int TRANSACTION_moveQueueItem = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
        static final int TRANSACTION_setQueuePosition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
        static final int TRANSACTION_getPath = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
        static final int TRANSACTION_getAudioId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
        static final int TRANSACTION_setShuffleMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 23);
        static final int TRANSACTION_getShuffleMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 24);
        static final int TRANSACTION_removeTracks = (android.os.IBinder.FIRST_CALL_TRANSACTION + 25);
        static final int TRANSACTION_removeTrack = (android.os.IBinder.FIRST_CALL_TRANSACTION + 26);
        static final int TRANSACTION_setRepeatMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 27);
        static final int TRANSACTION_getRepeatMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 28);
        static final int TRANSACTION_getMediaMountedCount = (android.os.IBinder.FIRST_CALL_TRANSACTION + 29);
        static final int TRANSACTION_getAudioSessionId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 30);
        static final int TRANSACTION_getMediaUri = (android.os.IBinder.FIRST_CALL_TRANSACTION + 31);

    }
    // Added by Hai
    public String updateRtmpSuck(String token, String playUrl) throws RemoteException;
    public void startRecord(String token, String fileName) throws android.os.RemoteException;
    public void stopRecord() throws android.os.RemoteException;
    public boolean isRecording() throws  android.os.RemoteException;
    public void markAB() throws android.os.RemoteException;
    public int getStateAB() throws android.os.RemoteException;
    public void stopAB() throws android.os.RemoteException;

    public long getAPos() throws android.os.RemoteException;
    public long getBPos() throws android.os.RemoteException;
    public boolean isFast() throws android.os.RemoteException;
    public boolean isSlow() throws android.os.RemoteException;

    public void doBack(int length) throws android.os.RemoteException;
    public void doFast(float level) throws android.os.RemoteException;

    public void stopFast() throws android.os.RemoteException;
    public void doSlow(float level) throws android.os.RemoteException;

    public void stopSlow() throws android.os.RemoteException;

    public boolean isStreaming() throws RemoteException;
    public void setStreaming(boolean isStreaming) throws RemoteException;
    public void openStream(String token, String objChannel) throws RemoteException;
    public String getChannelObject() throws RemoteException;
    public void setChannelObject(String channelObject) throws RemoteException;

    // Default
    public void openFile(java.lang.String path) throws android.os.RemoteException;
    public void open(long[] list, int position) throws android.os.RemoteException;
    public int getQueuePosition() throws android.os.RemoteException;
    public boolean isPlaying() throws android.os.RemoteException;
    public void stop() throws android.os.RemoteException;
    public void pause() throws android.os.RemoteException;
    public void play() throws android.os.RemoteException;
    public void prev() throws android.os.RemoteException;
    public void next() throws android.os.RemoteException;
    public long duration() throws android.os.RemoteException;
    public long position() throws android.os.RemoteException;
    public long seek(long pos) throws android.os.RemoteException;
    public java.lang.String getTrackName() throws android.os.RemoteException;
    public java.lang.String getAlbumName() throws android.os.RemoteException;
    public long getAlbumId() throws android.os.RemoteException;
    public java.lang.String getArtistName() throws android.os.RemoteException;
    public long getArtistId() throws android.os.RemoteException;
    public void enqueue(long[] list, int action) throws android.os.RemoteException;
    public long[] getQueue() throws android.os.RemoteException;
    public void moveQueueItem(int from, int to) throws android.os.RemoteException;
    public void setQueuePosition(int index) throws android.os.RemoteException;
    public java.lang.String getPath() throws android.os.RemoteException;
    public long getAudioId() throws android.os.RemoteException;
    public void setShuffleMode(int shufflemode) throws android.os.RemoteException;
    public int getShuffleMode() throws android.os.RemoteException;
    public int removeTracks(int first, int last) throws android.os.RemoteException;
    public int removeTrack(long id) throws android.os.RemoteException;
    public void setRepeatMode(int repeatmode) throws android.os.RemoteException;
    public int getRepeatMode() throws android.os.RemoteException;
    public int getMediaMountedCount() throws android.os.RemoteException;
    public int getAudioSessionId() throws android.os.RemoteException;
    public java.lang.String getMediaUri() throws android.os.RemoteException;
}
