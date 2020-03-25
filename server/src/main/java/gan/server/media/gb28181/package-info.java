package gan.server.media.gb28181;

/*
    如何实现 国标
    sip服务器可以去找，有人实现的，
    sip通知设备把流传输到本项目，实现解析流过程
    把流媒体解析成每一帧数据，

    创建一个MediaSource对象，以RtspMediaServer为列子

    RtspMediaServer server = SystemServer.startServer(RtspMediaServer.class,session);
    server.setHasAudio(request.hasAudio);
    server.setOutputEmptyAutoFinish(true);
    server.startInputStream(rtsp, response.content);

    在每一帧回调方法里面调用
    server.putFrame();把数据传给server，就可以在网页上播放了，

    在手机上同时支持rtsp
    需要给server.registerPlugin(new RtspFrame2RtpPlugin())注入插件
                              }
*/
