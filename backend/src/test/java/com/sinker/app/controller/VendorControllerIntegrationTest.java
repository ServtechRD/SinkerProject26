package com.sinker.app.controller;

import com.sinker.app.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Timeout(60)
class VendorControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private JdbcTemplate jdbc;

    private String token;

    @BeforeEach
    void setUp() {
        token = tokenProvider.generateToken(999999L, "test_vendor_user", "sales");
        jdbc.update("DELETE FROM vendor WHERE code IN ('V001', 'V002')");
        jdbc.update("INSERT INTO vendor (code, name) VALUES ('V001', '總公司')");
        jdbc.update("INSERT INTO vendor (code, name) VALUES ('V002', '測試分公司')");
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM vendor WHERE code IN ('V001', 'V002')");
    }

    @Test
    void findVendors_withoutKeyword_returnsAllOrderedByCode() throws Exception {
        mockMvc.perform(get("/api/vendors")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code", is("V001")))
                .andExpect(jsonPath("$[1].code", is("V002")));
    }

    @Test
    void findVendors_withKeyword_filtersByCodeOrName() throws Exception {
        mockMvc.perform(get("/api/vendors")
                        .param("keyword", "分公司")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code", is("V002")));
    }

    @Test
    void findVendors_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/vendors"))
                .andExpect(status().isUnauthorized());
    }
}
