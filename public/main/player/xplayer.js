(function () {

    var cur_js_path = (function(){
        var js=document.scripts;
        js=js[js.length-1].src.substring(0,js[js.length-1].src.lastIndexOf("/"));
        return js;
    })();
    function loadJs(url){
        var script=document.createElement('script');
        script.type="text/javascript";
        script.src=url;
        document.body.appendChild(script);
    }
    function loadCss(url){
        var script=document.createElement('link');
        script.rel="stylesheet";
        script.href=url;
        document.body.appendChild(script);
    }
    document.addEventListener('readystatechange',function () {
        loadJs(cur_js_path+'/webgl.js');
        loadCss(cur_js_path+'/xplay.css');
        if(!window.jQuery){
            loadJs(cur_js_path+'/zepto.min.js');
        }
    });

    window.IPlayer = class{

        /**
         * 错误 1:服务器未指定视频编码， 2：服务器返回数据错误， 3：编码不支持， 4:视频数据append出错, 5:websocket error
         * @param el dom元素
         * @param wsUri ws://116.62.33.55:88/ws/rtsp
         */

        constructor(el,wsUri) {
            this.ver = 1.0;
            this.status = 0;
            this.playing = false;
            this.buffer = [];
            this.muted = false;
            this.init(el,wsUri);
        }

        init(el, wsUri) {
            this.reset();
            this.fixSize = false;
            this.layout = el;
            this.layoutWidth = el.style.width;
            this.layoutHeight = el.style.height;
            this.wsUri = wsUri;
            this.mimeCodec = 'video/mp4; codecs="avc1.42E01E"';
            /*处理事件绑定得回调得作用域*/
            this.handleSourceOpen = this.onSourceopen.bind(this);
            this.handleMp42Buffer = this.updateEnd.bind(this);
            this.handleChange = this.onVisibilityChange.bind(this);
            document.removeEventListener("visibilitychange", this.handleChange);
            document.addEventListener("visibilitychange", this.handleChange);
        }

        reset() {
            this.status = 0;
            this.reconnectCount = 0;
            this.buffer = [];
            clearInterval(this.IntervalReconect);
            this.freeMediaSource();
            this.freeDecoder();
        }

        playFile(url) {
            this.play(url, 0, 2);
        }

        /**
         *
         * @param rtsp
         * rtsp://116.62.33.55:554/nvr?deviceID=51010100001310000011&channel=51010100001310000011
         * 或
         * rtsp://116.62.33.55:554/nvr?deviceID=51010100001310000011&channel=0
         * @param type  不填
         */
        play(rtsp, hasAudio=0,type=0) {
            console.log("url:" + rtsp + ",play");
            this.url = rtsp;
            this.hasAudio=hasAudio;
            this.mediaType = type;
            this.stop();
            this.initWebSocket();
        }

        replay() {
            this.stop();
            this.play(this.url, this.hasAudio, this.mediaType);
        }

        pause() {
            clearInterval(this.IntervalCheckBuffer);
            clearInterval(this.IntervalReconect);
            this.status = 1;
            this.notifyStatusBuffer(this.status);
            this.playing = false;
            if (this.video) {
                this.video.pause();
            }
        }

        stop() {
            if (this.playing) {
                console.log("url:" + this.url + ",stop");
            }
            this.pause();
            this.reset();
            this.closeWebSocket();
        }

        pause() {
            clearInterval(this.IntervalCheckBuffer);
            clearInterval(this.IntervalReconect);
            this.status = 1;
            this.notifyStatusBuffer(this.status);
            this.playing = false;
            if (this.video) {
                this.video.pause();
            }
        }

        resume() {
            this.status = 0;
            this.notifyStatusBuffer(this.status);
            this.intervalCheckBuffer();
            if (this.video) {
                this.video.play();
            }
            if (this.meidaInited) {
                this.playing = true;
                return true;
            } else {
                this.replay();
                return false;
            }
        }

        onVideoEvent(type, code, msg) {
            console.log("type:" + type + ",code:" + code + ",msg:" + msg);
            switch (type) {
                case 0:
                    if (typeof this.onVideoPlay === "function") {
                        this.intervalCheckBuffer();
                        this.onVideoPlay(msg, code);
                    }
                    break;
                case 1:
                    this.onVideoErrorEventCallBack(code, msg);
                    break;
            }
        }

        onVideoErrorEventCallBack(code, msg){
            if(this.playing){
                this.runReconnect();
            }
            if (typeof this.onVideoError === "function") {
                this.onVideoError(msg, code);
            }
        }
        getShotImgData(){
            if(this.video){
                var canvas = document.createElement("canvas");
                var scale = 1.0;
                canvas.width = this.video.videoWidth * scale;
                canvas.height = this.video.videoHeight * scale;
                canvas.getContext('2d').drawImage(this.video, 0, 0, canvas.width, canvas.height);
                return  canvas.toDataURL('image/png');
            }else if(this.canvas){
                return  this.canvas.toDataURL('image/png');

            }
        }

        removeVideoElement() {
            if (this.video) {
                this.video.remove();
                this.video = null;
            }
            if (this.canvas) {
                this.canvas.remove();
                this.canvas = null;
            }
        }

        initMediaSource(mimeCodec) {
            this.removeVideoElement();
            var video= this.video = document.createElement("video");
            this.video.autoplay = true;
            this.video.muted = this.muted;
            this.video.style.width = '100%';
            this.video.style.height = '100%';
            this.video.style.muted = true;
            this.video.className = 'x-video-tag';
            this.video.controls=false;
            this.video.oncontextmenu= function () {
                return false
            }
            this.video.oncanplay=function () {
                video.muted = this.muted;
                video.play();
            }
            if (!this.fixSize) {
                this.video.style.objectFit = 'fill';
            }
            this.layout.append(this.video);
            this.mimeCodec = mimeCodec;
            var MediaSource = window.MediaSource || window.WebKitMediaSource;
            if (MediaSource.isTypeSupported(this.mimeCodec)) {
                console.log("create mediaSource");
                this.mediaSource = new MediaSource();
                this.mediaSource.removeEventListener("sourceopen", this.handleSourceOpen);
                this.mediaSource.addEventListener("sourceopen", this.handleSourceOpen);
                this.video.src = URL.createObjectURL(this.mediaSource);
                this.playing = true;
                this.onVideoEvent(0, 0, "h264 started");
                this.video.muted = this.muted;
                this.video.play();
            } else {
                this.onVideoEvent(1, 3, "错误:3");
                console.error("Unsupported MIME type or codec: ", this.mimeCodec);
            }
        }

        muted(muted){
            this.muted = muted;
            if(this.video){
                this.video.muted = muted;
                this.video.play();
            }
        }

        changeLayoutSize(width,height) {
            if(this.layout){
                this.layout.style.width = width+"px";
                this.layout.style.height = height+"px";
                this.layoutWidth = width;
                this.layoutHeight = height;
            }
            if(this.webglPlayer){
                this.webglPlayer.notifyScreenSizeChanged();
            }
            this.layoutSizeChanged = true;
        }

        fullscreen() {
            if(this.webglPlayer){
                this.webglPlayer.fullscreen();
            }
            if(this.video){
                if (this.video.requestFullscreen) {
                    this.video.requestFullscreen();
                } else if (this.video.mozRequestFullScreen) {
                    this.video.mozRequestFullScreen();
                } else if (this.video.webkitRequestFullScreen) {
                    this.video.webkitRequestFullScreen();
                }
            }
        }

        exitfullscreen() {
            if(this.webglPlayer){
                this.webglPlayer.exitfullscreen();
            }
            if(this.video){
                if (this.video.exitFullscreen) {
                    this.video.exitFullscreen();
                } else if (this.video.mozCancelFullScreen) {
                    this.video.mozCancelFullScreen();
                } else if (this.video.webkitCancelFullScreen) {
                    this.video.webkitCancelFullScreen();
                }
            }
        }

        initH265Decoder(mimeCodec){
            this.removeVideoElement();
            var canvas = this.canvas = document.createElement("canvas");
            canvas.style.width = '100%';
            canvas.style.height = '100%';
            this.layout.append(this.canvas);
            var webglPlayer = this.webglPlayer = new WebGLPlayer(canvas, {preserveDrawingBuffer: false});
            var frame_width = 0, frame_height = 0;
            var last_frame_time = 0;
            var decoder=this.decoder= new Worker(cur_js_path+"/decoder.js");
            var frame_time_ary = new Array();
            var xplayer = this;
            decoder.onmessage = function (msg) {
                var msg_data = msg.data;
                if (msg_data.t == "frame") {
                    if(xplayer.fixSize){
                        if (frame_height == 0 || frame_width == 0
                            ||xplayer.layoutSizeChanged) {
                            frame_height = msg_data.h;
                            frame_width = msg_data.w;
                            var rate = frame_width / frame_height;
                            if (rate > (xplayer.layoutHeight / xplayer.layoutWidth)) {
                                canvas.style.width = xplayer.layoutWidth +"px";
                                canvas.style.height = xplayer.layoutWidth*rate+"px";
                            } else {
                                canvas.style.width = xplayer.layoutHeight*rate +"px";
                                canvas.style.height = xplayer.layoutHeight+"px";
                            }
                            xplayer.layoutSizeChanged = false;
                        }
                    }

                    var time = (new Date()).getTime();
                    last_frame_time = time;

                    var data = msg_data.d;
                    var width = msg_data.w;
                    var height = msg_data.h;
                    var yLength = width * height;
                    var uvLength = (width / 2) * (height / 2);
                    webglPlayer.renderFrame(data, width, height, yLength, uvLength);

                    if (frame_time_ary.length > 0) {
                        while ((time - frame_time_ary[0]) > 1000) {
                            frame_time_ary.shift();
                        }
                    }

                    frame_time_ary.push(time);
                }else if(msg_data.t == "event"){
                    xplayer.onVideoEvent(1, msg_data.code, msg_data.msg);
                }
            };
            decoder.postMessage({action: "play"});
            this.onVideoEvent(0, 0, "h265 started");
        }

        freeMediaSource() {
            if (this.sourceBuffer) {
                try {
                    this.sourceBuffer.abort();
                    this.sourceBuffer.removeAllRanges();
                } catch (e) {
                    console.info(e);
                }
                this.sourceBuffer.removeEventListener(
                    "updateend",
                    this.handleMp42Buffer
                );
            }

            if (this.mediaSource) {
                try {
                    this.mediaSource.removeSourceBuffer(this.sourceBuffer);
                    this.mediaSource.removeEventListener("sourceopen", this.handleSourceOpen);
                    this.mediaSource.endOfStream();
                } catch (e) {
                    console.info(e);
                }
            }

            this.sourceBuffer = null;
            this.mediaSource = null;
            this.removeVideoElement();
            this.meidaInited = false;
        }

        freeDecoder(){
            this.removeVideoElement();
            this.decoder = null;
            this.meidaInited = false;
        }

        onSourceopen() {
            console.log("addSourceBuffer");
            if(this.mediaSource){
                this.mediaSource.duration = Infinity;
                this.sourceBuffer = this.mediaSource.addSourceBuffer(this.mimeCodec);
                this.sourceBuffer.removeEventListener("updateend", this.handleMp42Buffer);
                this.sourceBuffer.addEventListener("updateend", this.handleMp42Buffer);
            }
        }

        initWebSocket() {
            this.meidaInited = false;
            this.playing = true;
            this.websocket = new WebSocket(this.wsUri);
            this.websocket.binaryType = "arraybuffer";
            this.websocket.onopen = this.onOpen.bind(this);
            this.websocket.onmessage = this.onMessage.bind(this);
            this.websocket.onerror = this.onError.bind(this);
            this.websocket.onclose = this.onClose.bind(this);
        }

        onOpen() {
            console.log("websocket CONNECTED");
            var json = {
                url: this.url,
                hasAudio: this.hasAudio,
                mediaType: this.mediaType,
                ver: this.ver
            };
            this.websocket.send(JSON.stringify(json));
        }

        onClose(evt) {
            console.log("websocket DISCONNECTED");
            this.reset();
            console.log(evt.code);
            if (this.playing) {
                this.runReconnect();
            }
        }

        runReconnect(){
            if(this.reconnectCount>10){
                clearInterval(this.IntervalReconect);
                return;
            }
            clearInterval(this.IntervalReconect);
            if(this.playing){
                this.IntervalReconect = setInterval(() => {
                    console.log("start RECONNECTED");
                    this.replay();
                    this.reconnectCount++;
            }, 5000);
            }
        }

        onMessage(msg) {
            if (this.meidaInited) {
                try {
                    if(this.decoder){
                        var data = new Uint8Array(msg.data);
                        this.decoder.postMessage({action: "decode", data: data});
                    }else{
                        this.buffer.push(msg.data);
                        this.mp4appendBuffer();
                    }
                } catch (e) {
                    console.log("onMessage:" + msg);
                    this.onVideoEvent(1, 4, "错误:4");
                    this.closeWebSocket();
                }
            } else {
                try {
                    var data = JSON.parse(msg.data);
                    if (data && data.ok && data.mediacodec) {
                        console.log("mediacodec:" + data.mediacodec);
                        if (data.mediacodec.toLowerCase().indexOf("h265") != -1) {
                            this.initH265Decoder(data.mediacodec);
                            this.meidaInited = true;
                        } else {
                            this.meidaInited = true;
                            this.initMediaSource(data.mediacodec);
                        }
                    } else {
                        console.log(msg.data);
                        this.onVideoEvent(1, 1,"错误:1");
                        this.closeWebSocket();
                    }
                } catch (e) {
                    this.closeWebSocket();
                    this.onVideoEvent(1, 2,"错误:2");
                    console.log(e);
                }
            }
        }

        onError() {
            console.log("websocket onError");
            this.onVideoEvent(1, 5, "错误：5");
        }

        closeWebSocket(){
            if (this.websocket) {
                console.log("closeWebSocket");
                this.websocket.close(3001);
                try{
                    this.websocket.onmessage = null;
                    this.websocket.onclose = null;
                    this.websocket.onopen = null;
                    this.websocket.onerror = null;
                    this.websocket = null;
                }catch (e) {
                    console.log(e);
                }
            }
        }

        updateEnd() {
            this.mp4appendBuffer();
        }

        mp4appendBuffer() {
            if (this.sourceBuffer) {
                if (this.buffer.length > 0 && !this.sourceBuffer.updating)
                    try {
                        this.sourceBuffer.appendBuffer(this.buffer.shift());
                    } catch (t) {
                        this.closeWebSocket();
                        console.log(t);
                    }
            }
        }

        seek(time_start, time_end = "") {
            var request = "PAUSE " + this.rtsp + " RTSP/1.0\r\n";
            this.sendRtspRequest(request);
            request = "PLAY " + this.rtsp + " RTSP/1.0\r\n" +
                "Range:" + "npt=" + time_start + "-" + time_end +
                "\r\n";
            this.sendRtspRequest(request);
        }

        sendRtspRequest(rtsp) {
            var json = { rtsp: rtsp };
            this.websocket.send(JSON.stringify(json));
        }

        intervalCheckBuffer(){
            if(this.video){
                clearInterval(this.IntervalCheckBuffer);
                this.IntervalCheckBuffer = setInterval(() => {
                    this.checkBuffer();
            }, 5000);
            }
        }

        checkBuffer() {
            try {
                var len = this.video.buffered.length;
                if (len < this.buffer.length) {
                    len = this.buffer.length;
                }
                if(this.sourceBuffer.buffered.length>0){
                    if (len < this.sourceBuffer.buffered.length) {
                        len = this.sourceBuffer.buffered.length;
                    }
                    this.sourceBuffer.buffered.start(0);
                    var end = this.sourceBuffer.buffered.end(0);
                    var len2 = end - this.video.currentTime;
                }
                var sleepTime = (len > len2 ? len : len2)*1000;
                if(sleepTime>1000){
                    this.notifyBuffer(sleepTime);
                }else{
                    this.notifyBuffer(0);
                }
            } catch (e) {
                console.log(e);
            }
        }

        notifyBuffer(sleeptime){
            this.notifyStatusBuffer(this.status,sleeptime);
        }

        notifyStatusBuffer(status,sleeptime){
            if (this.status != status) {
                console.log("status change" + status);
                this.status = status;
            }
            if (sleeptime && this.sleeptime != sleeptime) {
                console.log("sleeptime change" + sleeptime);
                this.sleeptime = sleeptime;
            }
            var json = {status: this.status, sleeptime: this.sleeptime};
            if (this.websocket) {
                this.websocket.send(JSON.stringify(json));
            }
        }

        onVisibilityChange() {
            /**
             * status 0:online,1:offline;
             */
            if (document.visibilityState === "hidden") {
                this.notifyStatusBuffer(1,this.sleeptime);
            } else {
                this.notifyStatusBuffer(0,this.sleeptime);
                if (this.sourceBuffer) {
                    try {
                        var len = this.sourceBuffer.buffered.end(0) - this.video.currentTime;
                        if (len > 60) {
                            console.log("检测当前浏览器为开启状态 len>60 close");
                            this.replay();
                            return;
                        }
                    } catch (e) {}
                }
            }
        }
    }


    window.XPlayer = class{
        /**
         *
         * @param dom  Dom元素对象
         * @param options
                {
              media_server:{
                  ip:'116.62.33.55',
                  rtspPort:554,
                  wsPort:88
              },
              player_option:{
                screenshot:false,
                pause:false,
                voice:false,
                fullscreen:true,
              }

          }
         */
        constructor(dom,options){
            let media_server = options.media_server;
            this.ws_url = `ws://${media_server.ip}:${media_server.wsPort}/ws/rtsp`;
            this.http_url = `http://${media_server.ip}:${media_server.wsPort}`;
            this.rtsp_url =  `rtsp://${media_server.ip}:${media_server.rtspPort}`;
            this.player_option = Object.assign(this.getDefaultOptions(),options.player_option);
            this.$dom = $(dom);
            this.player = new IPlayer(dom,this.ws_url);
            this.player.onVideoPlay = this.onVideoPlay.bind(this);
            this.device_name = '';
            this.channel_name = '';
            this.renderHtml();
        }

        getDefaultOptions(){
            return {
                screenshot:false,
                pause:false,
                voice:false,
                fullscreen:true,
            };
        }
        /**
         *
         * @param deviceID 51010100001310000011
         * @param channel 0 或 51010100001310000135
         * @param type
         * @param device_name 设备名称，截图使用
         * @param channel_name  设备通道号 ，截图使用
         * 'nvr', //GB28181 nvr或摄像头
         * 'device',//北斗车载sdk
         * 'jt1078',//808车载
         */
        play(deviceID,channel,type,device_name,channel_name){
            var rtsp_url = `${this.rtsp_url}/${type}?deviceID=${deviceID}&channel=${channel}`;
            this.$dom.find('.x-video-loading').show();
            this.playRtsp(rtsp_url);
            this.device_name = device_name;
            this.channel_name = channel_name;
        }


        renderHtml(){
            var html = `
<div class="x-video-loading"></div>
<div class="x-video-tip"></div>
<div class="x-video-control-bottom">
                             <span class="x-screen-shot-btn" title="截图"></span>
                            <span class="x-switch-stop-btn" title="暂停"></span>
                            <span class="x-muted-btn" title="静音"></span>
                            <span class="x-full-screen-btn" title="全屏"></span>
                        </div>`;
            this.$dom.css({position:'relative'});
            this.$dom.append(html);
            if(!this.player_option.screenshot){
                this.$dom.find('.x-screen-shot-btn').hide();
            }
            if(!this.player_option.pause){
                this.$dom.find('.x-switch-stop-btn').hide();
            }
            if(!this.player_option.voice){
                this.$dom.find('.x-muted-btn').hide();
            }
            if(!this.player_option.fullscreen){
                this.$dom.find('.x-full-screen-btn').hide();
            }
            this.initEvent();
        }
        initEvent(){
            this.$dom.on('click','.x-screen-shot-btn',this.onshot.bind(this));
            this.$dom.on('click','.x-switch-stop-btn',this.onpasue.bind(this));
            this.$dom.on('click','.x-muted-btn',this.onmuted.bind(this));
            this.$dom.on('click','.x-full-screen-btn',this.onscreen.bind(this));
        }
        onshot(){

            var imgData = this.player.getShotImgData();
            var type = "png";
            imgData = imgData.replace(this._fixType(type),'image/octet-stream');

            Date.prototype.format = function(fmt) {
                var o = {
                    "M+" : this.getMonth()+1,                 //月份
                    "d+" : this.getDate(),                    //日
                    "h+" : this.getHours(),                   //小时
                    "m+" : this.getMinutes(),                 //分
                    "s+" : this.getSeconds(),                 //秒
                    "q+" : Math.floor((this.getMonth()+3)/3), //季度
                    "S"  : this.getMilliseconds()             //毫秒
                };
                if(/(y+)/.test(fmt)) {
                    fmt=fmt.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));
                }
                for(var k in o) {
                    if(new RegExp("("+ k +")").test(fmt)){
                        fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));
                    }
                }
                return fmt;
            }

            this._saveFile(imgData,this.device_name +'_' + this.channel_name + '_' + new Date().format('yyyyMMddhhmmss') +'.'+ 'png');
        }
        _fixType (type) {
            type = type.toLowerCase().replace(/jpg/i, 'jpeg');
            var r = type.match(/png|jpeg|bmp|gif/)[0];
            return 'image/' + r;
        }
        _saveFile(data,filename){
            var save_link = document.createElementNS('http://www.w3.org/1999/xhtml', 'a');
            save_link.href = data;
            save_link.download = filename;
            console.log(save_link);
            var event = document.createEvent('MouseEvents');
            event = new MouseEvent('click');
            save_link.dispatchEvent(event);
        }
        onpasue(e){
            if($(e.target).hasClass('x-switch-play-btn')){
                $(e.target).removeClass('x-switch-play-btn').attr('title','暂停');
                this.resume();
            }else{
                $(e.target).addClass('x-switch-play-btn').attr('title','播放');
                this.pause();
            }
        }
        onmuted(e){
            if($(e.target).hasClass('x-muted-cancel-btn')){
                $(e.target).removeClass('x-muted-cancel-btn').attr('title','静音');
            }else{
                $(e.target).addClass('x-muted-cancel-btn').attr('title','取消静音');
            }

        }
        onscreen(){
            this.player.fullscreen();
        }
        onVideoPlay(){
            console.log('11111111')
            this.$dom.find('.x-video-control-bottom').show();
            this.$dom.find('.x-video-loading').hide();
        }

        playRtsp(rtsp_url){
            this.player.play(rtsp_url);
        }
        stop(){
            this.$dom.find('.x-video-control-bottom').hide();
            this.$dom.find('.x-video-loading').hide();
            this.player.stop();
        }
        pause(){
            this.player.pause();
        }
        resume(){
            this.player.resume();
        }
        replay(){
            this.player.replay();
        }
        fullscreen(){
        }
        exitfullscreen(){
            this.player.exitfullscreen();
        }
        changeLayoutSize(){
            this.player.changeLayoutSize();
        }
        seek(time_start, time_end = ""){
            this.player.seek(time_start, time_end);
        }
    }

})();
