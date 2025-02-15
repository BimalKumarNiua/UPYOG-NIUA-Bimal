import { AppContainer, BackButton, PrivateRoute } from "@nudmcdgnpm/digit-ui-react-components";
import React from "react";
import {Redirect, Switch, useRouteMatch } from "react-router-dom";

// Main Routing Page used for routing accorss the Water Tanker Module
const App = () => {
  const { path, url, ...match } = useRouteMatch();
  const WTCreate = Digit?.ComponentRegistryService?.getComponent("WTCreate");
  const WTApplicationDetails = Digit?.ComponentRegistryService?.getComponent("WTApplicationDetails");
  const WTMyApplications = Digit?.ComponentRegistryService?.getComponent("WTMyApplications");
  const Inbox = Digit.ComponentRegistryService.getComponent("WTEmpInbox");
  const WTCard = Digit.ComponentRegistryService.getComponent("WTCitizenCard");
  const ApplicationDetails = Digit?.ComponentRegistryService?.getComponent("ApplicationDetails");
  const inboxInitialState = {
    searchParams: {
      uuid: { code: "ASSIGNED_TO_ALL", name: "ES_INBOX_ASSIGNED_TO_ALL" },
      services: ["watertanker"],
      applicationStatus: [],
      locality: [],

    },
  };
 
  return (
    <span style={{width:"100%"}}>
      <Switch>
        <AppContainer>
          <BackButton>Back</BackButton>
          <PrivateRoute
            path={`${path}/inbox`}
            component={() => (
              <Inbox
                useNewInboxAPI={true}
                parentRoute={path}
                businessService="watertanker"
                filterComponent="WT_INBOX_FILTER"
                initialStates={inboxInitialState}
                isInbox={true}
              />
            )}
          />
          <PrivateRoute path={`${path}/request-service`} component={WTCreate} />
          <PrivateRoute path={`${path}/status`} component={WTMyApplications}></PrivateRoute>
          <PrivateRoute path={`${path}/booking/:acknowledgementIds/:tenantId`} component={WTApplicationDetails}></PrivateRoute>
          <PrivateRoute path={`${path}/booking-details/:id`} component={() => <ApplicationDetails parentRoute={path} />} />
          <PrivateRoute path={`${path}/bookingsearch/booking-details/:id`} component={() => <ApplicationDetails parentRoute={path} />} />
          <PrivateRoute path={`${path}/wt-Vendor`} component={() => Digit.UserService.hasAccess(["WT_VENDOR"]) ?  <WTCard parentRoute={path} /> :<Redirect to={{
            pathname: "/digit-ui/citizen/login",
            state: { from: `${path}/wt-Vendor`, role:"WT_VENDOR" }
          }} />} />
        </AppContainer>
      </Switch>
    </span>
  );
};

export default App;