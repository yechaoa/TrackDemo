package com.yechaoa.trackdemo.track;


/*public*/ class WrapperOnClickListener implements android.view.View.OnClickListener {
    private android.view.View.OnClickListener source;

    WrapperOnClickListener(android.view.View.OnClickListener source) {
        this.source = source;
    }

    @Override
    public void onClick(android.view.View view) {
        //调用原有的 OnClickListener
        try {
            if (source != null) {
                source.onClick(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //插入埋点代码
        SensorsDataPrivate.trackViewOnClick(view);
    }
}
