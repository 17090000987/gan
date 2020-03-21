// 创建XMLHttpRequest对象
function createXMLHttpRequest() {
    var xmlHttpReq = null;
    if (window.XMLHttpRequest) {// IE 7.0及以上版本和非IE的浏览器
        xmlHttpReq = new XMLHttpRequest();
    } else {// IE 6.0及以下版本
        try {
            xmlHttpReq = new ActiveXObject("MSXML2.XMLHTTP");
        }catch (e) {
            try {
                xmlHttpReq = new ActiveXObject("Microsoft.XMLHTTP");
            }catch (e) {}
        }
    }
    if (!xmlHttpReq) {
        alert("当前浏览器不支持!");
        return null;
    }
    return xmlHttpReq;
}

var Http = {
    host:"http://localhost/",
    mObjPool: [],
    getXmlRequest: function ()
    {
        for (var i = 0; i<this.mObjPool.length; i ++)
        {
            if (this.mObjPool.readyState == 0 || this.mObjPool.readyState == 4)
            {
                return this.mObjPool[i];
            }
        }
        // IE5中不支持push方法
        this.mObjPool[this.mObjPool.length] = this.createRequest();

        return this.mObjPool[this.mObjPool.length - 1];
    },

    createRequest: function () {
        if (window.XMLHttpRequest) {
            var objXMLHttp = new XMLHttpRequest();
        } else {
            var MSXML = ['Microsoft.XMLHTTP','MSXML2.XMLHTTP.5.0', 'MSXML2.XMLHTTP.4.0', 'MSXML2.XMLHTTP.3.0', 'MSXML2.XMLHTTP'];
            for (var n = 0; n < MSXML.length; n++) {
                try {
                    var objXMLHttp = new ActiveXObject(MSXML[n]);
                    break;
                } catch (e) {
                }
            }
        }
        // mozilla某些版本没有readyState属性
        if (objXMLHttp.readyState == null) {
            objXMLHttp.readyState = 0;
            objXMLHttp.addEventListener("load", function () {
                objXMLHttp.readyState = 4;
                if (typeof objXMLHttp.onreadystatechange == "function") {
                    objXMLHttp.onreadystatechange();
                }
            },  false);
        }
        return objXMLHttp;
    },

    post: function (url, params, scuess, error) {
        var fixUrl = url;
        if (fixUrl.indexOf("http") < 0) {
            fixUrl = Http.host + url;
        }
        var xmlHttpReq = Http.getXmlRequest();
        xmlHttpReq.open("POST", fixUrl, true);
        xmlHttpReq.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;");
        var paramStr = "";
        for (var key in params) {
            paramStr += key + "=" + params[key] + "&";
        }
        paramStr = paramStr.substring(0, paramStr.length - 1);

        xmlHttpReq.set
        xmlHttpReq.send(encodeURI(encodeURI(paramStr)));
        xmlHttpReq.onreadystatechange = function () {
            if (xmlHttpReq.readyState == 4) {
                if (xmlHttpReq.status == 200) {
                    console.log("http");
                    console.log(xmlHttpReq.responseText);
                    var obj = JSON.parse(xmlHttpReq.responseText);
                    var ok = obj.resultOk;
                    var el_msg = document.getElementById("message");
                    if (el_msg) {
                        el_msg.innerHTML = obj.message;
                    }
                    if (ok) {
                        scuess && scuess(obj);
                    } else {
                        alert(obj.message);
                        error && error(obj);
                    }
                }
            }
        }

        // $.ajax({
        //     type: "POST",
        //     url: fixUrl,
        //     data: params,
        //     dataType: 'JSON',
        //     success: function (html) {
        //         if(xmlHttpReq.readyState == 4){
        //             if(xmlHttpReq.status == 200){
        //                 console.log(html.responseText);
        //                 scuess && scuess(html.responseText);
        //             }
        //         }
        //     },
        //     error: function (textStatus, errorThrown) {
        //         console.log(textStatus);
        //         console.log(errorThrown);
        //         error && error(html.responseText);
        //     },
        // });
    },


    user:function(user){
        return {
            uid:user.uid,
            uname:user.name
        }
    }
}