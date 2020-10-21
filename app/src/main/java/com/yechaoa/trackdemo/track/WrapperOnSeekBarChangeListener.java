package com.yechaoa.trackdemo.track;

/*public*/ class WrapperOnSeekBarChangeListener implements android.widget.SeekBar.OnSeekBarChangeListener {
    private android.widget.SeekBar.OnSeekBarChangeListener source;

    WrapperOnSeekBarChangeListener(android.widget.SeekBar.OnSeekBarChangeListener source) {
        this.source = source;
    }

    @Override
    public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
        if (source != null) {
            source.onStopTrackingTouch(seekBar);
        }

        SensorsDataPrivate.trackViewOnClick(seekBar);
    }

    @Override
    public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
        if (source != null) {
            source.onStartTrackingTouch(seekBar);
        }
    }

    @Override
    public void onProgressChanged(android.widget.SeekBar seekBar, int i, boolean b) {
        if (source != null) {
            source.onProgressChanged(seekBar, i, b);
        }
    }
}
