import React, { useState, useMemo } from 'react';
import { Autocomplete, TextField, Box, Typography, Chip, Tooltip } from '@mui/material';
import { Controller } from 'react-hook-form';
import { debounce } from '@mui/material/utils';

interface SimpleAutocompleteProps {
  name: string;
  control: any;
  label: string;
  options: string[];
  onSearch: (query: string) => string[];
  placeholder?: string;
  helperText?: string;
  required?: boolean;
  descriptions?: Record<string, string>;
}

export const SimpleAutocomplete: React.FC<SimpleAutocompleteProps> = ({
  name,
  control,
  label,
  options,
  onSearch,
  placeholder,
  helperText,
  required = false,
  descriptions = {},
}) => {
  const [inputValue, setInputValue] = useState('');
  const [filteredOptions, setFilteredOptions] = useState<string[]>([]);

  // Debounced search function
  const debouncedSearch = useMemo(
    () => debounce((query: string) => {
      if (query.length >= 2) {
        const results = onSearch(query);
        setFilteredOptions(results);
      } else {
        setFilteredOptions([]);
      }
    }, 300),
    [onSearch]
  );

  const handleInputChange = React.useCallback((_event: React.SyntheticEvent, value: string) => {
    setInputValue(value);
    debouncedSearch(value);
  }, [debouncedSearch]);

  return (
    <Controller
      name={name}
      control={control}
      render={({ field, fieldState }) => {
        const selectedDescription = descriptions[field.value];
        
        return (
          <Box>
            <Autocomplete
              value={field.value || null}
              inputValue={inputValue}
              onInputChange={handleInputChange}
              onChange={(_event, newValue) => {
                field.onChange(newValue || '');
                if (newValue) {
                  setInputValue(newValue);
                }
              }}
              options={inputValue.length >= 2 ? filteredOptions : []}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label={label}
                  placeholder={placeholder}
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message || helperText}
                  required={required}
                />
              )}
              freeSolo
              selectOnFocus
              clearOnBlur
              handleHomeEndKeys
              sx={{ mb: 2 }}
            />
            
            {/* Show description as tooltip/chip when option is selected */}
            {selectedDescription && (
              <Tooltip title={selectedDescription} arrow>
                <Chip
                  label="Ver descrição"
                  variant="outlined"
                  size="small"
                  sx={{ mb: 1 }}
                />
              </Tooltip>
            )}
          </Box>
        );
      }}
    />
  );
};