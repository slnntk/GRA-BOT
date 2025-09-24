package com.gra.paradise.botattendance.service.csv;

import com.gra.paradise.botattendance.model.csv.MilitaryPersonnel;
import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Military Personnel CSV parser implementation.
 * Uses the Strategy pattern for different CSV formats.
 */
@Slf4j
@Component
public class MilitaryPersonnelCsvParser implements CsvParserFactory.CsvParser {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int HEADER_ROWS_TO_SKIP = 6; // Skip the header rows
    
    @Override
    public List<MilitaryPersonnel> parse(InputStream inputStream) throws CsvParserFactory.CsvParsingException {
        List<MilitaryPersonnel> personnel = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] line;
            int lineNumber = 0;
            
            // Skip header rows
            for (int i = 0; i < HEADER_ROWS_TO_SKIP; i++) {
                reader.readNext();
                lineNumber++;
            }
            
            while ((line = reader.readNext()) != null) {
                lineNumber++;
                
                try {
                    MilitaryPersonnel person = parseLine(line, lineNumber);
                    if (person != null && isValidPersonnelRecord(person)) {
                        // Calculate derived fields
                        person.calculateTotalCertifications();
                        person.calculateTotalAdministrativeRoles();
                        person.determineRankCategory();
                        
                        personnel.add(person);
                        log.debug("Successfully parsed personnel: {} - {}", person.getId(), person.getName());
                    }
                } catch (Exception e) {
                    log.warn("Error parsing line {}: {}. Line content: {}", 
                            lineNumber, e.getMessage(), String.join(",", line));
                    // Continue processing other lines
                }
            }
            
            log.info("Successfully parsed {} personnel records from CSV", personnel.size());
            
        } catch (Exception e) {
            throw new CsvParserFactory.CsvParsingException("Error parsing CSV file", e);
        }
        
        return personnel;
    }
    
    private MilitaryPersonnel parseLine(String[] fields, int lineNumber) {
        if (fields.length < 5 || isEmptyOrSeparatorRow(fields)) {
            return null; // Skip empty or separator rows
        }
        
        return MilitaryPersonnel.builder()
                .id(parseId(fields[1]))
                .rank(parseString(fields[2]))
                .ptt(parseString(fields[3]))
                .name(parseString(fields[4]))
                .unit(parseString(fields[5]))
                // Course/Certification fields - 0-indexed columns 7-13
                .gic(parseBoolean(fields, 7))
                .per(parseBoolean(fields, 8))
                .got(parseBoolean(fields, 9))
                .gra(parseBoolean(fields, 10))
                .gtm(parseBoolean(fields, 11))
                .spd(parseBoolean(fields, 12))
                .sasp(parseBoolean(fields, 13))
                // Administrative fields - 0-indexed columns 15-25
                .ab(parseBoolean(fields, 15))
                .ac(parseBoolean(fields, 16))
                .co(parseBoolean(fields, 17))
                .bo(parseBoolean(fields, 18))
                .sul(parseBoolean(fields, 19))
                .hc(parseBoolean(fields, 20))
                .p1(parseBoolean(fields, 21))
                .p2(parseBoolean(fields, 22))
                .p3(parseBoolean(fields, 23))
                .p4(parseBoolean(fields, 24))
                .aet(parseBoolean(fields, 25))
                // Command/Instructor fields - 0-indexed columns 27-32
                .cmd(parseBoolean(fields, 27))
                .inst(parseBoolean(fields, 28))
                .crs(parseBoolean(fields, 29))
                .cre(parseBoolean(fields, 30))
                .clg(parseBoolean(fields, 31))
                .advs(parseBoolean(fields, 32))
                // Status and dates - find correct positions based on column count
                .status(findFieldByContent(fields, "ATIVO", "INATIVO"))
                .medals(parseString(fields, fields.length > 36 ? 36 : -1))
                .entryDate(parseLastDate(fields))
                .lastPromotionDate(parseSecondLastDate(fields))
                .build();
    }
    
    private Long parseId(String value) {
        if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private String parseString(String value) {
        if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) {
            return null;
        }
        return value.trim();
    }
    
    private String parseString(String[] fields, int index) {
        if (index >= fields.length) {
            return null;
        }
        return parseString(fields[index]);
    }
    
    private Boolean parseBoolean(String[] fields, int index) {
        if (index >= fields.length) {
            return false;
        }
        String value = fields[index];
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String trimmedValue = value.trim();
        log.debug("Parsing boolean at index {}: '{}'", index, trimmedValue);
        return "TRUE".equalsIgnoreCase(trimmedValue) || "1".equals(trimmedValue);
    }
    
    private LocalDate parseDate(String[] fields, int index) {
        if (index < 0 || index >= fields.length) {
            return null;
        }
        String dateStr = parseString(fields[index]);
        if (dateStr == null) {
            return null;
        }
        
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.debug("Could not parse date: {}", dateStr);
            return null;
        }
    }
    
    private String findFieldByContent(String[] fields, String... possibleValues) {
        for (String field : fields) {
            if (field != null) {
                String trimmed = field.trim();
                for (String possible : possibleValues) {
                    if (possible.equalsIgnoreCase(trimmed)) {
                        return trimmed;
                    }
                }
            }
        }
        return null;
    }
    
    private LocalDate parseLastDate(String[] fields) {
        // Look for date in last non-empty columns
        for (int i = fields.length - 1; i >= 0; i--) {
            String field = parseString(fields[i]);
            if (field != null && field.matches("\\d{2}/\\d{2}/\\d{4}")) {
                try {
                    return LocalDate.parse(field, DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    log.debug("Could not parse last date: {}", field);
                }
            }
        }
        return null;
    }
    
    private LocalDate parseSecondLastDate(String[] fields) {
        int dateCount = 0;
        // Look for second date from end
        for (int i = fields.length - 1; i >= 0; i--) {
            String field = parseString(fields[i]);
            if (field != null && field.matches("\\d{2}/\\d{2}/\\d{4}")) {
                dateCount++;
                if (dateCount == 2) {
                    try {
                        return LocalDate.parse(field, DATE_FORMATTER);
                    } catch (DateTimeParseException e) {
                        log.debug("Could not parse second last date: {}", field);
                    }
                }
            }
        }
        return null;
    }
    
    private boolean isEmptyOrSeparatorRow(String[] fields) {
        // Check if all fields are empty or contain only separators/formatting
        for (String field : fields) {
            if (field != null && !field.trim().isEmpty() && 
                !field.trim().equals("-") && !field.trim().equals(",")) {
                return false;
            }
        }
        return true;
    }
    
    private boolean isValidPersonnelRecord(MilitaryPersonnel person) {
        // A valid record should have at least an ID and name
        return person.getId() != null && 
               person.getName() != null && 
               !person.getName().trim().isEmpty();
    }
    
    @Override
    public boolean supports(String fileExtension) {
        return "csv".equalsIgnoreCase(fileExtension);
    }
}