/**
 * 
 */
package com.hpe.iot.http.libelium.smartagriculture

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hpe.iot.dc.model.DeviceModel
import com.hpe.iot.southbound.handler.inflow.DeviceIdExtractor
import com.hpe.iot.southbound.handler.inflow.MessageTypeExtractor
import com.hpe.iot.southbound.handler.inflow.PayloadDecipher

/**
 * @author sveera
 *
 */
public class LibeliumSmartAgricultureProModel implements DeviceModel,MessageTypeExtractor,DeviceIdExtractor {

	@Override
	public String getManufacturer() {
		return "libelium";
	}

	@Override
	public String getModelId() {
		return "smartagricluturepro";
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
		return payload.get("EUI").getAsString();
	}
}

public class LibeliumSmartAgricultureProDecipher implements PayloadDecipher{

	private final JsonParser jsonParser = new JsonParser();
	private final Logger logger=LoggerFactory.getLogger(getClass());

	@Override
	public JsonObject decipherPayload(DeviceModel deviceModel, byte[] payloadBytes) {
		JsonObject payload =(JsonObject) jsonParser.parse(new String(payloadBytes));
		String asciiPayload=hexToAscii(payload.get("data").getAsString());
		String[] readings=asciiPayload.split("#");
		Map<String,String> decodedData=new HashMap<>();
		for(String reading:readings) {
			String[] readingValues=reading.split(":");
			decodedData.put(readingValues[0], readingValues[1]);
		}
		payload.add("data",jsonParser.parse(new Gson().toJson(decodedData)));
		logger.debug("Decoded payload for device model "+deviceModel+" is "+payload.toString());
		return payload;
	}

	private  String hexToAscii(String hexStr) {
		StringBuilder output = new StringBuilder("");
		for (int i = 0; i < hexStr.length(); i += 2) {
			String str = hexStr.substring(i, i + 2);
			output.append((char) Integer.parseInt(str, 16));
		}
		return output.toString();
	}
}

