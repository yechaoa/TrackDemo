package com.yechaoa.trackdemo.track;

import android.widget.AdapterView;

/*public*/ class WrapperAdapterViewOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
    private AdapterView.OnItemSelectedListener source;

    WrapperAdapterViewOnItemSelectedListener(AdapterView.OnItemSelectedListener source) {
        this.source = source;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, android.view.View view, int position, long id) {
        if (source != null) {
            source.onItemSelected(adapterView, view, position, id);
        }

        SensorsDataPrivate.trackAdapterView(adapterView, view, position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        if (source != null) {
            source.onNothingSelected(adapterView);
        }
    }
}
