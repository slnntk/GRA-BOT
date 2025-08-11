export interface Crime {
  crime: string;
  description: string;
}

export interface PoliceReportFormData {
  crime: string;
  local_inicio: string;
  local_fim: string;
  numero_pessoas_envolvidas: number;
  multas: number;
  tempo_pena: number;
  observacoes?: string;
}

export interface AutocompleteOption {
  label: string;
  description?: string;
}

export interface FormFieldProps {
  label: string;
  name: string;
  error?: string;
  required?: boolean;
}