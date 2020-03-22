# player

测试网页地址
http://{ip}:{port}/player/index.html

#### 使用说明
<code>
    
xplayer =new XPlayer(document.getElementById('video-w'),'ws://{host}/ws/rtsp');
    
xplayer.play('rtsp://{host}:554/xxx')

xplayer.onVideoPlay = function () {
    
}

xplayer.onVideoError = function (msg, code) {
    
}

stop() 停止播放

pause() 暂停

resume() 继续

replay() 停止并重新播放 (界面会有加载)

fullscreen() 全屏

exitfullscreen() 退出全屏

changeLayoutSize 改变窗口大小

</code>
注意: 1.界面退出一定要调用stop




