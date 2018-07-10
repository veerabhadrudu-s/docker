/**
 * 
 */
package com.hpe.iot.http.steamcode.metering

import com.google.gson.JsonObject
import com.hpe.iot.dc.model.DeviceModel
import com.hpe.iot.southbound.handler.inflow.DeviceIdExtractor
import com.hpe.iot.southbound.handler.inflow.MessageTypeExtractor

/**
 * @author sveera
 *
 */
public class SteamCodeDeviceModel implements DeviceModel,MessageTypeExtractor,DeviceIdExtractor{

	@Override
	public String getManufacturer(){
		return "streamcode"
	}

	@Override
	public String getModelId(){
		return "metering"
	}

	@Override
	public String getVersion() {
		return "1.0"
	}

	@Override
	public String extractMessageType(DeviceModel deviceModel, JsonObject payload) {
		return "notification";
	}

	@Override
	public String extractDeviceId(DeviceModel deviceModel, JsonObject payload) {
		return payload.entrySet().iterator().next().getKey();
	}
}
