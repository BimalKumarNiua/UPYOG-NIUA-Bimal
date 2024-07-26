import React, { useState } from 'react';
import { CardLabel, CardText, CardLabelDesc, CardSubHeader, Modal } from '@nudmcdgnpm/digit-ui-react-components';

// Close button component
const Close = () => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#FFFFFF">
    <path d="M0 0h24v24H0V0z" fill="none" />
    <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z" />
  </svg>
);

const CloseBtn = (props) => {
  return (
    <div className="icon-bg-secondary" onClick={props.onClick}>
      <Close />
    </div>
  );
};

const ChbCancellationPolicy = ({ slotDetail }) => {
  const [showCancellationPolicy, setShowCancellationPolicy] = useState(false);
  const [showPriceBreakup, setShowPriceBreakup] = useState(false);
  const [showdemandEstimation,setShowDemandEstimation]=useState(false);
  const stateId = Digit.ULBService.getStateId();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const { data: cancelpolicyData } = Digit.Hooks.useCustomMDMS(stateId, "CHB", [{ name: "CommunityHalls" }],
    {
      select: (data) => {
        const formattedData = data?.["CHB"]?.["CommunityHalls"];
        return formattedData;
      },
  });
  let hallDetails = slotDetail.map((slot) => {
    return { 
      hallCode: slot.hallCode1,
      bookingDate:slot.bookingDate,
      bookingFromTime:"10:00",
      bookingToTime:"23:59",
      status:"BOOKING_CREATED",
      capacity:slot.capacity
    }; });
  const mutation = Digit.Hooks.chb.useDemandEstimation();
      let formdata = {
        tenantId:tenantId,
        bookingSlotDetails:hallDetails,
        communityHallCode:slotDetail[0].code
        
      };

  if(showdemandEstimation===false){
    mutation.mutate(formdata);
    setShowDemandEstimation(true);
  }
  const handleCancellationPolicyClick = () => {
    setShowCancellationPolicy(!showCancellationPolicy);
  };

  const handlePriceBreakupClick = () => {
    setShowPriceBreakup(!showPriceBreakup);
  };

  const renderCancellationPolicy = (policy) => {
    const policyLines = policy
      .split('\n')
      .filter(line => line.trim() !== '')
      .map((line, index) => `${index + 1}. ${line.trim()}`);

    return (
      <ol style={{ paddingLeft: '20px' }}>
        {policyLines.map((line, index) => (
          <li key={index} style={{ marginBottom: '10px' }}><CardLabelDesc>{line}</CardLabelDesc></li>
        ))}
      </ol>
    );
  };

  const calculateTotalAmount = (CalculationType) => {
    return CalculationType.reduce((total, item) => total + item.taxAmount, 0);
  };
  return (
    <div>
      <CardSubHeader style={{ color: '#FE7A51', fontSize: '18px'}}>
        Total Booking Amount
      </CardSubHeader>
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <div style={{ marginLeft: '30px', marginRight: '60px', fontSize: '16px', fontWeight: 'bold' }}>
          Rs {mutation.data?.demands[0]?.demandDetails ? calculateTotalAmount(mutation.data?.demands[0]?.demandDetails) : 'Loading...'} /-
        </div>
        <div 
          onClick={handlePriceBreakupClick} 
          style={{ cursor: 'pointer', margin: '0 20px', color: '#FE7A51', fontSize: '16px', textDecoration: 'none' }}
        >
          VIEW ESTIMATE PRICE BREAKUP
        </div>
        <div 
          onClick={handleCancellationPolicyClick} 
          style={{ cursor: 'pointer', color: '#FE7A51', fontSize: '16px', textDecoration: 'none' }}
        >
          VIEW CANCELLATION POLICY
        </div>
      </div>

      {showCancellationPolicy && (
        <Modal
          headerBarMain={<CardSubHeader style={{ color: '#FE7A51', margin: '25px' }}>Cancellation Policy</CardSubHeader>}
          headerBarEnd={<CloseBtn onClick={handleCancellationPolicyClick} />}
          popupStyles={{ backgroundColor: "#fff", position: 'relative', maxHeight: '90vh', width: '80%', overflowY: 'auto' }}
          children={
            <div>
              {cancelpolicyData ? (
                <div>
                  {renderCancellationPolicy(cancelpolicyData[0].cancellationPolicy)}
                </div>
              ) : (
                <CardLabel style={{ fontSize: '20px' }}>Loading...</CardLabel>
              )}
            </div>
          }
          actionCancelLabel={null}  // Hide Cancel button
          actionCancelOnSubmit={null}  // No action for Cancel
          actionSaveLabel={null}  // Hide Save button
          actionSaveOnSubmit={null}  // No action for Save
          actionSingleLabel={null}  // Hide Submit button
          actionSingleSubmit={null}  // No action for Submit
          error={null}
          setError={() => {}}
          formId="modalForm"
          isDisabled={false}
          hideSubmit={true}  // Ensure submit is hidden
          style={{}}
          popupModuleMianStyles={{ padding: "10px" }}
          headerBarMainStyle={{ backgroundColor: "#f5f5f5" }}
          isOBPSFlow={false}
          popupModuleActionBarStyles={{ display: 'none' }}  // Hide Action Bar
          isOpen={showCancellationPolicy}  // Pass isOpen prop
          onClose={handleCancellationPolicyClick}  // Pass onClose prop
        />
      )}
      {showPriceBreakup && (
        <Modal
          headerBarMain={<CardSubHeader style={{ color: '#FE7A51', margin: '25px' }}>Price Breakup</CardSubHeader>}
          headerBarEnd={<CloseBtn onClick={handlePriceBreakupClick} />}
          popupStyles={{ backgroundColor: "#fff", position: 'relative', maxHeight: '90vh', width: '60%', overflowY: 'auto' }}
          children={
            <div>
              <CardText style={{ marginBottom: '15px' }}>Estimate Price Details</CardText>
              <ul>
                {mutation.data?.demands[0]?.demandDetails && mutation.data?.demands[0]?.demandDetails.map((demands, index) => (
                  <li key={index} style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <CardLabelDesc>{demands.taxHeadMasterCode}</CardLabelDesc>
                    <CardLabelDesc>Rs {demands.taxAmount}</CardLabelDesc>
                  </li>
                ))}
              </ul>
              <hr />
              <div style={{ fontWeight: 'bold', marginTop: '10px', display: 'flex', justifyContent: 'space-between' }}>
                <CardLabelDesc>Total</CardLabelDesc>
                <CardLabelDesc>Rs {mutation.data?.demands[0]?.demandDetails && calculateTotalAmount(mutation.data?.demands[0]?.demandDetails)}</CardLabelDesc>
              </div>
            </div>
          }
          actionCancelLabel={null}  // Hide Cancel button
          actionCancelOnSubmit={null}  // No action for Cancel
          actionSaveLabel={null}  // Hide Save button
          actionSaveOnSubmit={null}  // No action for Save
          actionSingleLabel={null}  // Hide Submit button
          actionSingleSubmit={null}  // No action for Submit
          error={null}
          setError={() => {}}
          formId="modalForm"
          isDisabled={false}
          hideSubmit={true}  // Ensure submit is hidden
          style={{}}
          // popupModuleMianStyles={{ padding: "10px" }}
          headerBarMainStyle={{ backgroundColor: "#f5f5f5" }}
          isOBPSFlow={false}
          popupModuleActionBarStyles={{ display: 'none' }}  // Hide Action Bar
          isOpen={showPriceBreakup}  // Pass isOpen prop
          onClose={handlePriceBreakupClick}  // Pass onClose prop
        />
      )}
    </div>
  );
};

export default ChbCancellationPolicy;
