/**
 * 检查并更新APP
 * 
 */
function UpdateApp() {}
UpdateApp.prototype.checkAndUpdate = function(successCallback,failureCallback,checkPath) {
	cordova.exec(successCallback, failureCallback, "UpdateApp", "checkAndUpdate", [checkPath]);
}
UpdateApp.prototype.getCurrentVerInfo = function(successCallback) {
	cordova.exec(successCallback, null, "UpdateApp", "getCurrentVersion", []);
}
UpdateApp.prototype.getServerVerInfo = function(successCallback, failureCallback, checkPath) {
	cordova.exec(successCallback, failureCallback, "UpdateApp", "getServerVersion", [checkPath]);
}
UpdateApp.prototype.getAppVersion = function(successCallback, failureCallback) {
	cordova.exec(successCallback, failureCallback, "UpdateApp", "getVersionName", []);
}
cordova.addConstructor(function() {
	if (!window.plugins) {
		window.plugins = {};
	}
	window.plugins.updateApp = new UpdateApp();
});