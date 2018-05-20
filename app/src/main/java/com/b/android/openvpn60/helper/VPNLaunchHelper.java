package com.b.android.openvpn60.helper;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.core.VpnStatus;
import com.b.android.openvpn60.model.VpnProfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import de.blinkt.openvpn.core.NativeUtils;

/**
 * Created by b on 5/15/17.
 */

public class VPNLaunchHelper {
    private static final String MININONPIEVPN;
    private static final String MINIPIEVPN;
    private static final String OVPNCONFIGFILE;

    static {
        MININONPIEVPN = "nopie_openvpn";
        MINIPIEVPN = "pie_openvpn";
        OVPNCONFIGFILE = "android.conf";
    }

    private VPNLaunchHelper() {

    }


    private static String writeMiniVPN(Context context) {
        String[] abis;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            abis = getSupportedABIsLollipop();
        else
            //noinspection deprecation
            abis = new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        String nativeAPI = NativeUtils.getNativeAPI();
        if (!nativeAPI.equals(abis[0])) {
            VpnStatus.logWarning(R.string.abi_mismatch, Arrays.toString(abis), nativeAPI);
            abis = new String[] {nativeAPI};
        }
        for (String abi: abis) {
            File vpnExecutable = new File(context.getCacheDir(), "c_" + getMiniVPNExecutableName() + "." + abi);
            if ((vpnExecutable.exists() && vpnExecutable.canExecute()) || writeMiniVPNBinary(context, abi, vpnExecutable)) {
                return vpnExecutable.getPath();
            }
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String[] getSupportedABIsLollipop() {
        return Build.SUPPORTED_ABIS;
    }

    private static String getMiniVPNExecutableName()
    {
        if (Build.VERSION.SDK_INT  >= Build.VERSION_CODES.JELLY_BEAN)
            return MINIPIEVPN;
        else
            return MININONPIEVPN;
    }

    public static String[] replacePieWithNoPie(String[] mArgv) {
        mArgv[0] = mArgv[0].replace(MINIPIEVPN, MININONPIEVPN);
        return mArgv;
    }

    public static String[] buildOpenvpnArgv(Context c) {
        ArrayList<String> args = new ArrayList<>();
        String binaryName = writeMiniVPN(c);
        if(binaryName==null) {
            VpnStatus.logError("Error writing minivpn binary");
            return new String[0];
        }
        args.add(binaryName);
        args.add("--config");
        args.add(getConfigFilePath(c));
        return args.toArray(new String[args.size()]);
    }

    private static boolean writeMiniVPNBinary(Context context, String abi, File mvpnout) {
        FileOutputStream fout = null;
        try {
            InputStream mvpn;
            try {
                mvpn = context.getAssets().open(getMiniVPNExecutableName() + "." + abi);
            } catch (IOException errabi) {
                VpnStatus.logInfo("Failed getting assets for archicture " + abi);
                return false;
            }
            fout = new FileOutputStream(mvpnout);
            byte buf[]= new byte[4096];
            int lenread = mvpn.read(buf);
            while(lenread> 0) {
                fout.write(buf, 0, lenread);
                lenread = mvpn.read(buf);
            }
            if(!mvpnout.setExecutable(true)) {
                VpnStatus.logError("Failed to make OpenVPN executable");
                return false;
            }
            return true;
        } catch (IOException e) {
            VpnStatus.logException(e);
            return false;
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void startOpenVpn(VpnProfile startprofile, Context context) {
        Intent startVPN = startprofile.prepareStartService(context);
        if(startVPN!=null)
            context.startService(startVPN);
    }

    public static String getConfigFilePath(Context context) {
        return context.getCacheDir().getAbsolutePath() + "/" + OVPNCONFIGFILE;
    }
}

