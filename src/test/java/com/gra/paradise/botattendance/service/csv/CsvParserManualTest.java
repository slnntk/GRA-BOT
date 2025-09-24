package com.gra.paradise.botattendance.service.csv;

import com.gra.paradise.botattendance.model.csv.MilitaryPersonnel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Manual test to debug CSV parsing.
 */
class CsvParserManualTest {
    
    @Test
    void debugCsvParsing() throws Exception {
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
        
        MilitaryPersonnelCsvParser parser = new MilitaryPersonnelCsvParser();
        List<MilitaryPersonnel> personnel = parser.parse(new ByteArrayInputStream(csvContent.getBytes()));
        
        System.out.println("=".repeat(80));
        System.out.println("PARSED PERSONNEL:");
        System.out.println("Total: " + personnel.size());
        
        for (MilitaryPersonnel person : personnel) {
            System.out.println("-".repeat(40));
            System.out.println("ID: " + person.getId());
            System.out.println("Name: " + person.getName());
            System.out.println("Rank: " + person.getRank());
            System.out.println("Unit: " + person.getUnit());
            System.out.println("Status: " + person.getStatus());
            System.out.println("Entry Date: " + person.getEntryDate());
            System.out.println("Rank Category: " + person.getRankCategory());
            System.out.println("Total Certifications: " + person.getTotalCertifications());
            System.out.println("Total Admin Roles: " + person.getTotalAdministrativeRoles());
            
            // Print boolean fields
            System.out.println("GIC: " + person.getGic());
            System.out.println("PER: " + person.getPer());
            System.out.println("GOT: " + person.getGot());
            System.out.println("GRA: " + person.getGra());
        }
        System.out.println("=".repeat(80));
    }
    
    @Test
    void debugCsvWithCertifications() throws Exception {
        String csvContent = """
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,1º Batalhão de Polícia Militar Alta Paradise,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Registro de Policiais,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,Cadete
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Identificação,,,,,,Unidades,,,,,,,,Cursos/Cerficações,,,,,,,,,,,,Administrativo,,,,,,,,,,,,,,,,,,,,,
                1,ID,Patente,PTT,Nome,Unidade,,GIC,PER,GOT,GRA,GTM,SPD,SASP,,AB,AC,CO,BO,SUL,HC,P1,P2,P3,P4,AET,,CMD,INST,CRS,CRE,CLG,ADVs,,,Status,,Medalhas,,,,,,,,,Entrada,Última promoção,
                ,123,Soldado 1ª C,15,Test Soldier,GTM,TRUE,TRUE,,,,,,,TRUE,TRUE,,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,10/07/2025,
                """;
        
        MilitaryPersonnelCsvParser parser = new MilitaryPersonnelCsvParser();
        List<MilitaryPersonnel> personnel = parser.parse(new ByteArrayInputStream(csvContent.getBytes()));
        
        System.out.println("=".repeat(80));
        System.out.println("PARSED PERSONNEL WITH CERTIFICATIONS:");
        System.out.println("Total: " + personnel.size());
        
        for (MilitaryPersonnel person : personnel) {
            System.out.println("-".repeat(40));
            System.out.println("ID: " + person.getId());
            System.out.println("Name: " + person.getName());
            System.out.println("Rank: " + person.getRank());
            System.out.println("Unit: " + person.getUnit());
            
            // Print certification fields
            System.out.println("Certifications:");
            System.out.println("  GIC: " + person.getGic());
            System.out.println("  PER: " + person.getPer());
            System.out.println("  GOT: " + person.getGot());
            System.out.println("  GRA: " + person.getGra());
            System.out.println("  Total: " + person.getTotalCertifications());
            
            // Print admin fields
            System.out.println("Admin roles:");
            System.out.println("  AB: " + person.getAb());
            System.out.println("  AC: " + person.getAc());
            System.out.println("  Total: " + person.getTotalAdministrativeRoles());
        }
        System.out.println("=".repeat(80));
    }
}