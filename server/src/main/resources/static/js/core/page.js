/**
 * Created by Administrator on 2017/4/7.
 */
$(document).ready(function () {
});

function setBackground(src) {
    document.getElementById("bg").background = src;
}

function launchHome(user) {
    launchPage({
        html:"home.html",
        data:user
    });
}

function launchIndex() {
    location.reload();
}

function launchIFrame(src,id="iframe") {
    document.getElementById(id).src=src;
}

function loadPage(page,id) {
    if(page.body){
        $(id).html(page.body);
        if(page.onCreate){
            page.onCreate();
        }
    }else{
        $.ajax({
            async:true,
            url : page.html,
            success : function(result){
                page.body = result;
                if(page.body){
                    loadPage(page,id);
                }
            }
        });
    }
}

function setWindowTitle(title) {
    document.getElementById("window-title").innerHTML=title;
}

var papes = new Array;
var page = null;
function launchHtml(html) {
    launchPage({
        html:html
    })
}

function launchPage(Page) {
    if(page&&page.onDestory){
        page.onDestory();
    }

    papes.push(Page);
    page = Page;
    loadPage(Page,"#root");
}

function back() {
    console.log("back");
    if(page!=null){
        papes.pop();
        var top = papes.pop();
        if(top){
            console.log(top);
            launchPage(top);
        }else{
            launchIndex();
        }
    }else{
        history.go(-1);
    }
}

function setUid(uid){
    setCookie("uid",uid);
}

function setCookie(name,val,day) {
    delCookie(name)
    var Days = day;
    var exp = new Date();
    exp.setTime(exp.getTime() + Days*24*60*60*1000);
    document.cookie = name + "="+ escape (value) + ";expires=" + exp.toGMTString();
}

function setCookie(name,value)
{
    delCookie(name)
    document.cookie = name + "="+ escape (value);
}

function getCookie(name)
{
    var arr = document.cookie.match(new RegExp("(^|)"+name+"=([^;]*)(;|$)"));
    if(arr != null){
        return arr[2];
    }
    return null;
}

function delCookie(name)
{
    var name = escape(name);
    var exp = new Date();
    exp.setTime(exp.getTime() - 1);
    document.cookie=name+"="+";expires="+exp.toGMTString;
}

function loadXML(xmlFile){
    var xmlDoc=null;
    //判断浏览器的类型
    //支持IE浏览器
    if(!window.DOMParser && window.ActiveXObject){
        var xmlDomVersions = ['MSXML.2.DOMDocument.6.0','MSXML.2.DOMDocument.3.0','Microsoft.XMLDOM'];
        for(var i=0;i<xmlDomVersions.length;i++){
            try{
                xmlDoc = new ActiveXObject(xmlDomVersions[i]);
                break;
            }catch(e){
            }
        }
    }
    //支持Mozilla浏览器
    else if(document.implementation && document.implementation.createDocument){
        try{
            /* document.implementation.createDocument('','',null); 方法的三个参数说明
             * 第一个参数是包含文档所使用的命名空间URI的字符串；
             * 第二个参数是包含文档根元素名称的字符串；
             * 第三个参数是要创建的文档类型（也称为doctype）
             */
            xmlDoc = document.implementation.createDocument('','',null);
        }catch(e){
        }
    }
    else{
        return null;
    }

    if(xmlDoc!=null){
        xmlDoc.async = false;
        xmlDoc.load(xmlFile);
    }
    return xmlDoc;
}

function setData(data) {
    page.data = data;
}

function getData() {
    return page.data;
}

function getUid() {
    return getCookie("uid");
}

function list(values,adapter) {
    for (var i=0;i<values.length;i++){
        adapter && adapter(values[i],i);
    }
}

var Page = {
    html:"error.html",
    data:{},
    body:null,
    onCreate:null,
    onDestory:null,
    title:function (text) {
        $("#title").html(html_title);
        document.getElementById("id_text").innerHTML=text;
    },
    back:function () {
        console.log("page back");
        back();
    },
    refresh:function () {
        document.getElementById("id_text").innerHTML="refresh..";
        location.reload();
    },
};

