package com.yechaoa.trackdemo.track;

import android.widget.CompoundButton;

/*public*/ class WrapperOnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
    private CompoundButton.OnCheckedChangeListener source;

    WrapperOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener source) {
        this.source = source;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        //调用原有的 OnClickListener
        try {
            if (source != null) {
                source.onCheckedChanged(compoundButton, b);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //插入埋点代码
        SensorsDataPrivate.trackViewOnClick(compoundButton);
    }
}
