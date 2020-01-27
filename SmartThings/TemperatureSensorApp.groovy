/**
 *  TemperatureSensorApp
 *
 *  Author: Victor Santana
 *  Date: 2018-02-18
 */

definition(
    name: "Temperature Sensor SmartApp",
    namespace: "Temperature Sensor",
    author: "Victor Santana",
    description: "Temperature Sensor",
    category: "My Apps",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn?displaySize=2x",
    iconX3Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn?displaySize=3x",
    singleInstance: true
)

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

preferences {
	page(name: "page1")
}

def page1() {
  dynamicPage(name: "page1", install: true, uninstall: true) {
    section("SmartThings Hub") {
      input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
    }
    section("SmartThings Raspberry") {
      input "proxyAddress", "text", title: "Proxy Address", description: "(ie. 192.168.1.10)", required: true
      input "proxyPort", "text", title: "Proxy Port", description: "(ie. 3001)", required: true, defaultValue: "3001"
    }
		section("Enable Debug Log at SmartThing IDE"){
			input "idelog", "bool", title: "Select True or False:", defaultValue: false, required: false
		}     
  }
}

def installed() {
  writeLog("TemperatureSensorSmartApp - TemperatureSensor Installed with settings: ${settings}")
	initialize()
  addTemperatureSensorDeviceType()
}

def updated() {
  writeLog("TemperatureSensorSmartApp - Updated with settings: ${settings}")
	//unsubscribe()
	initialize()
  sendCommand('/subscribe/'+getNotifyAddress())
}

def initialize() {
    subscribe(location, null, lanResponseHandler, [filterEvents:false])
    writeLog("TemperatureSensorSmartApp - Initialize")
}

def uninstalled() {
    removeChildDevices()
}

private removeChildDevices() {
    getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
    writeLog("TemperatureSensorSmartApp - Removing all child devices")
}

def lanResponseHandler(evt) {
    def map = stringToMap(evt.stringValue)

    //verify that this message is from STNP IP:Port
    //IP and Port are only set on HTTP GET response and we need the MAC
    if (map.ip == convertIPtoHex(settings.proxyAddress) &&
        map.port == convertPortToHex(settings.proxyPort)) {
            if (map.mac) {
            state.proxyMac = map.mac
        }
    }

    //verify that this message is from STNP MAC
    //MAC is set on both HTTP GET response and NOTIFY
    if (map.mac != state.proxyMac) {
        //return
    }

    def headers = getHttpHeaders(map.headers);
    def body = getHttpBody(map.body);

    //verify that this message is for this plugin
    //if (headers.'stnp-plugin' != settings.pluginType) {
        //return
    //}

    if (headers.'device' != 'temperaturesensor') {
      writeLog("TemperatureSensorSmartApp - Received event ${evt.stringValue} but it didn't came from TemperatureSensor")
      writeLog("TemperatureSensorSmartApp - Received event but it didn't came from TemperatureSensor headers:  ${headers}")
      writeLog("TemperatureSensorSmartApp - Received event but it didn't came from TemperatureSensor body: ${body}")      
      return
    }

    //log.trace "Honeywell Security event: ${evt.stringValue}"
    writeLog("TemperatureSensorSmartApp - Received event headers:  ${headers}")
    writeLog("TemperatureSensorSmartApp - Received event body: ${body}")
    updateTemperatureSensorceDeviceType(body.command)
}

private updateTemperatureSensorceDeviceType(String cmd) {
	def TemperatureSensorNetworkID = "TemperatureSensor"
  def TemperatureSensordevice = getChildDevice(TemperatureSensorNetworkID)
  if (TemperatureSensordevice) {
    TemperatureSensordevice.TemperatureSensorparse("${cmd}")
    writeLog("TemperatureSensorSmartApp - Updating TemperatureSensor Device ${TemperatureSensorNetworkID} using Command: ${cmd}")
  }
}

private addTemperatureSensorDeviceType() {
  def deviceId = 'TemperatureSensor'
  if (!getChildDevice(deviceId)) {
    addChildDevice("TemperatureSensor", "Temperature Sensor", deviceId, hostHub.id, ["name": "TemperatureSensor", label: "TemperatureSensor", completedSetup: true])
    writeLog("TemperatureSensorSmartApp - Added TemperatureSensorDeviceType device: ${deviceId}")
  }
}

private getProxyAddress() {
  return settings.proxyAddress + ":" + settings.proxyPort
}

private getNotifyAddress() {
  return settings.hostHub.localIP + ":" + settings.hostHub.localSrvPortTCP
}

private sendCommand(path) {
  if (settings.proxyAddress.length() == 0 ||
    settings.proxyPort.length() == 0) {
    log.error "SmartThings Node Proxy configuration not set!"
    return
  }

  def host = getProxyAddress()
  def headers = [:]
  headers.put("HOST", host)
  headers.put("Content-Type", "application/json")
  headers.put("stnp-auth", settings.authCode)

  def hubAction = new physicalgraph.device.HubAction(
      method: "GET",
      path: path,
      headers: headers
  )
  sendHubCommand(hubAction)
}

private getHttpHeaders(headers) {
  def obj = [:]
  new String(headers.decodeBase64()).split("\r\n").each {param ->
    def nameAndValue = param.split(":")
    obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
  }
  return obj
}

private getHttpBody(body) {
  def obj = null;
  if (body) {
    def slurper = new JsonSlurper()
    obj = slurper.parseText(new String(body.decodeBase64()))
  }
  return obj
}

private String convertIPtoHex(ipAddress) {
  return ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join().toUpperCase()
}

private String convertPortToHex(port) {
  return port.toString().format( '%04x', port.toInteger() ).toUpperCase()
}

private writeLog(message)
{
  if(idelog){
    log.debug "${message}"
  }
}