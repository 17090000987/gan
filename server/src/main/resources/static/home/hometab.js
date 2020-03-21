var Tab = {
    index:0,
    current:null,
    tabAdapter:function (tab,i) {
        var $tab = "tab"+i;
        if(tab.adapter){
            tab.adapter(tab,i);
        }else{
            var li = "<li><a id='"+$tab+"' class='nav-item nav-link' href=\"#\">"+tab.name+"</a><li>";
            $("#tab").append(li);
        }
        document.getElementById($tab).onclick= function () {
            if(tab.onclick){
                tab.onclick(tab,i);
            }else{
                Tab.onTabClick(tab,i);
            }
        }
    },
    onTabClick:function (tab,i) {
        if(Tab.current){
            if(Tab.current.page){
                if(Tab.current.page.onDestory){
                    Tab.current.page.onDestory();
                }
            }
        }

        var $tab = "tab"+i;
        document.getElementById($tab).focus();
        Tab.index = i;
        if(!tab.page){
            tab.page = {
                html:tab.html,
            }
        }
        Tab.current = tab;
        loadPage(tab.page,"#content")
    },
    select:function (i) {
        if(!i){
            i = Tab.index;
        }
        var $tab = "tab"+i;
        document.getElementById($tab).focus();
        Tab.index = i;
    }
}

page.onCreate=function () {
    console.log("hometab onCreate")

    var tabs = new Array();
    var firstTab = {
        name:"Gan",
        html:"home/home.html",
    };
    tabs.push(firstTab);

    tabs.push({
        name:"在线",
        html:"media/online/index.html",
    });
    
    tabs.push({
        name:"文档",
        html:"https://github.com/tablife/gan/blob/master/README.md",
        onclick:function () {
            window.open("https://github.com/tablife/gan/blob/master/README.md");
            Tab.select();
        }
    });
    tabs.push({
        name:"演示",
        html:"media/websocket/demo.html",
    });

    tabs.push({
        name:"h265",
        html:"h265/test.html",
        onclick:function () {
            window.open("h265/test.html");
            Tab.select();
        }
    });

    list(tabs,Tab.tabAdapter);
    Tab.onTabClick(firstTab,0);
}

page.onDestory=function () {
 Tab=null;
}