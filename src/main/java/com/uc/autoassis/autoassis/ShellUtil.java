package com.uc.autoassis.autoassis;


import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class ShellUtil {

    private static final Logger logger = Logger.getLogger(ShellUtil.class.getSimpleName());

    private static final String COMMAND_ADB = "adb shell";
    private static final String COMMAND_YUM = "yum";
    private static final String COMMAND_SH = "sh";
    private static final String COMMAND_EXIT = "exit\n";
    private static final String COMMAND_LINE_END = "\n";

    public static boolean checkRootPermission() {
        return execCommand("echo root", true, false).result == 0;
    }

    public static CommandResult execAdb(String command) {
        return execCommand(new String[]{command}, true, true);
    }

    public static CommandResult execAdbRetry(String command) {
        CommandResult commandResult;
        int count = 0;
        do {
            commandResult = execCommand(new String[]{command}, true, true);
        } while (!commandResult.isSuccess() && count++ < 5);
        return commandResult;
    }


    public static CommandResult execAdb(String[] commands) {
        return execCommand(commands, true, true);
    }

    public static CommandResult execCommand(String command) {
        return execCommand(new String[]{command}, false, true);
    }

    private static CommandResult execCommand(String command, boolean isAdb, boolean isNeedResultMsg) {
        return execCommand(new String[]{command}, isAdb, isNeedResultMsg);
    }

    private static CommandResult execCommand(String[] commands, boolean isAdb, boolean isNeedResultMsg) {
        int result = -1;
        if (commands == null || commands.length == 0) {
            return new CommandResult(result, null, null);
        }

        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;

        DataOutputStream os = null;
        try {
            String shell;
            if (isAdb) {
                shell = COMMAND_ADB;
            } else {
                shell = COMMAND_SH;
            }
            process = Runtime.getRuntime().exec(shell);
            os = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                if (command == null) {
                    continue;
                }

                os.write(command.getBytes());
                os.writeBytes(COMMAND_LINE_END);
                os.flush();
            }
            os.writeBytes(COMMAND_EXIT);
            os.flush();

            result = process.waitFor();

            // get command result
            if (isNeedResultMsg) {
                successMsg = new StringBuilder();
                errorMsg = new StringBuilder();
                successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
                errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String s;
                while ((s = successResult.readLine()) != null) {
                    successMsg.append(s).append("\n");
                }
                while ((s = errorResult.readLine()) != null) {
                    errorMsg.append(s).append("\n");
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            logger.warning("ShellUtils.execCommand ex:" + e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                System.out.println(e);
                logger.warning("ShellUtils.execCommand ex:" + e);
            }

            if (process != null) {
                process.destroy();
            }
        }
        return new CommandResult(result, successMsg == null ? null : successMsg.toString(), errorMsg == null ? null
                : errorMsg.toString());
    }

    public static class CommandResult {

        public int result;

        public String successMsg;

        public String errorMsg;

        public CommandResult(int result) {
            this.result = result;
        }

        public CommandResult(int result, String successMsg, String errorMsg) {
            this.result = result;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }

        @Override
        public String toString() {
            return "result: " + result +
                    (!StringUtils.hasText(successMsg) ? "" : "successMsg: " + successMsg) +
                    (!StringUtils.hasText(errorMsg) ? "" : "errorMsg: " + errorMsg);
        }

        public boolean isSuccess() {
            return result == 0;
        }
    }
}
