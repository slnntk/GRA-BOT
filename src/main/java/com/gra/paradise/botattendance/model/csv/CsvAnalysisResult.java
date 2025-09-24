package com.gra.paradise.botattendance.model.csv;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Domain model representing analysis results for CSV data.
 * Contains statistical information and insights about the data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvAnalysisResult {
    
    private String fileName;
    private LocalDateTime analysisDateTime;
    private Long totalRecords;
    private Long validRecords;
    private Long invalidRecords;
    
    // Statistical summaries
    private Map<String, Long> rankDistribution;
    private Map<String, Long> unitDistribution;
    private Map<String, Long> statusDistribution;
    private Map<String, Long> rankCategoryDistribution;
    
    // Certification analysis
    private Map<String, Long> certificationCounts;
    private Double averageCertificationsPerPerson;
    private String mostCommonCertification;
    
    // Administrative roles analysis
    private Map<String, Long> administrativeRoleCounts;
    private Double averageAdministrativeRolesPerPerson;
    
    // Time-based analysis
    private Map<String, Long> entryDatesByYear;
    private Map<String, Long> promotionDatesByYear;
    
    // Data quality metrics
    private List<String> dataQualityIssues;
    private Double dataCompletenessPercentage;
    
    // Top performers (most certifications/roles)
    private List<MilitaryPersonnel> topCertifiedPersonnel;
    private List<MilitaryPersonnel> mostAdministrativeRoles;
    
    // Summary insights
    private List<String> keyInsights;
    
    public void addDataQualityIssue(String issue) {
        this.dataQualityIssues.add(issue);
    }
    
    public void addKeyInsight(String insight) {
        this.keyInsights.add(insight);
    }
}