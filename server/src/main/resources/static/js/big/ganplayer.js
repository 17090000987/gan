//给大数据用的player 备份
class GanPlayer {
    constructor(el, wsUri) {
        this.video = el;
        this.wsUri = wsUri;
        this.init();
    }

    init() {
        this.ver = 1.0;
        this.reset();
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
        this.sleeptime = 0;
        this.checkCount = 0;
        this.buffer = [];
        clearInterval(this.IntervalReconect);
        clearInterval(this.IntervalKeepAlive);
        clearInterval(this.IntervalTimeOut);
        this.freeMediaSource();
    }

    playFile(url) {
        this.play(url, "file");
    }

    play(rtsp, type) {
        this.stop();
        console.log("url:" + rtsp + ",play");
        this.url = rtsp;
        this.mediaType = type;
        this.reset();
        this.initWebSocket();
        clearInterval(this.IntervalTimeOut)
        this.IntervalTimeOut = setInterval(() => {
            this.closeWebSocket();
        }, 60000);
    }

    replay() {
        this.stop();
        this.play(this.url, this.mediaType);
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
            this.video.autoplay = true;
            this.video.play();
            this.playing = true;
            if (typeof this.onPlay === "function") {
                this.onPlay();
            }
        } else {
            console.error("Unsupported MIME type or codec: ", this.mimeCodec);
        }
    }

    freeMediaSource() {
        if (this.mediaSource) {
            try {
                this.mediaSource.endOfStream();
            } catch (e) {}
            if (this.sourceBuffer) {
                try {
                    this.sourceBuffer.removeAllRanges();
                    this.sourceBuffer.abort();
                    this.mediaSource.removeSourceBuffer(this.sourceBuffer);
                } catch (e) {}
                this.sourceBuffer.removeEventListener(
                    "updateend",
                    this.handleMp42Buffer
                );
                this.sourceBuffer = null;
            }
            this.mediaSource.removeEventListener("sourceopen", this.handleSourceOpen);
            this.mediaSource = null;
        }
    }

    onSourceopen() {
        this.mediaSource.duration = Infinity;
        this.sourceBuffer = this.mediaSource.addSourceBuffer(this.mimeCodec);
        this.sourceBuffer.removeEventListener("updateend", this.handleMp42Buffer);
        this.sourceBuffer.addEventListener("updateend", this.handleMp42Buffer);
        if (!this.websocket) {
            this.initWebSocket();
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

    closeWebSocket(){
        if (this.websocket) {
            this.websocket.close();
            this.websocket = null;
        }
    }

    stop() {
        if (this.playing) {
            console.log("url:" + this.url + ",stop");
        }
        this.playing = false;
        this.reset();
        this.closeWebSocket();
    }

    pause() {
        this.status = 1;
        this.video.pause();
    }

    resume() {
        this.status = 0;
        this.video.play();
    }

    onOpen() {
        console.log("websocket CONNECTED");
        var json = {
            url: this.url,
            mediaType: this.mediaType,
            ver: this.ver
        };
        this.websocket.send(JSON.stringify(json));
        // clearInterval(this.IntervalKeepAlive);
        // this.IntervalKeepAlive = setInterval(() => {
        //     this.sendControlMessgae();
        // }, 1000);
    }

    onClose(evt) {
        console.log("websocket DISCONNECTED");
        this.reset();
        this.freeMediaSource();
        console.log(evt.code);
        if (this.playing) {
            clearInterval(this.IntervalReconect);
            this.IntervalReconect = setInterval(() => {
                console.log("start RECONNECTED");
                this.play(this.url);
            }, 3000);
        }
    }

    onMessage(msg) {
        clearInterval(this.IntervalTimeOut)
        if (this.meidaInited) {
            try {
                this.buffer.push(msg.data);
                this.mp4appendBuffer();
            } catch (e) {
                console.log(e)
                console.log("onMessage:" + msg);
                if (this.websocket) {
                    this.websocket.close();
                }
            }
        } else {
            try {
                this.playing = true;
                var data = JSON.parse(msg.data);
                if (data && data.mediacodec) {
                    console.log("mediacodec:" + data.mediacodec);
                    if (data.mediacodec.toLowerCase().indexOf("h265") != -1) {
                    } else {
                        this.meidaInited = true;
                        this.initMediaSource(data.mediacodec);
                    }
                } else {
                    console.log(msg.data);
                    this.websocket.close();
                }
            } catch (e) {
                this.websocket.close();
                console.log(e);
            }
        }
    }

    onError(evt) {
        console.log("websocket onError");
        if (this.websocket) {
            this.websocket.close();
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
                    this.websocket.close();
                    console.log(t);
                }
        } else {
            console.log(this.sourceBuffer, "sourceBuffer is null or undefined");
        }
    }

    seek(time_start, time_end = "") {
        var request = "PAUSE " + this.rtsp + " RTSP/1.0\r\n";
        this.sendRtspRequest(request);
        request =
            "PLAY " +
            this.rtsp +
            " RTSP/1.0\r\n" +
            "Range:" +
            "npt=" +
            time_start +
            "-" +
            time_end +
            "\r\n";
        this.sendRtspRequest(request);
    }

    sendRtspRequest(rtsp) {
        var json = { rtsp: rtsp };
        this.websocket.send(JSON.stringify(json));
    }

    sendControlMessgae() {
        try {
            if (this.sourceBuffer) {
                if (this.sourceBuffer.buffered.length <= 0) {
                    this.checkCount++;
                    if (this.checkCount > 10) {
                        console.log("checkSourceBuffer Close");
                        this.websocket.close();
                    }
                } else {
                    this.checkCount = 0;
                    var len = this.video.buffered.length;
                    if (len < this.buffer.length) {
                        len = this.buffer.length;
                    }
                    if (len < this.sourceBuffer.buffered.length) {
                        len = this.sourceBuffer.buffered.length;
                    }
                    this.sourceBuffer.buffered.start(0);
                    var end = this.sourceBuffer.buffered.end(0);
                    var len2 = end - this.video.currentTime;
                    var s = (len > len2 ? len : len2) * 1000;
                    var json = { status: this.status, sleeptime: s };
                    if (s > 5000) {
                        console.log(json);
                    }
                    // this.websocket.send(JSON.stringify(json));
                }
            }
        } catch (e) {
            console.log(e);
        }
    }

    notifyBuffer(size){
        this.notifyStatusBuffer(this.status,size);
    }

    notifyStatusBuffer(status,sleeptime){
        if(this.status!=status){
            console.log("status change"+status);
        }
        this.status = status;
        // if(this.sleeptime!=sleeptime){
        //     console.log("sleeptime change"+sleeptime);
        // }
        // this.sleeptime = sleeptime;
        var json = { status: this.status, sleeptime:0};
        this.websocket.send(JSON.stringify(json));
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
                        this.websocket.close();
                        return;
                    }
                } catch (e) {}
            }
        }
    }
}
