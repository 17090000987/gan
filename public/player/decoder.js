self.Module = {
    onRuntimeInitialized: function () {
        onWasmLoaded();
    }
};

importScripts("ffmpeg_decode.js");

self.Decoder = {
    handle:0,
    isloaded:false,
    cacheBuffer:null,
    init:function () {
        if(self.isWasmLoaded){
            if(!this.isloaded){
                this.cacheBuffer = self.Module._malloc(1000*1024);
                var videoCallback = self.Module.addFunction(function (buff, size, timestamp, width, height) {
                    var outArray = self.Module.HEAPU8.subarray(buff, buff + size);
                    var data = new Uint8Array(outArray);
                    var data = {t:"frame", d:data, h:height, w:width};
                    self.postMessage(data);
                });
                this.handle = self.Module._createDecoder(videoCallback);
                if(this.handle<0){
                    self.postMessage({t:"event", code:10, msg:"_createDecoder fail"});
                }
                this.isloaded = true;
                console.log("_createDecoder handle="+this.handle);
                return this.handle;
            }
        }
        return -1;
    },
    decode:function (msg) {
        if(this.isloaded){
            var data = msg.data;
            console.log(data[0]+"-"+data[1]+"-"+data[2]+"-"+data[3]);
            self.Module.HEAPU8.set(data, this.cacheBuffer);
            var ret = self.Module._decode(this.handle, this.cacheBuffer, data.length, 0);
        }
    }
};

function onWasmLoaded(){
    console.log("------------onWasmLoaded---------");
    self.isWasmLoaded = true;
    self.Decoder.init();
}

self.onmessage = function (ev) {
    var msg = ev.data;
    if(msg.action == "play"){
        self.Decoder.init();
    }else if(msg.action == "decode"){
        self.Decoder.decode(msg);
    }
}