import { Loader, Modal, FormComposer } from "@nudmcdgnpm/digit-ui-react-components";
import React, { useState, useEffect } from "react";

import { configWTApproverApplication } from "../config/WTApproverApplication";
/*
  ActionModal Component
  
  This component is responsible for rendering a modal that allows an approver to take action 
  on a specific task. It includes form fields for comments and file uploads, and provides the 
  ability to select an approver from a list fetched via an API. When the form is submitted, 
  it triggers the `submitAction` function with the necessary data.
  
  Key Features:
  - **File Upload**: Handles file upload with size validation (max 5MB).
  - **Approver Selection**: Dynamically loads approvers based on roles and sets the selected approver.
  - **Form Configuration**: Configures the form dynamically based on the `action` prop.
  - **Error Handling**: Displays error messages for file uploads or other validation issues.
  - **Submit Action**: Calls the provided `submitAction` function with the form data when submitted.
*/

// Heading component - renders an h1 element with a dynamic label prop.
const Heading = (props) => {
  return <h1 className="heading-m">{props.label}</h1>;
};

// Close component - renders an SVG icon representing a "close" button (X icon).
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

const ActionModal = ({ t, action, tenantId, state, id, closeModal, submitAction, actionData, applicationData, businessService, moduleCode }) => {

  const { data: approverData, isLoading: PTALoading } = Digit.Hooks.useEmployeeSearch(
    tenantId,
    {
      roles: action?.assigneeRoles?.map?.((e) => ({ code: e })),
      isActive: true,
    },
    { enabled: !action?.isTerminateState }
  );
  
 /*hook for vendor search */
  const { data: dsoData, isLoading: isLoading, isSuccess: isDsoSuccess, error: dsoError, refetch } = Digit.Hooks.fsm.useVendorSearch({
    tenantId,
    config: { enabled: action?.state === "PENDING_FOR_VEHICLE_DRIVER_ASSIGN" },
  });

  let vendorDescription = [];
  dsoData?.vendor?.map((item) => {
    if (item?.additionalDetails?.description === "WT") {
      vendorDescription.push({ code: item?.name, name: item?.name, i18nKey: item?.name });
    }
  });

/*hook for vehicle search */
  const { data:vehicleData,isSuccess } = Digit.Hooks.fsm.useVehiclesSearch({
    tenantId,
    config: { enabled: action?.state === "DELIVERY_PENDING" },
  });
  
  let vehicleDescription = [];
  vehicleData?.vehicle?.map((item) => {
    vehicleDescription.push({ code: item?.registrationNumber, name: item?.registrationNumber, i18nKey: item?.registrationNumber,tankerCapacity:item?.tankCapacity });
  });

  /*
for driver search for future use , if needed
  const { data: driverData, isLoading: isDriverDataLoading, isSuccess: isDriverSuccess, error: driverError, refetch: refetchDriver } = Digit.Hooks.fsm.useDriverSearch({
    tenantId, 
    filters: {
      sortBy: "name",
      sortOrder: "ASC",
      status: "ACTIVE",
      driverWithNoVendor: action?.state === "DELIVERY_PENDING" ? true : false,
      config:{enabled: action?.state === "DELIVERY_PENDING" ? true : false},
    },
  });

  let driverDescription = [];
  driverData?.driver?.map((item) => {
    driverDescription.push({ code: item?.name, name: item?.name, i18nKey: item?.name });
  });

  console.log("driverDescription", driverDescription);
*/

  const [config, setConfig] = useState({});
  const [defaultValues, setDefaultValues] = useState({});
  const [approvers, setApprovers] = useState([]);
  const [file, setFile] = useState(null);
  const [uploadedFile, setUploadedFile] = useState(null);
  const [error, setError] = useState(null);
  const [selectedApprover, setSelectedApprover] = useState(null); 
  const [selectVehicle, setSelectVehicle] = useState(null); 


  useEffect(() => {
    setApprovers(approverData?.Employees?.map((employee) => ({ uuid: employee?.uuid, name: employee?.user?.name })));
  }, [approverData]);

  function selectFile(e) {
    setFile(e.target.files[0]);
  }

  useEffect(() => {
    (async () => {
      setError(null);
      if (file) {
        if (file.size >= 5242880) {
          setError(t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED"));
        } else {
          try {
            const response = await Digit.UploadServices.Filestorage("WT", file, tenantId);
            if (response?.data?.files?.length > 0) {
              setUploadedFile(response?.data?.files[0]?.fileStoreId);
            } else {
              setError(t("CS_FILE_UPLOAD_ERROR"));
            }
          } catch (err) {
            setError(t("CS_FILE_UPLOAD_ERROR"));
          }
        }
      }
    })();
  }, [file]);
  

  function submit(data) {
      let workflow = { action: action?.action, comments: data?.comments, businessService, moduleName: moduleCode };
    if (uploadedFile)
      workflow["documents"] = [
        {
          documentType: action?.action + " DOC",
          fileName: file?.name,
          fileStoreId: uploadedFile,
        },
      ];
    submitAction({
        waterTankerBookingDetail: 
          {
        ...applicationData,
        workflow,
      },
    });
   
  }

  useEffect(() => {
    if (action) {
      setConfig(
        configWTApproverApplication({
          t,
          action,
          selectFile,
          uploadedFile,
          setUploadedFile,
          selectedApprover,
          setSelectedApprover,
          vendorDescription: dsoData ? vendorDescription : undefined,
          vehicleDescription: vehicleData ? vehicleDescription : undefined, 
          selectVehicle,
          setSelectVehicle,
        })
      );
      
    }
  },[action, approvers, uploadedFile, dsoData,selectVehicle]);

  return action && config.form ? (
    <Modal
      headerBarMain={<Heading label={t(config.label.heading)} />}
      headerBarEnd={<CloseBtn onClick={closeModal} />}
      actionCancelLabel={t(config.label.cancel)}
      actionCancelOnSubmit={closeModal}
      actionSaveLabel={t(config.label.submit)}
      actionSaveOnSubmit={() => {}}
      formId="modal-action"
    >
       
      <FormComposer
        config={config.form}
        noBoxShadow
        inline
        childrenAtTheBottom
        onSubmit={submit}
        defaultValues={defaultValues}
        formId="modal-action"
      />
      
    </Modal>
  ) : (
    <Loader />
  );
};

export default ActionModal;
