package org.upyog.rs.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.upyog.rs.constant.RequestServiceConstants;
import org.upyog.rs.repository.RequestServiceRepository;
import org.upyog.rs.service.DemandService;
import org.upyog.rs.service.EnrichmentService;
import org.upyog.rs.service.WaterTankerService;
import org.upyog.rs.service.WorkflowService;
import org.upyog.rs.web.models.WaterTankerBookingDetail;
import org.upyog.rs.web.models.WaterTankerBookingRequest;
import org.upyog.rs.web.models.WaterTankerBookingSearchCriteria;
import org.upyog.rs.web.models.workflow.State;

import digit.models.coremodels.PaymentRequest;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WaterTankerServiceImpl implements WaterTankerService {

	@Autowired
	EnrichmentService enrichmentService;

	@Autowired
	RequestServiceRepository requestServiceRepository;

	@Autowired
	WorkflowService workflowService;

	@Autowired
	DemandService demandService;

	@Override
	public WaterTankerBookingDetail createNewWaterTankerBookingRequest(WaterTankerBookingRequest waterTankerRequest) {

		log.info("Create water tanker booking for user : " + waterTankerRequest.getRequestInfo().getUserInfo().getUuid()
				+ " for the request : " + waterTankerRequest.getWaterTankerBookingDetail());

		enrichmentService.enrichCreateWaterTankerRequest(waterTankerRequest);

		workflowService.updateWorkflowStatus(null, waterTankerRequest);

		requestServiceRepository.saveWaterTankerBooking(waterTankerRequest);

		WaterTankerBookingDetail waterTankerDetail = waterTankerRequest.getWaterTankerBookingDetail();

		return waterTankerDetail;
	}

	@Override
	public List<WaterTankerBookingDetail> getWaterTankerBookingDetails(RequestInfo requestInfo,
			WaterTankerBookingSearchCriteria waterTankerBookingSearchCriteria) {
		/*
		 * Retrieve WT booking details from the repository based on search criteria and
		 * and give the data already retrieved to the repository layer
		 */
		List<WaterTankerBookingDetail> applications = requestServiceRepository
				.getWaterTankerBookingDetails(waterTankerBookingSearchCriteria);

		/**
		 * Check if the retrieved list is empty using Spring's CollectionUtils Prevents
		 * potential null pointer exceptions by returning an empty list Ensures
		 * consistent return type and prevents calling methods from handling null
		 */
		if (CollectionUtils.isEmpty(applications)) {
			return new ArrayList<>();
		}

		// Return retrieved application
		return applications;
	}

	@Override
	public Integer getApplicationsCount(WaterTankerBookingSearchCriteria waterTankerBookingSearchCriteria,
			RequestInfo requestInfo) {
		waterTankerBookingSearchCriteria.setCountCall(true);
		Integer bookingCount = 0;

		waterTankerBookingSearchCriteria = addCreatedByMeToCriteria(waterTankerBookingSearchCriteria, requestInfo);
		bookingCount = requestServiceRepository.getApplicationsCount(waterTankerBookingSearchCriteria);

		return bookingCount;
	}

	private WaterTankerBookingSearchCriteria addCreatedByMeToCriteria(WaterTankerBookingSearchCriteria criteria,
			RequestInfo requestInfo) {
		if (requestInfo.getUserInfo() == null) {
			log.info("Request info is null returning criteira");
			return criteria;
		}
		List<String> roles = new ArrayList<>();
		for (Role role : requestInfo.getUserInfo().getRoles()) {
			roles.add(role.getCode());
		}
		log.info("user roles for searching : " + roles);
		/**
		 * Citizen can see booking details only booked by him
		 */
		List<String> uuids = new ArrayList<>();
		if (roles.contains(RequestServiceConstants.CITIZEN)
				&& !StringUtils.isEmpty(requestInfo.getUserInfo().getUuid())) {
			uuids.add(requestInfo.getUserInfo().getUuid());
			criteria.setCreatedBy(uuids);
			log.debug("loading data of created and by me" + uuids.toString());
		}
		return criteria;
	}
/*
	@Override
	public WaterTankerBookingDetail updateWaterTankerBooking(WaterTankerBookingRequest waterTankerRequest) {
		String bookingNo = waterTankerRequest.getWaterTankerBookingDetail().getBookingNo();
		log.info("Updating booking for booking no : " + bookingNo);
		if (bookingNo == null) {
			throw new CustomException("INVALID_BOOKING_CODE",
					"Booking no not valid. Failed to update booking status for : " + bookingNo);
		}

		State state = workflowService.updateWorkflowStatus(null, waterTankerRequest);
		enrichmentService.enrichWaterTankerBookingUponUpdate(state.getApplicationStatus(), waterTankerRequest);

		if (RequestServiceConstants.ACTION_APPROVE
				.equals(waterTankerRequest.getWaterTankerBookingDetail().getWorkflow().getAction())) {
			demandService.createDemand(waterTankerRequest);

		}
		requestServiceRepository.updateWaterTankerBooking(waterTankerRequest);

		return waterTankerRequest.getWaterTankerBookingDetail();
	} */

	@Override
	public WaterTankerBookingDetail updateWaterTankerBooking(WaterTankerBookingRequest waterTankerRequest, PaymentRequest paymentRequest, String applicationStatus) {
	    String bookingNo = waterTankerRequest.getWaterTankerBookingDetail().getBookingNo();
	    log.info("Updating booking for booking no: {}", bookingNo);
	    
	    if (bookingNo == null) {
	        throw new CustomException("INVALID_BOOKING_CODE", "Booking no not valid. Failed to update booking status for : " + bookingNo);
	    }

	    // If no payment request, update workflow status and process booking request
	    if (paymentRequest == null) {
	        State state = workflowService.updateWorkflowStatus(null, waterTankerRequest);
	        enrichmentService.enrichWaterTankerBookingUponUpdate(state.getApplicationStatus(), waterTankerRequest);

	        // If action is APPROVE, create demand
	        if (RequestServiceConstants.ACTION_APPROVE.equals(waterTankerRequest.getWaterTankerBookingDetail().getWorkflow().getAction())) {
	            demandService.createDemand(waterTankerRequest);
	        }
	    }

	    // Handle the payment request and update the water tanker booking if applicable
	    if (paymentRequest != null) {
	        String consumerCode = paymentRequest.getPayment().getPaymentDetails().get(0).getBill().getConsumerCode();
	        WaterTankerBookingDetail waterTankerDetail = requestServiceRepository
	                .getWaterTankerBookingDetails(WaterTankerBookingSearchCriteria.builder()
	                        .bookingNo(consumerCode)
	                        .build())
	                .stream()
	                .findFirst()
	                .orElse(null);

	        if (waterTankerDetail == null) {
	            log.info("Application not found in consumer class while updating status");
	            return null;
	        }

	        // Update the booking details
	        waterTankerDetail.getAuditDetails().setLastModifiedBy(paymentRequest.getRequestInfo().getUserInfo().getUuid());
	        waterTankerDetail.getAuditDetails().setLastModifiedTime(System.currentTimeMillis());
	        waterTankerDetail.setBookingStatus(applicationStatus);
	        waterTankerDetail.setPaymentDate(System.currentTimeMillis());

	        // Update water tanker booking request
	        WaterTankerBookingRequest updatedWaterTankerRequest = WaterTankerBookingRequest.builder()
	                .requestInfo(paymentRequest.getRequestInfo())
	                .waterTankerBookingDetail(waterTankerDetail)
	                .build();
	        
	        log.info("Water Tanker Request to update application status in consumer: {}", updatedWaterTankerRequest);
	        requestServiceRepository.updateWaterTankerBooking(updatedWaterTankerRequest);

	        return waterTankerDetail;
	    }

	    // If no payment request, just update the water tanker booking request
	    requestServiceRepository.updateWaterTankerBooking(waterTankerRequest);

	    return waterTankerRequest.getWaterTankerBookingDetail();
	}


}
