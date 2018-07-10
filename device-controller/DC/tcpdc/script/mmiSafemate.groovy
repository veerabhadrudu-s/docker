package com.hpe.iot.dc.mmi.safemate;

import static com.hpe.iot.dc.mmi.safemate.TrackerNotification.NotificationType.ALERT
import static com.hpe.iot.dc.mmi.safemate.TrackerNotification.NotificationType.HISTORY_DATA;
import static com.hpe.iot.dc.mmi.safemate.TrackerNotification.NotificationType.IPCONNECT
import static com.hpe.iot.dc.mmi.safemate.TrackerNotification.NotificationType.REGULAR_DATA;
import static com.hpe.iot.dc.model.constants.ModelConstants.DEVICE_KEY
import static com.handson.iot.dc.util.DataParserUtility.*;
import static com.handson.iot.dc.util.UtilityLogger.convertArrayOfByteToString
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.util.Arrays.copyOfRange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.handson.logger.LiveLogger
import com.hpe.iot.dc.mmi.safemate.TrackerNotification.NotificationType;
import com.hpe.iot.dc.model.Device
import com.hpe.iot.dc.model.DeviceData;
import com.hpe.iot.dc.model.DeviceDataDeliveryStatus;
import com.hpe.iot.dc.model.DeviceImpl
import com.hpe.iot.dc.model.DeviceInfo;
import com.hpe.iot.dc.model.DeviceModel;
import com.hpe.iot.dc.northbound.converter.outflow.DownlinkDeviceDataConverter
import com.hpe.iot.dc.northbound.service.inflow.IOTPublisherService
import com.hpe.iot.dc.northbound.service.outflow.DownlinkMessageService
import com.hpe.iot.dc.service.MessageService
import com.hpe.iot.dc.southbound.converter.inflow.UplinkDeviceDataConverter
import com.hpe.iot.dc.southbound.converter.inflow.factory.UplinkDeviceDataConverterFactory
import com.hpe.iot.dc.southbound.service.inflow.UplinkMessageService;
import com.hpe.iot.dc.southbound.transformer.inflow.UplinkDataModelTransformer
import com.hpe.iot.dc.tcp.southbound.model.AbstractServerSocketToDeviceModel;
import com.hpe.iot.dc.tcp.southbound.service.outflow.TCPServerSocketWriter;
import com.handson.iot.dc.util.DataParserUtility;
import com.handson.iot.dc.util.UtilityLogger;

/**
 * @author sveera
 *
 */
public class MMIServerSocketToDeviceModel extends AbstractServerSocketToDeviceModel{

	public String getManufacturer(){
		return "MMI";
	}

	public String getModelId(){
		return "SafeMate";
	}

	public String getVersion() {
		return "1.0"
	}

	public String getBoundLocalAddress(){
		return "10.3.239.75";
	}

	public int getPortNumber(){
		return 3002;
	}

	public String getDescription() {
		return "This is a Personal Safety Tracker used for child and women safety.";
	}
}


public class MMIDataModelTransformer implements UplinkDataModelTransformer {

	private static def MESSAGE_STARTING_BYTES = [64, 64] as byte[];
	private static def MESSAGE_CLOSING_BYTES = [13, 10] as byte[];
	//private static final byte[] MESSAGE_STARTING_BYTES = new byte[] { 64, 64 };
	//private static final byte[] MESSAGE_CLOSING_BYTES = new byte[] { 13, 10 };


	private  final Logger logger = LoggerFactory.getLogger(getClass());

	private UplinkDeviceDataConverterFactory metaModelFactory;

	public MMIDataModelTransformer(UplinkDeviceDataConverterFactory metaModelFactory) {
		super();
		this.metaModelFactory = metaModelFactory;
	}

	@Override
	public List<DeviceInfo> convertToModel(DeviceModel deviceModel,final byte[] input) {
		byte[] messageBytes = DataParserUtility.truncateEmptyBytes(input);
		UtilityLogger.logRawDataInDecimalFormat(messageBytes, getClass());
		UtilityLogger.logRawDataInHexaDecimalFormat(messageBytes, getClass());
		return createDataFramesFromInputBytes(deviceModel,messageBytes);
	}

	private List<DeviceInfo> createDataFramesFromInputBytes(DeviceModel deviceModel,byte[] input) {
		List<Integer> closingByteIndexs = DataParserUtility.findAllClosingMessageByteIndexes(input,
				MESSAGE_CLOSING_BYTES);
		List<byte[]> dataFrames = createDataFrames(input, closingByteIndexs);
		List<byte[]> validDataFrames = filterInvalidDataFrames(dataFrames);
		logger.debug("Extracted valid Data Frames length is " + validDataFrames.size());
		return constructDataModelsFromFrames(deviceModel,validDataFrames);
	}

	private List<byte[]> createDataFrames(byte[] input, List<Integer> closingByteIndexs) {
		List<byte[]> dataFrames = new ArrayList<>();
		int startIndex = 0;
		for (Integer closingFrameByte : closingByteIndexs) {
			dataFrames.add(Arrays.copyOfRange(input, startIndex, closingFrameByte + 1));
			startIndex = closingFrameByte + 1;
		}
		return dataFrames;
	}

	private List<byte[]> filterInvalidDataFrames(List<byte[]> dataFrames) {
		List<byte[]> validDataFrames = new ArrayList<>();
		for (byte[] dataFrame : dataFrames)
			if (isMessageStartingWithStartingByte(dataFrame))
				validDataFrames.add(dataFrame);
		return validDataFrames;
	}

	private boolean isMessageStartingWithStartingByte(byte[] dataFrame) {
		boolean isValid = DataParserUtility.isMessageHasStartingBytes(dataFrame, MESSAGE_STARTING_BYTES);
		if (!isValid)
			logger.warn("Ignored Data Frame due to missing of Starting Bytes "+MESSAGE_STARTING_BYTES+" : "
					+ UtilityLogger.convertArrayOfByteToString(dataFrame));
		return isValid;
	}


	private List<DeviceInfo> constructDataModelsFromFrames(DeviceModel deviceModel,List<byte[]> dataFrames) {
		List<DeviceInfo> dataModels = new ArrayList<>();
		for (byte[] dataFrame : dataFrames) {
			DeviceInfo dataModel = convertDataFrameToDataModel(deviceModel,dataFrame);
			if (dataModel != null)
				dataModels.add(dataModel);
		}
		return dataModels;
	}

	private DeviceInfo convertDataFrameToDataModel(DeviceModel deviceModel,byte[] input) {
		final String messageType = findMessageTypeFromInput(input);
		logger.debug("Identified Message Type from Data frame is " + messageType);
		UplinkDeviceDataConverter metaModelConverter = metaModelFactory.getModelConverter(messageType);
		logForInvalidMessageType(metaModelConverter, messageType);
		return metaModelConverter != null ? metaModelConverter.createModel(deviceModel,input) : null;
	}

	private void logForInvalidMessageType(UplinkDeviceDataConverter metaModelConverter, String messageType) {
		if (metaModelConverter == null)
			logger.warn("Invalid Message Type identified in Data Frame : " + messageType);
	}

	private String findMessageTypeFromInput(byte[] input) {
		return "0x" + convertToHexValue(copyOfRange(input, 25, 27));
	}

}


public class TrackerStatus {

	private final AlarmStatus emergency;
	private final AlarmStatus accelaration;
	private final AlarmStatus speeding;
	private final AlarmStatus gpsFailure;
	private final AlarmStatus lowPower;
	private final AlarmStatus powerFailure;
	private final AlarmStatus crashAlarm;
	private final AlarmStatus geoFenceBreach;
	private final AlarmStatus towAlarm;
	private final AlarmStatus powerCut;

	public TrackerStatus(AlarmStatus emergency, AlarmStatus accelaration, AlarmStatus speeding, AlarmStatus gpsFailure,
	AlarmStatus lowPower, AlarmStatus powerFailure, AlarmStatus crashAlarm, AlarmStatus geoFenceBreach,
	AlarmStatus towAlarm, AlarmStatus powerCut) {
		super();
		this.emergency = emergency;
		this.accelaration = accelaration;
		this.speeding = speeding;
		this.gpsFailure = gpsFailure;
		this.lowPower = lowPower;
		this.powerFailure = powerFailure;
		this.crashAlarm = crashAlarm;
		this.geoFenceBreach = geoFenceBreach;
		this.towAlarm = towAlarm;
		this.powerCut = powerCut;
	}

	public AlarmStatus getEmergency() {
		return emergency;
	}

	public AlarmStatus getAccelaration() {
		return accelaration;
	}

	public AlarmStatus getSpeeding() {
		return speeding;
	}

	public AlarmStatus getGpsFailure() {
		return gpsFailure;
	}

	public AlarmStatus getLowPower() {
		return lowPower;
	}

	public AlarmStatus getPowerFailure() {
		return powerFailure;
	}

	public AlarmStatus getCrashAlarm() {
		return crashAlarm;
	}

	public AlarmStatus getGeoFenceBreach() {
		return geoFenceBreach;
	}

	public AlarmStatus getTowAlarm() {
		return towAlarm;
	}

	public AlarmStatus getPowerCut() {
		return powerCut;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accelaration == null) ? 0 : accelaration.hashCode());
		result = prime * result + ((crashAlarm == null) ? 0 : crashAlarm.hashCode());
		result = prime * result + ((emergency == null) ? 0 : emergency.hashCode());
		result = prime * result + ((geoFenceBreach == null) ? 0 : geoFenceBreach.hashCode());
		result = prime * result + ((gpsFailure == null) ? 0 : gpsFailure.hashCode());
		result = prime * result + ((lowPower == null) ? 0 : lowPower.hashCode());
		result = prime * result + ((powerCut == null) ? 0 : powerCut.hashCode());
		result = prime * result + ((powerFailure == null) ? 0 : powerFailure.hashCode());
		result = prime * result + ((speeding == null) ? 0 : speeding.hashCode());
		result = prime * result + ((towAlarm == null) ? 0 : towAlarm.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrackerStatus other = (TrackerStatus) obj;
		if (accelaration != other.accelaration)
			return false;
		if (crashAlarm != other.crashAlarm)
			return false;
		if (emergency != other.emergency)
			return false;
		if (geoFenceBreach != other.geoFenceBreach)
			return false;
		if (gpsFailure != other.gpsFailure)
			return false;
		if (lowPower != other.lowPower)
			return false;
		if (powerCut != other.powerCut)
			return false;
		if (powerFailure != other.powerFailure)
			return false;
		if (speeding != other.speeding)
			return false;
		if (towAlarm != other.towAlarm)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TrackerStatus [emergency=" + emergency + ", accelaration=" + accelaration + ", speeding=" + speeding
		+ ", gpsFailure=" + gpsFailure + ", lowPower=" + lowPower + ", powerFailure=" + powerFailure
		+ ", crashAlarm=" + crashAlarm + ", geoFenceBreach=" + geoFenceBreach + ", towAlarm=" + towAlarm
		+ ", powerCut=" + powerCut + "]";
	}

	public enum AlarmStatus {
		ON, OFF
	}
}

public class TrackerNotification implements DeviceData {

	public static final String TRACKER_NOTIF = "Tracker Notification";
	private final NotificationType notificationType;
	private final int packageItem;
	private final List<TrackerInfo> packageData;

	public TrackerNotification(NotificationType notificationType, int packageItem, List<TrackerInfo> packageData) {
		super();
		this.notificationType = notificationType;
		this.packageItem = packageItem;
		this.packageData = packageData;
	}

	public NotificationType getNotificationType() {
		return notificationType;
	}

	public int getPackageItem() {
		return packageItem;
	}

	public List<TrackerInfo> getPackageData() {
		return packageData;
	}

	@Override
	public String getDeviceDataInformation() {
		return TRACKER_NOTIF;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((notificationType == null) ? 0 : notificationType.hashCode());
		result = prime * result + ((packageData == null) ? 0 : packageData.hashCode());
		result = prime * result + packageItem;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrackerNotification other = (TrackerNotification) obj;
		if (notificationType != other.notificationType)
			return false;
		if (packageData == null) {
			if (other.packageData != null)
				return false;
		} else if (!packageData.equals(other.packageData))
			return false;
		if (packageItem != other.packageItem)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TrackerNotification [notificationType=" + notificationType + ", packageItem=" + packageItem
		+ ", packageData=" + packageData + "]";
	}

	public enum NotificationType {
		IPCONNECT,REGULAR_DATA, HISTORY_DATA,ALERT;
	}

}

public class TrackerInfo implements DeviceData {

	public static final String TRACKER_INFO = "Tracker Information";

	private final GPSInfo gpsInformation;
	private final int batteryPercentage;
	private final TrackerStatus trackerStatus;

	public TrackerInfo(GPSInfo gpsInformation, int batteryPercentage, TrackerStatus trackerStatus) {
		super();
		this.gpsInformation = gpsInformation;
		this.batteryPercentage = batteryPercentage;
		this.trackerStatus = trackerStatus;
	}

	public GPSInfo getGpsInformation() {
		return gpsInformation;
	}

	public int getBatteryPercentage() {
		return batteryPercentage;
	}

	public TrackerStatus getTrackerStatus() {
		return trackerStatus;
	}

	@Override
	public String getDeviceDataInformation() {
		return TRACKER_INFO;
	}

	@Override
	public String toString() {
		return "TrackerInfo [gpsInformation=" + gpsInformation + ", batteryPercentage=" + batteryPercentage
		+ ", trackerStatus=" + trackerStatus + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + batteryPercentage;
		result = prime * result + ((gpsInformation == null) ? 0 : gpsInformation.hashCode());
		result = prime * result + ((trackerStatus == null) ? 0 : trackerStatus.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrackerInfo other = (TrackerInfo) obj;
		if (batteryPercentage != other.batteryPercentage)
			return false;
		if (gpsInformation == null) {
			if (other.gpsInformation != null)
				return false;
		} else if (!gpsInformation.equals(other.gpsInformation))
			return false;
		if (trackerStatus == null) {
			if (other.trackerStatus != null)
				return false;
		} else if (!trackerStatus.equals(other.trackerStatus))
			return false;
		return true;
	}

}



public class GPSInfo implements DeviceData {

	public static final String GPS_INFO = "GPS Information";

	private final String date;
	private final String time;
	private final double latitude;
	private final double longitude;
	private final double speed;
	private final int direction;

	public GPSInfo(String date, String time, double latitude, double longitude, double speed, int direction) {
		super();
		this.date = date;
		this.time = time;
		this.latitude = latitude;
		this.longitude = longitude;
		this.speed = speed;
		this.direction = direction;
	}

	public String getDate() {
		return date;
	}

	public String getTime() {
		return time;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getSpeed() {
		return speed;
	}

	public int getDirection() {
		return direction;
	}

	@Override
	public String getDeviceDataInformation() {
		return GPS_INFO;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + direction;
		result = prime * result + (int) (latitude ^ (latitude >>> 32));
		result = prime * result + (int) (longitude ^ (longitude >>> 32));
		result = prime * result + speed;
		result = prime * result + ((time == null) ? 0 : time.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GPSInfo other = (GPSInfo) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (direction != other.direction)
			return false;
		if (latitude != other.latitude)
			return false;
		if (longitude != other.longitude)
			return false;
		if (speed != other.speed)
			return false;
		if (time == null) {
			if (other.time != null)
				return false;
		} else if (!time.equals(other.time))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GPSInfo [date=" + date + ", time=" + time + ", latitude=" + latitude + ", longitude=" + longitude+ ", speed=" + speed + ", direction=" + direction + "]";
	}

}

public class MMICRCAlgorithm {

	final short FCS_START = (short) 0xffff;

	static def  FCSTAB = [
		(short) 0X0000,
		(short) 0X1189,
		(short) 0X2312,
		(short) 0X329B,
		(short) 0X4624,
		(short) 0X57AD,
		(short) 0X6536,
		(short) 0X74BF,
		(short) 0X8C48,
		(short) 0X9DC1,
		(short) 0XAF5A,
		(short) 0XBED3,
		(short) 0XCA6C,
		(short) 0XDBE5,
		(short) 0XE97E,
		(short) 0XF8F7,
		(short) 0X1081,
		(short) 0X0108,
		(short) 0X3393,
		(short) 0X221A,
		(short) 0X56A5,
		(short) 0X472C,
		(short) 0X75B7,
		(short) 0X643E,
		(short) 0X9CC9,
		(short) 0X8D40,
		(short) 0XBFDB,
		(short) 0XAE52,
		(short) 0XDAED,
		(short) 0XCB64,
		(short) 0XF9FF,
		(short) 0XE876,
		(short) 0X2102,
		(short) 0X308B,
		(short) 0X0210,
		(short) 0X1399,
		(short) 0X6726,
		(short) 0X76AF,
		(short) 0X4434,
		(short) 0X55BD,
		(short) 0XAD4A,
		(short) 0XBCC3,
		(short) 0X8E58,
		(short) 0X9FD1,
		(short) 0XEB6E,
		(short) 0XFAE7,
		(short) 0XC87C,
		(short) 0XD9F5,
		(short) 0X3183,
		(short) 0X200A,
		(short) 0X1291,
		(short) 0X0318,
		(short) 0X77A7,
		(short) 0X662E,
		(short) 0X54B5,
		(short) 0X453C,
		(short) 0XBDCB,
		(short) 0XAC42,
		(short) 0X9ED9,
		(short) 0X8F50,
		(short) 0XFBEF,
		(short) 0XEA66,
		(short) 0XD8FD,
		(short) 0XC974,
		(short) 0X4204,
		(short) 0X538D,
		(short) 0X6116,
		(short) 0X709F,
		(short) 0X0420,
		(short) 0X15A9,
		(short) 0X2732,
		(short) 0X36BB,
		(short) 0XCE4C,
		(short) 0XDFC5,
		(short) 0XED5E,
		(short) 0XFCD7,
		(short) 0X8868,
		(short) 0X99E1,
		(short) 0XAB7A,
		(short) 0XBAF3,
		(short) 0X5285,
		(short) 0X430C,
		(short) 0X7197,
		(short) 0X601E,
		(short) 0X14A1,
		(short) 0X0528,
		(short) 0X37B3,
		(short) 0X263A,
		(short) 0XDECD,
		(short) 0XCF44,
		(short) 0XFDDF,
		(short) 0XEC56,
		(short) 0X98E9,
		(short) 0X8960,
		(short) 0XBBFB,
		(short) 0XAA72,
		(short) 0X6306,
		(short) 0X728F,
		(short) 0X4014,
		(short) 0X519D,
		(short) 0X2522,
		(short) 0X34AB,
		(short) 0X0630,
		(short) 0X17B9,
		(short) 0XEF4E,
		(short) 0XFEC7,
		(short) 0XCC5C,
		(short) 0XDDD5,
		(short) 0XA96A,
		(short) 0XB8E3,
		(short) 0X8A78,
		(short) 0X9BF1,
		(short) 0X7387,
		(short) 0X620E,
		(short) 0X5095,
		(short) 0X411C,
		(short) 0X35A3,
		(short) 0X242A,
		(short) 0X16B1,
		(short) 0X0738,
		(short) 0XFFCF,
		(short) 0XEE46,
		(short) 0XDCDD,
		(short) 0XCD54,
		(short) 0XB9EB,
		(short) 0XA862,
		(short) 0X9AF9,
		(short) 0X8B70,
		(short) 0X8408,
		(short) 0X9581,
		(short) 0XA71A,
		(short) 0XB693,
		(short) 0XC22C,
		(short) 0XD3A5,
		(short) 0XE13E,
		(short) 0XF0B7,
		(short) 0X0840,
		(short) 0X19C9,
		(short) 0X2B52,
		(short) 0X3ADB,
		(short) 0X4E64,
		(short) 0X5FED,
		(short) 0X6D76,
		(short) 0X7CFF,
		(short) 0X9489,
		(short) 0X8500,
		(short) 0XB79B,
		(short) 0XA612,
		(short) 0XD2AD,
		(short) 0XC324,
		(short) 0XF1BF,
		(short) 0XE036,
		(short) 0X18C1,
		(short) 0X0948,
		(short) 0X3BD3,
		(short) 0X2A5A,
		(short) 0X5EE5,
		(short) 0X4F6C,
		(short) 0X7DF7,
		(short) 0X6C7E,
		(short) 0XA50A,
		(short) 0XB483,
		(short) 0X8618,
		(short) 0X9791,
		(short) 0XE32E,
		(short) 0XF2A7,
		(short) 0XC03C,
		(short) 0XD1B5,
		(short) 0X2942,
		(short) 0X38CB,
		(short) 0X0A50,
		(short) 0X1BD9,
		(short) 0X6F66,
		(short) 0X7EEF,
		(short) 0X4C74,
		(short) 0X5DFD,
		(short) 0XB58B,
		(short) 0XA402,
		(short) 0X9699,
		(short) 0X8710,
		(short) 0XF3AF,
		(short) 0XE226,
		(short) 0XD0BD,
		(short) 0XC134,
		(short) 0X39C3,
		(short) 0X284A,
		(short) 0X1AD1,
		(short) 0X0B58,
		(short) 0X7FE7,
		(short) 0X6E6E,
		(short) 0X5CF5,
		(short) 0X4D7C,
		(short) 0XC60C,
		(short) 0XD785,
		(short) 0XE51E,
		(short) 0XF497,
		(short) 0X8028,
		(short) 0X91A1,
		(short) 0XA33A,
		(short) 0XB2B3,
		(short) 0X4A44,
		(short) 0X5BCD,
		(short) 0X6956,
		(short) 0X78DF,
		(short) 0X0C60,
		(short) 0X1DE9,
		(short) 0X2F72,
		(short) 0X3EFB,
		(short) 0XD68D,
		(short) 0XC704,
		(short) 0XF59F,
		(short) 0XE416,
		(short) 0X90A9,
		(short) 0X8120,
		(short) 0XB3BB,
		(short) 0XA232,
		(short) 0X5AC5,
		(short) 0X4B4C,
		(short) 0X79D7,
		(short) 0X685E,
		(short) 0X1CE1,
		(short) 0X0D68,
		(short) 0X3FF3,
		(short) 0X2E7A,
		(short) 0XE70E,
		(short) 0XF687,
		(short) 0XC41C,
		(short) 0XD595,
		(short) 0XA12A,
		(short) 0XB0A3,
		(short) 0X8238,
		(short) 0X93B1,
		(short) 0X6B46,
		(short) 0X7ACF,
		(short) 0X4854,
		(short) 0X59DD,
		(short) 0X2D62,
		(short) 0X3CEB,
		(short) 0X0E70,
		(short) 0X1FF9,
		(short) 0XF78F,
		(short) 0XE606,
		(short) 0XD49D,
		(short) 0XC514,
		(short) 0XB1AB,
		(short) 0XA022,
		(short) 0X92B9,
		(short) 0X8330,
		(short) 0X7BC7,
		(short) 0X6A4E,
		(short) 0X58D5,
		(short) 0X495C,
		(short) 0X3DE3,
		(short) 0X2C6A,
		(short) 0X1EF1,
		(short) 0X0F78 ] as short[];

	public boolean isCRCEqual(int crcInPayload, byte[] dataBytes) {
		String hexaCRC = calculateCRC(dataBytes);
		int crc = Integer.parseInt(hexaCRC, 16);
		return crcInPayload == crc ? true : false;
	}

	public String calculateCRC(byte[] dataBytes) {
		String reverseCrc = getCRC(dataBytes);
		return reverseCrc.substring(2, reverseCrc.length()) + reverseCrc.substring(0, 2);
	}

	public static String getCRC(byte[] pData) {
		short fcs = (short) 0xffff;
		for (int i = 0; i < pData.length; i++) {
			fcs = (short) (((fcs & 0xFFFF) >>> 8) ^ FCSTAB[(fcs ^ pData[i]) & 0xff]);
		}
		short a = (short) ~fcs;
		String hex = Integer.toHexString(a & 0xffff);
		int len = hex.length();
		if (len == 3) {
			hex = "0" + hex;
		} else if (len == 2) {
			hex = "00" + hex;
		} else if (len == 1) {
			hex = "000" + hex;
		}
		return hex;
	}

}


public class CRCMisMatchException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CRCMisMatchException(String message) {
		super(message);
	}

}

public abstract class AbstractMMIMessageConverter implements UplinkDeviceDataConverter {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final MMICRCAlgorithm mmicrcAlgorithm;

	public AbstractMMIMessageConverter(MMICRCAlgorithm mmicrcAlgorithm) {
		super();
		this.mmicrcAlgorithm = mmicrcAlgorithm;
	}

	@Override
	public DeviceInfo createModel(DeviceModel deviceModel,byte[] input) {
		performCRCCheckOperation(input);
		def deviceIdInByte = copyOfRange(input, 5, 25);
		deviceIdInByte = truncateEmptyBytes(deviceIdInByte);
		final String deviceId = convertBytesToASCIIString(deviceIdInByte, 0, deviceIdInByte.length);
		logger.debug("Identified Device Id is " + deviceId);
		final DeviceInfo dataModel = new DeviceInfo(new DeviceImpl(deviceModel.getManufacturer(), deviceModel.getModelId(),deviceModel.getVersion(), deviceId), ("0x" + convertToHexValue(copyOfRange(input, 25, 27))),
				input);
		addMessageSpecificParameters(dataModel.getDeviceData(), input);
		return dataModel;
	}

	private void performCRCCheckOperation(byte[] input) {
		def crcBytes = copyOfRange(input, input.length - 4, input.length - 2);
		int crcValue = parseInt(calculateUnsignedDecimalValFromSignedBytes(crcBytes));
		def crcTobeCalculated = copyOfRange(input, 0, input.length - 4);
		boolean isCRCEqual = mmicrcAlgorithm.isCRCEqual(crcValue, crcTobeCalculated);
		if (!isCRCEqual) {
			throw new CRCMisMatchException("CRC calculated from payload and CRC in payload are not same");
		}
	}

	abstract protected void addMessageSpecificParameters(Map<String, DeviceData> deviceData, byte[] rawData);
}

public class TrackerInfoCreator {

	public TrackerInfoCreator(){
		super();
	}

	public TrackerInfo constructTrackerInfo(byte[] rawBytes) {
		String date = calculateDate(rawBytes);
		String time = calculateTime(rawBytes);
		double latitude = calculateDecimalFormatLatitudeFromMilliSecondsFormat(rawBytes);
		double longitude = calculateDecimalFormatLongitudeFromMilliSecondsFormat(rawBytes);
		double speed = calculateSpeedInKmph(rawBytes);
		int direction = calculateDirection(rawBytes);
		TrackerStatus trackerStatus = calculateTrackerStatus(rawBytes);
		TrackerInfo trackerInfo = new TrackerInfo(new GPSInfo(date, time, latitude, longitude, speed, direction),
				rawBytes[23], trackerStatus);
		return trackerInfo;
	}

	private String calculateDate(byte[] rawPayload) {
		return rawPayload[0] + "-" + rawPayload[1] + "-" + (2000 + rawPayload[2]);
	}

	private String calculateTime(byte[] rawPayload) {
		return rawPayload[3] + ":" + rawPayload[4] + ":" + (rawPayload[5]) + " GMT";
	}

	private double calculateDecimalFormatLatitudeFromMilliSecondsFormat(byte[] rawBytes) {
		byte[] latitude = copyOfRange(rawBytes, 6, 10);
		reverseArray(latitude);
		return parseLong(calculateUnsignedDecimalValFromSignedBytes(latitude))/3600000;
	}

	private double calculateDecimalFormatLongitudeFromMilliSecondsFormat(byte[] rawBytes) {
		byte[] longitude = copyOfRange(rawBytes, 10, 14);
		reverseArray(longitude);
		return parseLong(calculateUnsignedDecimalValFromSignedBytes(longitude))/3600000;
	}

	private double calculateSpeedInKmph(byte[] rawBytes) {
		byte[] speed = copyOfRange(rawBytes, 14, 16);
		reverseArray(speed);
		return parseInt(calculateUnsignedDecimalValFromSignedBytes(speed))*0.036;
	}

	private int calculateDirection(byte[] rawBytes) {
		byte[] speed = copyOfRange(rawBytes, 16, 18);
		reverseArray(speed);
		return parseInt(calculateUnsignedDecimalValFromSignedBytes(speed));
	}

	private TrackerStatus calculateTrackerStatus(byte[] rawBytes) {
		TrackerStatus.AlarmStatus emergency = checkBitValue(rawBytes[19], 6) ? TrackerStatus.AlarmStatus.ON : TrackerStatus.AlarmStatus.OFF;
		TrackerStatus.AlarmStatus accelaration = checkBitValue(rawBytes[19], 2) ? TrackerStatus.AlarmStatus.ON : TrackerStatus.AlarmStatus.OFF;
		TrackerStatus.AlarmStatus speeding = checkBitValue(rawBytes[20], 6) ? TrackerStatus.AlarmStatus.ON : TrackerStatus.AlarmStatus.OFF;
		TrackerStatus.AlarmStatus gpsFailure = checkBitValue(rawBytes[20], 0) ? TrackerStatus.AlarmStatus.ON : TrackerStatus.AlarmStatus.OFF;
		TrackerStatus.AlarmStatus lowPower = checkBitValue(rawBytes[21], 2) ? TrackerStatus.AlarmStatus.ON : TrackerStatus.AlarmStatus.OFF;
		TrackerStatus.AlarmStatus powerFailure = checkBitValue(rawBytes[21], 0) ? TrackerStatus.AlarmStatus.ON : TrackerStatus.AlarmStatus.OFF;
		TrackerStatus.AlarmStatus crashAlarm = checkBitValue(rawBytes[22], 6) ? TrackerStatus.AlarmStatus.ON : TrackerStatus.AlarmStatus.OFF;
		TrackerStatus.AlarmStatus geoFenceBreach = checkBitValue(rawBytes[22], 5) ? TrackerStatus.AlarmStatus.ON : TrackerStatus.AlarmStatus.OFF;
		TrackerStatus.AlarmStatus towAlarm = checkBitValue(rawBytes[22], 4) ? TrackerStatus.AlarmStatus.ON : TrackerStatus.AlarmStatus.OFF;
		TrackerStatus.AlarmStatus powerCut = checkBitValue(rawBytes[22], 0) ? TrackerStatus.AlarmStatus.ON : TrackerStatus.AlarmStatus.OFF;
		return new TrackerStatus(emergency, accelaration, speeding, gpsFailure, lowPower, powerFailure, crashAlarm,
				geoFenceBreach, towAlarm, powerCut);
	}

}

public class IPConnectMessageConverter extends AbstractMMIMessageConverter {

	private static final String MESSAGE_TYPE = "0x4001";
	private final TrackerInfoCreator trackerInfoCreator;

	public IPConnectMessageConverter(MMICRCAlgorithm mmicrcAlgorithm, TrackerInfoCreator trackerInfoCreator) {
		super(mmicrcAlgorithm);
		this.trackerInfoCreator = trackerInfoCreator;
	}

	public String getMessageType() {
		return MESSAGE_TYPE;
	}

	@Override
	protected void addMessageSpecificParameters(Map<String, DeviceData> deviceData, byte[] rawBytes) {
		rawBytes = copyOfRange(rawBytes, 27, 63);
		TrackerInfo trackerInfo = trackerInfoCreator.constructTrackerInfo(rawBytes);
		List<TrackerInfo> trackerInfos=new ArrayList<>();
		trackerInfos.add(trackerInfo);
		TrackerNotification trackerNotification=new TrackerNotification(IPCONNECT, 1, trackerInfos);
		deviceData.put(trackerNotification.getDeviceDataInformation(), trackerNotification);
	}

}

public class NotificationMessageConverter extends AbstractMMIMessageConverter {

	private static final String MESSAGE_TYPE = "0x4206";
	private final TrackerInfoCreator trackerInfoCreator;

	public NotificationMessageConverter(MMICRCAlgorithm mmicrcAlgorithm, TrackerInfoCreator trackerInfoCreator) {
		super(mmicrcAlgorithm);
		this.trackerInfoCreator = trackerInfoCreator;
	}

	@Override
	public String getMessageType() {
		return MESSAGE_TYPE;
	}

	@Override
	protected void addMessageSpecificParameters(Map<String, DeviceData> deviceData, byte[] rawData) {
		NotificationType notificationType = rawData[27] == 0 ? REGULAR_DATA : HISTORY_DATA;
		int totalPackage = rawData[28];
		List<TrackerInfo> trackerInfo = new ArrayList<>();
		for (int packageItemIndex = 0; packageItemIndex < totalPackage; packageItemIndex++) {
			int startIndex = 29 + packageItemIndex * 35;
			int endIndex = 29 + packageItemIndex * 35 + 36;
			byte[] trackerInfoBytes = copyOfRange(rawData, startIndex, endIndex);
			trackerInfo.add(trackerInfoCreator.constructTrackerInfo(trackerInfoBytes));
		}
		deviceData.put(TrackerNotification.TRACKER_NOTIF,
				new TrackerNotification(notificationType, totalPackage, trackerInfo));
	}

}


public class AlarmMessageConverter extends AbstractMMIMessageConverter {

	private static final String MESSAGE_TYPE = "0x4203";
	private final TrackerInfoCreator trackerInfoCreator;

	public AlarmMessageConverter(MMICRCAlgorithm mmicrcAlgorithm, TrackerInfoCreator trackerInfoCreator) {
		super(mmicrcAlgorithm);
		this.trackerInfoCreator = trackerInfoCreator;
	}

	@Override
	public String getMessageType() {
		return MESSAGE_TYPE;
	}

	@Override
	protected void addMessageSpecificParameters(Map<String, DeviceData> deviceData, byte[] rawData) {
		rawData = copyOfRange(rawData, 27, 63);
		List<TrackerInfo> trackerInfos = new ArrayList<>();
		TrackerInfo trackerInfo = trackerInfoCreator.constructTrackerInfo(rawData);
		trackerInfos.add(trackerInfo);
		deviceData.put(TrackerNotification.TRACKER_NOTIF,
				new TrackerNotification(ALERT, 1, trackerInfos));

	}

}


public class HeartBeatPackageConverter implements UplinkDeviceDataConverter {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String EXPECTED_MESSAGE_TYPE = "0x4003";

	private final MMICRCAlgorithm mmicrcAlgorithm;

	public HeartBeatPackageConverter(MMICRCAlgorithm mmicrcAlgorithm) {
		super();
		this.mmicrcAlgorithm = mmicrcAlgorithm;
	}

	@Override
	public String getMessageType() {
		return EXPECTED_MESSAGE_TYPE;
	}

	@Override
	public DeviceInfo createModel(DeviceModel deviceModel,byte[] input) {
		performCRCCheckOperation(input);
		def deviceIdInByte = copyOfRange(input, 5, 25);
		deviceIdInByte = truncateEmptyBytes(deviceIdInByte);
		final String deviceId = convertBytesToASCIIString(deviceIdInByte, 0, deviceIdInByte.length);
		logger.debug("Identified Device Id is " + deviceId);
		return new DeviceInfo(new DeviceImpl(deviceModel.getManufacturer(), deviceModel.getModelId(),deviceModel.getVersion(), deviceId), ("0x" + convertToHexValue(copyOfRange(input, 25, 27))), input);
	}

	private void performCRCCheckOperation(byte[] input) {
		def crcBytes = copyOfRange(input, input.length - 4, input.length - 2);
		int crcValue = parseInt(calculateUnsignedDecimalValFromSignedBytes(crcBytes));
		def crcTobeCalculated = copyOfRange(input, 0, input.length - 4);
		boolean isCRCEqual = mmicrcAlgorithm.isCRCEqual(crcValue, crcTobeCalculated);
		if (!isCRCEqual) {
			throw new CRCMisMatchException("CRC calculated from payload and crc in payload are not same");
		}
	}

}

public abstract class AcknowledgementMessageService implements MessageService {

	protected final TCPServerSocketWriter tcpServerSocketSender;
	protected final MMICRCAlgorithm mmicrcAlgorithm;

	public AcknowledgementMessageService(TCPServerSocketWriter tcpServerSocketSender,
	MMICRCAlgorithm mmicrcAlgorithm) {
		super();
		this.tcpServerSocketSender = tcpServerSocketSender;
		this.mmicrcAlgorithm = mmicrcAlgorithm;
	}

	@Override
	public DeviceDataDeliveryStatus executeService(DeviceInfo model) {
		tcpServerSocketSender.sendMessage(model.getDevice(), constructDeviceCommandMessage(model));
		return new DeviceDataDeliveryStatus();
	}

	protected abstract String getResponseProtocolType();

	protected abstract byte[] getEmptyCommandBytes();

	protected abstract void fillDeviceID(byte[] commandBytes, DeviceInfo deviceInfo);

	protected abstract void fillProtocolData(byte[] commandBytes, DeviceInfo deviceInfo);

	protected byte[] constructDeviceCommandMessage(DeviceInfo deviceInfo) {
		byte[] commandBytes = getEmptyCommandBytes();
		fillHeader(commandBytes);
		fillLengthOfMessage(commandBytes);
		fillProtocolVersion(commandBytes);
		fillDeviceID(commandBytes, deviceInfo);
		fillProtocolType(commandBytes);
		fillProtocolData(commandBytes, deviceInfo);
		fillCRC(commandBytes);
		fillTail(commandBytes);
		return commandBytes;
	}

	private void fillCRC(byte[] commandBytes) {
		def dataWithOutCRC = copyOfRange(commandBytes, 0, commandBytes.length - 4);
		String crc = mmicrcAlgorithm.calculateCRC(dataWithOutCRC);
		String leftHexVal = crc.substring(0, 2);
		String rightHexVal = crc.substring(2, crc.length());
		commandBytes[commandBytes.length - 4] = (byte) parseInt(leftHexVal, 16);
		commandBytes[commandBytes.length - 3] = (byte) parseInt(rightHexVal, 16);
	}

	private void fillTail(byte[] commandBytes) {
		commandBytes[commandBytes.length - 2] = 13;
		commandBytes[commandBytes.length - 1] = 10;
	}

	private void fillProtocolType(byte[] commandBytes) {
		commandBytes[25] = (byte) parseInt(getResponseProtocolType().substring(2, 4), 16);
		commandBytes[26] = (byte) parseInt(getResponseProtocolType().substring(4, 6), 16);
	}

	private void fillProtocolVersion(byte[] commandBytes) {
		commandBytes[4] = 1;
	}

	private void fillLengthOfMessage(byte[] commandBytes) {
		if (commandBytes.length < 255)
			commandBytes[2] = (byte) (commandBytes.length);
		else {
			// TODO: Logic for splitting length into Two bytes.
		}
	}

	private void fillHeader(byte[] commandBytes) {
		commandBytes[0] = 64;
		commandBytes[1] = 64;
	}

}


public abstract class AbstractAcknowledgeAndUplinkService extends AcknowledgementMessageService
implements UplinkMessageService {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public AbstractAcknowledgeAndUplinkService(TCPServerSocketWriter tcpServerSocketSender,
	MMICRCAlgorithm mmicrcAlgorithm) {
		super(tcpServerSocketSender, mmicrcAlgorithm);
	}

	@Override
	public DeviceDataDeliveryStatus executeService(DeviceInfo deviceInfo) {
		DeviceDataDeliveryStatus deviceDataDeliveryStatus = executeMessageSpecificLogic(deviceInfo);
		sendAcknowledgementToDevice(deviceInfo.getDevice(), deviceInfo);
		return deviceDataDeliveryStatus;
	}

	protected abstract DeviceDataDeliveryStatus executeMessageSpecificLogic(DeviceInfo model);

	private void sendAcknowledgementToDevice(Device device, DeviceInfo deviceInfo) {
		logger.debug("Sending Acknowledgement to Device " + device + " with response Protocol Type "
				+ getResponseProtocolType());
		super.executeService(deviceInfo);
	}

	@Override
	protected byte[] getEmptyCommandBytes() {
		return new byte[31];
	}

	@Override
	protected void fillDeviceID(byte[] acknowledgement, DeviceInfo deviceInfo) {
		def deviceIdInByte = copyOfRange(deviceInfo.getRawPayload(), 5, 25);
		for (int deviceIdByteIndex = 0; deviceIdByteIndex < deviceIdInByte.length; deviceIdByteIndex++)
			acknowledgement[deviceIdByteIndex + 5] = deviceIdInByte[deviceIdByteIndex];
	}

	@Override
	protected void fillProtocolData(byte[] commandBytes, DeviceInfo deviceInfo) {

	}

}


public class IPConnectMessageService extends AbstractAcknowledgeAndUplinkService {

	private static final String CONTAINER_NAME = "ipconnect";

	private static final String MESSAGE_TYPE = "0x4001";

	private static final String MESSAGE_ACK_TYPE = "0x8001";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final IOTPublisherService<DeviceInfo, DeviceDataDeliveryStatus> iotPublisherService;

	public IPConnectMessageService(TCPServerSocketWriter tcpServerSocketSender, MMICRCAlgorithm mmicrcAlgorithm,
	IOTPublisherService<DeviceInfo, DeviceDataDeliveryStatus> iotPublisherService) {
		super(tcpServerSocketSender, mmicrcAlgorithm);
		this.iotPublisherService = iotPublisherService;
	}

	public String getMessageType() {
		return MESSAGE_TYPE;
	}

	@Override
	protected DeviceDataDeliveryStatus executeMessageSpecificLogic(DeviceInfo model) {
		logger.debug("Received request is " + model);
		iotPublisherService.receiveDataFromDevice(model, CONTAINER_NAME);
		return new DeviceDataDeliveryStatus();
	}

	@Override
	protected String getResponseProtocolType() {
		return MESSAGE_ACK_TYPE;
	}

}

public class NotificationMessageService implements UplinkMessageService {

	private static final String MESSAGE_TYPE = "0x4206";
	private static final String CONTAINER_NAME = "notification";
	private final IOTPublisherService<DeviceInfo, DeviceDataDeliveryStatus> iotPublisherService;
	private final LiveLogger liveLogger;

	public NotificationMessageService(IOTPublisherService<DeviceInfo, DeviceDataDeliveryStatus> iotPublisherService,LiveLogger liveLogger) {
		super();
		this.iotPublisherService = iotPublisherService;
		this.liveLogger=liveLogger;
	}

	@Override
	public String getMessageType() {
		return MESSAGE_TYPE;
	}

	@Override
	public DeviceDataDeliveryStatus executeService(DeviceInfo deviceInfo) {
		liveLogger.log(deviceInfo);
		iotPublisherService.receiveDataFromDevice(deviceInfo, CONTAINER_NAME);
		return new DeviceDataDeliveryStatus();
	}

}

public class AlarmMessageService implements UplinkMessageService {

	private static final String MESSAGE_TYPE = "0x4203";
	private static final String CONTAINER_NAME = "alarm";
	private final IOTPublisherService<DeviceInfo, DeviceDataDeliveryStatus> iotPublisherService;
	private final LiveLogger liveLogger;

	public AlarmMessageService(IOTPublisherService<DeviceInfo, DeviceDataDeliveryStatus> iotPublisherService,LiveLogger liveLogger) {
		super();
		this.iotPublisherService = iotPublisherService;
		this.liveLogger=liveLogger;
	}

	@Override
	public String getMessageType() {
		return MESSAGE_TYPE;
	}

	@Override
	public DeviceDataDeliveryStatus executeService(DeviceInfo deviceInfo) {
		liveLogger.log(deviceInfo);
		iotPublisherService.receiveDataFromDevice(deviceInfo, CONTAINER_NAME);
		return new DeviceDataDeliveryStatus();
	}

}

public class HeartBeatMessageService extends AcknowledgementMessageService {

	private static final String MESSAGE_TYPE = "0x4003";
	private static final String MESSAGE_ACK_TYPE = "0x8003";

	public HeartBeatMessageService(TCPServerSocketWriter tcpServerSocketSender, MMICRCAlgorithm mmicrcAlgorithm) {
		super(tcpServerSocketSender, mmicrcAlgorithm);
	}

	@Override
	public String getMessageType() {
		return MESSAGE_TYPE;
	}

	@Override
	protected String getResponseProtocolType() {
		return MESSAGE_ACK_TYPE;
	}

	@Override
	protected byte[] getEmptyCommandBytes() {
		return new byte[31];
	}

	@Override
	protected void fillDeviceID(byte[] commandBytes, DeviceInfo deviceInfo) {
		def deviceIdInByte = copyOfRange(deviceInfo.getRawPayload(), 5, 25);
		for (int deviceIdByteIndex = 0; deviceIdByteIndex < deviceIdInByte.length; deviceIdByteIndex++)
			commandBytes[deviceIdByteIndex + 5] = deviceIdInByte[deviceIdByteIndex];

	}

	@Override
	protected void fillProtocolData(byte[] commandBytes, DeviceInfo deviceInfo) {

	}

}

public class DeviceSettings implements DeviceData{
	public static final String SETTINGS = "settings";
	private final int notifInterval;

	public DeviceSettings(int notifInterval) {
		super();
		this.notifInterval = notifInterval;
	}

	public int getNotifInterval() {
		return notifInterval;
	}

	@Override
	public String getDeviceDataInformation() {
		return SETTINGS;
	}

}

public class SettingsDownlinkConverter implements DownlinkDeviceDataConverter{

	private static final String MESSAGE_TYPE = "0x5101";
	private  final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public String getMessageType() {
		return MESSAGE_TYPE;
	}

	@Override
	public DeviceInfo createModel(DeviceModel deviceModel, JsonObject downlinkData) {
		JsonObject deviceJson = (JsonObject) downlinkData.get(DEVICE_KEY);
		JsonObject settingsJson = (JsonObject) downlinkData.get(DeviceSettings.SETTINGS);
		Gson gson = new Gson();
		Device device = gson.fromJson(deviceJson, DeviceImpl.class);
		DeviceSettings settings=gson.fromJson(settingsJson,DeviceSettings.class);
		byte[] rawPayload=getBinaryPayloadFromSettings(settings);
		logger.debug("Binary Settings payload used to send towards device is "+convertArrayOfByteToString(rawPayload));
		return new DeviceInfo(device, getMessageType(), rawPayload);
	}

	private byte[] getBinaryPayloadFromSettings(DeviceSettings settings){
		byte[] settingsBytes=new byte[6];
		settingsBytes[0] = (byte) parseInt("0x0140".substring(2, 4), 16);
		settingsBytes[1] = (byte) parseInt("0x0140".substring(4, 6), 16);
		settingsBytes[2]=2;
		//TODO : This logic needs to be improved to split int value to 2 bytes.
		settingsBytes[4]=settings.getNotifInterval();
		settingsBytes[5]=0;
		return settingsBytes;
	}

}


public abstract class AbstractDownlinkService implements DownlinkMessageService {

	private static final int HEAD = 2,LENGTH = 2,PROTO_VER = 1,DEVICE_ID = 20,MESSAGE_TYPE = 2,CRC = 2,TAIL = 2;

	protected final TCPServerSocketWriter tcpServerSocketSender;
	protected final MMICRCAlgorithm mmicrcAlgorithm;

	public AbstractDownlinkService(TCPServerSocketWriter tcpServerSocketSender,
	MMICRCAlgorithm mmicrcAlgorithm) {
		super();
		this.tcpServerSocketSender = tcpServerSocketSender;
		this.mmicrcAlgorithm = mmicrcAlgorithm;
	}

	@Override
	public DeviceDataDeliveryStatus executeService(DeviceInfo model) {
		tcpServerSocketSender.sendMessage(model.getDevice(), constructDeviceCommandMessage(model.getRawPayload(),model.getDevice().getDeviceId()));
		return new DeviceDataDeliveryStatus();
	}


	protected byte[] constructDeviceCommandMessage(byte[] dataPayload,String deviceId) {
		int payloadLength=dataPayload==null?0:dataPayload.length;
		int commandByteLength=HEAD+LENGTH+PROTO_VER+DEVICE_ID+MESSAGE_TYPE+payloadLength+CRC+TAIL;
		byte[] commandBytes=new byte[commandByteLength];
		fillHeader(commandBytes);
		fillLengthOfMessage(commandBytes);
		fillProtocolVersion(commandBytes);
		fillDeviceID(commandBytes, deviceId);
		fillProtocolType(commandBytes);
		fillProtocolData(commandBytes, dataPayload);
		fillCRC(commandBytes);
		fillTail(commandBytes);
		return commandBytes;
	}

	private void fillTail(byte[] commandBytes) {
		commandBytes[commandBytes.length - 2] = 13;
		commandBytes[commandBytes.length - 1] = 10;
	}

	private void fillCRC(byte[] commandBytes) {
		def dataWithOutCRC = copyOfRange(commandBytes, 0, commandBytes.length - 4);
		String crc = mmicrcAlgorithm.calculateCRC(dataWithOutCRC);
		String leftHexVal = crc.substring(0, 2);
		String rightHexVal = crc.substring(2, crc.length());
		commandBytes[commandBytes.length - 4] = (byte) parseInt(leftHexVal, 16);
		commandBytes[commandBytes.length - 3] = (byte) parseInt(rightHexVal, 16);
	}

	private void fillProtocolData(byte[] commandBytes, byte[] dataPayload){
		if(dataPayload==null)
			return;
		for(int deviceIdByteIndex=0;deviceIdByteIndex<dataPayload.length;deviceIdByteIndex++)
			commandBytes[deviceIdByteIndex+27]=dataPayload[deviceIdByteIndex];
	}

	private void fillProtocolType(byte[] commandBytes) {
		commandBytes[25] = (byte) parseInt(getMessageType().substring(2, 4), 16);
		commandBytes[26] = (byte) parseInt(getMessageType().substring(4, 6), 16);
	}

	private void fillDeviceID(byte[] commandBytes, String deviceId){
		byte[] deviceIdBytes=deviceId.getBytes();
		if(deviceIdBytes.length>20)
			throw new InvalidDeviceId("Device ID length is more than 20 characters");
		for(int deviceIdByteIndex=0;deviceIdByteIndex<deviceIdBytes.length;deviceIdByteIndex++)
			commandBytes[deviceIdByteIndex + 5]=deviceIdBytes[deviceIdByteIndex]
	}

	private void fillProtocolVersion(byte[] commandBytes) {
		commandBytes[4] = 1;
	}

	private void fillLengthOfMessage(byte[] commandBytes) {
		if (commandBytes.length < 255)
			commandBytes[2] = (byte) (commandBytes.length);
		else {
			// TODO: Logic for splitting length into Two bytes.
		}
	}

	private void fillHeader(byte[] commandBytes) {
		commandBytes[0] = 64;
		commandBytes[1] = 64;
	}

	public class InvalidDeviceId extends RuntimeException{

		public InvalidDeviceId(String message) {
			super(message);
		}
	}

}

public class SettingsDownlinkService extends AbstractDownlinkService{

	private static final String MESSAGE_TYPE = "0x5101";

	public SettingsDownlinkService(TCPServerSocketWriter tcpServerSocketSender,
	MMICRCAlgorithm mmicrcAlgorithm) {
		super(tcpServerSocketSender,mmicrcAlgorithm);
	}

	@Override
	public String getMessageType() {
		return MESSAGE_TYPE;
	}

	@Override
	public DeviceDataDeliveryStatus executeService(DeviceInfo model) {
		//TODO: This code need to be improved according to protocol document 4.4 Section - to handle data limit size grater than 800 bytes.
		byte[] dataPayload=model.getRawPayload();
		byte[] setCommandPayload=new byte[2+1+dataPayload.length];
		setCommandPayload[0]=100;//TODO: This should be random generated value
		setCommandPayload[2]=1;
		for(int dataPayloadIndex=0;dataPayloadIndex<dataPayload.length;dataPayloadIndex++)
			setCommandPayload[3+dataPayloadIndex]=dataPayload[dataPayloadIndex];
		tcpServerSocketSender.sendMessage(model.getDevice(), constructDeviceCommandMessage(setCommandPayload,model.getDevice().getDeviceId()));
		return new DeviceDataDeliveryStatus();
	}


}
