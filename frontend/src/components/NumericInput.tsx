import React, { useRef, useCallback } from 'react';
import { TextField, type TextFieldProps } from '@mui/material';
import { Controller } from 'react-hook-form';

interface NumericInputProps extends Omit<TextFieldProps, 'name' | 'value' | 'onChange'> {
  name: string;
  control: any;
  min?: number;
  max?: number;
  step?: number;
}

export const NumericInput: React.FC<NumericInputProps> = ({
  name,
  control,
  min = 0,
  max,
  step = 1,
  label,
  error,
  helperText,
  ...textFieldProps
}) => {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFocus = useCallback((event: React.FocusEvent<HTMLInputElement>) => {
    // Select all text only on initial focus, not during typing
    const target = event.target;
    setTimeout(() => {
      if (document.activeElement === target) {
        target.select();
      }
    }, 0);
  }, []);

  const handleKeyDown = useCallback((event: React.KeyboardEvent<HTMLInputElement>) => {
    // Allow: backspace, delete, tab, escape, enter, arrows, decimal point
    const allowedKeys = [
      'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
      'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
      'Home', 'End'
    ];

    // Allow Ctrl+A, Ctrl+C, Ctrl+V, Ctrl+X, Ctrl+Z
    if (event.ctrlKey && ['a', 'c', 'v', 'x', 'z'].includes(event.key.toLowerCase())) {
      return;
    }

    // Allow numbers
    if (event.key >= '0' && event.key <= '9') {
      return;
    }

    // Prevent any other keys
    if (!allowedKeys.includes(event.key)) {
      event.preventDefault();
    }
  }, []);

  const formatValue = useCallback((value: string) => {
    // Remove non-numeric characters
    const numericValue = value.replace(/[^0-9]/g, '');
    
    // Apply min/max constraints
    let num = parseInt(numericValue, 10);
    if (isNaN(num)) return '';
    
    if (min !== undefined && num < min) num = min;
    if (max !== undefined && num > max) num = max;
    
    return num.toString();
  }, [min, max]);

  return (
    <Controller
      name={name}
      control={control}
      render={({ field, fieldState }) => (
        <TextField
          {...textFieldProps}
          {...field}
          inputRef={inputRef}
          label={label}
          error={!!fieldState.error}
          helperText={fieldState.error?.message || helperText}
          type="text" // Use text to have better control over input
          inputMode="numeric"
          onFocus={handleFocus}
          onKeyDown={handleKeyDown}
          onChange={(e) => {
            const formattedValue = formatValue(e.target.value);
            field.onChange(formattedValue === '' ? 0 : parseInt(formattedValue, 10));
          }}
          value={field.value || ''}
          InputProps={{
            ...textFieldProps.InputProps,
            sx: {
              '&.Mui-focused': {
                '& .MuiOutlinedInput-notchedOutline': {
                  borderColor: 'primary.main',
                  borderWidth: 2,
                },
              },
              ...textFieldProps.InputProps?.sx,
            },
          }}
        />
      )}
    />
  );
};