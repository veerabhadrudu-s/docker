
/**
 * 
 */
package com.hpe.iot.dc.mmi.vt15.v1;

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.handson.logger.LiveLogger
import com.hpe.iot.dc.model.Device
import com.hpe.iot.dc.model.DeviceData
import com.hpe.iot.dc.model.DeviceDataDeliveryStatus
import com.hpe.iot.dc.model.DeviceImpl
import com.hpe.iot.dc.model.DeviceInfo
import com.hpe.iot.dc.model.DeviceModel
import com.hpe.iot.dc.northbound.service.inflow.IOTPublisherService
import com.hpe.iot.dc.southbound.converter.inflow.UplinkDeviceDataConverter
import com.hpe.iot.dc.southbound.converter.inflow.factory.UplinkDeviceDataConverterFactory
import com.hpe.iot.dc.southbound.service.inflow.ExtendedUplinkMessageService
import com.hpe.iot.dc.southbound.transformer.inflow.UplinkDataModelTransformer
import com.hpe.iot.dc.tcp.southbound.model.AbstractServerSocketToDeviceModel

/**
 * @author sveera
 *
 */

public class MMIVT15ServerSocketToDeviceModel extends AbstractServerSocketToDeviceModel{

	@Override
	public String getBoundLocalAddress() {
		return "10.3.239.75";
	}

	@Override
	public int getPortNumber() {
		return 3005;
	}

	@Override
	public String getManufacturer() {
		return "MMI";
	}

	@Override
	public String getModelId() {
		return "VT15";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getDescription() {
		return "Deep installed vehicle tracker with serial data reader";
	}
}

public class MMIVT15DataModelTransformer implements UplinkDataModelTransformer{

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final UplinkDeviceDataConverterFactory uplinkDeviceDataConverterFactory;
	private final String delimiter="\n\r";

	public MMIVT15DataModelTransformer(UplinkDeviceDataConverterFactory uplinkDeviceDataConverterFactory) {
		super();
		this.uplinkDeviceDataConverterFactory = uplinkDeviceDataConverterFactory;
	}

	@Override
	public List<DeviceInfo> convertToModel(DeviceModel deviceModel, byte[] input) {
		String deviceRawData=new String(input);
		String[] deviceRawDataFrames=deviceRawData.split(delimiter);
		return createDeviceData(deviceModel,deviceRawDataFrames);
	}

	private List<DeviceInfo> createDeviceData(DeviceModel deviceModel,String[] deviceRawDataFrames){
		List<DeviceInfo> deviceData=new ArrayList<>();
		if(deviceRawDataFrames==null||deviceRawDataFrames.length==0)
			return deviceData;
		if(deviceRawDataFrames.length==1) {
			deviceData.add(constructDeviceInfo(deviceRawDataFrames[0], deviceModel));
			return deviceData;
		}
		for(String deviceRawDataFrame:deviceRawDataFrames) {
			deviceRawDataFrame=deviceRawDataFrame+delimiter;
			deviceData.add(constructDeviceInfo(deviceRawDataFrame, deviceModel));
		}
		return deviceData;
	}

	private DeviceInfo constructDeviceInfo(String deviceRawDataFrame, DeviceModel deviceModel) {
		int eventType=Integer.parseInt(deviceRawDataFrame.split(",")[2]);
		String messageType=eventType==15?'connection_packet':eventType==20||eventType==120?'serial_packet':'tracking_packet';
		DeviceInfo deviceInfo=uplinkDeviceDataConverterFactory.getModelConverter(messageType).createModel(deviceModel, deviceRawDataFrame.getBytes())
		return deviceInfo
	}
}

public class ConnectPacketData implements DeviceData{

	private final String clientId;
	private final String deviceId;
	private final String firmwareVersion;
	private final String ipAddress;
	private final int portNumber;
	private final String accessPointName;
	private final String ignitionOnReportIntrvl;
	private final String ignitionOffReportIntrvl;
	private final String adminNumber1;
	private final String adminNumber2;
	private final String gmtOffset;
	private final String overSpeedLim;
	private final String overSpeedDur;
	private final String gpsFixStatus;
	private final String ignitionStatus;

	public ConnectPacketData(String clientId, String deviceId, String firmwareVersion, String ipAddress, int portNumber,
	String accessPointName, String ignitionOnReportIntrvl, String ignitionOffReportIntrvl, String adminNumber1,
	String adminNumber2, String gmtOffset, String overSpeedLim, String overSpeedDur, String gpsFixStatus,
	String ignitionStatus) {
		super();
		this.clientId = clientId;
		this.deviceId = deviceId;
		this.firmwareVersion = firmwareVersion;
		this.ipAddress = ipAddress;
		this.portNumber = portNumber;
		this.accessPointName = accessPointName;
		this.ignitionOnReportIntrvl = ignitionOnReportIntrvl;
		this.ignitionOffReportIntrvl = ignitionOffReportIntrvl;
		this.adminNumber1 = adminNumber1;
		this.adminNumber2 = adminNumber2;
		this.gmtOffset = gmtOffset;
		this.overSpeedLim = overSpeedLim;
		this.overSpeedDur = overSpeedDur;
		this.gpsFixStatus = gpsFixStatus;
		this.ignitionStatus = ignitionStatus;
	}

	public String getClientId() {
		return clientId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public String getFirmwareVersion() {
		return firmwareVersion;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public int getPortNumber() {
		return portNumber;
	}

	public String getAccessPointName() {
		return accessPointName;
	}

	public String getIgnitionOnReportIntrvl() {
		return ignitionOnReportIntrvl;
	}

	public String getIgnitionOffReportIntrvl() {
		return ignitionOffReportIntrvl;
	}

	public String getAdminNumber1() {
		return adminNumber1;
	}

	public String getAdminNumber2() {
		return adminNumber2;
	}

	public String getGmtOffset() {
		return gmtOffset;
	}

	public String getOverSpeedLim() {
		return overSpeedLim;
	}

	public String getOverSpeedDur() {
		return overSpeedDur;
	}

	public String getGpsFixStatus() {
		return gpsFixStatus;
	}

	public String getIgnitionStatus() {
		return ignitionStatus;
	}

	@Override
	public String getDeviceDataInformation() {
		return "connection_packet_data";
	}


	@Override
	public String toString() {
		return "ConnectPacketData [clientId=" + clientId + ", deviceId=" + deviceId + ", firmwareVersion="+
				firmwareVersion + ", ipAddress=" + ipAddress + ", portNumber=" + portNumber + ", accessPointName="+
				accessPointName + ", ignitionOnReportIntrvl=" + ignitionOnReportIntrvl + ", ignitionOffReportIntrvl="+
				ignitionOffReportIntrvl + ", adminNumber1=" + adminNumber1 + ", adminNumber2=" + adminNumber2+
				", gmtOffset=" + gmtOffset + ", overSpeedLim=" + overSpeedLim + ", overSpeedDur=" + overSpeedDur+
				", gpsFixStatus=" + gpsFixStatus + ", ignitionStatus=" + ignitionStatus + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessPointName == null) ? 0 : accessPointName.hashCode());
		result = prime * result + ((adminNumber1 == null) ? 0 : adminNumber1.hashCode());
		result = prime * result + ((adminNumber2 == null) ? 0 : adminNumber2.hashCode());
		result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
		result = prime * result + ((deviceId == null) ? 0 : deviceId.hashCode());
		result = prime * result + ((firmwareVersion == null) ? 0 : firmwareVersion.hashCode());
		result = prime * result + ((gmtOffset == null) ? 0 : gmtOffset.hashCode());
		result = prime * result + ((gpsFixStatus == null) ? 0 : gpsFixStatus.hashCode());
		result = prime * result + ((ignitionOffReportIntrvl == null) ? 0 : ignitionOffReportIntrvl.hashCode());
		result = prime * result + ((ignitionOnReportIntrvl == null) ? 0 : ignitionOnReportIntrvl.hashCode());
		result = prime * result + ((ignitionStatus == null) ? 0 : ignitionStatus.hashCode());
		result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
		result = prime * result + ((overSpeedDur == null) ? 0 : overSpeedDur.hashCode());
		result = prime * result + ((overSpeedLim == null) ? 0 : overSpeedLim.hashCode());
		result = prime * result + portNumber;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof ConnectPacketData))
			return false;
		ConnectPacketData other = (ConnectPacketData) obj;
		if (accessPointName == null) {
			if (other.accessPointName != null)
				return false;
		} else if (!accessPointName.equals(other.accessPointName))
			return false;
		if (adminNumber1 == null) {
			if (other.adminNumber1 != null)
				return false;
		} else if (!adminNumber1.equals(other.adminNumber1))
			return false;
		if (adminNumber2 == null) {
			if (other.adminNumber2 != null)
				return false;
		} else if (!adminNumber2.equals(other.adminNumber2))
			return false;
		if (clientId == null) {
			if (other.clientId != null)
				return false;
		} else if (!clientId.equals(other.clientId))
			return false;
		if (deviceId == null) {
			if (other.deviceId != null)
				return false;
		} else if (!deviceId.equals(other.deviceId))
			return false;
		if (firmwareVersion == null) {
			if (other.firmwareVersion != null)
				return false;
		} else if (!firmwareVersion.equals(other.firmwareVersion))
			return false;
		if (gmtOffset == null) {
			if (other.gmtOffset != null)
				return false;
		} else if (!gmtOffset.equals(other.gmtOffset))
			return false;
		if (gpsFixStatus == null) {
			if (other.gpsFixStatus != null)
				return false;
		} else if (!gpsFixStatus.equals(other.gpsFixStatus))
			return false;
		if (ignitionOffReportIntrvl == null) {
			if (other.ignitionOffReportIntrvl != null)
				return false;
		} else if (!ignitionOffReportIntrvl.equals(other.ignitionOffReportIntrvl))
			return false;
		if (ignitionOnReportIntrvl == null) {
			if (other.ignitionOnReportIntrvl != null)
				return false;
		} else if (!ignitionOnReportIntrvl.equals(other.ignitionOnReportIntrvl))
			return false;
		if (ignitionStatus == null) {
			if (other.ignitionStatus != null)
				return false;
		} else if (!ignitionStatus.equals(other.ignitionStatus))
			return false;
		if (ipAddress == null) {
			if (other.ipAddress != null)
				return false;
		} else if (!ipAddress.equals(other.ipAddress))
			return false;
		if (overSpeedDur == null) {
			if (other.overSpeedDur != null)
				return false;
		} else if (!overSpeedDur.equals(other.overSpeedDur))
			return false;
		if (overSpeedLim == null) {
			if (other.overSpeedLim != null)
				return false;
		} else if (!overSpeedLim.equals(other.overSpeedLim))
			return false;
		if (portNumber != other.portNumber)
			return false;
		return true;
	}
}


public class ConnectPacketUplinkDataConverter implements UplinkDeviceDataConverter{

	@Override
	public String getMessageType() {
		return "connection_packet";
	}

	@Override
	public DeviceInfo createModel(DeviceModel deviceModel, byte[] input) {
		String connectionPacketRawData=new String(input);
		String[] connectionPacketDataValues=connectionPacketRawData.split(",");
		Device device=new DeviceImpl(deviceModel.getManufacturer(), deviceModel.getModelId(), deviceModel.getVersion(), connectionPacketDataValues[1]);
		DeviceInfo deviceInfo=new DeviceInfo(device, messageType, input);
		addConnectPacketData(connectionPacketDataValues,deviceInfo.getDeviceData());
		return deviceInfo;
	}

	private void addConnectPacketData(final String[] connectionPacketDataValues,final Map<String,DeviceData> deviceData) {
		final String clientId=connectionPacketDataValues[0].replace('$$', "");
		final String deviceId=connectionPacketDataValues[1];
		final String firmwareVersion=connectionPacketDataValues[3];
		final String ipAddress=connectionPacketDataValues[4];
		final int  portNumber=Integer.parseInt(connectionPacketDataValues[5]);
		final String accessPointName=connectionPacketDataValues[6];
		final String ignitionOnReportIntrvl=connectionPacketDataValues[7].split(":")[1];
		final String ignitionOffReportIntrvl=connectionPacketDataValues[8].split(":")[1];
		final String adminNumber1=connectionPacketDataValues[9].split(":")[1];
		final String adminNumber2=connectionPacketDataValues[10].split(":")[1];
		final String gmtOffset=connectionPacketDataValues[11].split(":")[1];
		final String overSpeedLim=connectionPacketDataValues[13].split(":")[1];
		final String overSpeedDur=connectionPacketDataValues[14].split(":")[1];
		final String gpsFixStatus=connectionPacketDataValues[15].split(":")[1];
		final String ignitionStatus=connectionPacketDataValues[16].split(":")[1];
		ConnectPacketData connectPacketData=new ConnectPacketData(clientId, deviceId, firmwareVersion, ipAddress, portNumber, accessPointName,
				ignitionOnReportIntrvl, ignitionOffReportIntrvl, adminNumber1, adminNumber2, gmtOffset, overSpeedLim, overSpeedDur, gpsFixStatus, ignitionStatus);
		deviceData.put(connectPacketData.getDeviceDataInformation(), connectPacketData);
	}
}

public class PacketData{
	protected final String packetType;
	protected final String clientId;
	protected final String deviceId;
	protected final String eventType;
	protected final String latitude;
	protected final String longitude;
	protected final String timeStamp;
	protected final String gpsValidFlag;

	public PacketData(String packetType, String clientId, String deviceId, String eventType, String latitude,
	String longitude, String timeStamp, String gpsValidFlag) {
		super();
		this.packetType = packetType;
		this.clientId = clientId;
		this.deviceId = deviceId;
		this.eventType = eventType;
		this.latitude = latitude;
		this.longitude = longitude;
		this.timeStamp = timeStamp;
		this.gpsValidFlag = gpsValidFlag;
	}

	public String getPacketType() {
		return packetType;
	}

	public String getClientId() {
		return clientId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public String getEventType() {
		return eventType;
	}

	public String getLatitude() {
		return latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public String getGpsValidFlag() {
		return gpsValidFlag;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
		result = prime * result + ((deviceId == null) ? 0 : deviceId.hashCode());
		result = prime * result + ((eventType == null) ? 0 : eventType.hashCode());
		result = prime * result + ((gpsValidFlag == null) ? 0 : gpsValidFlag.hashCode());
		result = prime * result + ((latitude == null) ? 0 : latitude.hashCode());
		result = prime * result + ((longitude == null) ? 0 : longitude.hashCode());
		result = prime * result + ((packetType == null) ? 0 : packetType.hashCode());
		result = prime * result + ((timeStamp == null) ? 0 : timeStamp.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof PacketData))
			return false;
		PacketData other = (PacketData) obj;
		if (clientId == null) {
			if (other.clientId != null)
				return false;
		} else if (!clientId.equals(other.clientId))
			return false;
		if (deviceId == null) {
			if (other.deviceId != null)
				return false;
		} else if (!deviceId.equals(other.deviceId))
			return false;
		if (eventType == null) {
			if (other.eventType != null)
				return false;
		} else if (!eventType.equals(other.eventType))
			return false;
		if (gpsValidFlag == null) {
			if (other.gpsValidFlag != null)
				return false;
		} else if (!gpsValidFlag.equals(other.gpsValidFlag))
			return false;
		if (latitude == null) {
			if (other.latitude != null)
				return false;
		} else if (!latitude.equals(other.latitude))
			return false;
		if (longitude == null) {
			if (other.longitude != null)
				return false;
		} else if (!longitude.equals(other.longitude))
			return false;
		if (packetType == null) {
			if (other.packetType != null)
				return false;
		} else if (!packetType.equals(other.packetType))
			return false;
		if (timeStamp == null) {
			if (other.timeStamp != null)
				return false;
		} else if (!timeStamp.equals(other.timeStamp))
			return false;
		return true;
	}
}


public class TrackingPacketData extends PacketData implements DeviceData{

	private final String gsmlevel;
	private final String speed;
	private final float distancekm;
	private final String heading;
	private final String numberOfSatellites;
	private final String hdop;
	private final float powerSupplyVoltage;
	private final String digitalInput1;
	private final String caseOpen;
	private final String overSpeedStarted;
	private final String overSpeedEnded;
	private final String immobilizerAlert;
	private final String powerOn;
	private final String digitalInput2;
	private final String ignitionStatus;
	private final String internalBatteryLowAlert;
	private final String analogPolling;
	private final String digitalOutput1;
	private final String harshAcceleration;
	private final String harshBraking;
	private final float externalBatteryVoltage;
	private final float internalBatteryVoltage;

	public TrackingPacketData(String packetType, String clientId, String deviceId, String eventType, String latitude,
	String longitude, String timeStamp, String gpsValidFlag, String gsmlevel, String speed, float distancekm,
	String heading, String numberOfSatellites, String hdop, float powerSupplyVoltage, String digitalInput1,
	String caseOpen, String overSpeedStarted, String overSpeedEnded, String immobilizerAlert, String powerOn,
	String digitalInput2, String ignitionStatus, String internalBatteryLowAlert, String analogPolling,
	String digitalOutput1, String harshAcceleration, String harshBraking, float externalBatteryVoltage,
	float internalBatteryVoltage) {
		super(packetType, clientId, deviceId, eventType, latitude, longitude, timeStamp, gpsValidFlag);
		this.gsmlevel = gsmlevel;
		this.speed = speed;
		this.distancekm = distancekm;
		this.heading = heading;
		this.numberOfSatellites = numberOfSatellites;
		this.hdop = hdop;
		this.powerSupplyVoltage = powerSupplyVoltage;
		this.digitalInput1 = digitalInput1;
		this.caseOpen = caseOpen;
		this.overSpeedStarted = overSpeedStarted;
		this.overSpeedEnded = overSpeedEnded;
		this.immobilizerAlert = immobilizerAlert;
		this.powerOn = powerOn;
		this.digitalInput2 = digitalInput2;
		this.ignitionStatus = ignitionStatus;
		this.internalBatteryLowAlert = internalBatteryLowAlert;
		this.analogPolling = analogPolling;
		this.digitalOutput1 = digitalOutput1;
		this.harshAcceleration = harshAcceleration;
		this.harshBraking = harshBraking;
		this.externalBatteryVoltage = externalBatteryVoltage;
		this.internalBatteryVoltage = internalBatteryVoltage;
	}

	public String getGsmlevel() {
		return gsmlevel;
	}

	public String getSpeed() {
		return speed;
	}

	public float getDistancekm() {
		return distancekm;
	}

	public String getHeading() {
		return heading;
	}

	public String getNumberOfSatellites() {
		return numberOfSatellites;
	}

	public String getHdop() {
		return hdop;
	}

	public float getPowerSupplyVoltage() {
		return powerSupplyVoltage;
	}

	public String getDigitalInput1() {
		return digitalInput1;
	}

	public String getCaseOpen() {
		return caseOpen;
	}

	public String getOverSpeedStarted() {
		return overSpeedStarted;
	}

	public String getOverSpeedEnded() {
		return overSpeedEnded;
	}

	public String getImmobilizerAlert() {
		return immobilizerAlert;
	}

	public String getPowerOn() {
		return powerOn;
	}

	public String getDigitalInput2() {
		return digitalInput2;
	}

	public String getIgnitionStatus() {
		return ignitionStatus;
	}

	public String getInternalBatteryLowAlert() {
		return internalBatteryLowAlert;
	}

	public String getAnalogPolling() {
		return analogPolling;
	}

	public String getDigitalOutput1() {
		return digitalOutput1;
	}

	public String getHarshAcceleration() {
		return harshAcceleration;
	}

	public String getHarshBraking() {
		return harshBraking;
	}

	public float getExternalBatteryVoltage() {
		return externalBatteryVoltage;
	}

	public float getInternalBatteryVoltage() {
		return internalBatteryVoltage;
	}

	@Override
	public String getDeviceDataInformation() {
		return "tracking_packet_data";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((analogPolling == null) ? 0 : analogPolling.hashCode());
		result = prime * result + ((caseOpen == null) ? 0 : caseOpen.hashCode());
		result = prime * result + ((digitalInput1 == null) ? 0 : digitalInput1.hashCode());
		result = prime * result + ((digitalInput2 == null) ? 0 : digitalInput2.hashCode());
		result = prime * result + ((digitalOutput1 == null) ? 0 : digitalOutput1.hashCode());
		result = prime * result + Float.floatToIntBits(distancekm);
		result = prime * result + Float.floatToIntBits(externalBatteryVoltage);
		result = prime * result + ((gsmlevel == null) ? 0 : gsmlevel.hashCode());
		result = prime * result + ((harshAcceleration == null) ? 0 : harshAcceleration.hashCode());
		result = prime * result + ((harshBraking == null) ? 0 : harshBraking.hashCode());
		result = prime * result + ((hdop == null) ? 0 : hdop.hashCode());
		result = prime * result + ((heading == null) ? 0 : heading.hashCode());
		result = prime * result + ((ignitionStatus == null) ? 0 : ignitionStatus.hashCode());
		result = prime * result + ((immobilizerAlert == null) ? 0 : immobilizerAlert.hashCode());
		result = prime * result + ((internalBatteryLowAlert == null) ? 0 : internalBatteryLowAlert.hashCode());
		result = prime * result + Float.floatToIntBits(internalBatteryVoltage);
		result = prime * result + ((numberOfSatellites == null) ? 0 : numberOfSatellites.hashCode());
		result = prime * result + ((overSpeedEnded == null) ? 0 : overSpeedEnded.hashCode());
		result = prime * result + ((overSpeedStarted == null) ? 0 : overSpeedStarted.hashCode());
		result = prime * result + ((powerOn == null) ? 0 : powerOn.hashCode());
		result = prime * result + Float.floatToIntBits(powerSupplyVoltage);
		result = prime * result + ((speed == null) ? 0 : speed.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof TrackingPacketData))
			return false;
		TrackingPacketData other = (TrackingPacketData) obj;
		if (analogPolling == null) {
			if (other.analogPolling != null)
				return false;
		} else if (!analogPolling.equals(other.analogPolling))
			return false;
		if (caseOpen == null) {
			if (other.caseOpen != null)
				return false;
		} else if (!caseOpen.equals(other.caseOpen))
			return false;
		if (digitalInput1 == null) {
			if (other.digitalInput1 != null)
				return false;
		} else if (!digitalInput1.equals(other.digitalInput1))
			return false;
		if (digitalInput2 == null) {
			if (other.digitalInput2 != null)
				return false;
		} else if (!digitalInput2.equals(other.digitalInput2))
			return false;
		if (digitalOutput1 == null) {
			if (other.digitalOutput1 != null)
				return false;
		} else if (!digitalOutput1.equals(other.digitalOutput1))
			return false;
		if (Float.floatToIntBits(distancekm) != Float.floatToIntBits(other.distancekm))
			return false;
		if (Float.floatToIntBits(externalBatteryVoltage) != Float.floatToIntBits(other.externalBatteryVoltage))
			return false;
		if (gsmlevel == null) {
			if (other.gsmlevel != null)
				return false;
		} else if (!gsmlevel.equals(other.gsmlevel))
			return false;
		if (harshAcceleration == null) {
			if (other.harshAcceleration != null)
				return false;
		} else if (!harshAcceleration.equals(other.harshAcceleration))
			return false;
		if (harshBraking == null) {
			if (other.harshBraking != null)
				return false;
		} else if (!harshBraking.equals(other.harshBraking))
			return false;
		if (hdop == null) {
			if (other.hdop != null)
				return false;
		} else if (!hdop.equals(other.hdop))
			return false;
		if (heading == null) {
			if (other.heading != null)
				return false;
		} else if (!heading.equals(other.heading))
			return false;
		if (ignitionStatus == null) {
			if (other.ignitionStatus != null)
				return false;
		} else if (!ignitionStatus.equals(other.ignitionStatus))
			return false;
		if (immobilizerAlert == null) {
			if (other.immobilizerAlert != null)
				return false;
		} else if (!immobilizerAlert.equals(other.immobilizerAlert))
			return false;
		if (internalBatteryLowAlert == null) {
			if (other.internalBatteryLowAlert != null)
				return false;
		} else if (!internalBatteryLowAlert.equals(other.internalBatteryLowAlert))
			return false;
		if (Float.floatToIntBits(internalBatteryVoltage) != Float.floatToIntBits(other.internalBatteryVoltage))
			return false;
		if (numberOfSatellites == null) {
			if (other.numberOfSatellites != null)
				return false;
		} else if (!numberOfSatellites.equals(other.numberOfSatellites))
			return false;
		if (overSpeedEnded == null) {
			if (other.overSpeedEnded != null)
				return false;
		} else if (!overSpeedEnded.equals(other.overSpeedEnded))
			return false;
		if (overSpeedStarted == null) {
			if (other.overSpeedStarted != null)
				return false;
		} else if (!overSpeedStarted.equals(other.overSpeedStarted))
			return false;
		if (powerOn == null) {
			if (other.powerOn != null)
				return false;
		} else if (!powerOn.equals(other.powerOn))
			return false;
		if (Float.floatToIntBits(powerSupplyVoltage) != Float.floatToIntBits(other.powerSupplyVoltage))
			return false;
		if (speed == null) {
			if (other.speed != null)
				return false;
		} else if (!speed.equals(other.speed))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TrackingPacketData [gsmlevel=" + gsmlevel + ", speed=" + speed + ", distancekm=" + distancekm+
				", heading=" + heading + ", numberOfSatellites=" + numberOfSatellites + ", hdop=" + hdop+
				", powerSupplyVoltage=" + powerSupplyVoltage + ", digitalInput1=" + digitalInput1 + ", caseOpen="+
				caseOpen + ", overSpeedStarted=" + overSpeedStarted + ", overSpeedEnded=" + overSpeedEnded+
				", immobilizerAlert=" + immobilizerAlert + ", powerOn=" + powerOn + ", digitalInput2=" + digitalInput2+
				", ignitionStatus=" + ignitionStatus + ", internalBatteryLowAlert=" + internalBatteryLowAlert+
				", analogPolling=" + analogPolling + ", digitalOutput1=" + digitalOutput1 + ", harshAcceleration="+
				harshAcceleration + ", harshBraking=" + harshBraking + ", externalBatteryVoltage="+
				externalBatteryVoltage + ", internalBatteryVoltage=" + internalBatteryVoltage + ", packetType="+
				packetType + ", clientId=" + clientId + ", deviceId=" + deviceId + ", eventType=" + eventType+
				", latitude=" + latitude + ", longitude=" + longitude + ", timeStamp=" + timeStamp + ", gpsValidFlag="+
				gpsValidFlag + "]";
	}
}

public class PacketDataExtractor {

	public PacketData extractPacketData(String[] packetDataValues) {
		final String clientId=packetDataValues[0].replace('$$', "");
		final String deviceId=packetDataValues[1];
		final int eventType=Integer.parseInt(packetDataValues[2]);
		final String eventTypeVal=eventType>100?Integer.valueOf(eventType-100):packetDataValues[2];
		final String packetType=eventType>100?"historical":"live";
		final String latitude=packetDataValues[3];
		final String longitude=packetDataValues[4];
		final String timeStamp=(2000+Integer.parseInt(packetDataValues[5].substring(0, 2)))+"-"+packetDataValues[5].substring(2, 4)+
				"-"+packetDataValues[5].substring(4, 6)+" "+packetDataValues[5].substring(6, 8)+":"+
				packetDataValues[5].substring(8, 10)+":"+packetDataValues[5].substring(10, 12);
		final String gpsValidFlag=packetDataValues[6];
		return new PacketData(packetType, clientId, deviceId, eventTypeVal, latitude, longitude, timeStamp, gpsValidFlag);
	}
}


public class TrackingPacketUplinkDataConverter implements UplinkDeviceDataConverter{

	private final PacketDataExtractor packetDataExtractor;

	public TrackingPacketUplinkDataConverter(PacketDataExtractor packetDataExtractor) {
		super();
		this.packetDataExtractor = packetDataExtractor;
	}

	@Override
	public String getMessageType() {
		return "tracking_packet";
	}

	@Override
	public DeviceInfo createModel(DeviceModel deviceModel, byte[] input) {
		String trackingPacketRawData=new String(input);
		String[] trackingPacketDataValues=trackingPacketRawData.split(",");
		Device device=new DeviceImpl(deviceModel.getManufacturer(), deviceModel.getModelId(), deviceModel.getVersion(), trackingPacketDataValues[1]);
		DeviceInfo deviceInfo=new DeviceInfo(device, messageType, input);
		addTrackingPacketData(trackingPacketDataValues,deviceInfo.getDeviceData());
		return deviceInfo;
	}

	private void addTrackingPacketData(final String[] trackingPacketDataValues,final Map<String,DeviceData> deviceData) {
		final PacketData packetData=packetDataExtractor.extractPacketData(trackingPacketDataValues);
		final String gsmlevel=trackingPacketDataValues[7];
		final String speed=trackingPacketDataValues[8];
		final float distancekm=Integer.parseInt(trackingPacketDataValues[9])/1000;
		final String heading=trackingPacketDataValues[10];
		final String numberOfSatellites=trackingPacketDataValues[11];
		final String hdop=trackingPacketDataValues[12];
		final float powerSupplyVoltage=Integer.parseInt(trackingPacketDataValues[15])/1000;
		final String digitalInput1=trackingPacketDataValues[16];
		final String caseOpen=trackingPacketDataValues[17];
		final String overSpeedStarted=trackingPacketDataValues[18];
		final String overSpeedEnded=trackingPacketDataValues[19];
		final String immobilizerAlert=trackingPacketDataValues[22];
		final String powerOn=trackingPacketDataValues[23];
		final String digitalInput2=trackingPacketDataValues[24];
		final String ignitionStatus=trackingPacketDataValues[27];
		final String internalBatteryLowAlert=trackingPacketDataValues[34];
		final String analogPolling=trackingPacketDataValues[35];
		final String digitalOutput1=trackingPacketDataValues[40];
		final String harshAcceleration=trackingPacketDataValues[42];
		final String harshBraking=trackingPacketDataValues[43];
		final float externalBatteryVoltage=Integer.parseInt(trackingPacketDataValues[48])/1000;
		final float internalBatteryVoltage=Integer.parseInt(trackingPacketDataValues[49])/1000;
		final TrackingPacketData trackingPacketData=new TrackingPacketData(packetData.getPacketType(), packetData.getClientId(), packetData.getDeviceId(),
				packetData.getEventType(), packetData.getLatitude(), packetData.getLongitude(),
				packetData.getTimeStamp(), packetData.getGpsValidFlag(), gsmlevel, speed, distancekm, heading, numberOfSatellites, hdop, powerSupplyVoltage,
				digitalInput1, caseOpen, overSpeedStarted, overSpeedEnded, immobilizerAlert, powerOn, digitalInput2,
				ignitionStatus, internalBatteryLowAlert, analogPolling, digitalOutput1, harshAcceleration, harshBraking,
				externalBatteryVoltage, internalBatteryVoltage);
		deviceData.put(trackingPacketData.getDeviceDataInformation(), trackingPacketData);
	}
}

public class SerialPacketData extends PacketData implements DeviceData{

	private final String serialDataType;
	private final String serialData;

	public SerialPacketData(String packetType, String clientId, String deviceId, String eventType, String latitude,
	String longitude, String timeStamp, String gpsValidFlag, String serialDataType, String serialData) {
		super(packetType, clientId, deviceId, eventType, latitude, longitude, timeStamp, gpsValidFlag);
		this.serialDataType = serialDataType;
		this.serialData = serialData;
	}

	public String getSerialDataType() {
		return serialDataType;
	}

	public String getSerialData() {
		return serialData;
	}

	@Override
	public String getDeviceDataInformation() {
		return "serial_packet_data";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((serialData == null) ? 0 : serialData.hashCode());
		result = prime * result + ((serialDataType == null) ? 0 : serialDataType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof SerialPacketData))
			return false;
		SerialPacketData other = (SerialPacketData) obj;
		if (serialData == null) {
			if (other.serialData != null)
				return false;
		} else if (!serialData.equals(other.serialData))
			return false;
		if (serialDataType == null) {
			if (other.serialDataType != null)
				return false;
		} else if (!serialDataType.equals(other.serialDataType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SerialPacketData [serialDataType=" + serialDataType + ", serialData=" + serialData + ", packetType="+
				packetType + ", clientId=" + clientId + ", deviceId=" + deviceId + ", eventType=" + eventType+
				", latitude=" + latitude + ", longitude=" + longitude + ", timeStamp=" + timeStamp + ", gpsValidFlag="+
				gpsValidFlag + "]";
	}
}

public class SerialPacketDataUplinkDataConverter implements UplinkDeviceDataConverter{

	private final PacketDataExtractor packetDataExtractor;

	public SerialPacketDataUplinkDataConverter(PacketDataExtractor packetDataExtractor) {
		super();
		this.packetDataExtractor = packetDataExtractor;
	}

	@Override
	public String getMessageType() {
		return "serial_packet";
	}

	@Override
	public DeviceInfo createModel(DeviceModel deviceModel, byte[] input) {
		String serialDataPacketRawData=new String(input);
		String[] serialDataPacketDataValues=serialDataPacketRawData.split(",");
		Device device=new DeviceImpl(deviceModel.getManufacturer(), deviceModel.getModelId(), deviceModel.getVersion(), serialDataPacketDataValues[1]);
		DeviceInfo deviceInfo=new DeviceInfo(device, messageType, input);
		addSerialDataPacketData(serialDataPacketDataValues,deviceInfo.getDeviceData());
		return deviceInfo;
	}

	private void addSerialDataPacketData(final String[] trackingPacketDataValues,final Map<String,DeviceData> deviceData) {
		final PacketData packetData=packetDataExtractor.extractPacketData(trackingPacketDataValues);
		final String serialDataType= trackingPacketDataValues[7];
		final String serialData= trackingPacketDataValues[8];
		DeviceData serialDataPacket=new SerialPacketData(packetData.getPacketType(), packetData.getClientId(), packetData.getDeviceId(),
				packetData.getEventType(), packetData.getLatitude(),packetData.getLongitude(), packetData.getTimeStamp(), packetData.getGpsValidFlag(),
				serialDataType, serialData);
		deviceData.put(serialDataPacket.getDeviceDataInformation(), serialDataPacket);
	}
}

public class MMIVT15UplinkMessageService implements ExtendedUplinkMessageService{

	private final List<String> supportedMessageServices=new ArrayList<>();
	private final IOTPublisherService<DeviceInfo, DeviceDataDeliveryStatus> iotPublisherService;
	private final LiveLogger liveLogger;

	public MMIVT15UplinkMessageService(LiveLogger liveLogger,IOTPublisherService<DeviceInfo, DeviceDataDeliveryStatus> iotPublisherService) {
		this.liveLogger=liveLogger;
		this.iotPublisherService = iotPublisherService;
		this.supportedMessageServices.add("connection_packet");
		this.supportedMessageServices.add("serial_packet");
		this.supportedMessageServices.add("tracking_packet");
	}

	@Override
	public String getMessageType() {
		return supportedMessageServices.get(0);
	}

	@Override
	public List<String> getMessageTypes() {
		return supportedMessageServices;
	}

	@Override
	public DeviceDataDeliveryStatus executeService(DeviceInfo deviceInfo) {
		liveLogger.log("Received device uplink payload @ "+new Date()+" with data as "+deviceInfo);
		return iotPublisherService.receiveDataFromDevice(deviceInfo, deviceInfo.getMessageType());
	}
}