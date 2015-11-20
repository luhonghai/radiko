package com.gmail.radioserver2.fragment;

/*Copyright*/

import com.gmail.radioserver2.adapter.ChannelAdapter;
import com.gmail.radioserver2.data.Channel;
import com.gmail.radioserver2.data.sqlite.ext.ChannelDBAdapter;
import com.gmail.radioserver2.utils.SimpleAppLog;

import java.util.Collection;

public class SearchFragmentTab extends ChannelFragmentTab {
    private String searchStr = "";

    public void setSearchStr(String searchStr) {
        this.searchStr = searchStr;
    }

    @Override
    protected void loadData() {
        if (listView == null) return;
        ChannelDBAdapter dbAdapter = new ChannelDBAdapter(getActivity());
        try {
            dbAdapter.open();
            Collection<Channel> channels = dbAdapter.search(searchStr);
            SimpleAppLog.info("Load " + (channels == null ? 0 : channels.size()) + " channels to listview");
            Channel[] items;
            if (channels != null && channels.size() > 0) {
                items = new Channel[channels.size()];
                channels.toArray(items);
            } else {
                items = new Channel[]{};
            }
            ChannelAdapter adapter = new ChannelAdapter(getActivity(), items, this);
            listView.setAdapter(adapter);
            listView.dismissSelected();
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            SimpleAppLog.error("Could not load channel", e);
        } finally {
            dbAdapter.close();
        }
    }

    @Override
    public void onSelectItem(Channel obj) {
        super.onSelectItem(obj);
        if (onSelectedChannelInSearch != null) {
            onSelectedChannelInSearch.onSelectedChannel();
        }
    }

    private OnSelectedChannelInSearch onSelectedChannelInSearch;

    public void setOnSelectedChannelInSearch(OnSelectedChannelInSearch onSelectedChannelInSearch) {
        this.onSelectedChannelInSearch = onSelectedChannelInSearch;
    }

    public interface OnSelectedChannelInSearch {
        void onSelectedChannel();
    }
}
