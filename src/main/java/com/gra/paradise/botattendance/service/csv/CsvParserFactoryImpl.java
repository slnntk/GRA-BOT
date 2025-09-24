package com.gra.paradise.botattendance.service.csv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Factory implementation for creating CSV parsers.
 * Implements the Factory design pattern.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvParserFactoryImpl implements CsvParserFactory {
    
    private final List<CsvParser> availableParsers;
    
    @Override
    public CsvParser createParser(String fileExtension) {
        return availableParsers.stream()
                .filter(parser -> parser.supports(fileExtension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No parser available for file extension: " + fileExtension));
    }
}