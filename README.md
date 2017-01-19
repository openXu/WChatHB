# WXhongbao
微信自动抢红包插件，支持锁屏（无密保）聊天界面等各种情况

微信6.5.3插件地址： [](6.5.3版本.apk)

##优势 ：
> 1.可以在聊天界面自动强不依赖于通知栏推送
2.可以在屏幕熄灭的时候的时候点亮屏幕自动抢（没有设置屏保的情况下）

##效果：
![](wchathb.gif)


##注意：
> 1.微信不同版本的资源id不同，在AirAccessibilityService中需要修改对应版本“开”按钮的资源id，否则不能自动点开红包
2.“开”按钮的资源id可通过Tools-->Android-->Android Device Monitor 

如下图：
    ![](getResourId.png)

 

