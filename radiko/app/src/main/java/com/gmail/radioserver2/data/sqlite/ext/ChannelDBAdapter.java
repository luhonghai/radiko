package com.gmail.radioserver2.data.sqlite.ext;

import android.content.Context;
import android.database.Cursor;

import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.sqlite.DBAdapter;
import com.gmail.radioserver2.utils.DateHelper;
import com.gmail.radioserver2.utils.StringUtil;

import java.util.Collection;

/**
 * Created by luhonghai on 25/02/2015.
 */
public class ChannelDBAdapter extends DBAdapter<Channel> {

    public ChannelDBAdapter(Context ctx) {
        super(ctx);
    }

    @Override
    public Cursor getAll() throws Exception {
        return getAll(KEY_LAST_PLAYED_TIME + " DESC, " + KEY_NAME + " ASC");
    }

    @Override
    public String getTableName() {
        return TABLE_CHANNEL;
    }

    @Override
    public String[] getAllColumns() {
        return new String[] {
                KEY_ROW_ID,
                KEY_NAME,
                KEY_CHANNEL_KEY,
                KEY_TYPE,
                KEY_DESCRIPTION,
                KEY_LAST_PLAYED_TIME,
                KEY_URL,
                KEY_CREATED_DATE
        };
    }

    @Override
    public Channel toObject(Cursor cursor) {
        Channel channel = new Channel();
        channel.setId(cursor.getInt(cursor.getColumnIndex(KEY_ROW_ID)));
        channel.setName(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
        channel.setKey(cursor.getString(cursor.getColumnIndex(KEY_CHANNEL_KEY)));
        channel.setType(cursor.getString(cursor.getColumnIndex(KEY_TYPE)));
        channel.setDescription(cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION)));
        channel.setUrl(cursor.getString(cursor.getColumnIndex(KEY_URL)));
        channel.setLastPlayedTime(DateHelper.convertStringToDate(cursor.getString(cursor.getColumnIndex(KEY_LAST_PLAYED_TIME))));
        channel.setCreatedDate(DateHelper.convertStringToDate(cursor.getString(cursor.getColumnIndex(KEY_CREATED_DATE))));
        return channel;
    }

    @Override
    public Collection<Channel> search(String s) throws Exception {
        if (s == null || s.length() == 0) return findAll();
        s = StringUtil.escapeJapanSpecialChar(s);
        return toCollection(getDB().query(getTableName(), getAllColumns(),
                KEY_NAME + " like ? or " + KEY_DESCRIPTION + " like ?",
                new String[] {
                    "%" + s + "%",
                    "%" + s + "%",
                },
                null,
                null,
                KEY_LAST_PLAYED_TIME + " DESC, " + KEY_NAME + " ASC"));
    }

    @Override
    public long insert(Channel obj) throws Exception {
        Cursor cursor = getDB().query(getTableName(), getAllColumns(),
                KEY_URL + "=?",
                new String[]{
                        obj.getUrl()
                },
                null,
                null,
                null);
        if (cursor.moveToFirst()) {
            long oldId =cursor.getLong(cursor.getColumnIndex(KEY_ROW_ID));
            cursor.close();
            Channel oldObject = find(oldId);
            obj.setId(oldId);
            obj.setLastPlayedTime(oldObject.getLastPlayedTime());
            obj.setCreatedDate(oldObject.getCreatedDate());
            update(obj);
            return oldId;
        }
        return super.insert(obj);
    }
}
