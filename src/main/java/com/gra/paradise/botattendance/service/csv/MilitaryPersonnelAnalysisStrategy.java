package com.gra.paradise.botattendance.service.csv;

import com.gra.paradise.botattendance.model.csv.CsvAnalysisResult;
import com.gra.paradise.botattendance.model.csv.MilitaryPersonnel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive analysis strategy for military personnel data.
 * Implements the Strategy pattern for CSV analysis.
 */
@Slf4j
@Component
public class MilitaryPersonnelAnalysisStrategy implements CsvAnalysisStrategy {
    
    @Override
    public CsvAnalysisResult analyze(List<MilitaryPersonnel> personnel, String fileName) {
        log.info("Starting comprehensive analysis for {} personnel records", personnel.size());
        
        CsvAnalysisResult.CsvAnalysisResultBuilder resultBuilder = CsvAnalysisResult.builder()
                .fileName(fileName)
                .analysisDateTime(LocalDateTime.now())
                .totalRecords((long) personnel.size())
                .validRecords((long) personnel.size())
                .invalidRecords(0L)
                .dataQualityIssues(new ArrayList<>())
                .keyInsights(new ArrayList<>());
        
        // Basic distributions
        Map<String, Long> rankDistribution = calculateRankDistribution(personnel);
        Map<String, Long> unitDistribution = calculateUnitDistribution(personnel);
        Map<String, Long> statusDistribution = calculateStatusDistribution(personnel);
        Map<String, Long> rankCategoryDistribution = calculateRankCategoryDistribution(personnel);
        
        // Certification analysis
        Map<String, Long> certificationCounts = calculateCertificationCounts(personnel);
        Double avgCertifications = calculateAverageCertifications(personnel);
        String mostCommonCertification = findMostCommonCertification(certificationCounts);
        
        // Administrative roles analysis
        Map<String, Long> adminRoleCounts = calculateAdministrativeRoleCounts(personnel);
        Double avgAdminRoles = calculateAverageAdministrativeRoles(personnel);
        
        // Time-based analysis
        Map<String, Long> entryDatesByYear = calculateEntryDatesByYear(personnel);
        Map<String, Long> promotionDatesByYear = calculatePromotionDatesByYear(personnel);
        
        // Top performers
        List<MilitaryPersonnel> topCertified = getTopCertifiedPersonnel(personnel, 5);
        List<MilitaryPersonnel> topAdminRoles = getTopAdministrativeRoles(personnel, 5);
        
        // Data quality assessment
        Double completenessPercentage = calculateDataCompleteness(personnel);
        List<String> qualityIssues = identifyDataQualityIssues(personnel);
        
        // Generate insights
        List<String> insights = generateKeyInsights(personnel, rankCategoryDistribution, 
                avgCertifications, avgAdminRoles, mostCommonCertification);
        
        return resultBuilder
                .rankDistribution(rankDistribution)
                .unitDistribution(unitDistribution)
                .statusDistribution(statusDistribution)
                .rankCategoryDistribution(rankCategoryDistribution)
                .certificationCounts(certificationCounts)
                .averageCertificationsPerPerson(avgCertifications)
                .mostCommonCertification(mostCommonCertification)
                .administrativeRoleCounts(adminRoleCounts)
                .averageAdministrativeRolesPerPerson(avgAdminRoles)
                .entryDatesByYear(entryDatesByYear)
                .promotionDatesByYear(promotionDatesByYear)
                .topCertifiedPersonnel(topCertified)
                .mostAdministrativeRoles(topAdminRoles)
                .dataCompletenessPercentage(completenessPercentage)
                .dataQualityIssues(qualityIssues)
                .keyInsights(insights)
                .build();
    }
    
    private Map<String, Long> calculateRankDistribution(List<MilitaryPersonnel> personnel) {
        return personnel.stream()
                .filter(p -> p.getRank() != null)
                .collect(Collectors.groupingBy(
                        MilitaryPersonnel::getRank,
                        Collectors.counting()
                ));
    }
    
    private Map<String, Long> calculateUnitDistribution(List<MilitaryPersonnel> personnel) {
        return personnel.stream()
                .filter(p -> p.getUnit() != null)
                .collect(Collectors.groupingBy(
                        MilitaryPersonnel::getUnit,
                        Collectors.counting()
                ));
    }
    
    private Map<String, Long> calculateStatusDistribution(List<MilitaryPersonnel> personnel) {
        return personnel.stream()
                .filter(p -> p.getStatus() != null)
                .collect(Collectors.groupingBy(
                        MilitaryPersonnel::getStatus,
                        Collectors.counting()
                ));
    }
    
    private Map<String, Long> calculateRankCategoryDistribution(List<MilitaryPersonnel> personnel) {
        return personnel.stream()
                .filter(p -> p.getRankCategory() != null)
                .collect(Collectors.groupingBy(
                        MilitaryPersonnel::getRankCategory,
                        Collectors.counting()
                ));
    }
    
    private Map<String, Long> calculateCertificationCounts(List<MilitaryPersonnel> personnel) {
        Map<String, Long> counts = new LinkedHashMap<>();
        
        counts.put("GIC", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getGic()) ? 1 : 0).sum());
        counts.put("PER", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getPer()) ? 1 : 0).sum());
        counts.put("GOT", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getGot()) ? 1 : 0).sum());
        counts.put("GRA", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getGra()) ? 1 : 0).sum());
        counts.put("GTM", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getGtm()) ? 1 : 0).sum());
        counts.put("SPD", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getSpd()) ? 1 : 0).sum());
        counts.put("SASP", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getSasp()) ? 1 : 0).sum());
        
        return counts;
    }
    
    private Double calculateAverageCertifications(List<MilitaryPersonnel> personnel) {
        return personnel.stream()
                .mapToInt(p -> p.getTotalCertifications() != null ? p.getTotalCertifications() : 0)
                .average()
                .orElse(0.0);
    }
    
    private String findMostCommonCertification(Map<String, Long> certificationCounts) {
        return certificationCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");
    }
    
    private Map<String, Long> calculateAdministrativeRoleCounts(List<MilitaryPersonnel> personnel) {
        Map<String, Long> counts = new LinkedHashMap<>();
        
        counts.put("AB", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getAb()) ? 1 : 0).sum());
        counts.put("AC", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getAc()) ? 1 : 0).sum());
        counts.put("CO", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getCo()) ? 1 : 0).sum());
        counts.put("BO", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getBo()) ? 1 : 0).sum());
        counts.put("SUL", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getSul()) ? 1 : 0).sum());
        counts.put("HC", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getHc()) ? 1 : 0).sum());
        counts.put("P1", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getP1()) ? 1 : 0).sum());
        counts.put("P2", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getP2()) ? 1 : 0).sum());
        counts.put("P3", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getP3()) ? 1 : 0).sum());
        counts.put("P4", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getP4()) ? 1 : 0).sum());
        counts.put("AET", personnel.stream().mapToLong(p -> Boolean.TRUE.equals(p.getAet()) ? 1 : 0).sum());
        
        return counts;
    }
    
    private Double calculateAverageAdministrativeRoles(List<MilitaryPersonnel> personnel) {
        return personnel.stream()
                .mapToInt(p -> p.getTotalAdministrativeRoles() != null ? p.getTotalAdministrativeRoles() : 0)
                .average()
                .orElse(0.0);
    }
    
    private Map<String, Long> calculateEntryDatesByYear(List<MilitaryPersonnel> personnel) {
        return personnel.stream()
                .filter(p -> p.getEntryDate() != null)
                .collect(Collectors.groupingBy(
                        p -> String.valueOf(p.getEntryDate().getYear()),
                        Collectors.counting()
                ));
    }
    
    private Map<String, Long> calculatePromotionDatesByYear(List<MilitaryPersonnel> personnel) {
        return personnel.stream()
                .filter(p -> p.getLastPromotionDate() != null)
                .collect(Collectors.groupingBy(
                        p -> String.valueOf(p.getLastPromotionDate().getYear()),
                        Collectors.counting()
                ));
    }
    
    private List<MilitaryPersonnel> getTopCertifiedPersonnel(List<MilitaryPersonnel> personnel, int limit) {
        return personnel.stream()
                .filter(p -> p.getTotalCertifications() != null && p.getTotalCertifications() > 0)
                .sorted((a, b) -> Integer.compare(
                        b.getTotalCertifications() != null ? b.getTotalCertifications() : 0,
                        a.getTotalCertifications() != null ? a.getTotalCertifications() : 0))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    private List<MilitaryPersonnel> getTopAdministrativeRoles(List<MilitaryPersonnel> personnel, int limit) {
        return personnel.stream()
                .filter(p -> p.getTotalAdministrativeRoles() != null && p.getTotalAdministrativeRoles() > 0)
                .sorted((a, b) -> Integer.compare(
                        b.getTotalAdministrativeRoles() != null ? b.getTotalAdministrativeRoles() : 0,
                        a.getTotalAdministrativeRoles() != null ? a.getTotalAdministrativeRoles() : 0))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    private Double calculateDataCompleteness(List<MilitaryPersonnel> personnel) {
        if (personnel.isEmpty()) return 0.0;
        
        int totalFields = 0;
        int completedFields = 0;
        
        for (MilitaryPersonnel person : personnel) {
            totalFields += 8; // Basic fields: id, rank, name, unit, status, etc.
            
            if (person.getId() != null) completedFields++;
            if (person.getRank() != null) completedFields++;
            if (person.getName() != null) completedFields++;
            if (person.getUnit() != null) completedFields++;
            if (person.getStatus() != null) completedFields++;
            if (person.getEntryDate() != null) completedFields++;
            if (person.getLastPromotionDate() != null) completedFields++;
            if (person.getPtt() != null) completedFields++;
        }
        
        return (double) completedFields / totalFields * 100.0;
    }
    
    private List<String> identifyDataQualityIssues(List<MilitaryPersonnel> personnel) {
        List<String> issues = new ArrayList<>();
        
        long missingIds = personnel.stream().mapToLong(p -> p.getId() == null ? 1 : 0).sum();
        long missingNames = personnel.stream().mapToLong(p -> p.getName() == null ? 1 : 0).sum();
        long missingRanks = personnel.stream().mapToLong(p -> p.getRank() == null ? 1 : 0).sum();
        long missingUnits = personnel.stream().mapToLong(p -> p.getUnit() == null ? 1 : 0).sum();
        
        if (missingIds > 0) issues.add("Missing IDs in " + missingIds + " records");
        if (missingNames > 0) issues.add("Missing names in " + missingNames + " records");
        if (missingRanks > 0) issues.add("Missing ranks in " + missingRanks + " records");
        if (missingUnits > 0) issues.add("Missing units in " + missingUnits + " records");
        
        return issues;
    }
    
    private List<String> generateKeyInsights(List<MilitaryPersonnel> personnel, 
                                           Map<String, Long> rankCategoryDistribution,
                                           Double avgCertifications,
                                           Double avgAdminRoles,
                                           String mostCommonCertification) {
        List<String> insights = new ArrayList<>();
        
        // Rank distribution insights
        String mostCommonRank = rankCategoryDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
        
        insights.add("Most common rank category: " + mostCommonRank + " (" + 
                    rankCategoryDistribution.getOrDefault(mostCommonRank, 0L) + " personnel)");
        
        // Certification insights
        insights.add("Average certifications per person: " + String.format("%.2f", avgCertifications));
        insights.add("Most common certification: " + mostCommonCertification);
        
        // Administrative roles insights
        insights.add("Average administrative roles per person: " + String.format("%.2f", avgAdminRoles));
        
        // Active status insights
        long activePersonnel = personnel.stream()
                .mapToLong(p -> "ATIVO".equals(p.getStatus()) ? 1 : 0)
                .sum();
        
        insights.add("Active personnel: " + activePersonnel + " (" + 
                    String.format("%.1f%%", (double) activePersonnel / personnel.size() * 100) + ")");
        
        return insights;
    }
    
    @Override
    public String getAnalysisType() {
        return "COMPREHENSIVE_MILITARY_ANALYSIS";
    }
}