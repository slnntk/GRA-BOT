package com.gra.paradise.botattendance.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for CSV Analysis REST endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
class CsvAnalysisControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/csv/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("CSV Analysis Service"));
    }
    
    @Test
    void shouldReturnAvailableAnalysisTypes() throws Exception {
        mockMvc.perform(get("/api/csv/analysis-types"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value("COMPREHENSIVE_MILITARY_ANALYSIS"));
    }
    
    @Test
    void shouldAnalyzeCsvFile() throws Exception {
        String csvContent = """
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,1º Batalhão de Polícia Militar Alta Paradise,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Registro de Policiais,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,Cadete
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Identificação,,,,,,Unidades,,,,,,,,Cursos/Cerficações,,,,,,,,,,,,Administrativo,,,,,,,,,,,,,,,,,,,,,
                1,ID,Patente,PTT,Nome,Unidade,,GIC,PER,GOT,GRA,GTM,SPD,SASP,,AB,AC,CO,BO,SUL,HC,P1,P2,P3,P4,AET,,CMD,INST,CRS,CRE,CLG,ADVs,,,Status,,Medalhas,,,,,,,,,Entrada,Última promoção,
                ,102,Comissário,1,Logan Reynolds,S.A.S.P,,,,,,,,,,,,,,,,,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,30/05/2025,
                ,583,3º Sargento,13,Adeusa Dokasi Void,GOT,TRUE,,TRUE,,,,,,,,,,,,,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,15/09/2025,
                """;
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes()
        );
        
        mockMvc.perform(multipart("/api/csv/analyze")
                        .file(file)
                        .param("analysisType", "COMPREHENSIVE_MILITARY_ANALYSIS"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.fileName").value("test.csv"))
                .andExpect(jsonPath("$.totalRecords").exists())
                .andExpect(jsonPath("$.analysisDateTime").exists())
                .andExpect(jsonPath("$.keyInsights").isArray());
    }
    
    @Test
    void shouldParseCsvFile() throws Exception {
        String csvContent = """
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,1º Batalhão de Polícia Militar Alta Paradise,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Registro de Policiais,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,Cadete
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Identificação,,,,,,Unidades,,,,,,,,Cursos/Cerficações,,,,,,,,,,,,Administrativo,,,,,,,,,,,,,,,,,,,,,
                1,ID,Patente,PTT,Nome,Unidade,,GIC,PER,GOT,GRA,GTM,SPD,SASP,,AB,AC,CO,BO,SUL,HC,P1,P2,P3,P4,AET,,CMD,INST,CRS,CRE,CLG,ADVs,,,Status,,Medalhas,,,,,,,,,Entrada,Última promoção,
                ,102,Comissário,1,Logan Reynolds,S.A.S.P,,,,,,,,,,,,,,,,,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,30/05/2025,
                """;
        
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes()
        );
        
        mockMvc.perform(multipart("/api/csv/parse").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalRecords").exists())
                .andExpect(jsonPath("$.personnel").isArray());
    }
    
    @Test
    void shouldReturnBadRequestForInvalidFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "invalid content".getBytes()
        );
        
        mockMvc.perform(multipart("/api/csv/analyze").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());
    }
}