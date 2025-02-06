package org.upyog.rs.util;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.upyog.rs.config.RequestServiceConfiguration;
import org.upyog.rs.constant.RequestServiceConstants;
import org.upyog.rs.kafka.Producer;
import org.upyog.rs.repository.ServiceRequestRepository;
import org.upyog.rs.web.models.WaterTankerBookingDetail;
import org.upyog.rs.web.models.events.EventRequest;
import org.upyog.rs.web.models.notification.Email;
import org.upyog.rs.web.models.notification.EmailRequest;
import org.upyog.rs.web.models.notification.SMSRequest;

import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NotificationUtil {

	private ServiceRequestRepository serviceRequestRepository;

	private RequestServiceConfiguration config;

	private Producer producer;

	private RestTemplate restTemplate;

	@Value("${egov.mdms.host}")
	private String mdmsHost;

	@Value("${egov.mdms.search.endpoint}")
	private String mdmsUrl;

	@Autowired
	public NotificationUtil(ServiceRequestRepository serviceRequestRepository, RequestServiceConfiguration config,
			Producer producer, RestTemplate restTemplate) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.config = config;
		this.producer = producer;
		this.restTemplate = restTemplate;
	}

	public static final String ACTION_STATUS_APPLY = "APPLY";
	
	public static final String ACTION_STATUS_APPROVE = "APPROVE";

	public static final String ACTION_STATUS_ASSIGN_VENDOR = "ASSIGN_VENDOR";

	public static final String ACTION_STATUS_ASSIGN_VEHICLE_DRIVER = "ASSIGN_VEHICLE_DRIVER";

	public static final String ACTION_STATUS_REJECT = "REJECT";
	
	public static final String ACTION_STATUS_COMPLETE_REQUEST = "COMPLETE_REQUEST";

	public static final String ACTION_STATUS_PAY = "PAY";
	
	public static final String ACTION_LINK = "ACTION_LINK";
	
	public static final String MESSAGE_TEXT = "MESSAGE_TEXT";
	
	private final String URL = "url";

	/**
	 * Extracts message for the specific code
	 *
	 * @param notificationCode    The code for which message is required
	 * @param localizationMessage The localization messages
	 * @return message for the specific code
	 */
	public String getMessageTemplate(String notificationCode, String localizationMessage) {

		String path = "$..messages[?(@.code==\"{}\")].message";
		path = path.replace("{}", notificationCode);
		String message = "";
		try {
			Object messageObj = JsonPath.parse(localizationMessage).read(path);
			message = ((ArrayList<String>) messageObj).get(0);
		} catch (Exception e) {
			log.warn("Fetching from localization failed", e);
		}
		return message;
	}

	/**
	 * Fetches messages from localization service
	 *
	 * @param tenantId    tenantId of the PTR
	 * @param requestInfo The requestInfo of the request
	 * @return Localization messages for the module
	 */
	public String getLocalizationMessages(String tenantId, RequestInfo requestInfo) {

		String locale = RequestServiceConstants.NOTIFICATION_LOCALE;
		Boolean isRetryNeeded = false;
		String jsonString = null;
		LinkedHashMap responseMap = null;

		if (!StringUtils.isEmpty(requestInfo.getMsgId()) && requestInfo.getMsgId().split("\\|").length >= 2) {
			locale = requestInfo.getMsgId().split("\\|")[1];
			isRetryNeeded = true;
		}

		responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(getUri(tenantId, requestInfo, locale),
				requestInfo);
		jsonString = new JSONObject(responseMap).toString();

		if (StringUtils.isEmpty(jsonString) && isRetryNeeded) {

			responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(
					getUri(tenantId, requestInfo, RequestServiceConstants.NOTIFICATION_LOCALE), requestInfo);
			jsonString = new JSONObject(responseMap).toString();
			if (StringUtils.isEmpty(jsonString))
				throw new CustomException("EG_EW_LOCALE_ERROR",
						"Localisation values not found for Request Service notifications");
		}
		return jsonString;
	}

	/**
	 * Returns the uri for the localization call
	 *
	 * @return The uri for localization search call
	 */
	public StringBuilder getUri(String tenantId, RequestInfo requestInfo, String locale) {

		if (config.getIsLocalizationStateLevel())
			tenantId = tenantId.split("\\.")[0];

		StringBuilder uri = new StringBuilder();
		uri.append(config.getLocalizationHost()).append(config.getLocalizationContextPath())
				.append(config.getLocalizationSearchEndpoint()).append("?").append("locale=").append(locale)
				.append("&tenantId=").append(tenantId).append("&module=")
				.append(RequestServiceConstants.NOTIFICATION_MODULE_NAME);

		return uri;
	}

	/**
	 * Creates sms request for the each owners
	 * 
	 * @param mobileNumberToOwnerName Map of mobileNumber to OwnerName
	 * @return List of SMSRequest
	 */
	public List<SMSRequest> createSMSRequest(String message, Map<String, String> mobileNumberToOwnerName) {

		List<SMSRequest> smsRequest = new LinkedList<>();
		for (Map.Entry<String, String> entryset : mobileNumberToOwnerName.entrySet()) {
			String customizedMsg = message.replace(RequestServiceConstants.NOTIFICATION_OWNERNAME, entryset.getValue());
			smsRequest.add(new SMSRequest(entryset.getKey(), customizedMsg));
		}
		return smsRequest;
	}

	/**
	 * Send the SMSRequest on the SMSNotification kafka topic
	 *
	 * @param smsRequestList The list of SMSRequest to be sent
	 */
	public void sendSMS(List<SMSRequest> smsRequestList) {

		if (config.getIsSMSNotificationEnabled()) {
			if (CollectionUtils.isEmpty(smsRequestList))
				log.info("Messages from localization couldn't be fetched!");
			for (SMSRequest smsRequest : smsRequestList) {
				producer.push(config.getSmsNotifTopic(), smsRequest);
				log.info("Sending SMS notification: ");
				log.info("MobileNumber: " + smsRequest.getMobileNumber() + " Messages: " + smsRequest.getMessage());
			}
		}
	}

	/**
	 * Fetches UUIDs of CITIZENs based on the phone number.
	 *
	 * @param mobileNumbers
	 * @param requestInfo
	 * @param tenantId
	 * @return
	 */
	public Map<String, String> fetchUserUUIDs(Set<String> mobileNumbers, RequestInfo requestInfo, String tenantId) {

		Map<String, String> mapOfPhnoAndUUIDs = new HashMap<>();
		StringBuilder uri = new StringBuilder();
		uri.append(config.getUserHost()).append(config.getUserSearchEndpoint());
		Map<String, Object> userSearchRequest = new HashMap<>();
		userSearchRequest.put("RequestInfo", requestInfo);
		userSearchRequest.put("tenantId", tenantId);
		userSearchRequest.put("userType", "CITIZEN");
		for (String mobileNo : mobileNumbers) {
			userSearchRequest.put("userName", mobileNo);
			try {
				Object user = serviceRequestRepository.fetchResult(uri, userSearchRequest);
				if (null != user) {
					String uuid = JsonPath.read(user, "$.user[0].uuid");
					mapOfPhnoAndUUIDs.put(mobileNo, uuid);
				} else {
					log.error("Service returned null while fetching user for username - " + mobileNo);
				}
			} catch (Exception e) {
				log.error("Exception while fetching user for username - " + mobileNo);
				log.error("Exception trace: ", e);
				continue;
			}
		}
		return mapOfPhnoAndUUIDs;
	}

	/**
	 * Pushes the event request to Kafka Queue.
	 *
	 * @param request
	 */
	public void sendEventNotification(EventRequest request) {
		log.info("EVENT notification sent!" + request);
		producer.push(config.getSaveUserEventsTopic(), request);
	}

	/**
	 * Creates email request for the each owners
	 *
	 * @param mobileNumberToEmailId Map of mobileNumber to emailIds
	 * @return List of EmailRequest
	 */

	public List<EmailRequest> createEmailRequest(RequestInfo requestInfo, String message,
			Map<String, String> mobileNumberToEmailId) {

		List<EmailRequest> emailRequest = new LinkedList<>();
		for (Map.Entry<String, String> entryset : mobileNumberToEmailId.entrySet()) {
			String customizedMsg = "";
			if (message.contains(RequestServiceConstants.NOTIFICATION_EMAIL))
				customizedMsg = message.replace(RequestServiceConstants.NOTIFICATION_EMAIL, entryset.getValue());

			String subject = "";
			String body = customizedMsg;
			Email emailobj = Email.builder().emailTo(Collections.singleton(entryset.getValue())).isHTML(false)
					.body(body).subject(subject).build();
			EmailRequest email = new EmailRequest(requestInfo, emailobj);
			emailRequest.add(email);
		}
		return emailRequest;
	}

	/**
	 * Send the EmailRequest on the EmailNotification kafka topic
	 *
	 * @param emailRequestList The list of EmailRequest to be sent
	 */
	public void sendEmail(List<EmailRequest> emailRequestList) {

		if (config.getIsEmailNotificationEnabled()) {
			if (CollectionUtils.isEmpty(emailRequestList))
				log.info("Messages from localization couldn't be fetched!");
			for (EmailRequest emailRequest : emailRequestList) {
				if (!StringUtils.isEmpty(emailRequest.getEmail().getBody())) {
					producer.push(config.getEmailNotifTopic(), emailRequest);
					log.info("Sending EMAIL notification! ");
					log.info("Email Id: " + emailRequest.getEmail().toString());
				} else {
					log.info("Email body is empty, hence no email notification will be sent.");
				}
			}

		}
	}

	/**
	 * Fetches email ids of CITIZENs based on the phone number.
	 *
	 * @param mobileNumbers
	 * @param requestInfo
	 * @param tenantId
	 * @return
	 */

	public Map<String, String> fetchUserEmailIds(Set<String> mobileNumbers, RequestInfo requestInfo, String tenantId) {
		Map<String, String> mapOfPhnoAndEmailIds = new HashMap<>();
		StringBuilder uri = new StringBuilder();
		uri.append(config.getUserHost()).append(config.getUserSearchEndpoint());
		Map<String, Object> userSearchRequest = new HashMap<>();
		userSearchRequest.put("RequestInfo", requestInfo);
		userSearchRequest.put("tenantId", tenantId);
		userSearchRequest.put("userType", "CITIZEN");
		for (String mobileNo : mobileNumbers) {
			userSearchRequest.put("userName", mobileNo);
			try {
				Object user = serviceRequestRepository.fetchResult(uri, userSearchRequest);
				if (null != user) {
					if (JsonPath.read(user, "$.user[0].emailId") != null) {
						String email = JsonPath.read(user, "$.user[0].emailId");
						mapOfPhnoAndEmailIds.put(mobileNo, email);
					}
				} else {
					log.error("Service returned null while fetching user for username - " + mobileNo);
				}
			} catch (Exception e) {
				log.error("Exception while fetching user for username - " + mobileNo);
				log.error("Exception trace: ", e);
				continue;
			}
		}
		return mapOfPhnoAndEmailIds;
	}

	/**
	 * Method to fetch the list of channels for a particular action from mdms
	 * configd from mdms configs returns the message minus some lines to match In
	 * App Templates
	 * 
	 * @param requestInfo
	 * @param tenantId
	 * @param moduleName
	 * @param action
	 */
	private List<String> fetchChannelList(RequestInfo requestInfo, String tenantId, String moduleName, String action) {
		List<String> masterData = new ArrayList<>();
		StringBuilder uri = new StringBuilder();
		uri.append(config.getMdmsHost()).append(config.getMdmsPath());
		if (StringUtils.isEmpty(tenantId))
			return masterData;
		MdmsCriteriaReq mdmsCriteriaReq = getMdmsRequestForChannelList(requestInfo, tenantId);

		Filter masterDataFilter = filter(
				where(RequestServiceConstants.MODULE).is(moduleName).and(RequestServiceConstants.ACTION).is(action));

		try {
			Object response = serviceRequestRepository.fetchResult(uri, mdmsCriteriaReq);
			masterData = JsonPath.parse(response).read("$.MdmsRes.Channel.channelList[?].channelNames[*]",
					masterDataFilter);
		} catch (Exception e) {
			log.error("Exception while fetching workflow states to ignore: ", e);
		}

		return masterData;
	}

	private MdmsCriteriaReq getMdmsRequestForChannelList(RequestInfo requestInfo, String tenantId) {
		MasterDetail masterDetail = new MasterDetail();
		masterDetail.setName(RequestServiceConstants.CHANNEL_LIST);
		List<MasterDetail> masterDetailList = new ArrayList<>();
		masterDetailList.add(masterDetail);

		ModuleDetail moduleDetail = new ModuleDetail();
		moduleDetail.setMasterDetails(masterDetailList);
		moduleDetail.setModuleName(RequestServiceConstants.CHANNEL);
		List<ModuleDetail> moduleDetailList = new ArrayList<>();
		moduleDetailList.add(moduleDetail);

		MdmsCriteria mdmsCriteria = new MdmsCriteria();
		mdmsCriteria.setTenantId(tenantId);
		mdmsCriteria.setModuleDetails(moduleDetailList);

		MdmsCriteriaReq mdmsCriteriaReq = new MdmsCriteriaReq();
		mdmsCriteriaReq.setMdmsCriteria(mdmsCriteria);
		mdmsCriteriaReq.setRequestInfo(requestInfo);

		return mdmsCriteriaReq;
	}

	public Map<String, String> getCustomizedMsg(RequestInfo requestInfo, WaterTankerBookingDetail waterTankerDetail,
			String localizationMessage) {
		String message = null, messageTemplate;
		String link = null;
		String ACTION_STATUS = waterTankerDetail.getWorkflow().getAction();
		switch (ACTION_STATUS) {

		case ACTION_STATUS_APPLY:
			messageTemplate = getMessageTemplate(RequestServiceConstants.NOTIFICATION_APPLY, localizationMessage);
			message = getMessageWithNumberAndFinalDetails(waterTankerDetail, messageTemplate);
			link = getPayUrl(waterTankerDetail);
			break;
			
		case ACTION_STATUS_APPROVE:
			messageTemplate = getMessageTemplate(RequestServiceConstants.NOTIFICATION_APPROVED, localizationMessage);
			message = getMessageWithNumberAndFinalDetails(waterTankerDetail, messageTemplate);
			link = getPayUrl(waterTankerDetail);
			break;	
			
		case ACTION_STATUS_ASSIGN_VENDOR:
			messageTemplate = getMessageTemplate(RequestServiceConstants.NOTIFICATION_ASSIGN_VENDOR, localizationMessage);
			message = getMessageWithNumberAndFinalDetails(waterTankerDetail, messageTemplate);
			link = getPayUrl(waterTankerDetail);
			break;
		
		case ACTION_STATUS_ASSIGN_VEHICLE_DRIVER:
			messageTemplate = getMessageTemplate(RequestServiceConstants.NOTIFICATION_ASSIGN_VEHICLE_DRIVER, localizationMessage);
			message = getMessageWithNumberAndFinalDetails(waterTankerDetail, messageTemplate);
			link = getPayUrl(waterTankerDetail);
			break;
			
		case ACTION_STATUS_COMPLETE_REQUEST:
			messageTemplate = getMessageTemplate(RequestServiceConstants.NOTIFICATION_COMPLETE_REQUEST, localizationMessage);
			message = getMessageWithNumberAndFinalDetails(waterTankerDetail, messageTemplate);
			link = getPayUrl(waterTankerDetail);
			break;		
	
		case ACTION_STATUS_REJECT:
			messageTemplate = getMessageTemplate(RequestServiceConstants.NOTIFICATION_REJECT, localizationMessage);
			message = getMessageWithNumberAndFinalDetails(waterTankerDetail, messageTemplate);
			link = getPayUrl(waterTankerDetail);
			break;

		case ACTION_STATUS_PAY:
			messageTemplate = getMessageTemplate(RequestServiceConstants.NOTIFICATION_TANKERBOOKED,
					localizationMessage);
			message = getMessageWithNumberAndFinalDetails(waterTankerDetail, messageTemplate);
			link = getPayUrl(waterTankerDetail);
			break;
		}
		
		Map<String, String> messageMap = new HashMap<String, String>();
		messageMap.put(ACTION_LINK, link);
		messageMap.put(MESSAGE_TEXT, message);
		
		log.info("getCustomizedMsg messageTemplate : " + message);
		return messageMap;

		//return message;
	}

/*	private String getMessageWithNumber(WaterTankerBookingDetail waterTankerDetail, String message) {
		message = message.replace("{1}", waterTankerDetail.getApplicantDetail().getName());
		message = message.replace("{2}", waterTankerDetail.getBookingNo());
		return message;
	} */

	private String getMessageWithNumberAndFinalDetails(WaterTankerBookingDetail waterTankerDetail, String message) {
		message = message.replace("{1}", waterTankerDetail.getApplicantDetail().getName());
		message = message.replace("{2}", waterTankerDetail.getBookingNo());
		return message;
	}
	
	  /**
     * Prepares and return url for view screen
     *
     * @param waterTankerDetail
     * @return
     */
	public String getPayUrl(WaterTankerBookingDetail waterTankerDetail) {
	    String payLinkUrl= config.getUiAppHost() + config.getPayLinkSMS();

	    String payLink = String.format(
	        payLinkUrl, 
	        waterTankerDetail.getBookingNo(),
	        waterTankerDetail.getApplicantDetail().getMobileNumber(),
	        waterTankerDetail.getTenantId(),
	        config.getBusinessServiceName()
	    );

	    return getShortnerURL(payLink);
	}

	private String getShortnerURL(String actualURL) {
		net.minidev.json.JSONObject obj = new net.minidev.json.JSONObject();
		obj.put(URL, actualURL);
		String url = config.getUrlShortnerHost() + config.getShortenerEndpoint();

		Object response = serviceRequestRepository.getShorteningURL(new StringBuilder(url), obj);
		return response.toString();
	}

}
