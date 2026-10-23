package com.parallelc.micts.shizuku;

// Runs inside a Shizuku user service process, i.e. as shell (uid 2000) or root.
interface IShellService {
    // Reserved by Shizuku for unbindUserService(..., remove = true).
    void destroy() = 16777114;

    // Calls IContextualSearchManager.startContextualSearch(entryPoint) with shell identity.
    boolean startContextualSearch(int entryPoint) = 1;

    // Streams the output of `screencap` (raw RGBA, or PNG when png is true).
    ParcelFileDescriptor captureScreen(boolean png) = 2;

    // Runs a shell command and returns "<exit code>\n<stdout+stderr>".
    String exec(String command) = 3;
}
