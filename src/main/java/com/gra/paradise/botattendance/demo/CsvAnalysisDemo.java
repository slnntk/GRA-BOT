package com.gra.paradise.botattendance.demo;

import com.gra.paradise.botattendance.model.csv.CsvAnalysisResult;
import com.gra.paradise.botattendance.service.csv.CsvAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;

/**
 * Demo application to test CSV analysis functionality.
 * Enable with: --csv.demo.enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "csv.demo.enabled", havingValue = "true")
public class CsvAnalysisDemo implements CommandLineRunner {
    
    private final CsvAnalysisService csvAnalysisService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("=".repeat(80));
        log.info("CSV ANALYSIS DEMO - STARTED");
        log.info("=".repeat(80));
        
        String sampleCsv = createSampleCsv();
        MockMultipartFile testFile = new MockMultipartFile(
                "demo",
                "demo.csv",
                "text/csv",
                sampleCsv.getBytes()
        );
        
        try {
            log.info("Available analysis types: {}", csvAnalysisService.getAvailableAnalysisTypes());
            
            CsvAnalysisResult result = csvAnalysisService.analyzeCsvFile(
                    testFile,
                    "COMPREHENSIVE_MILITARY_ANALYSIS"
            );
            
            printAnalysisResults(result);
            
        } catch (Exception e) {
            log.error("Error during demo analysis", e);
        }
        
        log.info("=".repeat(80));
        log.info("CSV ANALYSIS DEMO - COMPLETED");
        log.info("=".repeat(80));
    }
    
    private String createSampleCsv() {
        return """
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
                ,456,Aluno,17,João Silva,RPM,,TRUE,,,,,,,,,,,,,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,01/09/2025,
                """;
    }
    
    private void printAnalysisResults(CsvAnalysisResult result) {
        log.info("\n" + "=".repeat(60));
        log.info("ANALYSIS RESULTS");
        log.info("=".repeat(60));
        log.info("File: {}", result.getFileName());
        log.info("Analysis Time: {}", result.getAnalysisDateTime());
        log.info("Total Records: {}", result.getTotalRecords());
        log.info("Valid Records: {}", result.getValidRecords());
        log.info("Data Completeness: {:.1f}%", result.getDataCompletenessPercentage());
        
        log.info("\nRANK DISTRIBUTION:");
        result.getRankDistribution().forEach((rank, count) -> 
                log.info("  {} : {}", rank, count));
        
        log.info("\nRANK CATEGORIES:");
        result.getRankCategoryDistribution().forEach((category, count) -> 
                log.info("  {} : {}", category, count));
        
        log.info("\nUNIT DISTRIBUTION:");
        result.getUnitDistribution().forEach((unit, count) -> 
                log.info("  {} : {}", unit, count));
        
        log.info("\nCERTIFICATION ANALYSIS:");
        log.info("  Average certifications per person: {:.2f}", result.getAverageCertificationsPerPerson());
        log.info("  Most common certification: {}", result.getMostCommonCertification());
        
        log.info("\nCERTIFICATION COUNTS:");
        result.getCertificationCounts().forEach((cert, count) -> 
                log.info("  {} : {}", cert, count));
        
        log.info("\nADMINISTRATIVE ROLES:");
        log.info("  Average admin roles per person: {:.2f}", result.getAverageAdministrativeRolesPerPerson());
        
        log.info("\nTOP CERTIFIED PERSONNEL:");
        result.getTopCertifiedPersonnel().forEach(person -> 
                log.info("  {} ({}) - {} certifications", person.getName(), person.getRank(), person.getTotalCertifications()));
        
        log.info("\nKEY INSIGHTS:");
        result.getKeyInsights().forEach(insight -> 
                log.info("  • {}", insight));
        
        if (!result.getDataQualityIssues().isEmpty()) {
            log.info("\nDATA QUALITY ISSUES:");
            result.getDataQualityIssues().forEach(issue -> 
                    log.warn("  ! {}", issue));
        }
    }
}