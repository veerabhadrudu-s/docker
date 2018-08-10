package com.hpe.iot.dc.valid

import static com.handson.iot.dc.util.DataParserUtility.convertBytesToASCIIString
import static com.handson.iot.dc.util.DataParserUtility.truncateEmptyBytes
import static java.util.Arrays.copyOfRange

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.hpe.iot.dc.model.DeviceData
import com.hpe.iot.dc.model.DeviceDataDeliveryStatus
import com.hpe.iot.dc.model.DeviceImpl
import com.hpe.iot.dc.model.DeviceInfo
import com.hpe.iot.dc.model.DeviceModel
import com.hpe.iot.dc.northbound.converter.inflow.impl.AbstractIOTModelConverterImpl
import com.hpe.iot.dc.northbound.service.inflow.IOTPublisherService
import com.hpe.iot.dc.southbound.service.inflow.UplinkMessageService
import com.hpe.iot.dc.southbound.transformer.inflow.UplinkDataModelTransformer
import com.hpe.iot.dc.tcp.southbound.model.AbstractServerSocketToDeviceModel
import com.handson.iot.dc.util.DataParserUtility
import com.handson.iot.dc.util.UtilityLogger

/**
 * @author sveera
 *
 */
class SampleServerSocketToDeviceModel  extends AbstractServerSocketToDeviceModel {

	def manufacturer = "Testing"
	def modelId="Sample"
	def version="1.0"

	@Override
	public String getManufacturer() {
		return manufacturer;
	}

	@Override
	public String getModelId() {
		return modelId;
	}

	@Override
	public String getVersion() {
		return "1.0"
	}

	@Override
	public String getBoundLocalAddress() {
		return "0.0.0.0";
	}

	@Override
	public int getPortNumber() {
		return 2001;
	}

	@Override
	public String getDescription() {
		return "This a Sample Test Device.";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((manufacturer == null) ? 0 : manufacturer.hashCode());
		result = prime * result + ((modelId == null) ? 0 : modelId.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof SampleServerSocketToDeviceModel))
			return false;
		SampleServerSocketToDeviceModel other = (SampleServerSocketToDeviceModel) obj;
		if (manufacturer == null) {
			if (other.manufacturer != null)
				return false;
		} else if (!manufacturer.equals(other.manufacturer))
			return false;
		if (modelId == null) {
			if (other.modelId != null)
				return false;
		} else if (!modelId.equals(other.modelId))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SampleServerSocketToDeviceModel [manufacturer=" + manufacturer + ", modelId=" + modelId + ", version="+
				version + "]";
	}
}

class SampleDataModelTransformer implements UplinkDataModelTransformer{
	private static def MESSAGE_STARTING_BYTES = [60] as byte[];
	private static def MESSAGE_CLOSING_BYTES = [62] as byte[];
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public SampleDataModelTransformer() {
		logger.debug(this.getClass().getSimpleName()+" Modified Class " );
	}


	@Override
	public List<DeviceInfo> convertToModel(DeviceModel deviceModel,byte[] input) {
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
		def deviceIdInByte = copyOfRange(input, 1, 11);
		deviceIdInByte = truncateEmptyBytes(deviceIdInByte);
		final String deviceId = convertBytesToASCIIString(deviceIdInByte, 0, deviceIdInByte.length);
		DeviceInfo deviceData=new DeviceInfo(new DeviceImpl(deviceModel.getManufacturer(), deviceModel.getModelId(),deviceModel.getVersion(), deviceId), "deviceDataMessageType", input);
		SampleData sampleData=new SampleData(new String(copyOfRange(input,12,input.length-2)));
		deviceData.addDeviceData(sampleData.getDeviceDataInformation(),sampleData);
		return deviceData;
	}
}

public class SampleData implements DeviceData {

	public static final String SAMPLE_INFO = "Device data";

	public final String deviceData;


	public SampleData(String deviceData) {
		super();
		this.deviceData = deviceData;
	}

	public String getDeviceData() {
		return deviceData;
	}

	@Override
	public String toString() {
		return "SampleData [deviceData=" + deviceData + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((deviceData == null) ? 0 : deviceData.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SampleData other = (SampleData) obj;
		if (deviceData == null) {
			if (other.deviceData != null)
				return false;
		} else if (!deviceData.equals(other.deviceData))
			return false;
		return true;
	}

	@Override
	public String getDeviceDataInformation() {
		return SAMPLE_INFO;
	}
}


class SampleDataService implements UplinkMessageService {

	private static final String MESSAGE_TYPE = "deviceDataMessageType";
	private static final String CONTAINER_NAME = "default";

	private final IOTPublisherService<DeviceInfo, DeviceDataDeliveryStatus> iotPublisherService;

	public SampleDataService(IOTPublisherService<DeviceInfo, DeviceDataDeliveryStatus> iotPublisherService) {
		super();
		this.iotPublisherService = iotPublisherService;
	}

	@Override
	public String getMessageType() {
		return MESSAGE_TYPE;
	}

	@Override
	public DeviceDataDeliveryStatus executeService(DeviceInfo deviceInfo) {
		iotPublisherService.receiveDataFromDevice(deviceInfo, CONTAINER_NAME);
		return new DeviceDataDeliveryStatus();
	}
}

class SampleIOTModelConverter extends AbstractIOTModelConverterImpl{

	@Override
	public String getDeviceUniqueIDName() {
		return "SampleDevId";
	}
}
