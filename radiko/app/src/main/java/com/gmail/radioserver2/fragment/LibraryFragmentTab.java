package com.gmail.radioserver2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gmail.radioserver2.adapter.LibraryPickerAdapter;
import com.gmail.radioserver2.adapter.OnListItemActionListener;
import com.gmail.radioserver2.data.Library;
import com.gmail.radioserver2.data.sqlite.ext.LibraryDBAdapter;
import com.gmail.radioserver2.view.swipelistview.SwipeListView;
import com.gmail.radioserver2.R;
import com.gmail.radioserver2.adapter.LibraryAdapter;

import java.util.Collection;

/**
 * Created by luhonghai on 2/17/15.
 */
public class LibraryFragmentTab extends FragmentTab implements OnListItemActionListener<Library> {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_library_tab, container, false);
        SwipeListView listView = (SwipeListView) v.findViewById(R.id.list_library);
        LibraryDBAdapter dbAdapter = new LibraryDBAdapter(getActivity());
        try {
            Collection<Library> libraries = dbAdapter.findAll();
            if (libraries != null && libraries.size() > 0) {
                Library[] items = new Library[libraries.size()];
                libraries.toArray(items);
                LibraryAdapter adapter = new LibraryAdapter(getActivity(), items, this);
                listView.setAdapter(adapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return v;
    }

    @Override
    public void onDeleteItem(Library obj) {

    }

    @Override
    public void onSelectItem(Library obj) {

    }

    @Override
    public void onEditItem(Library obj) {

    }
}
