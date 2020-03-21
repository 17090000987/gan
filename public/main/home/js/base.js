var gLoginResult = "0";

function timestampToDate(timestamp) {
    var date = new Date(timestamp);
    Y = date.getFullYear() + '-';
    M = (date.getMonth()+1 < 10 ? '0'+(date.getMonth()+1) : date.getMonth()+1) + '-';
    D = date.getDate() + ' ';
    h = date.getHours() + ':';
    m = date.getMinutes() + ':';
    s = date.getSeconds();
    return Y+M+D+h+m+s;
}

function dumpObj(obj){
	var names="";
	for(var name in obj){
		names+=name+": "+obj[name]+", ";
	}
	return names;
}

function isEmpty(obj){
	if(typeof obj == "undefined" || obj == null || obj == ""){
		return true;
	}else{
		return false;
	}
}

function asyncHttp(url, params, callback){
	var xmlhttp;
	if(window.XMLHttpRequest){
		xmlhttp = new XMLHttpRequest();
	}else{
		xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
	}
	xmlhttp.onreadystatechange = function(){
		callback(xmlhttp);
	};
	xmlhttp.open("post", url, true);
	xmlhttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
	xmlhttp.send(params); 
}


function isMobileDevice(){
	var userAgent = navigator.userAgent;
	var agents = ["Android","iPhone","iPad","iPod"];
	var flag = false;
	for(var v = 0; v < agents.length; v++){
		if(userAgent.indexOf(agents[v]) > 0){
			flag = true;
			break;
		}
	}
	return flag;
}


function cssAutoFit(){
	var head = document.getElementsByTagName('head')[0];
	var link = document.createElement('link');
	link.type='text/css';
	link.rel = 'stylesheet';
	link.href = isMobileDevice()?"/css/mobile.css":"/css/base.css";
	head.appendChild(link);
}


function getCookie(c_name)
{
	if (document.cookie.length>0)
	{ 
		c_start=document.cookie.indexOf(c_name + "=")
		if (c_start!=-1)
		{ 
			c_start=c_start + c_name.length+1 
			c_end=document.cookie.indexOf(";",c_start)
			if (c_end==-1) c_end=document.cookie.length
				return unescape(document.cookie.substring(c_start,c_end))
		} 
	}
	return ""
}

function setCookie(c_name,value,expiredays)
{
	var exdate=new Date()
	exdate.setDate(exdate.getDate()+expiredays)
	document.cookie=c_name+ "=" +escape(value)+
	((expiredays==null) ? "" : "; expires="+exdate.toGMTString())
}

function checkCookie()
{
	username=getCookie('username')
	if (username!=null && username!="")
		{alert('Welcome again '+username+'!')}
	else 
	{
		username=prompt('Please enter your name:',"")
		if (username!=null && username!="")
		{
			setCookie('username',username,365)
		}
	}
}

function getHost(){
	return window.location.host;
}

function getHostWithoutPort(){
	var host = window.location.host;
	return host.substring(0,host.indexOf(":"));
}
