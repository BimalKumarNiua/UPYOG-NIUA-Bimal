import React, { useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { SubmitBar, ActionBar, Menu, CardLabel } from "@upyog/digit-ui-react-components";
import { useParams } from "react-router-dom";

function ApplicationDetailsActionBar({
  workflowDetails,
  data,
  displayMenu,
  onActionSelect,
  setDisplayMenu,
  businessService,
  forcedActionPrefix,
  ActionBarStyle = {},
  MenuStyle = {},
}) {
  const { t } = useTranslation();
  let user = Digit.UserService.getUser();
  const menuRef = useRef();
  if (window.location.href.includes("/obps") || window.location.href.includes("/noc")) {
    const userInfos = sessionStorage.getItem("Digit.citizen.userRequestObject");
    const userInfo = userInfos ? JSON.parse(userInfos) : {};
    user = userInfo?.value;
  }

  const { id: requestId } = useParams();
  const tenantId = Digit.ULBService.getCurrentTenantId();

  const { data: applicationDetails } = Digit.Hooks.ew.useEwApplicationDetail(t, tenantId, requestId);
  console.log("applicationDetails", applicationDetails);
  const Session = Digit.SessionStorage.get("User");
  console.log("Session", Session);
  const uuid = Session?.info?.uuid;
  const modified = applicationDetails?.applicationData?.applicationData?.auditDetails?.lastModifiedBy;

  const userRoles = user?.info?.roles?.map((e) => e.code);
  let isSingleButton = false;
  let isMenuBotton = false;
  let actions =
    workflowDetails?.data?.actionState?.nextActions?.filter((e) => {
      return userRoles?.some((role) => e.roles?.includes(role)) || !e.roles;
    }) ||
    workflowDetails?.data?.nextActions?.filter((e) => {
      return userRoles?.some((role) => e.roles?.includes(role)) || !e.roles;
    });

  const closeMenu = () => {
    setDisplayMenu(false);
  };
  Digit.Hooks.useClickOutside(menuRef, closeMenu, displayMenu);

  if (
    ((window.location.href.includes("/obps") || window.location.href.includes("/noc")) && actions?.length == 1) ||
    (actions?.[0]?.redirectionUrl?.pathname.includes("/pt/property-details/") && actions?.length == 1)
  ) {
    isMenuBotton = false;
    isSingleButton = true;
  } else if (actions?.length > 0) {
    isMenuBotton = true;
    isSingleButton = false;
  }

  return (
    <React.Fragment>
      {!workflowDetails?.isLoading && isMenuBotton && !isSingleButton && (
        <ActionBar style={{ ...ActionBarStyle }}>
          {displayMenu && (workflowDetails?.data?.actionState?.nextActions || workflowDetails?.data?.nextActions) ? (
            <Menu
              localeKeyPrefix={forcedActionPrefix || `WF_EMPLOYEE_${businessService?.toUpperCase()}`}
              options={actions}
              optionKey={"action"}
              t={t}
              onSelect={onActionSelect}
              style={MenuStyle}
            />
          ) : null}
          {businessService === "ewst" ? (
            modified === uuid || modified == null ? (
              <SubmitBar ref={menuRef} label={t("WF_TAKE_ACTION")} onSubmit={() => setDisplayMenu(!displayMenu)} />
            ) : (
              <CardLabel style={{ color: "red", font: "30px", fontWeight: "bold" }}>{`${t("EW_ALERT_ANOTHER_VENDOR")}`}</CardLabel>
            )
          ) : (
            <SubmitBar ref={menuRef} label={t("WF_TAKE_ACTION")} onSubmit={() => setDisplayMenu(!displayMenu)} />
          )}
          {/* Conditional rendering for e-waste service */}
          {/* <SubmitBar ref={menuRef} label={t("WF_TAKE_ACTION")} onSubmit={() => setDisplayMenu(!displayMenu)} /> */}
        </ActionBar>
      )}
      {!workflowDetails?.isLoading && !isMenuBotton && isSingleButton && (
        <ActionBar style={{ ...ActionBarStyle }}>
          <button
            style={{ color: "#FFFFFF", fontSize: "18px" }}
            className={"submit-bar"}
            name={actions?.[0]?.action}
            value={actions?.[0]?.action}
            onClick={(e) => {
              onActionSelect(actions?.[0] || {});
            }}
          >
            {t(`${forcedActionPrefix || `WF_EMPLOYEE_${businessService?.toUpperCase()}`}_${actions?.[0]?.action}`)}
          </button>
        </ActionBar>
      )}
    </React.Fragment>
  );
}

export default ApplicationDetailsActionBar;