import * as yup from 'yup';

export const policeReportSchema = yup.object().shape({
  crime: yup
    .string()
    .required('Campo obrigatório')
    .min(2, 'Mínimo de 2 caracteres'),
  
  local_inicio: yup
    .string()
    .required('Campo obrigatório')
    .min(2, 'Mínimo de 2 caracteres'),
  
  local_fim: yup
    .string()
    .required('Campo obrigatório')
    .min(2, 'Mínimo de 2 caracteres'),
  
  numero_pessoas_envolvidas: yup
    .number()
    .required('Campo obrigatório')
    .min(1, 'Mínimo de 1 pessoa')
    .max(50, 'Máximo de 50 pessoas')
    .integer('Deve ser um número inteiro'),
  
  multas: yup
    .number()
    .required('Campo obrigatório')
    .min(0, 'Valor não pode ser negativo')
    .max(999999, 'Valor muito alto'),
  
  tempo_pena: yup
    .number()
    .required('Campo obrigatório')
    .min(0, 'Tempo não pode ser negativo')
    .max(9999, 'Tempo máximo de 9999 minutos'),
  
  observacoes: yup
    .string()
    .optional()
    .max(1000, 'Máximo de 1000 caracteres'),
});