import React, { useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  Slider,
  FormControl,
  FormLabel,
  Alert,
  Divider,
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { motion, AnimatePresence } from 'framer-motion';

import type { PoliceReportFormData } from '../types';
import { policeReportSchema } from '../utils/validation';
import { 
  searchCrimes, 
  searchLocations, 
  getAllCrimes, 
  getAllLocations 
} from '../utils/searchUtils';
import { NumericInput } from './NumericInput';
import { SimpleAutocomplete } from './SimpleAutocomplete';
import { ReportPreview } from './ReportPreview';

const defaultValues: PoliceReportFormData = {
  crime: '',
  local_inicio: '',
  local_fim: '',
  numero_pessoas_envolvidas: 1,
  multas: 0,
  tempo_pena: 0,
  observacoes: '',
};

export const SimplePoliceReportForm: React.FC = () => {
  const { control, handleSubmit, watch, formState: { errors, isValid, isDirty } } = useForm<PoliceReportFormData>({
    defaultValues,
    resolver: yupResolver(policeReportSchema),
    mode: 'onChange',
  });

  const formData = watch();

  // Search functions for autocomplete
  const searchCrimesCallback = useCallback((query: string): string[] => {
    const results = searchCrimes(query);
    return results.map(crime => crime.crime);
  }, []);

  const searchLocationsCallback = useCallback((query: string): string[] => {
    return searchLocations(query);
  }, []);

  // Get all options for initial load
  const allCrimes = getAllCrimes().map(crime => crime.crime);
  const allLocations = getAllLocations();
  
  // Create descriptions map for crimes
  const crimeDescriptions = React.useMemo(() => {
    const descriptions: Record<string, string> = {};
    getAllCrimes().forEach(crime => {
      descriptions[crime.crime] = crime.description;
    });
    return descriptions;
  }, []);

  const onSubmit = (data: PoliceReportFormData) => {
    console.log('Relatório submetido:', data);
    alert('Relatório gerado com sucesso! Verifique o console para os dados.');
  };

  const handleReset = () => {
    window.location.reload();
  };

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto', p: 3 }}>
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <Typography variant="h4" component="h1" gutterBottom align="center" color="primary">
          Sistema de Relatórios Policiais - GRA-BOT
        </Typography>
        
        <Box sx={{ display: 'flex', gap: 3, mt: 2, flexDirection: { xs: 'column', md: 'row' } }}>
          {/* Form Section */}
          <Box sx={{ flex: 1 }}>
            <Card elevation={3}>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Dados do Incidente
                </Typography>
                <Divider sx={{ mb: 3 }} />

                <form onSubmit={handleSubmit(onSubmit)}>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    {/* Crime Autocomplete */}
                    <SimpleAutocomplete
                      name="crime"
                      control={control}
                      label="Crime *"
                      options={allCrimes}
                      onSearch={searchCrimesCallback}
                      placeholder="Digite para buscar crime..."
                      helperText="Comece digitando para ver sugestões"
                      required
                      descriptions={crimeDescriptions}
                    />

                    {/* Location Start Autocomplete */}
                    <SimpleAutocomplete
                      name="local_inicio"
                      control={control}
                      label="Local de Início *"
                      options={allLocations}
                      onSearch={searchLocationsCallback}
                      placeholder="Digite para buscar local..."
                      helperText="Local onde o crime teve início"
                      required
                    />

                    {/* Location End Autocomplete */}
                    <SimpleAutocomplete
                      name="local_fim"
                      control={control}
                      label="Local de Fim *"
                      options={allLocations}
                      onSearch={searchLocationsCallback}
                      placeholder="Digite para buscar local..."
                      helperText="Local onde o crime terminou"
                      required
                    />

                    {/* Number of People Slider */}
                    <FormControl>
                      <FormLabel>Número de Pessoas Envolvidas *</FormLabel>
                      <Controller
                        name="numero_pessoas_envolvidas"
                        control={control}
                        render={({ field }) => (
                          <Box sx={{ px: 1, mt: 1 }}>
                            <Slider
                              {...field}
                              min={1}
                              max={10}
                              step={1}
                              marks
                              valueLabelDisplay="on"
                              aria-labelledby="numero-pessoas-slider"
                              sx={{
                                '& .MuiSlider-valueLabel': {
                                  backgroundColor: 'primary.main',
                                },
                              }}
                            />
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                              Valor atual: {field.value} {field.value === 1 ? 'pessoa' : 'pessoas'}
                            </Typography>
                          </Box>
                        )}
                      />
                    </FormControl>

                    {/* Fines Numeric Input */}
                    <NumericInput
                      name="multas"
                      control={control}
                      label="Valor da Multa (R$) *"
                      min={0}
                      max={999999}
                      helperText="Valor em reais (ex: 5000)"
                      fullWidth
                    />

                    {/* Prison Time Numeric Input */}
                    <NumericInput
                      name="tempo_pena"
                      control={control}
                      label="Tempo de Pena (minutos) *"
                      min={0}
                      max={9999}
                      helperText="Tempo em minutos (ex: 120 para 2 horas)"
                      fullWidth
                    />

                    {/* Observations Text Field */}
                    <Controller
                      name="observacoes"
                      control={control}
                      render={({ field, fieldState }) => (
                        <TextField
                          {...field}
                          label="Observações Adicionais"
                          multiline
                          rows={4}
                          placeholder="Informações adicionais sobre o caso..."
                          error={!!fieldState.error}
                          helperText={fieldState.error?.message || 'Máximo 1000 caracteres'}
                          inputProps={{ maxLength: 1000 }}
                          fullWidth
                        />
                      )}
                    />

                    {/* Error Alert */}
                    <AnimatePresence>
                      {Object.keys(errors).length > 0 && (
                        <motion.div
                          initial={{ opacity: 0, height: 0 }}
                          animate={{ opacity: 1, height: 'auto' }}
                          exit={{ opacity: 0, height: 0 }}
                        >
                          <Alert severity="error">
                            Por favor, corrija os erros no formulário antes de continuar.
                          </Alert>
                        </motion.div>
                      )}
                    </AnimatePresence>

                    {/* Action Buttons */}
                    <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
                      <Button
                        type="submit"
                        variant="contained"
                        size="large"
                        disabled={!isValid}
                        sx={{ flex: 1 }}
                      >
                        Gerar Relatório
                      </Button>
                      <Button
                        type="button"
                        variant="outlined"
                        size="large"
                        onClick={handleReset}
                        disabled={!isDirty}
                        sx={{ flex: 1 }}
                      >
                        Limpar Formulário
                      </Button>
                    </Box>
                  </Box>
                </form>
              </CardContent>
            </Card>
          </Box>

          {/* Preview Section */}
          <Box sx={{ flex: 1 }}>
            <ReportPreview formData={formData} />
          </Box>
        </Box>
      </motion.div>
    </Box>
  );
};