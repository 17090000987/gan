package gan.server.web.controller.rtsp;

import gan.web.base.Result;
import gan.web.base.StringException;
import gan.core.utils.TextUtils;
import gan.core.system.server.SystemServer;
import gan.web.spring.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import gan.server.GanUtils;
import gan.media.*;
import gan.media.mp4.Mp4MeidaOutputStream;
import gan.media.rtsp.RtspMediaServerManager;
import gan.server.web.service.rtsp.RtspService;

import java.util.Collection;

@Controller
@RequestMapping(value = "/rtsp")
public class RtspController extends BaseController {

    RtspService mRtspService = RtspService.getInstance();

    @RequestMapping(value = "/list")
    @ResponseBody
    public Result list() {
        Collection<MediaSourceInfo> list = mRtspService.getAll();
        RtspInfo rtspInfo = new RtspInfo();
        rtspInfo.sourceCount = list.size();
        for(MediaSourceInfo sourceInfo:list){
            rtspInfo.watchCount += sourceInfo.watchNum;
        }
        return Result.ok().setList(list).setData(rtspInfo);
    }

    @RequestMapping(value = "/getInfo")
    @ResponseBody
    public Result getInfo(String url) {
        if (TextUtils.isEmpty(url)) {
            return Result.error("token 参数没有找到");
        }
        MediaInfo session = mRtspService.getByUrl(url);
        if(session==null){
            session = mRtspService.getById(GanUtils.parseToken(url));
            if(session==null){
                return Result.error("找不到session");
            }
        }
        return Result.ok().setData(session);
    }

    @RequestMapping(value = "/record",method=RequestMethod.POST)
    @ResponseBody
    public Result record(String token){
        if (TextUtils.isEmpty(token)) {
            return Result.error("token 参数没有找到");
        }
        MediaRequest request = MediaRequest.obtainRequest(token);
        try{
            final MediaSource source = MediaServerManager.getInstance().getMediaSource(request);
            if(source==null){
                return Result.error("找不到数据源");
            }
            final String filePath = SystemServer.getRootPath("/media/h264/"+source.getMediaInfo().name+".mp4");
            String progress = RtspService.getInstance().getFileProgress(filePath);
            if(!TextUtils.isEmpty(progress)){
                return Result.ok().setData(progress);
            }
            MediaOutputInfo session = new MediaOutputInfo("file_output_"+source.getMediaInfo().name
                    ,source.getMediaInfo().url,"file_output_"+source.getMediaInfo().name);
            MediaOutputStreamRunnable runnable = new MediaOutputStreamRunnableFrame(
                    new Mp4MeidaOutputStream(source.getMediaInfo().url,SystemServer.getRootPath("/media/video/m.mp4"),
                            MediaConfig.createConfigH264()),
                    session).setPacketBufferMaxCount(Integer.MAX_VALUE);
            source.addMediaOutputStreamRunnable(runnable);
            SystemServer.executeThread(new Runnable() {
                @Override
                public void run() {
                    try{
                        RtspService.getInstance().managerFileProgress(filePath);
                        runnable.start();
                    }finally {
                        RtspService.getInstance().removeFileProgress(filePath);
                        source.removeMediaOutputStreamRunnable(runnable);
                    }
                }
            });
            return Result.ok().setData(0);
        }finally {
            request.recycle();
        }
    }

    @RequestMapping(value = "/pull",method=RequestMethod.POST)
    @ResponseBody
    public Result pull(){
        try{
            String url = checkEmpty("url");
            MediaRequest request = MediaRequest.obtainRequest(url);
            try{
                RtspMediaServerManager.getInstance().getRtspSourceByPull(request);
                return Result.ok();
            }finally {
                request.recycle();
            }
        }catch (StringException e){
            return Result.error(e);
        }
    }

}
