package com.emanuelef.remote_capture;

import android.content.Context;
import android.widget.MediaController;

public class AlwaysVisibleMediaController extends MediaController {

    public AlwaysVisibleMediaController(Context context) {
        super(context);
    }

    @Override
    public void hide() {
        // 覆盖此方法以阻止MediaController隐藏
//        this.show(500); // 0 means always show
    }
}
