/**
 * Created by Administrator on 2017/4/11.
 */

function upload(file,success) {
    var client = new OSS.Wrapper({
        region: 'oss-cn-shanghai',
        accessKeyId: 'LTAIKHphHT74hbWS',
        accessKeySecret: 'zs9OzPeGP5HcMHksn2wHfr7BDMTtDE',
        bucket: 'ganhb'
    });
    var storeAs = "hb/"+file.name;
    console.log(file.name + ' => ' + "oss:"+ storeAs);
    client.multipartUpload(storeAs, file).then(function (result) {
        success&&success(result);
        console.log("file upload");
        console.log(result);
    }).catch(function (err) {
        console.log("file upload");
        console.log(err);
    });
}

function download(url,success) {
    // var client = new OSS.Wrapper({
    //     region: 'oss-cn-shanghai',
    //     accessKeyId: 'LTAIKHphHT74hbWS',
    //     accessKeySecret: 'zs9OzPeGP5HcMHksn2wHfr7BDMTtDE',
    //     bucket: 'ganhb'
    // });
    // var arrUrl=url.split("/");
    // var name=arrUrl[arrUrl.length-1];
    //
    // var saveAs = "cache/image/"+name;
    // console.log("download");
    // var objectKey = "hb/"+name;
    // var url = client.signatureUrl(objectKey);
    // console.log("download:"+url);
}

function downloadThumb(url,success,save) {
    var client = new OSS.Wrapper({
        region: 'oss-cn-shanghai',
        accessKeyId: 'LTAIKHphHT74hbWS',
        accessKeySecret: 'zs9OzPeGP5HcMHksn2wHfr7BDMTtDE',
        bucket: 'ganhb'
    });
    var arrUrl=url.split("/");
    var name=arrUrl[arrUrl.length-1];
    var objectKey = "hb/"+name;
    var saveAs = save;
    var result = client.signatureUrl(objectKey, {
        expires: 3600,
        process : 'image/resize,w_300',
        response: {
            'content-disposition': 'attachment; filename="' + saveAs + '"'
        }
    });
    console.log("download");
    console.log(result);
    success&&success(result,saveAs);
}

function browseFolder(path) {
    try {
        var Message = "\u8bf7\u9009\u62e9\u6587\u4ef6\u5939"; //选择框提示信息
        var Shell = new ActiveXObject("Shell.Application");
        var Folder = Shell.BrowseForFolder(0, Message, 64, 17); //起始目录为：我的电脑
        //var Folder = Shell.BrowseForFolder(0, Message, 0); //起始目录为：桌面
        if (Folder != null) {
            Folder = Folder.items(); // 返回 FolderItems 对象
            Folder = Folder.item(); // 返回 Folderitem 对象
            Folder = Folder.Path; // 返回路径
            if (Folder.charAt(Folder.length - 1) != "\\") {
                Folder = Folder + "\\";
            }
            document.getElementById(path).value = Folder;
            return Folder;
        }
    }
    catch (e) {
        return null;
    }
}

function isHasImg(pathImg){
    var ImgObj=new Image();
    ImgObj.src= pathImg;
    if(ImgObj.fileSize > 0 || (ImgObj.width > 0 && ImgObj.height > 0)) {
        return true;
    } else {
        return false;
    }
}