package com.gra.paradise.botattendance.service.csv;

import com.gra.paradise.botattendance.model.csv.CsvAnalysisResult;
import com.gra.paradise.botattendance.model.csv.MilitaryPersonnel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for CSV analysis system.
 * Tests the complete flow from file upload to analysis results.
 */
@SpringBootTest
class CsvAnalysisIntegrationTest {
    
    @Autowired
    private CsvAnalysisService csvAnalysisService;
    
    private MockMultipartFile testCsvFile;
    
    @BeforeEach
    void setUp() {
        String csvContent = """
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,1º Batalhão de Polícia Militar Alta Paradise,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Registro de Policiais,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,Cadete
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Identificação,,,,,,Unidades,,,,,,,,Cursos/Cerficações,,,,,,,,,,,,Administrativo,,,,,,,,,,,,,,,,,,,,,
                1,ID,Patente,PTT,Nome,Unidade,,GIC,PER,GOT,GRA,GTM,SPD,SASP,,AB,AC,CO,BO,SUL,HC,P1,P2,P3,P4,AET,,CMD,INST,CRS,CRE,CLG,ADVs,,,Status,,Medalhas,,,,,,,,,Entrada,Última promoção,
                ,102,Comissário,1,Logan Reynolds,S.A.S.P,,,,,,,,,,,,,,,,,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,30/05/2025,
                ,2893,Coronel,4,Otávio De Franceschi,RPM,,,,COMANDO GERAL,,,,,,,,,,,,,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,22/07/2025,
                ,583,3º Sargento,13,Adeusa Dokasi Void,GOT,TRUE,,TRUE,,,,,,,,,,,,,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,15/09/2025,
                ,976,Cabo,14,Blake Bratz,SPEED,,,,,,,TRUE,,,,,,,,,,,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,24/07/2025,
                ,123,Soldado 1ª C,15,Test Soldier,GTM,TRUE,TRUE,,,,,,,,,TRUE,TRUE,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,10/07/2025,15/01/2024,
                """;
        
        testCsvFile = new MockMultipartFile(
                "file", 
                "test-personnel.csv", 
                "text/csv", 
                csvContent.getBytes()
        );
    }
    
    @Test
    void shouldParseCsvFile() throws CsvAnalysisService.CsvAnalysisException {
        List<MilitaryPersonnel> personnel = csvAnalysisService.parseCsvFile(testCsvFile);
        
        assertNotNull(personnel);
        assertTrue(personnel.size() >= 3); // At least the valid records
        
        // Verify we can find specific personnel
        boolean foundLogan = personnel.stream()
                .anyMatch(p -> "Logan Reynolds".equals(p.getName()));
        assertTrue(foundLogan, "Should find Logan Reynolds in parsed data");
        
        boolean foundOtavio = personnel.stream()
                .anyMatch(p -> "Otávio De Franceschi".equals(p.getName()));
        assertTrue(foundOtavio, "Should find Otávio De Franceschi in parsed data");
    }
    
    @Test
    void shouldAnalyzeCsvFile() throws CsvAnalysisService.CsvAnalysisException {
        CsvAnalysisResult result = csvAnalysisService.analyzeCsvFile(
                testCsvFile, 
                "COMPREHENSIVE_MILITARY_ANALYSIS"
        );
        
        assertNotNull(result);
        assertEquals("test-personnel.csv", result.getFileName());
        assertTrue(result.getTotalRecords() >= 3);
        assertNotNull(result.getAnalysisDateTime());
        
        // Verify analysis components
        assertNotNull(result.getRankDistribution());
        assertNotNull(result.getUnitDistribution());
        assertNotNull(result.getKeyInsights());
        
        // Should have some insights
        assertFalse(result.getKeyInsights().isEmpty());
        
        // Should have rank categories
        assertNotNull(result.getRankCategoryDistribution());
        assertTrue(result.getRankCategoryDistribution().size() > 0);
    }
    
    @Test
    void shouldHandleEmptyFile() {
        String emptyContent = """
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,1º Batalhão de Polícia Militar Alta Paradise,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Registro de Policiais,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,Cadete
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Identificação,,,,,,Unidades,,,,,,,,Cursos/Cerficações,,,,,,,,,,,,Administrativo,,,,,,,,,,,,,,,,,,,,,
                1,ID,Patente,PTT,Nome,Unidade,,GIC,PER,GOT,GRA,GTM,SPD,SASP,,AB,AC,CO,BO,SUL,HC,P1,P2,P3,P4,AET,,CMD,INST,CRS,CRE,CLG,ADVs,,,Status,,Medalhas,,,,,,,,,Entrada,Última promoção,
                """;
        
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                emptyContent.getBytes()
        );
        
        assertDoesNotThrow(() -> {
            CsvAnalysisResult result = csvAnalysisService.analyzeCsvFile(
                    emptyFile, 
                    "COMPREHENSIVE_MILITARY_ANALYSIS"
            );
            assertNotNull(result);
            assertEquals(0L, result.getTotalRecords());
        });
    }
    
    @Test
    void shouldGetAvailableAnalysisTypes() {
        List<String> types = csvAnalysisService.getAvailableAnalysisTypes();
        
        assertNotNull(types);
        assertFalse(types.isEmpty());
        assertTrue(types.contains("COMPREHENSIVE_MILITARY_ANALYSIS"));
    }
    
    @Test
    void shouldThrowExceptionForInvalidFileType() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "invalid content".getBytes()
        );
        
        assertThrows(CsvAnalysisService.CsvAnalysisException.class, () -> {
            csvAnalysisService.parseCsvFile(invalidFile);
        });
    }
    
    @Test
    void shouldThrowExceptionForInvalidAnalysisType() {
        assertThrows(CsvAnalysisService.CsvAnalysisException.class, () -> {
            csvAnalysisService.analyzeCsvFile(testCsvFile, "INVALID_ANALYSIS_TYPE");
        });
    }
}