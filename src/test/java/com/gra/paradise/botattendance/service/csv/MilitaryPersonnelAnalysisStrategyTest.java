package com.gra.paradise.botattendance.service.csv;

import com.gra.paradise.botattendance.model.csv.CsvAnalysisResult;
import com.gra.paradise.botattendance.model.csv.MilitaryPersonnel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MilitaryPersonnelAnalysisStrategy.
 */
class MilitaryPersonnelAnalysisStrategyTest {
    
    private MilitaryPersonnelAnalysisStrategy analysisStrategy;
    private List<MilitaryPersonnel> testData;
    
    @BeforeEach
    void setUp() {
        analysisStrategy = new MilitaryPersonnelAnalysisStrategy();
        setupTestData();
    }
    
    private void setupTestData() {
        MilitaryPersonnel officer = MilitaryPersonnel.builder()
                .id(1L)
                .name("Test Officer")
                .rank("Coronel")
                .unit("RPM")
                .status("ATIVO")
                .gic(true)
                .per(true)
                .got(true)
                .ab(true)
                .ac(true)
                .entryDate(LocalDate.of(2020, 1, 1))
                .lastPromotionDate(LocalDate.of(2023, 1, 1))
                .build();
        officer.calculateTotalCertifications();
        officer.calculateTotalAdministrativeRoles();
        officer.determineRankCategory();
        
        MilitaryPersonnel soldier = MilitaryPersonnel.builder()
                .id(2L)
                .name("Test Soldier")
                .rank("Soldado 1ª C")
                .unit("GTM")
                .status("ATIVO")
                .spd(true)
                .p1(true)
                .entryDate(LocalDate.of(2022, 6, 15))
                .lastPromotionDate(LocalDate.of(2024, 6, 15))
                .build();
        soldier.calculateTotalCertifications();
        soldier.calculateTotalAdministrativeRoles();
        soldier.determineRankCategory();
        
        MilitaryPersonnel student = MilitaryPersonnel.builder()
                .id(3L)
                .name("Test Student")
                .rank("Aluno")
                .unit("GOT")
                .status("ATIVO")
                .entryDate(LocalDate.of(2024, 9, 1))
                .build();
        student.calculateTotalCertifications();
        student.calculateTotalAdministrativeRoles();
        student.determineRankCategory();
        
        testData = Arrays.asList(officer, soldier, student);
    }
    
    @Test
    void shouldReturnCorrectAnalysisType() {
        assertEquals("COMPREHENSIVE_MILITARY_ANALYSIS", analysisStrategy.getAnalysisType());
    }
    
    @Test
    void shouldAnalyzePersonnelData() {
        CsvAnalysisResult result = analysisStrategy.analyze(testData, "test.csv");
        
        assertNotNull(result);
        assertEquals("test.csv", result.getFileName());
        assertEquals(3L, result.getTotalRecords());
        assertEquals(3L, result.getValidRecords());
        assertEquals(0L, result.getInvalidRecords());
        assertNotNull(result.getAnalysisDateTime());
    }
    
    @Test
    void shouldCalculateRankDistribution() {
        CsvAnalysisResult result = analysisStrategy.analyze(testData, "test.csv");
        
        Map<String, Long> rankDistribution = result.getRankDistribution();
        assertNotNull(rankDistribution);
        assertEquals(1L, rankDistribution.get("Coronel"));
        assertEquals(1L, rankDistribution.get("Soldado 1ª C"));
        assertEquals(1L, rankDistribution.get("Aluno"));
    }
    
    @Test
    void shouldCalculateRankCategoryDistribution() {
        CsvAnalysisResult result = analysisStrategy.analyze(testData, "test.csv");
        
        Map<String, Long> rankCategoryDistribution = result.getRankCategoryDistribution();
        assertNotNull(rankCategoryDistribution);
        assertEquals(1L, rankCategoryDistribution.get("OFFICER"));
        assertEquals(1L, rankCategoryDistribution.get("SOLDIER"));
        assertEquals(1L, rankCategoryDistribution.get("STUDENT"));
    }
    
    @Test
    void shouldCalculateUnitDistribution() {
        CsvAnalysisResult result = analysisStrategy.analyze(testData, "test.csv");
        
        Map<String, Long> unitDistribution = result.getUnitDistribution();
        assertNotNull(unitDistribution);
        assertEquals(1L, unitDistribution.get("RPM"));
        assertEquals(1L, unitDistribution.get("GTM"));
        assertEquals(1L, unitDistribution.get("GOT"));
    }
    
    @Test
    void shouldCalculateCertificationAnalysis() {
        CsvAnalysisResult result = analysisStrategy.analyze(testData, "test.csv");
        
        Map<String, Long> certificationCounts = result.getCertificationCounts();
        assertNotNull(certificationCounts);
        assertEquals(1L, certificationCounts.get("GIC"));
        assertEquals(1L, certificationCounts.get("PER"));
        assertEquals(1L, certificationCounts.get("GOT"));
        assertEquals(1L, certificationCounts.get("SPD"));
        
        // Average: (3 + 1 + 0) / 3 = 1.33
        assertTrue(result.getAverageCertificationsPerPerson() > 1.0);
        assertTrue(result.getAverageCertificationsPerPerson() < 2.0);
        
        // Most common certification should be one of the single certifications
        assertNotNull(result.getMostCommonCertification());
    }
    
    @Test
    void shouldCalculateAdministrativeRolesAnalysis() {
        CsvAnalysisResult result = analysisStrategy.analyze(testData, "test.csv");
        
        Map<String, Long> adminRoleCounts = result.getAdministrativeRoleCounts();
        assertNotNull(adminRoleCounts);
        assertEquals(1L, adminRoleCounts.get("AB"));
        assertEquals(1L, adminRoleCounts.get("AC"));
        assertEquals(1L, adminRoleCounts.get("P1"));
        
        // Average: (2 + 1 + 0) / 3 = 1.0
        assertEquals(1.0, result.getAverageAdministrativeRolesPerPerson(), 0.01);
    }
    
    @Test
    void shouldCalculateTimeBasedAnalysis() {
        CsvAnalysisResult result = analysisStrategy.analyze(testData, "test.csv");
        
        Map<String, Long> entryDatesByYear = result.getEntryDatesByYear();
        assertNotNull(entryDatesByYear);
        assertEquals(1L, entryDatesByYear.get("2020"));
        assertEquals(1L, entryDatesByYear.get("2022"));
        assertEquals(1L, entryDatesByYear.get("2024"));
        
        Map<String, Long> promotionDatesByYear = result.getPromotionDatesByYear();
        assertNotNull(promotionDatesByYear);
        assertEquals(1L, promotionDatesByYear.get("2023"));
        assertEquals(1L, promotionDatesByYear.get("2024"));
    }
    
    @Test
    void shouldIdentifyTopPerformers() {
        CsvAnalysisResult result = analysisStrategy.analyze(testData, "test.csv");
        
        List<MilitaryPersonnel> topCertified = result.getTopCertifiedPersonnel();
        assertNotNull(topCertified);
        assertTrue(topCertified.size() > 0);
        // Officer should be first with 3 certifications
        assertEquals("Test Officer", topCertified.get(0).getName());
        
        List<MilitaryPersonnel> topAdminRoles = result.getMostAdministrativeRoles();
        assertNotNull(topAdminRoles);
        assertTrue(topAdminRoles.size() > 0);
        // Officer should be first with 2 admin roles
        assertEquals("Test Officer", topAdminRoles.get(0).getName());
    }
    
    @Test
    void shouldCalculateDataQuality() {
        CsvAnalysisResult result = analysisStrategy.analyze(testData, "test.csv");
        
        Double completenessPercentage = result.getDataCompletenessPercentage();
        assertNotNull(completenessPercentage);
        assertTrue(completenessPercentage > 50.0); // Should be reasonably complete
        
        List<String> qualityIssues = result.getDataQualityIssues();
        assertNotNull(qualityIssues);
        // All test records have complete basic data, so should be minimal issues
    }
    
    @Test
    void shouldGenerateKeyInsights() {
        CsvAnalysisResult result = analysisStrategy.analyze(testData, "test.csv");
        
        List<String> insights = result.getKeyInsights();
        assertNotNull(insights);
        assertFalse(insights.isEmpty());
        
        // Should contain insights about rank distribution, certifications, etc.
        boolean hasRankInsight = insights.stream()
                .anyMatch(insight -> insight.contains("Most common rank category"));
        assertTrue(hasRankInsight);
        
        boolean hasCertificationInsight = insights.stream()
                .anyMatch(insight -> insight.contains("Average certifications"));
        assertTrue(hasCertificationInsight);
        
        boolean hasActiveInsight = insights.stream()
                .anyMatch(insight -> insight.contains("Active personnel"));
        assertTrue(hasActiveInsight);
    }
    
    @Test
    void shouldHandleEmptyData() {
        CsvAnalysisResult result = analysisStrategy.analyze(Arrays.asList(), "empty.csv");
        
        assertNotNull(result);
        assertEquals(0L, result.getTotalRecords());
        assertEquals(0L, result.getValidRecords());
        assertNotNull(result.getRankDistribution());
        assertTrue(result.getRankDistribution().isEmpty());
    }
}