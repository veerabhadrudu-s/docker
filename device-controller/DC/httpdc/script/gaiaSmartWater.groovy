/**
 * 
 */
package com.hpe.iot.http.gaia.smartwater.v1

import static com.handson.iot.dc.util.DataParserUtility.calculateUnsignedDecimalValFromSignedBytes
import static com.handson.iot.dc.util.UtilityLogger.logRawDataInDecimalFormat
import static com.handson.iot.dc.util.UtilityLogger.logRawDataInHexaDecimalFormat

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.google.gson.JsonObject
import com.hpe.iot.dc.model.DeviceModel
import com.hpe.iot.southbound.handler.inflow.DeviceIdExtractor
import com.hpe.iot.southbound.handler.inflow.PayloadDecipher

/**
 * @author sveera
 *
 */
public class GaiaSmartWaterModel implements DeviceModel,PayloadDecipher,DeviceIdExtractor {

	private final Logger logger=LoggerFactory.getLogger(this.getClass());

	@Override
	public String getManufacturer() {
		return "gaia";
	}

	@Override
	public String getModelId() {
		return "smartwater";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public JsonObject decipherPayload(DeviceModel deviceModel, byte[] payload) {
		JsonObject jsonObject=new JsonObject();
		logRawDataInHexaDecimalFormat(payload, this.getClass());
		String gaiaId=calculateUnsignedDecimalValFromSignedBytes(Arrays.copyOfRange(payload, 0, 8));
		String seqeunceNumber=calculateUnsignedDecimalValFromSignedBytes(Arrays.copyOfRange(payload, 8, 12));
		String forwardTotalizerInLts=calculateUnsignedDecimalValFromSignedBytes(Arrays.copyOfRange(payload, 12, 16));
		String reverseTotalizerInLts=calculateUnsignedDecimalValFromSignedBytes(Arrays.copyOfRange(payload, 16, 20));
		String totalTotalizerInLts=calculateUnsignedDecimalValFromSignedBytes(Arrays.copyOfRange(payload, 20, 24));
		String flag1=calculateUnsignedDecimalValFromSignedBytes(payload[24]);
		String flag2=calculateUnsignedDecimalValFromSignedBytes(payload[25]);
		jsonObject.addProperty("gaiaId", gaiaId);
		jsonObject.addProperty("seqeunceNumber", seqeunceNumber);
		jsonObject.addProperty("forwardTotalizerInLts", forwardTotalizerInLts);
		jsonObject.addProperty("reverseTotalizerInLts", reverseTotalizerInLts);
		jsonObject.addProperty("totalTotalizerInLts", totalTotalizerInLts);
		jsonObject.addProperty("flag1", flag1);
		jsonObject.addProperty("flag2", flag2);
		return jsonObject;
	}

	@Override
	public String extractDeviceId(DeviceModel deviceModel, JsonObject payload) {
		return payload.get("gaiaId").getAsString();
	}
}
