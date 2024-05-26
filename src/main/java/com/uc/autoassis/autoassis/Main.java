package com.uc.autoassis.autoassis;


import com.ctc.wstx.util.DataUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.aspectj.util.FileUtil;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static boolean runToggle = true;
    private static boolean running = false;
    private static int mGlobalId = 0;


    public static void main() {
        if (running) {
            log("正在运行");
            return;
        }
        if (!runToggle) {
            log("开关为关闭，不抓取");
            return;
        }
        running = true;
        main(new String[]{});
        running = false;
    }

    public static void main(String[] args) {
        log("");

        log("抓取中...");
        String externalCacheDir = "/sdcard";
        ShellUtil.CommandResult commandResult = ShellUtil.execAdb("");
        if (!commandResult.isSuccess()) {
            log("连接异常，请检查手机USB调试是否已打开");
            return;
        }
        ////
        String pngName = "app.png";
        String uixName = "app.uix";
//        String uidName = "uid";
        String scrcpyPath = externalCacheDir + "/" + pngName;
        String uixPath = externalCacheDir + "/" + uixName;
        ShellUtil.CommandResult dumpActivityCmd = ShellUtil.execAdb("dumpsys activity | grep mResumedActivity");
        ShellUtil.CommandResult scrcpyCmd = ShellUtil.execAdbRetry("screencap -p " + scrcpyPath);
        ShellUtil.CommandResult dumpCmd = ShellUtil.execAdbRetry("uiautomator dump " + uixPath);
        if (dumpActivityCmd.isSuccess() && scrcpyCmd.isSuccess() && dumpCmd.isSuccess()) {
            log("dump 成功");
        }

        ShellUtil.CommandResult pwd = ShellUtil.execCommand("pwd");
        if (pwd.isSuccess()) {
            String currDir = pwd.successMsg.replace("\n", "");
            String distDir = "./";
//            String uidPath = "/sdcard/Android/data/com.uc.android/cache/" + uidName;
            ShellUtil.CommandResult commandResult1 = ShellUtil.execCommand("adb pull " + scrcpyPath + " " + distDir);
            ShellUtil.CommandResult commandResult2 = ShellUtil.execCommand("adb pull " + uixPath + " " + distDir);
//            ShellUtil.CommandResult commandResult3 = ShellUtil.execCommand("adb pull " + uidPath + " " + distDir);
            if (commandResult1.isSuccess() && commandResult2.isSuccess()) {
//            if (commandResult1.isSuccess() && commandResult2.isSuccess() && commandResult3.isSuccess()) {
                log("pull 成功");
                log("位置: " + currDir);

                String successMsg = dumpActivityCmd.successMsg;
                String packageName = "";
                String activity = "";
                if (StringUtils.hasText(successMsg)) {
                    Pattern compile = Pattern.compile("mResumedActivity: ActivityRecord\\{\\S+ \\S+ (\\S+) \\S+}");
                    Matcher matcher = compile.matcher(successMsg);
                    if (matcher.find()) {
                        String activityPackage = matcher.group(1);
                        String[] split = activityPackage.split("/");
                        if (split.length == 2) {
                            packageName = split[0];
                            if (split[1].startsWith(".")) {
                                activity = packageName + split[1];
                            } else {
                                activity = split[1];
                            }
                        }
                    }

                }
                upload(packageName, activity, new File(pngName), new File(uixName));
//                upload(packageName, activity, new File(pngName), new File(uixName), new File(uidName));
            }
        }


        log("完成");
    }

    public static final Map<String, Object> m = new HashMap<>();

    private static void upload(String packageName, String activity, File file, File file1) {
//    private static void upload(String packageName, String activity, File file, File file1, File file3) {
        try {

//            String uid = FileUtil.readAsString(file3);
            String pngBase64 = Base64.getEncoder().encodeToString(FileUtil.readAsByteArray(file));
            String uix = FileUtil.readAsString(file1);


//            if (!StringUtils.hasText(uix)) {
//                return;
//            }
            // 使用XmlMapper将XML转换为JsonNode
            JsonNode jsonNode = convertXmlToJson(uix);
            if (jsonNode == null) {
                return;
            }
            mGlobalId = 0;
            processObjectChildNode(jsonNode);
            log("页面: " + packageName + ": " + activity);
            m.put("package", packageName);
            m.put("activity", activity);
            m.put("png", "data:image/png;base64," + pngBase64);
            m.put("uix", Collections.singletonList(jsonNode));
//            m.put("uid", uid);
            m.put("time", System.currentTimeMillis());

        } catch (Exception e) {
            e.printStackTrace();
            log("失败");
        }
    }

    private static void processObjectChildNode(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            ObjectNode parentNode = (ObjectNode) jsonNode;
            JsonNode aClass = parentNode.get("class");
            StringBuilder label = new StringBuilder();
            if (aClass == null) {
                label.append("节点");
            } else {
                label.append(aClass.asText());
            }
            JsonNode bounds = parentNode.get("bounds");
            if (bounds != null && StringUtils.hasText(bounds.asText())) {
                String[] split = bounds.asText().replaceAll("]\\[", ",").replaceAll("\\[", "").replaceAll("]", "")
                        .split(",");
                if (split.length == 4) {
                    ArrayNode rect = parentNode.putArray("rect");
                    for (String s : split) {
                        rect.add(Integer.parseInt(s));
                    }
                }
            }
            parentNode.put("label", label.toString());
            parentNode.put("id", mGlobalId++);
            JsonNode childNode = parentNode.get("node");
            if (childNode != null && childNode.isObject()) {
                ArrayNode arrayNode = parentNode.putArray("node");
                arrayNode.add(childNode);
            }
            if (childNode != null) {
                processObjectChildNode(childNode);
            }
        } else if (jsonNode.isArray()) {
            ArrayNode parentNode = (ArrayNode) jsonNode;
            for (JsonNode node : parentNode) {
                processObjectChildNode(node);
            }
        }
    }

    private static JsonNode convertXmlToJson(String xmlData) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            return xmlMapper.readTree(xmlData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void log(String msg) {
        if (StringUtils.hasText(msg)) {
            System.out.println("=> " + msg);
        } else {
            System.out.println();
        }
    }

}
