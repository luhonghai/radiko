package com.gmail.radioserver2.view.slidingtab;

import java.util.ArrayList;
import java.util.Arrays;

public class TabPagerTracking {

    private ArrayList<Tab> tabTitles = new ArrayList<>();
    private int mCurrent = 0;
    private OnTabChange tabChange;

    public void addTab(Tab tab) {
        tabTitles.add(tab);
    }

    public void addTabs(Tab... tabs) {
        tabTitles.addAll(Arrays.asList(tabs));
    }

    public int getCount() {
        return tabTitles.size();
    }

    public String getPageTitle(int i) {
        return tabTitles.get(i).getTitle();
    }

    public int getCurrentItem() {
        return mCurrent;
    }

    public void setCurrentItem(int i) {
        mCurrent = i;
        if (tabChange != null) {
            tabChange.selectedTab(i, tabTitles.get(i));
        }
    }

    public void setTabChange(OnTabChange tabChange) {
        this.tabChange = tabChange;
    }

    public static class Tab {
        private String title;
        private String name;

        public Tab(String title, String name) {
            this.title = title;
            this.name = name;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public interface OnTabChange {
        void selectedTab(int pos, final Tab tab);
    }
}
