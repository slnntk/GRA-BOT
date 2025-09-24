package com.gra.paradise.botattendance.model.csv;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Domain model representing a Military Personnel record from CSV data.
 * Follows the existing model patterns in the project.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilitaryPersonnel {
    
    private Long id;
    private String rank;
    private String ptt;
    private String name;
    private String unit;
    
    // Course/Certification columns
    private Boolean gic;
    private Boolean per;
    private Boolean got;
    private Boolean gra;
    private Boolean gtm;
    private Boolean spd;
    private Boolean sasp;
    
    // Administrative columns
    private Boolean ab;
    private Boolean ac;
    private Boolean co;
    private Boolean bo;
    private Boolean sul;
    private Boolean hc;
    private Boolean p1;
    private Boolean p2;
    private Boolean p3;
    private Boolean p4;
    private Boolean aet;
    
    // Command/Instructor columns
    private Boolean cmd;
    private Boolean inst;
    private Boolean crs;
    private Boolean cre;
    private Boolean clg;
    private Boolean advs;
    
    private String status;
    private String medals;
    private LocalDate entryDate;
    private LocalDate lastPromotionDate;
    
    // Derived fields for analysis
    private String rankCategory;
    private Integer totalCertifications;
    private Integer totalAdministrativeRoles;
    
    /**
     * Calculate total number of certifications
     */
    public Integer calculateTotalCertifications() {
        int count = 0;
        if (Boolean.TRUE.equals(gic)) count++;
        if (Boolean.TRUE.equals(per)) count++;
        if (Boolean.TRUE.equals(got)) count++;
        if (Boolean.TRUE.equals(gra)) count++;
        if (Boolean.TRUE.equals(gtm)) count++;
        if (Boolean.TRUE.equals(spd)) count++;
        if (Boolean.TRUE.equals(sasp)) count++;
        this.totalCertifications = count;
        return count;
    }
    
    /**
     * Calculate total number of administrative roles
     */
    public Integer calculateTotalAdministrativeRoles() {
        int count = 0;
        if (Boolean.TRUE.equals(ab)) count++;
        if (Boolean.TRUE.equals(ac)) count++;
        if (Boolean.TRUE.equals(co)) count++;
        if (Boolean.TRUE.equals(bo)) count++;
        if (Boolean.TRUE.equals(sul)) count++;
        if (Boolean.TRUE.equals(hc)) count++;
        if (Boolean.TRUE.equals(p1)) count++;
        if (Boolean.TRUE.equals(p2)) count++;
        if (Boolean.TRUE.equals(p3)) count++;
        if (Boolean.TRUE.equals(p4)) count++;
        if (Boolean.TRUE.equals(aet)) count++;
        this.totalAdministrativeRoles = count;
        return count;
    }
    
    /**
     * Determine rank category based on rank
     */
    public String determineRankCategory() {
        if (rank == null) {
            this.rankCategory = "UNKNOWN";
            return rankCategory;
        }
        
        String lowerRank = rank.toLowerCase();
        if (lowerRank.contains("coronel") || lowerRank.contains("comissário")) {
            this.rankCategory = "OFFICER";
        } else if (lowerRank.contains("tenente")) {
            this.rankCategory = "LIEUTENANT";
        } else if (lowerRank.contains("sargento")) {
            this.rankCategory = "SERGEANT";
        } else if (lowerRank.contains("cabo")) {
            this.rankCategory = "CORPORAL";
        } else if (lowerRank.contains("soldado")) {
            this.rankCategory = "SOLDIER";
        } else if (lowerRank.contains("aluno")) {
            this.rankCategory = "STUDENT";
        } else {
            this.rankCategory = "OTHER";
        }
        
        return rankCategory;
    }
}