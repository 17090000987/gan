class GanPlayer {

    /**
     * 错误 1:服务器未指定视频编码， 2：服务器返回数据错误， 3：编码不支持， 4:视频数据append出错, 5:websocket error
     * @param el
     * @param wsUri
     */

    constructor(canvas,wsUri) {
        this.ver = 1.0;
        this.status = 0;
        this.checkCount = 0;
        this.playing = false;
        this.buffer = [];
        this.init(canvas,wsUri);
    }

    init(canvas, wsUri) {
        this.reset();
        this.width = 1280;
        this.height = 720;
        this.yLength = this.width * this.height;
        this.uvLength = (this.width / 2) * (this.height / 2);
        this.canvas = canvas;
        this.webglPlayer = new WebGLPlayer(canvas, {preserveDrawingBuffer: false});
        var rate = this.width / this.height;
        if (rate > (600 / 400)) {
            canvas.setAttribute("width", "600px");
            canvas.setAttribute("height", (600 / rate) + "px");
        } else {
            canvas.setAttribute("width", (400 * rate) + "px");
            canvas.setAttribute("height", "400px");
        }

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
        this.checkCount = 0;
        this.reconnectCount = 0;
        this.buffer = [];
        clearInterval(this.IntervalReconect);
        clearInterval(this.IntervalKeepAlive);
        this.freeMediaSource();
    }

    playFile(url) {
        this.play(url, "file");
    }

    play(rtsp, type) {
        console.log("url:" + rtsp + ",play");
        this.url = rtsp;
        this.mediaType = type;
        this.reset();
        this.initWebSocket();
    }

    replay() {
        this.stop();
        this.play(this.url, this.mediaType);
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
        this.status = 1;
        this.playing = false;
    }

    resume() {
        this.status = 0;
    }

    onVideoEvent(code, msg) {
        console.log("onVideoEvent code:"+code+",msg:"+msg);
        switch (code) {
            case 0:
                if (typeof this.onVideoPlay === "function") {
                    this.intervalCheckBuffer();
                    this.onVideoPlay(msg, code);
                }
                break;
            case 1:
                if (typeof this.onVideoError === "function") {
                    this.runReconnect();
                    this.onVideoError(msg, code);
                }
                break;
        }
    }

    initMediaSource(mimeCodec) {
        this.mimeCodec = mimeCodec;
        var MediaSource = window.MediaSource || window.WebKitMediaSource;
        if (MediaSource.isTypeSupported(this.mimeCodec)) {
            console.log("create mediaSource");
            this.mediaSource = new MediaSource();
            this.mediaSource.removeEventListener("sourceopen", this.handleSourceOpen);
            this.mediaSource.addEventListener("sourceopen", this.handleSourceOpen);
            this.video.src = URL.createObjectURL(this.mediaSource);
            this.video.play();
            this.playing = true;
            this.onVideoEvent(0);
        } else {
            this.onVideoEvent(1, "错误:3");
            console.error("Unsupported MIME type or codec: ", this.mimeCodec);
        }
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
        this.IntervalReconect = setInterval(() => {
            console.log("start RECONNECTED");
            this.play(this.url);
            this.reconnectCount++;
        }, 5000);
    }

    onMessage(msg) {
        if (this.meidaInited) {
            try {
                if(this.webglPlayer){
                    this.webglPlayer.renderFrame(new Uint8Array(msg.data), this.width, this.height, this.yLength, this.uvLength);
                }
            } catch (e) {
                console.log(e);
                console.log("onMessage:" + msg);
                this.onVideoEvent(1, "错误:4");
                this.closeWebSocket();
            }
        } else {
            try {
                var data = JSON.parse(msg.data);
                this.meidaInited = true;
                if (data && data.mediacodec) {
                    console.log("mediacodec:" + data.mediacodec);
                    if (data.mediacodec.toLowerCase().indexOf("h265") != -1) {
                    } else {
                        this.meidaInited = true;
                        // this.initMediaSource(data.mediacodec);
                    }
                } else {
                    console.log(msg.data);
                    this.onVideoEvent(1, "错误:1");
                    this.closeWebSocket();
                }
            } catch (e) {
                this.closeWebSocket();
                this.onVideoEvent(1, "错误:2");
                console.log(e);
            }
        }
    }

    onError() {
        console.log("websocket onError");
        this.onVideoEvent(1,"错误：5");
    }

    closeWebSocket(){
        // if (this.websocket) {
        //     console.log("closeWebSocket");
        //     this.websocket.close(3001);
        //     try{
        //         this.websocket.onmessage = null;
        //         this.websocket.onclose = null;
        //         this.websocket.onopen = null;
        //         this.websocket.onerror = null;
        //         this.websocket = null;
        //     }catch (e) {
        //         console.log(e);
        //     }
        // }
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
        clearInterval(this.IntervalCheckBuffer);
        this.IntervalCheckBuffer = setInterval(() => {
            this.checkBuffer();
        }, 5000);
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
        if(this.status!=status){
            console.log("status change"+status);
            this.status = status;
            var json = { status: this.status, sleeptime: this.sleeptime};
            if(this.websocket){
                this.websocket.send(JSON.stringify(json));
            }
        }
        if(this.sleeptime!=sleeptime){
            console.log("sleeptime change"+sleeptime);
            this.sleeptime = sleeptime;
            var json = { status: this.status, sleeptime: this.sleeptime};
            if(this.websocket){
                this.websocket.send(JSON.stringify(json));
            }
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

    fullscreen() {
        this.webglPlayer.fullscreen();
    }

    exitfullscreen() {
        this.webglPlayer.exitfullscreen();
    }

}
