import React, { useState, useMemo, useCallback } from 'react';
import { Autocomplete, TextField, Box, Typography, Chip, Tooltip } from '@mui/material';
import { Controller, type Control, type FieldPath } from 'react-hook-form';
import { debounce } from '@mui/material/utils';
import type { PoliceReportFormData, AutocompleteOption } from '../types';

interface IntelligentAutocompleteProps {
  name: FieldPath<PoliceReportFormData>;
  control: Control<PoliceReportFormData>;
  label: string;
  options: AutocompleteOption[];
  onSearch: (query: string) => AutocompleteOption[];
  placeholder?: string;
  helperText?: string;
  required?: boolean;
  showDescription?: boolean;
}

export const IntelligentAutocomplete: React.FC<IntelligentAutocompleteProps> = ({
  name,
  control,
  label,
  options,
  onSearch,
  placeholder,
  helperText,
  required = false,
  showDescription = false,
}) => {
  const [inputValue, setInputValue] = useState('');
  const [filteredOptions, setFilteredOptions] = useState<AutocompleteOption[]>([]);

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

  const handleInputChange = useCallback((_event: React.SyntheticEvent, value: string) => {
    setInputValue(value);
    debouncedSearch(value);
  }, [debouncedSearch]);

  const renderOption = useCallback((props: React.HTMLAttributes<HTMLLIElement>, option: AutocompleteOption) => {
    return (
      <li {...props} key={option.label}>
        <Box sx={{ display: 'flex', flexDirection: 'column', width: '100%' }}>
          <Typography variant="body1" sx={{ fontWeight: 500 }}>
            {option.label}
          </Typography>
          {option.description && showDescription && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {option.description.length > 100 
                ? `${option.description.substring(0, 100)}...` 
                : option.description
              }
            </Typography>
          )}
        </Box>
      </li>
    );
  }, [showDescription]);

  return (
    <Controller
      name={name}
      control={control}
      render={({ field, fieldState }) => {
        const selectedOption = options.find(opt => opt.label === field.value) || null;
        
        return (
          <Box>
            <Autocomplete
              {...field}
              value={selectedOption}
              inputValue={inputValue}
              onInputChange={handleInputChange}
              onChange={(_event, newValue: AutocompleteOption | null) => {
                field.onChange(newValue?.label || '');
                if (newValue) {
                  setInputValue(newValue.label);
                }
              }}
              options={inputValue.length >= 2 ? filteredOptions : []}
              getOptionLabel={(option: AutocompleteOption) => option.label}
              isOptionEqualToValue={(option, value) => option.label === value?.label}
              renderOption={renderOption}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label={label}
                  placeholder={placeholder}
                  error={!!fieldState.error}
                  helperText={fieldState.error?.message || helperText}
                  required={required}
                  InputProps={{
                    ...params.InputProps,
                    sx: {
                      '&.Mui-focused': {
                        '& .MuiOutlinedInput-notchedOutline': {
                          borderColor: 'primary.main',
                          borderWidth: 2,
                        },
                      },
                    },
                  }}
                />
              )}
              freeSolo
              selectOnFocus
              clearOnBlur
              handleHomeEndKeys
              sx={{ mb: 2 }}
            />
            
            {/* Show description as tooltip/chip when option is selected */}
            {selectedOption && selectedOption.description && showDescription && (
              <Tooltip title={selectedOption.description} arrow>
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