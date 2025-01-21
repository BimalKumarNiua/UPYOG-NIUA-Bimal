import { Banner, Card, CardText, LinkButton, LinkLabel, Loader, Row, StatusTable, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { waterTankerPayload } from "../../../utils";


/* This component, WTAcknowledgement, is responsible for displaying the acknowledgement 
 of a service request submission. It utilizes the Digit UI library components for 
 rendering the UI elements. 

 Key functionalities include:
 1. **Data Submission**: On component mount (using useEffect), it prepares the data 
    by extracting the tenantId from the address and formats it using the `svPayloadData` 
    utility function. It then calls the `useSvCreateApi` hook to submit this data.
 2. **Loading State**: While the data submission is in progress, a Loader component 
    is displayed to indicate that the request is being processed.
 3. **Acknowledgement Banner**: Once the data submission is complete, a Banner 
    component is rendered to show the result of the submission, which can either 
    indicate success or failure based on the response from the API.
 4. **Navigation**: A LinkButton is provided to navigate back to the home page 
    after the acknowledgement is displayed.
*/



const GetActionMessage = (props) => {
    const { t } = useTranslation();
    if (props.isSuccess) {
      return t("WT_SUBMIT_SUCCESSFULL");
    }
    else if (props.isLoading){
      return t("WT_APPLICATION_PENDING");
    }
    else if (!props.isSuccess)
    return t("WT_APPLICATION_FAILED");
  };


//style object to pass inside row container which shows the application ID and status of application of banner image
const rowContainerStyle = {
  padding: "4px 0px",
  justifyContent: "space-between",
};

const BannerPicker = (props) => {
  return (
    <Banner
      message={GetActionMessage(props)}
      applicationNumber={props.data?.waterTankerBookingDetail?.bookingNo}
      info={props.isSuccess ? props.t("WT_BOOKING_NO") : ""}
      successful={props.isSuccess}
      style={{width: "100%"}}
    />
  );
};

const WTAcknowledgement = ({ data, onSuccess }) => {
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const mutation = Digit.Hooks.wt.useTankerCreateAPI("pg.citya"); 
  const user = Digit.UserService.getUser().info;

  useEffect(() => {
    try {
      data.tenantId = tenantId;
      let formdata = waterTankerPayload(data);
      mutation.mutate(formdata, {onSuccess});
    } catch (err) {
    }
  }, []);

  Digit.Hooks.useCustomBackNavigation({
    redirectPath: '/digit-ui/citizen'
  })

//   const handleDownloadPdf = async () => {
//     const { SVDetail = [] } = mutation.data;
//     let SVData = (SVDetail) || {};
//     const tenantInfo = tenants.find((tenant) => tenant.code === SVData.tenantId);
//     let tenantId = SVData.tenantId || tenantId;
   
//     const data = await getSVAcknowledgementData({ ...SVData }, tenantInfo, t);
//     Digit.Utils.pdf.generate(data);
//   };

  return mutation.isLoading || mutation.isIdle ? (
    <Loader />
  ) : (
    <Card>
      <BannerPicker t={t} data={mutation.data} isSuccess={mutation.isSuccess} isLoading={mutation.isIdle || mutation.isLoading} />
      <StatusTable>
        {mutation.isSuccess && (
          <Row
            rowContainerStyle={rowContainerStyle}
            last       
            textStyle={{ whiteSpace: "pre", width: "60%" }}
          />
        )}
      </StatusTable>
      {/* {mutation.isSuccess && <SubmitBar label={t("SV_ACKNOWLEDGEMENT")} onSubmit={handleDownloadPdf} />} */}
      {user?.type==="CITIZEN"?
      <Link to={`/digit-ui/citizen`}>
        <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
      </Link>
      :
      <Link to={`/digit-ui/employee`}>
        <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
      </Link>}
    </Card>
  );
};

export default WTAcknowledgement;