var Demo = {
    player:null,
    wsUrl:"ws://" + window.location.host + "/ws/rtsp",
    play:function () {
        console.log("demo play");
        if(Demo.player){
            Demo.player.stop();
        }
        Demo.player = new IPlayer(document.getElementById("demo_video"), Demo.wsUrl);
        Demo.player.play(document.getElementById('input_video').value);
    },
    stop:function () {
        console.log("demo stop");
        if(Demo.player){
            Demo.player.stop();
            Demo.wsUrl = null;
        }
    }
}

Tab.current.page.onCreate=function(){
    console.log("demo onCreate");
    var playUrl = "rtsp://"+getHostWithoutPort()+":554/800.sdp";
    document.getElementById('input_video').value=playUrl;
    Demo.play();
}

Tab.current.page.onDestory = function(){
    console.log("demo onDestory");
    Demo.stop();
    Demo = null;
}

