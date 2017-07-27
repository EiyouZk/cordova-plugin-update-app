
```javascript
// 获取APP当前版本号
window.plugins.updateApp.getCurrentVerInfo(function (currentVersionCode) {
    console.log(currentVersionCode);
});

// 获取服务器上APP的版本号，versionServer为版本信息文件地址
window.plugins.updateApp.getServerVerInfo(function (serverVersionCode) {
    console.log(serverVersionCode);
}, function () {
    console.log("出现异常");
}, versionServer);

// 检查并更新，versionServer为版本信息文件地址
window.plugins.updateApp.checkAndUpdate(versionServer);

// 获取APP当前版本号
// 与getCurrentVerInfo方法不同之处在于android下getCurrentVerInfo返回的是versionCode
// 该方法返回的是versionName
window.plugins.updateApp.getAppVersion(function (version) {
    console.log(version);
});
```
Android:

http://192.168.1.7/androidVersion.json
androidVersion.json:
=========
`[{"verCode":"最新版apk的versionCode","verName":"最新版apk的versionName","apkPath":"apk的地址"}]`

iOS:

https://cugcqh.github.io/iosVersion.json

iosVersion.json:
`{"verName":"最新版ipa的版本号","ipaPath":"plist的地址或者app的itunes地址"}`

plist的地址
"itms-services://?action=download-manifest&amp;url=https://cugcqh.github.io/plist.xml"

plist.xml参考https://cugcqh.github.io/plist.xml