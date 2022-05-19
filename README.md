# com-plugin-talkback
 对讲插件
 登录
 
 login() {
   Talkback.login(ent_id,user_id,'password','server', message=>alert(message), error=>alert(error))
 }
 
 enterChannel(){
   Talkback.enterChannel(channelId, message=>alert(message), error=>alert(error))
 }
 
 joinChannel(){
   Talkback.joinChannel(channelId, 'password', '插件开发', message=>alert(message), error=>alert(error))
 }
 
 quitChannel(){
   Talkback.quitChannel(channelId, message=>alert(message), error=>alert(error))
 }
 
 channelList(){
   Talkback.channelList('', message=>alert(message), error=>alert(error))
 }
 
 userList(){
   Talkback.userList('', message=>alert(message), error=>alert(error))
 }
 
 pttKeyDown(){
   Talkback.pttKeyDown('', message=>alert(message), error=>alert(error))
 }
 
 pttKeyUp(){
   Talkback.pttKeyUp('', message=>alert(message), error=>alert(error))
 }
 
 cancelSelect(){
   Talkback.cancelSelect('', message=>alert(message), error=>alert(error))
 }
