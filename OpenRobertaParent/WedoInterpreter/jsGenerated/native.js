(function (factory) {
    if (typeof module === "object" && typeof module.exports === "object") {
        var v = factory(require, exports);
        if (v !== undefined) module.exports = v;
    }
    else if (typeof define === "function" && define.amd) {
        define(["require", "exports", "./state", "./constants", "./util", "./wedo.model", "./webview.controller"], factory);
    }
})(function (require, exports) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var S = require("./state");
    var C = require("./constants");
    var U = require("./util");
    var WEDO = require("./wedo.model");
    var WEBVIEW_C = require("./webview.controller");
    function clearDisplay() {
        U.p('clear display');
        WEBVIEW_C.jsToDisplay({ "clear": true });
    }
    exports.clearDisplay = clearDisplay;
    function driveAction(driveDirection, distance, speed) {
        U.p("drive, dir: " + driveDirection + ", dist: " + distance + ", speed: " + speed);
    }
    exports.driveAction = driveAction;
    function getSample(name, port, sensor, slot) {
        var robotText = 'robot: ' + name + ', port: ' + port;
        U.p(robotText + ' getsample from ' + sensor);
        var sensorName;
        switch (sensor) {
            case "infrared":
                sensorName = "motionsensor";
                break;
            case "gyro":
                sensorName = "tiltsensor";
                break;
            case "buttons":
                sensorName = "button";
                break;
            case C.TIMER:
                S.push(timerGet(port));
                return;
            default:
                throw 'invalid get sample for ' + name + ' - ' + port + ' - ' + sensor + ' - ' + slot;
        }
        var brickid = WEDO.getBrickIdByName(name);
        S.push(WEDO.getSensorValue(brickid, sensorName, port, slot));
    }
    exports.getSample = getSample;
    var timers = {};
    timers['start'] = Date.now();
    function timerReset(port) {
        timers[port] = Date.now();
        U.p('timerReset for ' + port);
    }
    exports.timerReset = timerReset;
    function timerGet(port) {
        var now = Date.now();
        var startTime = timers[port];
        if (startTime === undefined) {
            startTime = timers['start'];
        }
        var delta = now - startTime;
        U.p('timerGet for ' + port + ' returned ' + delta);
        return delta;
    }
    exports.timerGet = timerGet;
    function ledOnAction(name, port, color) {
        var brickid = WEDO.getBrickIdByName(name);
        var robotText = 'robot: ' + name + ', port: ' + port;
        U.p(robotText + ' led on color ' + color);
        var cmd = { 'target': 'wedo', 'type': 'command', 'actuator': 'light', 'brickid': brickid, 'color': color };
        WEBVIEW_C.jsToAppInterface(cmd);
    }
    exports.ledOnAction = ledOnAction;
    function statusLightOffAction(name, port) {
        var brickid = WEDO.getBrickIdByName(name);
        var robotText = 'robot: ' + name + ', port: ' + port;
        U.p(robotText + ' led off');
        var cmd = { 'target': 'wedo', 'type': 'command', 'actuator': 'light', 'brickid': brickid, 'color': 0 };
        WEBVIEW_C.jsToAppInterface(cmd);
    }
    exports.statusLightOffAction = statusLightOffAction;
    function toneAction(name, frequency, duration) {
        var brickid = WEDO.getBrickIdByName(name); // TODO: better style
        var robotText = 'robot: ' + name;
        U.p(robotText + ' piezo: ' + ', frequency: ' + frequency + ', duration: ' + duration);
        var cmd = { 'target': 'wedo', 'type': 'command', 'actuator': 'piezo', 'brickid': brickid, 'frequency': frequency, 'duration': duration };
        WEBVIEW_C.jsToAppInterface(cmd);
    }
    exports.toneAction = toneAction;
    function motorOnAction(name, port, duration, speed) {
        var brickid = WEDO.getBrickIdByName(name); // TODO: better style
        var robotText = 'robot: ' + name + ', port: ' + port;
        var durText = duration === -1 ? ' w.o. duration' : (' for ' + duration + ' msec');
        U.p(robotText + ' motor speed ' + speed + durText);
        var cmd = { 'target': 'wedo', 'type': 'command', 'actuator': 'motor', 'brickid': brickid, 'action': 'on', 'id': port, 'direction': speed < 0 ? 1 : 0, 'power': Math.abs(speed) };
        WEBVIEW_C.jsToAppInterface(cmd);
    }
    exports.motorOnAction = motorOnAction;
    function motorStopAction(name, port) {
        var brickid = WEDO.getBrickIdByName(name); // TODO: better style
        var robotText = 'robot: ' + name + ', port: ' + port;
        U.p(robotText + ' motor stop');
        var cmd = { 'target': 'wedo', 'type': 'command', 'actuator': 'motor', 'brickid': brickid, 'action': 'stop', 'id': port };
        WEBVIEW_C.jsToAppInterface(cmd);
    }
    exports.motorStopAction = motorStopAction;
    function showTextAction(text) {
        var showText = "" + text;
        U.p('***** show "' + showText + '" *****');
        WEBVIEW_C.jsToDisplay({ "show": text });
    }
    exports.showTextAction = showTextAction;
    function close() {
        var ids = WEDO.getConnectedBricks();
        for (var id in ids) {
            if (ids.hasOwnProperty(id)) {
                var name = WEDO.getBrickById(ids[id]).brickname;
                motorStopAction(name, 1);
                motorStopAction(name, 2);
                ledOnAction(name, 99, 3);
            }
        }
    }
    exports.close = close;
});
