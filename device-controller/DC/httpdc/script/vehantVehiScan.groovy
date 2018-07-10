/**
 * 
 */
package com.hpe.iot.http.vehant.vehiscan

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.handson.logger.LiveLogger
import com.hpe.iot.dc.model.Device
import com.hpe.iot.dc.model.DeviceDataDeliveryStatus
import com.hpe.iot.dc.model.DeviceImpl
import com.hpe.iot.dc.model.DeviceModel
import com.hpe.iot.model.DeviceInfo
import com.hpe.iot.northbound.service.inflow.IOTPublisherService
import com.hpe.iot.southbound.handler.inflow.impl.AbstractJsonPathDeviceIdExtractor
import com.hpe.iot.southbound.handler.inflow.impl.AbstractUplinkPayloadProcessor

/**
 * @author sveera
 *
 */
public class VehantVehiScanModel implements DeviceModel {

	@Override
	public String getManufacturer() {
		return "vehant";
	}

	@Override
	public String getModelId() {
		return 'vehiscan';
	}

	@Override
	public String getVersion() {
		return '1.0';
	}
}

public class VehantVehiScanDeviceIdExtractor extends AbstractJsonPathDeviceIdExtractor{

	@Override
	public String getDeviceIdJsonPath() {
		return '$.Vehicle[0].LicenseNum';
	}
}


public class  VehantVehiScanUplinkPayloadProcessor extends  AbstractUplinkPayloadProcessor{

	private final LiveLogger liveLogger;
	private final Logger logger=LoggerFactory.getLogger(this.getClass());

	public VehantVehiScanUplinkPayloadProcessor(LiveLogger liveLogger,IOTPublisherService<DeviceInfo, DeviceDataDeliveryStatus> iotPublisherService) {
		super(iotPublisherService);
		this.liveLogger=liveLogger;
	}

	@Override
	public void processPayload(DeviceInfo decipheredPayload) {
		JsonObject vehicleNotificationsCaptuaredByCamaras=decipheredPayload.getPayload();
		List<DeviceInfo> camerasDeviceInfo=convertOneVehicleNotficationsCaptuaredByCamarasToIndividualCamarasPayload(
				decipheredPayload.getDevice(),vehicleNotificationsCaptuaredByCamaras);
		logger.trace(""+liveLogger);
		liveLogger.log("Vehicle notifications captured by cameras "+camerasDeviceInfo);
		logger.trace("Vehicle notifications captured by cameras "+camerasDeviceInfo);
		for (DeviceInfo cameraDeviceInfo: camerasDeviceInfo)
			this.iotPublisherService.receiveDataFromDevice(cameraDeviceInfo, "notification");
	}

	private List<DeviceInfo> convertOneVehicleNotficationsCaptuaredByCamarasToIndividualCamarasPayload(Device vehicleDevice,
			JsonObject vehicleNotificationsCaptuaredByCamaras){
		List<DeviceInfo> camerasDeviceInfo=new ArrayList<>();
		JsonArray camaraPayloadJsons=vehicleNotificationsCaptuaredByCamaras.get('Vehicle').getAsJsonArray();
		for(JsonElement jsonElement:camaraPayloadJsons) {
			JsonObject camaraPayload=jsonElement.getAsJsonObject();
			camerasDeviceInfo.add(new DeviceInfo(new DeviceImpl(
					vehicleDevice.getManufacturer(), vehicleDevice.getModelId(), vehicleDevice.getVersion(), camaraPayload.get("CamName").getAsString())
					, "notification", camaraPayload));
		}
		camerasDeviceInfo;
	}
}
