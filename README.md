#gan

##项目介绍

直播、点播 视频流媒体服务器。
推流，拉流，使用rtsp协议。支持海康，大华摄像头拉流，支持APP和网页播放同时播放

##软件架构
软件架构说明

![image](./doc/project.png)

##安装教程

idea 导入项目，
maven parent 执行install执行完成

再执行package带执行完成

执行package.cmd 打包生成package路径中运行文件

##使用说明
进入打包好的package目录
1.  把application.yml.tpl 改成application.yml
2.  根据自己项目使用功能配置 application.yml 

###启动服务
####windos平台
直接执行start.bat 
####linux平台
1. 首先配置环境 vi envsetup.sh 把版本号修改到最新2.0.0
2. 启动 
    1. source envsetup.sh 
    2. sh start.sh
    
##测试网页
http://{serverIp}:{serverPort}/player/index.html

## 推流APP 版本下载 ##
下载地址[https://github.com/EasyDarwin/EasyPusher]

- Windows：[https://github.com/EasyDarwin/EasyPusher/releases](https://github.com/EasyDarwin/EasyPusher/releases "EasyPusher")

- Android：[https://fir.im/EasyPusher ](https://fir.im/EasyPusher "EasyPusher_Android")

![EasyPusher_Android](http://www.easydarwin.org/skin/bs/images/app/EasyPusher_AN.png)

- iOS：[https://itunes.apple.com/us/app/easypusher/id1211967057](https://itunes.apple.com/us/app/easypusher/id1211967057 "EasyPusher_iOS")

![EasyPusher_iOS](http://www.easydarwin.org/skin/bs/images/app/EasyPusher_iOS.png)
    

## 技术支持 ##

免费使用，技术交流直接到QQ群：459944381，交流意见！


## 获取更多信息 ##

Copyright &copy; Gan Team 2013-2020

