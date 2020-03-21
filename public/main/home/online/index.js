var Online={
    player:null,
    liveList:new Array,
    clear:function () {
        Online.liveList.splice(0,Online.liveList.length);
        $("#live_list").html("");
    },
    adapter: function (live, i) {
        console.log("live adapter");
        console.log(live);
        var $content = "live_content"+i;
        var $live_pic= "live_pic"+i;
        var image = '<img id="'+$live_pic+'" style="width:100%;z-index: -1" src="'+live.pic+'" onerror="Online.picError('+i+')">';
        var item = '<div id="'+$content+'" class="grid_item_warpper" style="width: 300px">'+
            '<div class="grid_item">'+
            '<div>'+image+'</div>'+
            '<p class="fs14" style="margin-top: 10px">名称: '+live.name+'</p>'+
            '<p class="fs12" style="margin-top: 5px">流类型：'+live.type+'</p>'+
            '<p class="fs12" style="margin-top: 5px">观看数：'+live.watchNum+'</p>'+
            '<p class="fs12" style="margin-top: 5px">地址：'+live.url+'</p>'+
            '</div>'+
            '</div>';
        $("#live_list").append(item);
        document.getElementById($content).onclick=function (ev) {
            Online.onItemClicked(item,live,i);
        };
    },
    picError:function(i){
        console.log("picError"+i);
        var $live_pic = "live_pic"+i;
        document.getElementById($live_pic).src = "img/welcome.jpg";
    },
    onItemClicked:function (html,live,i) {
        Online.play(live.url,live.type);
    },
    play:function(url,type){
        console.log(url);
        var video = document.getElementById("video");
        Online.stopPlay();
        if(!Online.player){
            var wsUrl = "ws://" + window.location.host + "/ws/rtsp"
            Online.player = new IPlayer(video,wsUrl);
        }
        if(type==2){
            Online.player.play(url,type,1);
        }else{
            Online.player.play(url,type);
        }
    },
    stopPlay:function () {
        if(Online.player){
            Online.player.stop();
        }
    },
    free:function () {
        Online.clear();
        Online.stopPlay();
    }
}

Tab.current.page.onCreate=function () {
    console.log("online page onCreate");
    Online.play("file/frag_bunny.mp4",2);

    Http.host="http://"+window.location.host;
    Http.post("/rtsp/list",null,function (obj) {
        Online.clear();
        var liveList = obj.list;
        list(liveList, Online.adapter);
    })
}

Tab.current.page.onDestory=function () {
    console.log("online page onDestory");
    Online.free();
}