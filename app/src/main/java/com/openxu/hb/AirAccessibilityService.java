package com.openxu.hb;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;


public class AirAccessibilityService extends AccessibilityService {

    public static boolean ALL = false;
    private List<AccessibilityNodeInfo> parents;
    private boolean auto = false;
    private int lastbagnum;
    String pubclassName;
    String lastMAIN;
    private boolean WXMAIN = false;

    private boolean enableKeyguard = true;//默认有屏幕锁
    private KeyguardManager km;
    private KeyguardManager.KeyguardLock kl;
    //唤醒屏幕相关
    private PowerManager pm;
    private PowerManager.WakeLock wl = null;
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        parents = new ArrayList<>();

    }

    /**
     * 通过AccessibilityService监听到状态栏通知，进行模拟点击，获取屏幕中view节点为领取红包的list并且点击最后一个
     * */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        if (auto)
            Log.e("openXu", "检测到事件" + eventType);
        switch (eventType) {
            //当通知栏发生改变时
            case 2048:
                pubclassName = event.getClassName().toString();
                Log.e("openXu", "有2048事件" + pubclassName + auto);

                if (!auto && pubclassName.equals("android.widget.TextView") && ALL) {
                    Log.e("openXu", "有2048事件被识别" + auto + pubclassName);
                    getLastPacket(1);
                }
                if (auto && WXMAIN) {
                    getLastPacket();
                    auto = false;
                }
                break;
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Log.e("openXu", "1、监测到通知栏变化");
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        Log.e("openXu", "消息内容："+content);
                        //接受到通知栏的红包消息亮屏设置标志位为自动领取，调取领取函数
                        if (content.contains("[微信红包]")) {
                            if (event.getParcelableData() != null &&
                                    event.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) event.getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    auto = true;
                                    wakeAndUnlock2(true);
                                    pendingIntent.send();
                                    Log.e("openXu", "进入微信：" + auto + event.getClassName().toString());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                break;
            //当窗口的状态发生改变时
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                Log.e("openXu", "2、监测到窗口状态变化");
                String className = event.getClassName().toString();
                if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                    //点击最后一个红包
                    Log.e("openXu", "微信启动了，点击红包");
                    if (auto)
                        getLastPacket();
                    auto = false;
                    WXMAIN = true;
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {
                    Log.e("openXu", "开红包");
                    click("com.tencent.mm:id/bi3");
                    auto = false;
                    WXMAIN = false;
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {
                    //退出红包
                    Log.e("openXu", "退出红包");
                    click("com.tencent.mm:id/gq");
                    WXMAIN = false;
                } else {
                    WXMAIN = false;
                    lastMAIN = className;
                }
                break;
        }
    }

    /**
     * 点击界面控件方法：（不同微信版本控件id不一样我的是6.3.25）
     * 如果不知道怎么获取这个id可以看下面的参考文献最后一个，右侧的resource-id就是这个id

     不同版本“开”按钮资源id：
     6.3.31                     com.tencent.mm:id/bg7
     6.3.32                     com.tencent.mm:id/bdh
     6.5.3（1月13号最新版本）      com.tencent.mm:id/be_
     6.5.4（1月19号内测 ）         com.tencent.mm:id/bi3

     目前不知道什么问题，在6.5.3下有一些手机无法监控到红包推送的通知，我也正在修复中。稳定版本6.3.32仍然可用。
     * @param clickId
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void click(String clickId) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(clickId);
            for (AccessibilityNodeInfo item : list) {
                item.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void getLastPacket() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        recycle(rootNode);
        Log.e("openXu", "当前页面红包数老方法" + parents.size());
        if (parents.size() > 0) {
            parents.get(parents.size() - 1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            lastbagnum = parents.size();
            parents.clear();
        }
    }

    /**
     * 所以在接受的时候，去处理一下去点击领取红包。然而这个方法会被频发调用，
     * 我们就加一个”Android.widget.TextView”事件类的筛选，然后还是会被很频繁调用，会比较迟性能耗电。
     那就加个布尔ALL标志位，让用户控制是否开启聊天界面内也抢红包的模式。
     为了不反复领取要记录上次界面内的红包数量在红包增加的时候才去领取：

     目前在通过插件领过几个红包后，一个聊天界面突然发第一个红包（没有通知栏）会有一定几率领取不到，
     再继续发就正常了，应该是清空问题，该bug笔者也在更改逻辑中。
     * @param c
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void getLastPacket(int c) {
        Log.e("openXu", "新方法" + parents.size());
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        recycle(rootNode);
        Log.e("openXu", "last++" + lastbagnum + "当前页面红包数" + parents.size());
        if (parents.size() > 0 && WXMAIN) {
            Log.e("openXu", "页面大于O且在微信界面");
            if (lastbagnum < parents.size())
                parents.get(parents.size() - 1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            lastbagnum = parents.size();
            parents.clear();
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void recycle(AccessibilityNodeInfo info) {
        try {
            if (info.getChildCount() == 0) {
                if (info.getText() != null) {
                    if ("领取红包".equals(info.getText().toString())) {
                        if (info.isClickable()) {
                            info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                        AccessibilityNodeInfo parent = info.getParent();
                        while (parent != null) {
                            if (parent.isClickable()) {
                                parents.add(parent);
                                break;
                            }
                            parent = parent.getParent();
                        }
                    }
                }
            } else {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (info.getChild(i) != null) {
                        recycle(info.getChild(i));
                    }
                }
            }
        } catch (Exception e) {


        }
    }

    /***
     * 点亮屏幕方法（有密码肯定是不行的）：
     * @param b
     */
    private void wakeAndUnlock2(boolean b){
        if(b) {
            //获取电源管理器对象
            pm=(PowerManager) getSystemService(Context.POWER_SERVICE);
            //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
            wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
            //点亮屏幕
            wl.acquire();
            //得到键盘锁管理器对象
            km= (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
            kl = km.newKeyguardLock("unLock");
            //解锁
            kl.disableKeyguard();
        } else {
            //锁屏
            kl.reenableKeyguard();
            //释放wakeLock，关灯
            wl.release();
        }
    }

    @Override
    public void onInterrupt() {

    }
}
