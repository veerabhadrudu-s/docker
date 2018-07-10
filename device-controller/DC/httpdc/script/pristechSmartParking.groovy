/**
 * 
 */
package com.hpe.iot.http.pristech.parking.v1

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hpe.iot.dc.model.DeviceModel
import com.hpe.iot.http.southbound.service.holder.GroovyServicesHolder
import com.hpe.iot.model.DeviceInfo
import com.hpe.iot.northbound.handler.outflow.DownlinkPayloadProcessor
import com.hpe.iot.southbound.handler.inflow.DeviceIdExtractor
import com.hpe.iot.southbound.handler.inflow.MessageTypeExtractor
import com.hpe.iot.southbound.handler.inflow.PayloadDecipher

/**
 * @author sveera
 *
 */
class PristechSmartParkingDeviceModel implements DeviceModel {

	@Override
	public String getManufacturer() {
		return "pristech";
	}

	@Override
	public String getModelId() {
		return "smartparking";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}
}

class PristechSmartParkingUplinkHandler implements PayloadDecipher,DeviceIdExtractor,MessageTypeExtractor,DownlinkPayloadProcessor{

	private final String ppParkDownlinkEndpoint = "https://pparke.in/Parking/instance/pparke_india/iot/testpush";
	private final JsonParser jsonParser=new JsonParser();
	private final GroovyServicesHolder groovyServicesHolder;

	public PristechSmartParkingUplinkHandler(GroovyServicesHolder groovyServicesHolder) {
		super();
		this.groovyServicesHolder = groovyServicesHolder;
	}

	@Override
	public JsonObject decipherPayload(DeviceModel deviceModel, byte[] payload) {
		return jsonParser.parse(new String(payload));
	}

	@Override
	public String extractDeviceId(DeviceModel deviceModel, JsonObject payload) {
		return  payload.get("sensor_id").getAsString();
	}

	@Override
	public String extractMessageType(DeviceModel deviceModel, JsonObject payload) {
		return  payload.get("event_str").getAsString();
	}

	@Override
	public void processPayload(DeviceModel deviceModel, DeviceInfo decipheredPayload) {
		groovyServicesHolder.getHttpClientUtility().
				postRequestOnHttps(ppParkDownlinkEndpoint, new HashMap<>(), decipheredPayload.getPayload().toString());
	}
}
