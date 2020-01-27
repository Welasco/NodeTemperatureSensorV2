#!/usr/bin/env node
// Loading Dependencies
var http = require("http");
var https = require('https');
var express = require("express");
var app = express();
var nconf = require('nconf');
nconf.file({ file: './config.json' });
fs = require('fs');
var sensor = require('node-dht-sensor');


////////////////////////////////////////
// Logger Function
////////////////////////////////////////
var logger = function(mod,str) {
    console.log("[%s] [%s] %s", new Date().toISOString(), mod, str);
}

logger("Modules","Modules loaded");
var httpport = nconf.get('httpport');

//////////////////////////////////////////////////////////////////
// Creating Endpoints
// Those Endpoints will receive a HTTP GET Request
// Execute the associated Method to make the following:
//  "/" - Used to check if the TemperatureSensor is running
//  "/api/temperaturesensor" - Used to refresh the current temperature at SmartThings TemperatureSensor
//////////////////////////////////////////////////////////////////

// Used only to check if NodeJS is running
app.get("/", function (req, res) {
    sensor.read(11, 21, function(err, temperature, humidity) {
        if (!err) {

            var vartemperatureF = (temperature.toFixed(1) * 9 / 5 + 32 )
            var vartemperatureC = (temperature.toFixed(1))
            var varhumidity = humidity.toFixed(1)

            logger("Reading Temperature Loop","Temperature in Fahrenheit: " + vartemperatureF + ' °F');
            logger("Reading Temperature Loop","Temperature in Celsius: " + vartemperatureC + ' °C');
            logger("Reading Temperature Loop","Humidity: " + varhumidity + ' %');
            var temphumi = (vartemperatureF + "-" +varhumidity) 
            res.send("<html><body><h1>TemperatureSensor</h1><br>Temperature in Fahrenheit: " + vartemperatureF + "<br>Temperature in Celsius: " + vartemperatureC +"<br>Humidity: " + varhumidity + "</body></html>");
        }
        else{
            logger("Reading Temperature Loop","Error: " + err);
            res.send("<html><body><h1>TemperatureSensor ON</h1></body></html>");
        }
    });        
    //res.send("<html><body><h1>TemperatureSensor ON</h1></body></html>");
});

app.get("/api/temperaturesensor", function (req, res) {
    collecttemperature();
    logger("HTTP","Request at /api/temperaturesensor");
    //res.send("200 OK");
    res.end();
});

/**
 * Subscribe route used by SmartThings Hub to register for callback/notifications and write to config.json
 * @param {String} host - The SmartThings Hub IP address and port number
 */
app.get('/subscribe/:host', function (req, res) {
    var parts = req.params.host.split(":");
    nconf.set('notify:address', parts[0]);
    nconf.set('notify:port', parts[1]);
    nconf.save(function (err) {
      if (err) {
        logger("Subscribe",'Configuration error: '+err.message);
        res.status(500).json({ error: 'Configuration error: '+err.message });
        return;
      }
    });
    res.end();
    logger("Subscribe","SmartThings HUB IpAddress: "+parts[0] +" Port: "+ parts[1]);
});

logger("HTTP Endpoint","All HTTP endpoints loaded");

////////////////////////////////////////
// Creating Server
////////////////////////////////////////
var server = http.createServer(app);
server.listen(httpport);
logger("HTTP Endpoint","HTTP Server Created at port: "+httpport);

///////////////////////////////////////////
// Function to send TemperatureSensor msgs to SmartThing
///////////////////////////////////////////
function sendSmartThingMsg(command) {
    var msg = JSON.stringify({command: command});
    notify(msg);
    logger("SendMartthingsMsg","Sending SmartThing comand: " + msg);
}

///////////////////////////////////////////
// Send HTTP callback to SmartThings HUB
///////////////////////////////////////////
/**
 * Callback to the SmartThings Hub via HTTP NOTIFY
 * @param {String} data - The HTTP message body
 */
var notify = function(data) {
    if (!nconf.get('notify:address') || nconf.get('notify:address').length == 0 ||
      !nconf.get('notify:port') || nconf.get('notify:port') == 0) {
      logger("Notify","Notify server address and port not set!");
      return;
    }
  
    var opts = {
      method: 'NOTIFY',
      host: nconf.get('notify:address'),
      port: nconf.get('notify:port'),
      path: '/notify',
      headers: {
        'CONTENT-TYPE': 'application/json',
        'CONTENT-LENGTH': Buffer.byteLength(data),
        'device': 'temperaturesensor'
      }
    };
  
    var req = http.request(opts);
    req.on('error', function(err, req, res) {
      logger("Notify","Notify error: "+err);
    });
    req.write(data);
    req.end();
}

function collecttemperature(){
    
    sensor.read(11, 21, function(err, temperature, humidity) {
        if (!err) {

            var vartemperatureF = (temperature.toFixed(1) * 9 / 5 + 32 )
            var vartemperatureC = (temperature.toFixed(1))
            var varhumidity = humidity.toFixed(1)

            logger("Reading Temperature Loop","Temperature in Fahrenheit: " + vartemperatureF + ' °F');
            logger("Reading Temperature Loop","Temperature in Celsius: " + vartemperatureC + ' °C');
            logger("Reading Temperature Loop","Humidity: " + varhumidity + ' %');
            var temphumi = (vartemperatureF.toString() + "-" + varhumidity.toString()) 
            sendSmartThingMsg(temphumi);
        }
        else{
            logger("Reading Temperature Loop","Error: " + err);
        }
    });    
    

};


function celsiusToFahrenheit(celsius)
{
  var cTemp = celsius;
  var cToFahr = cTemp * 9 / 5 + 32;
  var message = cTemp+'\xB0C is ' + cToFahr + ' \xB0F.';
  return cToFahr;
  //  console.log(message);
}

function fahrenheitToCelsius(fahrenheit)
{
  var fTemp = fahrenheit;
  var fToCel = (fTemp - 32) * 5 / 9;
  var message = fTemp+'\xB0F is ' + fToCel + '\xB0C.';
  //  console.log(message);
}


interval = setInterval(function () { //#C
    collecttemperature()
}, 900000);

// 5 seconds is 5000
// 15 minutes is 900000