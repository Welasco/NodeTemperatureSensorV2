/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

preferences {
    input("hostaddress", "text", title: "IP Address for Server:", description: "Ex: 10.0.0.12 or 192.168.0.4 (no http://)")
    input("hostport", "number", title: "Port of Server", description: "port")
}

metadata {
	definition (name: "Temperature Sensorv2", namespace: "TemperatureSensorv2", author: "SmartThings") {
		capability "Temperature Measurement"
		capability "Relative Humidity Measurement"
		capability "Sensor"
		capability "Refresh"
	}

	// UI tile definitions
	tiles {
		valueTile("temperature", "device.temperature", width: 2, height: 2) {
			state("temperature", label:'${currentValue}°',
				backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				],
				icon:"st.Weather.weather2"
			)
		}
		valueTile("humidity", "device.humidity") {
			state "humidity", label:'${currentValue}%', unit:""
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, width: 1, height: 1, canChangeIcon: false, canChangeBackground: false) {
				state "default", action:"refresh", icon:"st.secondary.refresh"
		}
		main(["temperature", "humidity", "refresh"])
		details(["temperature", "humidity", "refresh"])
	}
}

def TemperatureSensorparse(String description){
	//log.debug "TemperatureSensor DeviceType - Description: ${description}"
	def temphumid = description.split("-")
	def temperature = temphumid[0]
	def humidity = temphumid[1]
	//log.debug "TemperatureSensor DeviceType - temperature: ${temperature} humidity: ${humidity}"
    sendEvent(name: "temperature", value: temperature, unit: getTemperatureScale())
    sendEvent(name: "humidity", value: humidity, unit: "%")
}

def refresh(){
    parent.writeLog("TemperatureSensor Device Type - Sending command")
    sendRaspberryCommand("temperaturesensor")
}

def updated() {
	//def path = "/config/"+settings.deviceID
	def path = "/config/"+device.deviceNetworkId
	def endpoint = settings.hostaddress + ":" + settings.hostport
	parent.writeLog("TemperatureSensor Device Type - deviceID is: "+path)
    parent.sendCommand(path,endpoint);
	def subscribepath = '/subscribe/'+parent.getNotifyAddress() 
	parent.sendCommand(subscribepath,endpoint)
	parent.writeLog("TemperatureSensor Device Type - subscribe is: "+subscribepath)
}

def sendRaspberryCommand(String command) {
	def path = "/api/$command"
	def endpoint = settings.hostaddress + ":" + settings.hostport
    parent.sendCommand(path,endpoint);
}

// def setdevicesettings(String proxyAddress, String proxyPort){
// 	settings.hostaddress = proxyAddress
// 	settings.hostport = proxyPort
// 	parent.writeLog("TemperatureSensor Device Type - received proxyAddress $proxyAddress")
// 	parent.writeLog("TemperatureSensor Device Type - received proxyPort $proxyPort")
// 	parent.writeLog("TemperatureSensor Device Type - defined setting hostaddress " + settings.hostaddress)
// 	parent.writeLog("TemperatureSensor Device Type - defined setting hostaport " + settings.hostport)
// }

// Parse incoming device messages to generate events
/*
def parse(String description) {
	def name = parseName(description)
	def value = parseValue(description)
	def unit = name == "temperature" ? getTemperatureScale() : (name == "humidity" ? "%" : null)
	def result = createEvent(name: name, value: value, unit: unit)
	log.debug "Parse returned ${result?.descriptionText}"
	return result
}

private String parseName(String description) {
	if (description?.startsWith("temperature: ")) {
		return "temperature"
	} else if (description?.startsWith("humidity: ")) {
		return "humidity"
	}
	null
}

private String parseValue(String description) {
	if (description?.startsWith("temperature: ")) {
		return zigbee.parseHATemperatureValue(description, "temperature: ", getTemperatureScale())
	} else if (description?.startsWith("humidity: ")) {
		def pct = (description - "humidity: " - "%").trim()
		if (pct.isNumber()) {
			return Math.round(new BigDecimal(pct)).toString()
		}
	}
	null
}
*/
