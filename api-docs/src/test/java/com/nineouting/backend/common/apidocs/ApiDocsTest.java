package com.nineouting.backend.common.apidocs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nineouting.backend.common.security.jwt.AuthenticatedUser;
import com.nineouting.backend.common.security.web.CurrentUser;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = ApiDocsTest.TestApplication.class, properties = "spring.application.name=sample-service")
@AutoConfigureMockMvc
class ApiDocsTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 공개_API만_문서에_나오고_JWT_입력과_서비스_이름이_들어간다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("sample-service API"))
                .andExpect(jsonPath("$.servers[0].url").value("/"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/things/{id}']").exists())
                .andExpect(jsonPath("$.paths['/internal/things']").doesNotExist())
                // @CurrentUser는 토큰에서 채워지므로 파라미터로 나오지 않는다
                .andExpect(jsonPath("$.paths['/api/things/{id}'].get.parameters[*].name", Matchers.contains("id")));
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @SpringBootApplication
    static class TestApplication {

        @RestController
        static class ThingController {

            @GetMapping("/api/things/{id}")
            public String thing(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
                return "thing";
            }

            @GetMapping("/internal/things")
            public String internal() {
                return "internal";
            }
        }
    }
}
