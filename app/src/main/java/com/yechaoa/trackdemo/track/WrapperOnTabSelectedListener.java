package com.yechaoa.trackdemo.track;

import com.google.android.material.tabs.TabLayout;

/*public*/ class WrapperOnTabSelectedListener implements TabLayout.OnTabSelectedListener {
    private TabLayout.OnTabSelectedListener source;

    WrapperOnTabSelectedListener(TabLayout.OnTabSelectedListener source) {
        this.source = source;
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        if (source != null) {
            source.onTabSelected(tab);
        }
        SensorsDataPrivate.trackViewOnClick(tab.view);
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        if (source != null) {
            source.onTabUnselected(tab);
        }
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        if (source != null) {
            source.onTabReselected(tab);
        }
    }
}
