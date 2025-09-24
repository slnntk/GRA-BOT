package com.gra.paradise.botattendance.service.csv;

import com.gra.paradise.botattendance.model.csv.MilitaryPersonnel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MilitaryPersonnelCsvParser.
 */
class MilitaryPersonnelCsvParserTest {
    
    private MilitaryPersonnelCsvParser parser;
    
    @BeforeEach
    void setUp() {
        parser = new MilitaryPersonnelCsvParser();
    }
    
    @Test
    void shouldSupportCsvFileExtension() {
        assertTrue(parser.supports("csv"));
        assertTrue(parser.supports("CSV"));
        assertFalse(parser.supports("txt"));
        assertFalse(parser.supports("xlsx"));
    }
    
    @Test
    void shouldParseValidCsvFile() throws CsvParserFactory.CsvParsingException {
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
                """;
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<MilitaryPersonnel> personnel = parser.parse(inputStream);
        
        assertNotNull(personnel);
        assertEquals(3, personnel.size());
        
        // Test first person
        MilitaryPersonnel first = personnel.get(0);
        assertEquals(102L, first.getId());
        assertEquals("Comissário", first.getRank());
        assertEquals("Logan Reynolds", first.getName());
        assertEquals("S.A.S.P", first.getUnit());
        assertEquals("ATIVO", first.getStatus());
        assertEquals(LocalDate.of(2025, 5, 30), first.getEntryDate());
        
        // Test third person with certifications
        MilitaryPersonnel third = personnel.get(2);
        assertEquals(583L, third.getId());
        assertEquals("3º Sargento", third.getRank());
        assertEquals("Adeusa Dokasi Void", third.getName());
        assertEquals("GOT", third.getUnit());
        assertTrue(third.getGic());
        assertTrue(third.getGot());
        assertEquals(2, third.getTotalCertifications());
    }
    
    @Test
    void shouldHandleEmptyFile() throws CsvParserFactory.CsvParsingException {
        String csvContent = """
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,1º Batalhão de Polícia Militar Alta Paradise,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Registro de Policiais,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,Cadete
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Identificação,,,,,,Unidades,,,,,,,,Cursos/Cerficações,,,,,,,,,,,,Administrativo,,,,,,,,,,,,,,,,,,,,,
                1,ID,Patente,PTT,Nome,Unidade,,GIC,PER,GOT,GRA,GTM,SPD,SASP,,AB,AC,CO,BO,SUL,HC,P1,P2,P3,P4,AET,,CMD,INST,CRS,CRE,CLG,ADVs,,,Status,,Medalhas,,,,,,,,,Entrada,Última promoção,
                """;
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<MilitaryPersonnel> personnel = parser.parse(inputStream);
        
        assertNotNull(personnel);
        assertTrue(personnel.isEmpty());
    }
    
    @Test
    void shouldCalculateDerivedFields() throws CsvParserFactory.CsvParsingException {
        String csvContent = """
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,1º Batalhão de Polícia Militar Alta Paradise,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Registro de Policiais,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,Cadete
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Identificação,,,,,,Unidades,,,,,,,,Cursos/Cerficações,,,,,,,,,,,,Administrativo,,,,,,,,,,,,,,,,,,,,,
                1,ID,Patente,PTT,Nome,Unidade,,GIC,PER,GOT,GRA,GTM,SPD,SASP,,AB,AC,CO,BO,SUL,HC,P1,P2,P3,P4,AET,,CMD,INST,CRS,CRE,CLG,ADVs,,,Status,,Medalhas,,,,,,,,,Entrada,Última promoção,
                ,123,Soldado 1ª C,15,Test Soldier,GTM,TRUE,TRUE,TRUE,,,,,TRUE,TRUE,,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,10/07/2025,
                """;
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<MilitaryPersonnel> personnel = parser.parse(inputStream);
        
        assertNotNull(personnel);
        assertEquals(1, personnel.size());
        
        MilitaryPersonnel person = personnel.get(0);
        assertEquals(1, person.getTotalCertifications()); // Only GIC is true  
        assertEquals(1, person.getTotalAdministrativeRoles()); // Only AB is true
        assertEquals("SOLDIER", person.getRankCategory());
    }
    
    @Test
    void shouldHandleInvalidData() throws CsvParserFactory.CsvParsingException {
        String csvContent = """
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,1º Batalhão de Polícia Militar Alta Paradise,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Registro de Policiais,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,Cadete
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Identificação,,,,,,Unidades,,,,,,,,Cursos/Cerficações,,,,,,,,,,,,Administrativo,,,,,,,,,,,,,,,,,,,,,
                1,ID,Patente,PTT,Nome,Unidade,,GIC,PER,GOT,GRA,GTM,SPD,SASP,,AB,AC,CO,BO,SUL,HC,P1,P2,P3,P4,AET,,CMD,INST,CRS,CRE,CLG,ADVs,,,Status,,Medalhas,,,,,,,,,Entrada,Última promoção,
                ,invalid_id,Soldado 1ª C,15,Test Soldier,GTM,,,,,,,,,,,,,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,invalid_date,
                ,-,,,,,,,,,,,,,,,,,,,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,,
                """;
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<MilitaryPersonnel> personnel = parser.parse(inputStream);
        
        // Should still process valid parts and skip invalid records
        assertNotNull(personnel);
        // No valid records with both ID and name
        assertTrue(personnel.isEmpty());
    }
    
    @Test
    void debugActualCsvContent() throws CsvParserFactory.CsvParsingException {
        String csvContent = """
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,1º Batalhão de Polícia Militar Alta Paradise,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Registro de Policiais,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,Cadete
                ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
                ,Identificação,,,,,,Unidades,,,,,,,,Cursos/Cerficações,,,,,,,,,,,,Administrativo,,,,,,,,,,,,,,,,,,,,,
                1,ID,Patente,PTT,Nome,Unidade,,GIC,PER,GOT,GRA,GTM,SPD,SASP,,AB,AC,CO,BO,SUL,HC,P1,P2,P3,P4,AET,,CMD,INST,CRS,CRE,CLG,ADVs,,,Status,,Medalhas,,,,,,,,,Entrada,Última promoção,
                ,583,3º Sargento,13,Adeusa Dokasi Void,GOT,TRUE,,TRUE,,,,,,,,,,,,,,,,,,,,,,,FALSE,FALSE,FALSE,ATIVO,,,,,,,,,,,,15/09/2025,
                """;
        
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());
        List<MilitaryPersonnel> personnel = parser.parse(inputStream);
        
        System.out.println("Parsed " + personnel.size() + " personnel");
        if (!personnel.isEmpty()) {
            MilitaryPersonnel person = personnel.get(0);
            System.out.println("Person details:");
            System.out.println("ID: " + person.getId());
            System.out.println("Name: " + person.getName());
            System.out.println("GIC: " + person.getGic());
            System.out.println("PER: " + person.getPer());
            System.out.println("GOT: " + person.getGot());
            System.out.println("GRA: " + person.getGra());
            System.out.println("Total certs: " + person.getTotalCertifications());
        }
    }
}