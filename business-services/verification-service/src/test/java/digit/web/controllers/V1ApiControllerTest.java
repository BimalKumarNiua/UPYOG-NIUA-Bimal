package digit.web.controllers;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.upyog.web.controllers.VerificationServiceController;
import org.upyog.web.models.CommonModuleRequest;
import org.upyog.web.models.CommonModuleResponse;
import org.upyog.web.models.ErrorRes;
import org.upyog.web.models.RequestInfo;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import digit.TestConfiguration;

    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
* API tests for VerificationServiceController
*/
@Ignore
@RunWith(SpringRunner.class)
@WebMvcTest(VerificationServiceController.class)
@Import(TestConfiguration.class)
public class V1ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void v1RegistrationCreatePostSuccess() throws Exception {
        mockMvc.perform(post("/birth-services/v1/registration/_create").contentType(MediaType
        .APPLICATION_JSON_UTF8))
        .andExpect(status().isOk());
    }

    @Test
    public void v1RegistrationCreatePostFailure() throws Exception {
        mockMvc.perform(post("/birth-services/v1/registration/_create").contentType(MediaType
        .APPLICATION_JSON_UTF8))
        .andExpect(status().isBadRequest());
    }

    @Test
    public void v1RegistrationSearchPostSuccess() throws Exception {
        mockMvc.perform(post("/birth-services/v1/registration/_search").contentType(MediaType
        .APPLICATION_JSON_UTF8))
        .andExpect(status().isOk());
    }

    @Test
    public void v1RegistrationSearchPostFailure() throws Exception {
        mockMvc.perform(post("/birth-services/v1/registration/_search").contentType(MediaType
        .APPLICATION_JSON_UTF8))
        .andExpect(status().isBadRequest());
    }

    @Test
    public void v1RegistrationUpdatePostSuccess() throws Exception {
        mockMvc.perform(post("/birth-services/v1/registration/_update").contentType(MediaType
        .APPLICATION_JSON_UTF8))
        .andExpect(status().isOk());
    }

    @Test
    public void v1RegistrationUpdatePostFailure() throws Exception {
        mockMvc.perform(post("/birth-services/v1/registration/_update").contentType(MediaType
        .APPLICATION_JSON_UTF8))
        .andExpect(status().isBadRequest());
    }

}
