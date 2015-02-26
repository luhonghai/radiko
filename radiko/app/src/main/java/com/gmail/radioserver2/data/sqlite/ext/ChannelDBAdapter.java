package com.gmail.radioserver2.data.sqlite.ext;

import android.content.Context;
import android.database.Cursor;

import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.sqlite.DBAdapter;
import com.gmail.radioserver2.utils.DateHelper;

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
}
