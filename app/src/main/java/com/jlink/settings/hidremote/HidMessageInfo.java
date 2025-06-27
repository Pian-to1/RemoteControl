package com.jlink.settings.hidremote;

public class HidMessageInfo {
    public static int TYPE_KEY = 0;
    public static int TYPE_ACTION = 1;
    public static int TYPE_CMD = 2;
    public static int TYPE_INFO = 3;

    public int id;
    public int type;
    public int keycode;
    public int keycodeAct = -1; //default for sendKeyDownUpSync

    public String action;
    public String packageName;
    public String actName;
    public String cmdline;
    public String info;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getKeycode() {
        return keycode;
    }

    public void setKeycode(int keycode) {
        this.keycode = keycode;
    }

    public int getKeycodeAct() {
        return keycodeAct;
    }

    public void setKeycodeAct(int keycodeAct) {
        this.keycodeAct = keycodeAct;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getActName() {
        return actName;
    }

    public void setActName(String actName) {
        this.actName = actName;
    }

    public String getCmdline() {
        return cmdline;
    }

    public void setCmdline(String cmdline) {
        this.cmdline = cmdline;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
