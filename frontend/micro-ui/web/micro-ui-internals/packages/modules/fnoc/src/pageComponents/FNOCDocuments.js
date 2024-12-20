import React, { useEffect, useState } from "react";
import { CardLabel, Dropdown, UploadFile, Toast, Loader, FormStep, LabelFieldPair } from "@nudmcdgnpm/digit-ui-react-components";


const FNOCDocuments = ({ t, config, onSelect, formData }) => {
  const [documents, setDocuments] = useState(formData?.documents?.documents || []);
  const [error, setError] = useState(null);
  const [enableSubmit, setEnableSubmit] = useState(true);
  const [checkRequiredFields, setCheckRequiredFields] = useState(true);

  const stateId = Digit.ULBService.getStateId();
  const { isLoading, data } = Digit.Hooks.fnoc.useFnocDoc(stateId, "FireNoc", "Documents");

  const handleSubmit = () => {
    let documentStep = { ...formData.documents, documents };
    onSelect(config.key, documentStep);
  };

  const onSkip = () => onSelect();

  useEffect(() => {
    let count = 0;
    data?.FireNoc?.Documents.forEach((doc) => {
      doc.hasDropdown = true;
      let isRequired = documents.some((data) => doc.required && data?.documentType.includes(doc.code));
      if (!isRequired && doc.required) count += 1;
    });
    setEnableSubmit(!(count === 0 && documents.length > 0));
  }, [documents, checkRequiredFields, data]);

  return (
    <div>
      {/* <Timeline currentStep={4} /> */}
      {!isLoading ? (
        <FormStep t={t} config={config} onSelect={handleSubmit} onSkip={onSkip} isDisabled={enableSubmit}>
          {data?.FireNoc?.Documents?.map((document, index) => (
            <FNOCSelectDocument
              key={index}
              document={document}
              t={t}
              error={error}
              setError={setError}
              setDocuments={setDocuments}
              documents={documents}
              setCheckRequiredFields={setCheckRequiredFields}
              formData={formData}
            />
          ))}
          {error && <Toast label={error} onClose={() => setError(null)} error />}
        </FormStep>
      ) : (
        <Loader />
      )}
    </div>
  );
};

function FNOCSelectDocument({
  t,
  document: doc,
  setDocuments,
  setError,
  documents,
  formData,
  id,
}) {
  const filteredDocument = documents?.find((item) => item?.documentType?.includes(doc?.code));

  const [selectedDocument, setSelectedDocument] = useState(
    filteredDocument
      ? { ...filteredDocument, active: doc?.active === true, code: filteredDocument?.documentType }
      : doc?.dropdownData?.length === 1
      ? doc?.dropdownData[0]
      : {}
  );

  const [file, setFile] = useState(null);
  const [uploadedFile, setUploadedFile] = useState(filteredDocument?.fileStoreId || null);
  

  const handleFNOCSelectDocument = (value) => setSelectedDocument(value);

 

 

  useEffect(() => {
    if (file) {
      if (file.size >= 5242880) {
        setError(t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED"));
      } else {
        setUploadedFile(null);
        Digit.UploadServices.Filestorage("FireNoc", file, Digit.ULBService.getStateId())
          .then(response => {
            if (response?.data?.files?.length > 0) {
              setUploadedFile(response.data.files[0].fileStoreId);
            } else {
              setError(t("CS_FILE_UPLOAD_ERROR"));
            }
          })
          .catch(() => setError(t("CS_FILE_UPLOAD_ERROR")));
      }
    }
  }, [file, t]);

  useEffect(() => {
    if (selectedDocument?.code) {
      setDocuments((prev) => {
        const filteredDocumentsByDocumentType = prev?.filter((item) => item?.documentType !== selectedDocument?.code);
        if (!uploadedFile) {
          return filteredDocumentsByDocumentType;
        }
        const filteredDocumentsByFileStoreId = filteredDocumentsByDocumentType?.filter((item) => item?.fileStoreId !== uploadedFile);
        return [
          ...filteredDocumentsByFileStoreId,
          {
            documentType: selectedDocument?.code,
            fileStoreId: uploadedFile,
            documentUid: uploadedFile,
          },
        ];
      });
    }
  }, [uploadedFile, selectedDocument,setDocuments]);

  const handleFileUpload = (e) => {
    const file = e.target.files[0];
    setFile(file);
  };


  return (
    <div style={{ marginBottom: "24px" }}>
      {doc?.hasDropdown && (
        <LabelFieldPair>
          <CardLabel className="card-label-smaller">{t(doc?.code.replaceAll(".", "_")) + "  *"}</CardLabel>
          <Dropdown
            className="form-field"
            selected={selectedDocument}
            style={{ width: "80%" }}
            option={[
                ...(doc?.dropdownData || []).map((e) => ({ ...e, i18nKey: e.code?.replaceAll(".", "_") })),
                ...(doc?.options || []).map((e) => ({ ...e, i18nKey: e.code?.replaceAll(".", "_") }))
              ]}
            select={handleFNOCSelectDocument}
            optionKey="i18nKey"
            t={t}
            placeholder={"Select"}
          />
        </LabelFieldPair>
      )}
      <LabelFieldPair>
        <CardLabel className="card-label-smaller"></CardLabel>
        <div className="field">
          <UploadFile
            onUpload={handleFileUpload}
            onDelete={() => {
              setUploadedFile(null);
            }}
            id={id}
            message={uploadedFile ? `1 ${t(`CS_ACTION_FILEUPLOADED`)}` : t(`CS_ACTION_NO_FILEUPLOADED`)}
            textStyles={{ width: "100%" }}
            inputStyles={{ width: "280px" }}
            accept=".pdf, .jpeg, .jpg, .png"
            buttonType="button"
            error={!uploadedFile}
          />
        </div>
      </LabelFieldPair>
    </div>
  );
}

export default FNOCDocuments;
