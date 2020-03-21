/**
 * Created by Administrator on 2017/4/8.
 */
var User = {
    login: function (account,pwd) {
        var login = ({
            success : function (obj) {
                $("#message").html("<div/>登陆成功</div>");
                console.log("登陆成功");
                var user = obj.data;
                console.log(user);
                setCookie("uid",user.uid);
                launchHome(user);
            }
        });

        var username = $("#username").val(), password = $("#password").val();

        if(account != null
            ||pwd != null){
            username = account;
            password = pwd;
        }

        if ((username === "") || (password === "")) {
            alert("请输入用户名和密码");
            $("#message").html("<div/>请输入用户名和密码</div>");
        } else {
            $("#message").html("<p class='text-center'><img src='images/loader_1.gif'>登陆...</p>");
            Http.post("/user/login",{
                account:username,
                pwd:password
            }, login.success);
        }
    },
    get:function (uid,success) {
        Http.post("/user/get",{
            uid:uid
        },success);
    },
    register:function () {
        var register = ({
            success : function (obj) {
                $("#message").html("<div/>注册成功</div>");
                User.login(account,pwd);
            }
        });

        console.log("register");
        var account = $("#account").val(), pwd = $("#pwd").val(), pwd2 = $("#pwd2").val();
        if ((account === "") || (pwd === "")) {
            alert("请输入用户名和密码");
            $("#message").html("<div/>请输入用户名和密码</div>");
        } else if(pwd!==pwd2){
            alert("确认密码不一致");
            $("#message").html("<div/>确认密码不一致</div>");
        } else {
            $("#message").html("<p class='text-center'><img src='images/loader_1.gif'>提交...</p>");
            Http.post("/user/register",{
                account:account,
                pwd:pwd,
            },register.success);
        }
    },
    list:function (offset) {
        var list = ({
            success : function (obj) {
                var users = obj.list;
                Page.list(users, User.adapter);
            },
        });
        Http.post("/user/list",{
            offset:offset
        },list.success);
    },
    adapter:function (user) {
        var image = "<img class='square_image_50px' src='"+user.avatar+"'>";
        var name = "<span class='text_gray' style='margin-left: 10px;position: absolute'>"+user.name+"</span>";
        var html = "<div class='list_item' onclick='User.detail()'>"+image+name+"</div>";
        $("#list_user").append(html);
    },
    detail:function (id,user) {

    }
};