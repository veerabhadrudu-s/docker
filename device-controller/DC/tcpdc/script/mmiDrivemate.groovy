/**
 * 
 */
package com.hpe.iot.dc.mmi.drivemate


import static com.handson.iot.dc.util.DataParserUtility.calculateUnsignedDecimalValFromSignedBytes

import java.nio.channels.SocketChannel

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.hpe.iot.dc.model.Device
import com.hpe.iot.dc.model.DeviceData
import com.hpe.iot.dc.model.DeviceDataDeliveryStatus
import com.hpe.iot.dc.model.DeviceImpl
import com.hpe.iot.dc.model.DeviceInfo
import com.hpe.iot.dc.model.DeviceModel
import com.hpe.iot.dc.northbound.service.inflow.IOTPublisherService
import com.hpe.iot.dc.service.MessageService
import com.hpe.iot.dc.southbound.converter.inflow.UplinkDeviceDataConverter
import com.hpe.iot.dc.southbound.converter.inflow.factory.UplinkDeviceDataConverterFactory
import com.hpe.iot.dc.southbound.converter.inflow.session.SessionBasedUplinkDeviceDataConverter
import com.hpe.iot.dc.southbound.service.inflow.UplinkMessageService
import com.hpe.iot.dc.southbound.transformer.inflow.session.SessionBasedUplinkDataModelTransformer
import com.hpe.iot.dc.tcp.southbound.model.ProcessingTaskType
import com.hpe.iot.dc.tcp.southbound.model.ServerSocketToDeviceModel
import com.hpe.iot.dc.tcp.southbound.model.TCPOptions
import com.hpe.iot.dc.tcp.southbound.service.inflow.session.DeviceClientSocketExtractor
import com.hpe.iot.dc.tcp.southbound.service.outflow.TCPServerSocketWriter
import com.hpe.iot.dc.tcp.southbound.socketpool.ClientSocketDeviceReader
import com.handson.iot.dc.util.DataParserUtility
import com.handson.iot.dc.util.UtilityLogger

/**
 * @author sveera
 *
 */

public class DrivatemateConstants{
	public static final int handShakeMsgLen=17;
	public static final String HAND_SHAKE = "HandShake"
	public static final String NOTIFICATION="notification"
}

public class MMIDrivemateServerSocketToDeviceModel implements ServerSocketToDeviceModel{

	public String getManufacturer(){
		return "MMI";
	}

	public String getModelId(){
		return "Drivemate";
	}
	
	public String getVersion() {
		return "2.0"
	}

	public String getBoundLocalAddress(){
		return "10.3.239.75";
	}

	public int getPortNumber(){
		return 3004;
	}

	public String getDescription() {
		return "This is a OBD type vehicle tracking plug & sense connector.";
	}

	@Override
	public TCPOptions getTCPOptions() {
		return new TCPOptions(){
					@Override
					public ProcessingTaskType getProcessingTaskType() {
						return ProcessingTaskType.SOCKET_SESSION_BASED_DATA_PROCESSING;
					}

					@Override
					public int getBufferCapacity() {
						return 2048;
					}

					@Override
					public int getSocketBacklogCount() {
						return 10000;
					}
				};
	}
}


public class MMIDrivemateDeviceClientSocketExtractor implements DeviceClientSocketExtractor{

	private static final int GPS_PAYLOAD_LEN = 15;

	@Override
	public Device extractConnectedDevice(byte[] clientSocketData, SocketChannel socketChannel, DeviceModel deviceModel,
			ClientSocketDeviceReader clientSocketDeviceReader) {
		Device device=clientSocketDeviceReader.getDevice(socketChannel);
		return device!=null?device:extractDeviceFromPayload(clientSocketData,deviceModel);
	}

	private Device extractDeviceFromPayload(byte[] clientSocketData,DeviceModel deviceModel){
		UtilityLogger.logRawDataInDecimalFormat(clientSocketData,getClass());
		return clientSocketData!=null&&clientSocketData.length==DrivatemateConstants.handShakeMsgLen?
				new DeviceImpl(deviceModel.getManufacturer(), deviceModel.getModelId(),deviceModel.getVersion(),
				DataParserUtility.convertBytesToASCIIString(clientSocketData,2,GPS_PAYLOAD_LEN)):null;
	}
}

public class MMIDrivemateDataModelTransformer extends SessionBasedUplinkDataModelTransformer {

	private final byte[] header=[0, 0, 0, 0] as byte[];
	private final UplinkDeviceDataConverter uplinkDeviceDataConverter;

	public MMIDrivemateDataModelTransformer(UplinkDeviceDataConverterFactory uplinkDeviceDataConverterFactory) {
		super(uplinkDeviceDataConverterFactory);
		uplinkDeviceDataConverter=uplinkDeviceDataConverterFactory.getModelConverter(DrivatemateConstants.NOTIFICATION);
	}

	@Override
	protected List<DeviceInfo> convertToModelForDevice(Device device, byte[] input) {
		List<DeviceInfo> deviceInfo=new ArrayList<>();
		boolean isHandShakeMsgType=checkWhetherHandShakeMsgType(input);
		if(isHandShakeMsgType)
			deviceInfo.add(new DeviceInfo(device, DrivatemateConstants.HAND_SHAKE, input));
		else
			deviceInfo=handleForNonHandShakeMessageType(device,input);
		return deviceInfo;
	}

	private boolean checkWhetherHandShakeMsgType(byte[] input){
		return input!=null&&input.length==DrivatemateConstants.handShakeMsgLen?true:false;
	}

	private List<DeviceInfo> handleForNonHandShakeMessageType(Device device, byte[] input){
		List<DeviceInfo> deviceInfo=new ArrayList<>();
		int startingArrayIndex=0,endingArrayIndex;
		while(isInvalidData(input,startingArrayIndex)){
			endingArrayIndex=startingArrayIndex+12+Integer.parseInt(DataParserUtility.calculateUnsignedDecimalValFromSignedBytes(
					Arrays.copyOfRange(input,startingArrayIndex+4,startingArrayIndex+8)));
			deviceInfo.add(uplinkDeviceDataConverter.createModel(device,Arrays.copyOfRange(input,startingArrayIndex,endingArrayIndex)));
			startingArrayIndex=endingArrayIndex;
		}
		return deviceInfo;
	}

	private boolean isInvalidData(byte[] input,int dataArrayIndex){
		return input.length<=(dataArrayIndex+8)||
				(input.length<dataArrayIndex+12+
				Integer.parseInt(DataParserUtility.calculateUnsignedDecimalValFromSignedBytes(Arrays.copyOfRange(input,dataArrayIndex+4,dataArrayIndex+8))))?
				false:true;
	}
}


public class HandshakeMessageService implements MessageService{

	private final TCPServerSocketWriter tcpServerSocketWriter;

	private final byte[] ackMessage;

	public HandshakeMessageService(TCPServerSocketWriter tcpServerSocketWriter) {
		super();
		this.tcpServerSocketWriter = tcpServerSocketWriter;
		ackMessage=new byte[4];
		ackMessage[0]=1;
	}

	@Override
	public String getMessageType() {
		return DrivatemateConstants.HAND_SHAKE;
	}

	@Override
	public DeviceDataDeliveryStatus executeService(DeviceInfo deviceInfo) {
		tcpServerSocketWriter.sendMessage(deviceInfo.getDevice(),ackMessage);
		return new DeviceDataDeliveryStatus();
	}
}


public class NotificationRecord {

	private final String GPSDateTime;
	private final String Latitude;
	private final String Longitude;
	private final String Heading;
	private final int GPS_VSSSpeed;
	private final Map<String,Object> CustomInfo;
	public NotificationRecord(String gpsTimeStamp, String latitude, String longitude, String altitude,
	String angle, int visibleSatillites, int speed) {
		super();
		this.GPSDateTime = gpsTimeStamp;
		this.Latitude = latitude;
		this.Longitude = longitude;
		this.Heading = angle;
		this.GPS_VSSSpeed = speed;
		CustomInfo = new LinkedHashMap<>();
		CustomInfo.put("GPSSatelliteUsed",visibleSatillites);
		CustomInfo.put("GPSAltitude",altitude);
	}
	public String getGPSDateTime() {
		return GPSDateTime;
	}
	public String getLatitude() {
		return Latitude;
	}
	public String getLongitude() {
		return Longitude;
	}
	public String getHeading() {
		return Heading;
	}
	public int getGPS_VSSSpeed() {
		return GPS_VSSSpeed;
	}
	public Map<String, Object> getCustomInfo() {
		return CustomInfo;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((CustomInfo == null) ? 0 : CustomInfo.hashCode());
		result = prime * result + ((GPSDateTime == null) ? 0 : GPSDateTime.hashCode());
		result = prime * result + GPS_VSSSpeed;
		result = prime * result + ((Heading == null) ? 0 : Heading.hashCode());
		result = prime * result + ((Latitude == null) ? 0 : Latitude.hashCode());
		result = prime * result + ((Longitude == null) ? 0 : Longitude.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NotificationRecord other = (NotificationRecord) obj;
		if (CustomInfo == null) {
			if (other.CustomInfo != null)
				return false;
		} else if (!CustomInfo.equals(other.CustomInfo))
			return false;
		if (GPSDateTime == null) {
			if (other.GPSDateTime != null)
				return false;
		} else if (!GPSDateTime.equals(other.GPSDateTime))
			return false;
		if (GPS_VSSSpeed != other.GPS_VSSSpeed)
			return false;
		if (Heading == null) {
			if (other.Heading != null)
				return false;
		} else if (!Heading.equals(other.Heading))
			return false;
		if (Latitude == null) {
			if (other.Latitude != null)
				return false;
		} else if (!Latitude.equals(other.Latitude))
			return false;
		if (Longitude == null) {
			if (other.Longitude != null)
				return false;
		} else if (!Longitude.equals(other.Longitude))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "NotificationRecord [GPSDateTime=" + GPSDateTime + ", Latitude=" + Latitude + ", Longitude=" + Longitude	+ ", Heading=" + Heading + ", GPS_VSSSpeed=" + GPS_VSSSpeed +", CustomInfo=" + CustomInfo + "]";
	}
}

public class Notification implements DeviceData{

	private final int noOfRecords;
	private final List<NotificationRecord> notificationRecords;

	public Notification(int noOfRecords,List<NotificationRecord> notificationRecords) {
		super();
		this.notificationRecords = notificationRecords;
		this.noOfRecords=noOfRecords;
	}

	@Override
	public String getDeviceDataInformation() {
		return "notification";
	}

	public int getNoOfRecords() {
		return noOfRecords;
	}

	public List<NotificationRecord> getNotificationRecords() {
		return notificationRecords;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + noOfRecords;
		result = prime * result + ((notificationRecords == null) ? 0 : notificationRecords.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (getClass() != obj.getClass())
			return false;
		Notification other = (Notification) obj;
		if (noOfRecords != other.noOfRecords)
			return false;
		if (notificationRecords == null) {
			if (other.notificationRecords != null)
				return false;
		} else if (!notificationRecords.equals(other.notificationRecords))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Notification [noOfRecords=" + noOfRecords + ", notificationRecords=" + notificationRecords + "]";
	}
}

public class EventIdToNameMapper{

	private final Map<Integer,String> eventIdMap=new HashMap<>();

	public EventIdToNameMapper(){
		initEventMap();
	}

	private void initEventMap(){
		eventIdMap.put(0,"Regular Update");
		eventIdMap.put(1,"Ignition");
		eventIdMap.put(181,"GPS_PDOP");
		eventIdMap.put(182,"GPS_HDOP");
		eventIdMap.put(66,"MainPowerVoltage");
		eventIdMap.put(240,"Movement");
		eventIdMap.put(199,"Trip Distance (Km)");
		eventIdMap.put(241,"Active GSM operator");
		eventIdMap.put(24,"GPS speed (km/h)");
		eventIdMap.put(80,"Data mode");
		eventIdMap.put(21,"GSMSignalQuality");
		eventIdMap.put(200,"Deep sleep");
		eventIdMap.put(205,"GSM cell ID");
		eventIdMap.put(206,"GSM area code");
		eventIdMap.put(67,"BackupBatteryVoltage");
		eventIdMap.put(68,"Battery current (mA)");
		eventIdMap.put(16,"Total Distance (km)");
		eventIdMap.put(155,"Geofence zone 01");
		eventIdMap.put(156,"Geofence zone 02");
		eventIdMap.put(157,"Geofence zone 03");
		eventIdMap.put(158,"Geofence zone 04");
		eventIdMap.put(159,"Geofence zone 05");
		eventIdMap.put(175,"Auto Geofence");
		eventIdMap.put(250,"Trip");
		eventIdMap.put(255,"Over Speeding");
		eventIdMap.put(251,"VehicleIdleEventStatus");
		eventIdMap.put(253,"Green driving type");
		eventIdMap.put(252,"Unplug detection");
		eventIdMap.put(247,"Crash detection");
		eventIdMap.put(248,"Alarm");
		eventIdMap.put(254,"Green driving value");
		eventIdMap.put(249,"Jamming");
	}

	public String getEventName(int eventId){
		return eventIdMap.get(eventId);
	}
}


public class UplinkNotificationMessageConverter extends SessionBasedUplinkDeviceDataConverter{

	private static final int GPS_PAYLOAD_LEN = 15
	private static final int TIMESTAMP_PAYLOAD_LEN = 8
	private static final int PRIORITY_BYTE = 1

	private final EventIdToNameMapper eventIdToNameMapper;

	public UplinkNotificationMessageConverter(EventIdToNameMapper eventIdToNameMapper){
		this.eventIdToNameMapper=eventIdToNameMapper;
	}

	@Override
	public String getMessageType() {
		return "notification";
	}

	protected DeviceInfo createModel(Device device, byte[] input){
		DeviceInfo deviceInfo=new DeviceInfo(device, messageType, input)
		Map<String, DeviceData> deviceData=deviceInfo.getDeviceData();
		fillDeviceData(deviceData,input);
		return deviceInfo;
	}

	private void fillDeviceData(Map<String, DeviceData> deviceData,byte[] input){
		int noOfRecords=input[9];
		List<NotificationRecord> notificationRecords=new ArrayList<>(noOfRecords);
		Notification  notification=new Notification(noOfRecords,notificationRecords);
		deviceData.put(notification.getDeviceDataInformation(),notification);
		if(noOfRecords==0)
			return;

		handleForNonZeroRecords(noOfRecords,notificationRecords,input);
	}

	private void handleForNonZeroRecords(int noOfRecords,List<NotificationRecord> notificationRecords,byte[] input){
		int startingIndexOfRecord=0,endingIndexOfRecord=0;
		for(int recordCounter=1;recordCounter<=noOfRecords;recordCounter++){
			if(startingIndexOfRecord==0)
				startingIndexOfRecord=10;
			endingIndexOfRecord=calculateEndingIndex(startingIndexOfRecord,input);
			notificationRecords.add(createNotificationRecord(Arrays.copyOfRange(input,startingIndexOfRecord,endingIndexOfRecord+1)));
			startingIndexOfRecord=endingIndexOfRecord+1;
		}
	}

	private int calculateEndingIndex(int startingIndexOfRecord,byte[] input){
		int noOfIOEventsIndex=startingIndexOfRecord+TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+GPS_PAYLOAD_LEN+1;
		int noOfIOEvents=input[noOfIOEventsIndex];
		if(noOfIOEvents==0)
			return noOfIOEventsIndex;
		int endingIndexOfRecord=noOfIOEventsIndex;
		for(int ioEventCounterType=1;ioEventCounterType<=8;ioEventCounterType=ioEventCounterType*2)
			endingIndexOfRecord=endingIndexOfRecord+1+((ioEventCounterType+1)*input[endingIndexOfRecord+1]);
		return endingIndexOfRecord;
	}

	private NotificationRecord createNotificationRecord(byte[] notificationRecordData){
		long timeStampEpochTimeInSeconds=Long.parseLong(calculateUnsignedDecimalValFromSignedBytes(Arrays.copyOf(notificationRecordData,TIMESTAMP_PAYLOAD_LEN)))/1000;
		int priority=notificationRecordData[TIMESTAMP_PAYLOAD_LEN];
		long longitudeVal=Long.parseLong(calculateUnsignedDecimalValFromSignedBytes(
				Arrays.copyOfRange(notificationRecordData,TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE,TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+4)));
		int longitudeDigits=Math.log10(longitudeVal<0?longitudeVal*-1:longitudeVal) + 1;
		String longitude=longitudeVal/Math.pow(10,longitudeDigits-2);
		long latitudeVal=Long.parseLong(calculateUnsignedDecimalValFromSignedBytes(
				Arrays.copyOfRange(notificationRecordData,TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+4,TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+8)));
		int latitudeDigits=Math.log10(latitudeVal<0?latitudeVal*-1:latitudeVal) + 1;
		String latitude=latitudeVal/Math.pow(10,latitudeDigits-2);
		String altitude=calculateUnsignedDecimalValFromSignedBytes(
				Arrays.copyOfRange(notificationRecordData,TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+8,TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+10));
		String angle=calculateUnsignedDecimalValFromSignedBytes(
				Arrays.copyOfRange(notificationRecordData,TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+10,TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+12));
		int noOfSatilites=notificationRecordData[TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+12];
		int speed=Integer.parseInt(calculateUnsignedDecimalValFromSignedBytes(
				Arrays.copyOfRange(notificationRecordData,TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+13,TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+15)));
		int eventType=notificationRecordData[TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+15];
		int noOfevents=notificationRecordData[TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+16];
		Map<String,String> events=noOfevents==0?new LinkedHashMap<>():extractEventsFromPayload(notificationRecordData);
		NotificationRecord notificationRecord=new NotificationRecord(Long.toString(timeStampEpochTimeInSeconds),latitude,longitude,altitude,angle,noOfSatilites,speed);
		notificationRecord.getCustomInfo().putAll(events);
		return notificationRecord;
	}

	private Map<String,String> extractEventsFromPayload(byte[] notificationRecordData){
		final Map<Integer,byte[][]> eventTypeArrays=new LinkedHashMap<>();
		int eventArrayStartingIndex=TIMESTAMP_PAYLOAD_LEN+PRIORITY_BYTE+17,eventArrayEndingIndex=0;
		for(int ioEventCounterType=1;ioEventCounterType<=8;ioEventCounterType=ioEventCounterType*2){
			eventArrayEndingIndex=(ioEventCounterType+1)*notificationRecordData[eventArrayStartingIndex];
			if(eventArrayEndingIndex!=0){
				eventArrayEndingIndex+=eventArrayStartingIndex+1;
				eventTypeArrays.put(ioEventCounterType,
						DataParserUtility.splitArray(Arrays.copyOfRange(notificationRecordData,eventArrayStartingIndex+1,eventArrayEndingIndex),ioEventCounterType+1));
				eventArrayStartingIndex=eventArrayEndingIndex;
			}else
				eventArrayStartingIndex++;
		}
		return extractEventsFromEventTypeArrays(eventTypeArrays);
	}

	private Map<String,String> extractEventsFromEventTypeArrays(final Map<Integer,byte[][]> eventTypeArrays){
		final Map<String,String> allEvents=new LinkedHashMap<>();
		for(Map.Entry<Integer, byte[][]> eventType:eventTypeArrays.entrySet())
			allEvents.putAll(extractEventsFromEventTypeArray(eventType.getKey(),eventType.getValue()));
		return allEvents;
	}

	private Map<String,String> extractEventsFromEventTypeArray(int eventType,byte[][] eventTypeArray){
		final Map<String,String> events=new LinkedHashMap<>();
		for(int arrayIndex=0;arrayIndex<eventTypeArray.length;arrayIndex++){
			byte[] eventArray=eventTypeArray[arrayIndex];
			String enventType=eventIdToNameMapper.getEventName(eventArray[0]);
			if(enventType?.trim())
				events.put(enventType,DataParserUtility.calculateUnsignedDecimalValFromSignedBytes(Arrays.copyOfRange(eventArray,1,eventArray.length)));
		}
		return events;
	}
}

public class NotificationMessageService implements UplinkMessageService{

	private final Logger logger=LoggerFactory.getLogger(this.getClass());

	def notification = "notification";

	private final IOTPublisherService<DeviceInfo, DeviceDataDeliveryStatus> iotPublisherService;
	private final TCPServerSocketWriter tcpServerSocketWriter;

	public NotificationMessageService(IOTPublisherService<DeviceInfo, DeviceDataDeliveryStatus> iotPublisherService,TCPServerSocketWriter tcpServerSocketWriter) {
		super();
		this.iotPublisherService = iotPublisherService;
		this.tcpServerSocketWriter=tcpServerSocketWriter;
	}

	@Override
	public String getMessageType() {
		return notification;
	}

	@Override
	public DeviceDataDeliveryStatus executeService(DeviceInfo deviceInfo) {
		DeviceDataDeliveryStatus deviceDataDeliveryStatus=iotPublisherService.receiveDataFromDevice(deviceInfo,notification);
		int noOfRecords=((Notification)deviceInfo.getDeviceData().get(notification)).getNoOfRecords();
		byte[] acknowlegement=DataParserUtility.getByteArrayValueInBigEndian(noOfRecords,4);
		tcpServerSocketWriter.sendMessage(deviceInfo.getDevice(),acknowlegement);
		return deviceDataDeliveryStatus;
	}

}
