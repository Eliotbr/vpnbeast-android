package com.b.android.openvpn60.core;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

import com.b.android.openvpn60.R;
import com.b.android.openvpn60.constant.BuildConstants;
import com.b.android.openvpn60.helper.LogHelper;
import com.b.android.openvpn60.listener.ProxyListener;
import com.b.android.openvpn60.model.VpnProfile;

import junit.framework.Assert;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;

import de.blinkt.openvpn.core.NativeUtils;

/**
 * Created by b on 5/15/17.
 */

public class OpenVPNManagementThread implements Runnable, OpenVPNManagement {

    private static final Vector<OpenVPNManagementThread> active = new Vector<>();
    private static OpenVPNService openvpnService;
    private final Handler resumeHandler;
    private LocalSocket localSocket;
    private VpnProfile vpnProfile;
    private LinkedList<FileDescriptor> fdList = new LinkedList<>();
    private LocalServerSocket localServerSocket;
    private boolean waitingForRelease = false;
    private long lastHoldRelease = 0;
    private LocalSocket mServerSocketLocal;
    private PauseReason lastPauseReason = PauseReason.noNetwork;
    private PausedStateCallback mPauseCallback;
    private boolean mShuttingDown;
    private LogHelper logHelper;


    public OpenVPNManagementThread(VpnProfile profile, OpenVPNService openVpnService) {
        vpnProfile = profile;
        openvpnService = openVpnService;
        resumeHandler = new Handler(openVpnService.getMainLooper());
        logHelper = LogHelper.getLogHelper(OpenVPNManagementThread.class.getName());
    }


    public static VpnService getInstance() {
        return openvpnService;
    }


    private Runnable mResumeHoldRunnable = new Runnable() {
        @Override
        public void run() {
            if (shouldBeRunning()) {
                releaseHoldCmd();
            }
        }
    };


    public boolean openManagementInterface(@NonNull Context c) {
        // Could take a while to open connection
        int tries = 8;
        String socketName = (c.getCacheDir().getAbsolutePath() + "/" + "mgmtsocket");
        // The mServerSocketLocal is transferred to the LocalServerSocket, ignore warning
        mServerSocketLocal = new LocalSocket();
        while (tries > 0 && !mServerSocketLocal.isBound()) {
            try {
                mServerSocketLocal.bind(new LocalSocketAddress(socketName,
                        LocalSocketAddress.Namespace.FILESYSTEM));
            } catch (IOException e) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                    logHelper.logException(ignored);
                }
            }
            tries--;
        }

        try {
            localServerSocket = new LocalServerSocket(mServerSocketLocal.getFileDescriptor());
            return true;
        } catch (IOException e) {
            logHelper.logException(e);
        }
        return false;


    }

    /**
     * @param cmd command to write to management socket
     * @return true if command have been sent
     */
    public boolean managmentCommand(String cmd) {
        try {
            if (localSocket != null && localSocket.getOutputStream() != null) {
                localSocket.getOutputStream().write(cmd.getBytes());
                localSocket.getOutputStream().flush();
                return true;
            }
        } catch (IOException e) {
            logHelper.logException(e);
        }
        return false;
    }


    @Override
    public void run() {
        byte[] buffer = new byte[2048];
        //	localSocket.setSoTimeout(5); // Setting a timeout cannot be that bad
        String pendingInput = "";
        synchronized (active) {
            active.add(this);
        }
        try {
            // Wait for a client to connect
            localSocket = localServerSocket.accept();
            InputStream instream = localSocket.getInputStream();
            // Close the management socket after client connected
            try {
                localServerSocket.close();
            } catch (IOException e) {
                logHelper.logException(e);
            }
            // Closing one of the two sockets also closes the other
            //mServerSocketLocal.close();
            while (true) {
                int numbytesread = instream.read(buffer);
                if (numbytesread == -1)
                    return;
                FileDescriptor[] fds = null;
                try {
                    fds = localSocket.getAncillaryFileDescriptors();
                } catch (IOException e) {
                    VpnStatus.logException("Error reading fds from socket", e);
                }
                if (fds != null) {
                    Collections.addAll(fdList, fds);
                }
                String input = new String(buffer, 0, numbytesread, "UTF-8");
                pendingInput += input;
                pendingInput = processInput(pendingInput);
            }
        } catch (IOException e) {
            logHelper.logException(e);
        }

        synchronized (active) {
            active.remove(this);
        }
    }


    private void protectFileDescriptor(FileDescriptor fd) {
        try {
            Method getInt = FileDescriptor.class.getDeclaredMethod("getInt$");
            int fdint = (Integer) getInt.invoke(fd);
            boolean result = openvpnService.protect(fdint);
            if (!result)
                VpnStatus.logWarning("Could not protect VPN socket");
            //ParcelFileDescriptor pfd = ParcelFileDescriptor.fromFd(fdint);
            //pfd.close();
            NativeUtils.jniclose(fdint);
            return;
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException | NullPointerException e) {
            logHelper.logException("Failed to retrieve fd from socket (" + fd + ")", e);
        }
    }


    private String processInput(String pendingInput) {
        while (pendingInput.contains("\n")) {
            String[] tokens = pendingInput.split("\\r?\\n", 2);
            processCommand(tokens[0]);
            if (tokens.length == 1)
                pendingInput = "";
            else
                pendingInput = tokens[1];
        }
        return pendingInput;
    }


    private void processCommand(String command) {
        if (command.startsWith(">") && command.contains(":")) {
            String[] parts = command.split(":", 2);
            String cmd = parts[0].substring(1);
            final String argument = parts[1];
            switch (cmd) {
                case "INFO":
                    return;
                case "PASSWORD":
                    processPWCommand(argument);
                    break;
                case "HOLD":
                    handleHold(argument);
                    break;
                case "NEED-OK":
                    processNeedCommand(argument);
                    break;
                case "BYTECOUNT":
                    processByteCount(argument);
                    break;
                case "STATE":
                    if (!mShuttingDown)
                        processState(argument);
                    break;
                case "PROXY":
                    processProxyCMD(argument);
                    break;
                case "LOG":
                    processLogMessage(argument);
                    break;
                case "RSA_SIGN":
                    processSignCommand(argument);
                    break;
                default:
                    logHelper.logWarning("MGMT: Got unrecognized command" + command);
                    break;
            }
        } else if (command.startsWith("SUCCESS:")) {
            logHelper.logInfo("SUCCESS");
            return;
        } else if (command.startsWith("PROTECTFD: ")) {
            FileDescriptor fdtoprotect = fdList.pollFirst();
            if (fdtoprotect != null)
                protectFileDescriptor(fdtoprotect);
        } else {
            logHelper.logWarning("Got unrecognized line from managment" + command);
        }
    }


    private void processLogMessage(String argument) {
        String[] args = argument.split(",", 4);
        // 0 unix time stamp
        // 1 log level N,I,E etc.
                /*
                  (b) zero or more message flags in a single string:
          I -- informational
          F -- fatal error
          N -- non-fatal error
          W -- warning
          D -- debug, and
                 */
        // 2 log message

        VpnStatus.LogLevel level;
        switch (args[1]) {
            case "I":
                level = VpnStatus.LogLevel.INFO;
                break;
            case "W":
                level = VpnStatus.LogLevel.WARNING;
                break;
            case "D":
                level = VpnStatus.LogLevel.VERBOSE;
                break;
            case "F":
                level = VpnStatus.LogLevel.ERROR;
                break;
            default:
                level = VpnStatus.LogLevel.INFO;
                break;
        }

        int ovpnlevel = Integer.parseInt(args[2]) & 0x0F;
        String msg = args[3];

        if (msg.startsWith("MANAGEMENT: CMD"))
            ovpnlevel = Math.max(4, ovpnlevel);

        //VpnStatus.logMessageOpenVPN(level, ovpnlevel, msg);
    }


    boolean shouldBeRunning() {
        if (mPauseCallback == null)
            return false;
        else
            return mPauseCallback.shouldBeRunning();
    }


    private void handleHold(String argument) {
        int waittime = 0;
        if (shouldBeRunning()) {
            if (waittime >= 0)
                VpnStatus.updateStateString("CONNECTRETRY", String.valueOf(waittime),
                        R.string.state_waitconnectretry, ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET);
            resumeHandler.postDelayed(mResumeHoldRunnable, waittime * 1000);
        } else {
            waitingForRelease = true;
            VpnStatus.updateStatePause(lastPauseReason);
        }
    }


    private void releaseHoldCmd() {
        resumeHandler.removeCallbacks(mResumeHoldRunnable);
        if ((System.currentTimeMillis() - lastHoldRelease) < 5000) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                logHelper.logException(ignored);
            }
        }
        waitingForRelease = false;
        lastHoldRelease = System.currentTimeMillis();
        managmentCommand("hold release\n");
        managmentCommand("bytecount " + mBytecountInterval + "\n");
        managmentCommand("state on\n");
        //managmentCommand("log on all\n");
    }


    public void releaseHold() {
        if (waitingForRelease)
            releaseHoldCmd();
    }


    private void processProxyCMD(String argument) {
        String[] args = argument.split(",", 3);
        SocketAddress proxyaddr = ProxyListener.detectProxy(vpnProfile);
        if (args.length >= 2) {
            String proto = args[1];
            if (proto.equals("UDP")) {
                proxyaddr = null;
            }
        }
        if (proxyaddr instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) proxyaddr;
            VpnStatus.logInfo(R.string.using_proxy, isa.getHostName(), isa.getPort());
            String proxycmd = String.format(Locale.ENGLISH, "proxy HTTP %s %d\n", isa.getHostName(), isa.getPort());
            managmentCommand(proxycmd);
        } else {
            managmentCommand("proxy NONE\n");
        }
    }


    private void processState(String argument) {
        String[] args = argument.split(",", 3);
        String currentstate = args[1];
        if (args[2].equals(",,"))
            VpnStatus.updateStateString(currentstate, "");
        else
            VpnStatus.updateStateString(currentstate, args[2]);
    }


    private void processByteCount(String argument) {
        //   >BYTECOUNT:{BYTES_IN},{BYTES_OUT}
        int comma = argument.indexOf(',');
        long in = Long.parseLong(argument.substring(0, comma));
        long out = Long.parseLong(argument.substring(comma + 1));
        VpnStatus.updateByteCount(in, out);
    }


    private void processNeedCommand(String argument) {
        int p1 = argument.indexOf('\'');
        int p2 = argument.indexOf('\'', p1 + 1);
        String needed = argument.substring(p1 + 1, p2);
        String extra = argument.split(":", 2)[1];
        String status = "ok";
        switch (needed) {
            case "PROTECTFD":
                FileDescriptor fdtoprotect = fdList.pollFirst();
                protectFileDescriptor(fdtoprotect);
                break;
            case "DNSSERVER":
            case "DNS6SERVER":
                openvpnService.addDNS(extra);
                break;
            case "DNSDOMAIN":
                openvpnService.setDomain(extra);
                break;
            case "ROUTE": {
                String[] routeparts = extra.split(" ");
            /*
            buf_printf (&out, "%s %s %s dev %s", network, netmask, gateway, rgi->iface);
            else
            buf_printf (&out, "%s %s %s", network, netmask, gateway);
            */
                if (routeparts.length == 5) {
                    if (BuildConstants.DEBUG) Assert.assertEquals("dev", routeparts[3]);
                    openvpnService.addRoute(routeparts[0], routeparts[1], routeparts[2], routeparts[4]);
                } else if (routeparts.length >= 3) {
                    openvpnService.addRoute(routeparts[0], routeparts[1], routeparts[2], null);
                } else {
                    logHelper.logWarning("Unrecognized ROUTE cmd:" + Arrays.toString(routeparts) + " | " + argument);
                }
                break;
            }
            case "ROUTE6": {
                String[] routeparts = extra.split(" ");
                openvpnService.addRoutev6(routeparts[0], routeparts[1]);
                break;
            }
            case "IFCONFIG":
                String[] ifconfigparts = extra.split(" ");
                int mtu = Integer.parseInt(ifconfigparts[2]);
                openvpnService.setLocalIP(ifconfigparts[0], ifconfigparts[1], mtu, ifconfigparts[3]);
                break;
            case "IFCONFIG6":
                openvpnService.setLocalIPv6(extra);
                break;
            case "PERSIST_TUN_ACTION":
                // check if tun cfg stayed the same
                status = openvpnService.getTunReopenStatus();
                break;
            case "OPENTUN":
                if (sendTunFD(needed, extra))
                    return;
                else
                    status = "cancel";
                // This not nice or anything but setFileDescriptors accepts only FilDescriptor class :(
                break;
            default:
                logHelper.logWarning("Unknown needok command " + argument);
                return;
        }
        String cmd = String.format("needok '%s' %s\n", needed, status);
        managmentCommand(cmd);
    }


    private boolean sendTunFD(String needed, String extra) {
        if (!extra.equals("tun")) {
            // only tun is available
            return false;
        }
        ParcelFileDescriptor pfd = openvpnService.openTun();
        if (pfd == null)
            return false;
        Method setInt;
        int fdint = pfd.getFd();
        try {
            setInt = FileDescriptor.class.getDeclaredMethod("setInt$", int.class);
            FileDescriptor fdtosend = new FileDescriptor();
            setInt.invoke(fdtosend, fdint);
            FileDescriptor[] fds = {fdtosend};
            localSocket.setFileDescriptorsForSend(fds);
            String cmd = String.format("needok '%s' %s\n", needed, "ok");
            managmentCommand(cmd);
            // Set the FileDescriptor to null to stop this mad behavior
            localSocket.setFileDescriptorsForSend(null);
            pfd.close();
            return true;
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException |
                IOException | IllegalAccessException exp) {
            logHelper.logException("Could not send fd over socket", exp);
        }
        return false;
    }


    private void processPWCommand(String argument) {
        //argument has the form 	Need 'Private Key' password
        // or  ">PASSWORD:Verification Failed: '%s' ['%s']"
        String needed;
        try {
            int p1 = argument.indexOf('\'');
            int p2 = argument.indexOf('\'', p1 + 1);
            needed = argument.substring(p1 + 1, p2);
            if (argument.startsWith("Verification Failed")) {
                proccessPWFailed(needed, argument.substring(p2 + 1));
                return;
            }
        } catch (StringIndexOutOfBoundsException sioob) {
            logHelper.logException("Could not parse management Password command: " + argument, sioob);
            return;
        }
        String pw = null;

        if (needed.equals("Private Key")) {
            pw = vpnProfile.getPasswordPrivateKey();
        } else if (needed.equals("Auth")) {
            pw = vpnProfile.getPasswordAuth();
            String usercmd = String.format("username '%s' %s\n",
                    needed, VpnProfile.openVpnEscape(vpnProfile.getUserName()));
            managmentCommand(usercmd);
        }
        if (pw != null) {
            String cmd = String.format("password '%s' %s\n", needed, VpnProfile.openVpnEscape(pw));
            managmentCommand(cmd);
        } else {
            openvpnService.requestInputFromUser(R.string.password, needed);
            logHelper.logWarning(String.format("Openvpn requires Authentication type '%s' but no password/key information available", needed));
        }
    }


    private void proccessPWFailed(String needed, String args) {
        VpnStatus.updateStateString("AUTH_FAILED", needed + args, R.string.state_auth_failed, ConnectionStatus.LEVEL_AUTH_FAILED);
    }


    private static boolean stopOpenVPN() {
        synchronized (active) {
            boolean sendCMD = false;
            for (OpenVPNManagementThread mt : active) {
                sendCMD = mt.managmentCommand("signal SIGINT\n");
                try {
                    if (mt.localSocket != null)
                        mt.localSocket.close();
                } catch (IOException e) {
                    // Ignore close error on already closed socket
                }
            }
            return sendCMD;
        }
    }


    @Override
    public void networkChange(boolean samenetwork) {
        if (waitingForRelease)
            releaseHold();
        else if (samenetwork)
            managmentCommand("network-change\n");
        else
            managmentCommand("network-change\n");
    }


    @Override
    public void setPauseCallback(PausedStateCallback callback) {
        mPauseCallback = callback;
    }


    public void signalusr1() {
        resumeHandler.removeCallbacks(mResumeHoldRunnable);
        if (!waitingForRelease)
            managmentCommand("signal SIGUSR1\n");
        else
            // If signalusr1 is called update the state string
            // if there is another for stopping
            VpnStatus.updateStatePause(lastPauseReason);
    }


    public void reconnect() {
        signalusr1();
        releaseHold();
    }


    private void processSignCommand(String b64data) {
        String signed_string = vpnProfile.getSignedData(b64data);
        if (signed_string == null) {
            managmentCommand("rsa-sig\n");
            managmentCommand("\nEND\n");
            stopOpenVPN();
            return;
        }
        managmentCommand("rsa-sig\n");
        managmentCommand(signed_string);
        managmentCommand("\nEND\n");
    }


    @Override
    public void pause(PauseReason reason) {
        lastPauseReason = reason;
        signalusr1();
    }


    @Override
    public void resume() {
        releaseHold();
        /* Reset the reason why we are disconnected */
        lastPauseReason = PauseReason.noNetwork;
    }


    @Override
    public boolean stopVPN(boolean replaceConnection) {
        boolean stopSucceed = stopOpenVPN();
        if (stopSucceed)
            mShuttingDown = true;
        return stopSucceed;
    }
}