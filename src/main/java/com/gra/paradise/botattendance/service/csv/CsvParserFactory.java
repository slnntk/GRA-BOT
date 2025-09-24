package com.gra.paradise.botattendance.service.csv;

import com.gra.paradise.botattendance.model.csv.MilitaryPersonnel;

import java.io.InputStream;
import java.util.List;

/**
 * Factory interface for creating CSV parsers.
 * Implements the Factory design pattern for extensibility.
 */
public interface CsvParserFactory {
    
    /**
     * Creates a CSV parser for the given file type.
     * 
     * @param fileExtension the file extension (e.g., "csv")
     * @return the appropriate CSV parser
     */
    CsvParser createParser(String fileExtension);
    
    /**
     * Interface for CSV parsers implementing Strategy pattern.
     */
    interface CsvParser {
        List<MilitaryPersonnel> parse(InputStream inputStream) throws CsvParsingException;
        boolean supports(String fileExtension);
    }
    
    /**
     * Custom exception for CSV parsing errors.
     */
    class CsvParsingException extends Exception {
        public CsvParsingException(String message) {
            super(message);
        }
        
        public CsvParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}