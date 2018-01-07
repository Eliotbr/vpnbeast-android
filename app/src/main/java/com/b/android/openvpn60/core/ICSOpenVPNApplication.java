/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.b.android.openvpn60.core;
import android.app.Application;

import com.b.android.openvpn60.helper.LogHelper;

/*
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
*/

public class ICSOpenVPNApplication extends Application {
    private StatusListener mStatus;
    private LogHelper logHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        PRNGHelper.apply();
        logHelper = LogHelper.getLogHelper(getApplicationContext());
        mStatus = new StatusListener();
        mStatus.init(getApplicationContext());
        logHelper.logInfo("StatusListener created on ICSOpenVPNApplication...");
    }
}
