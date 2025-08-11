import React, { useMemo } from 'react';
import { Box, Typography, Paper, Divider } from '@mui/material';
import type { PoliceReportFormData } from '../types';
import { pluralizationTerms } from '../utils/pluralization';

interface ReportPreviewProps {
  formData: PoliceReportFormData;
}

export const ReportPreview: React.FC<ReportPreviewProps> = ({ formData }) => {
  const reportText = useMemo(() => {
    const {
      crime,
      local_inicio,
      local_fim,
      numero_pessoas_envolvidas,
      multas,
      tempo_pena,
      observacoes
    } = formData;

    if (!crime || !local_inicio || !local_fim || !numero_pessoas_envolvidas) {
      return 'Preencha os campos obrigatórios para visualizar o relatório.';
    }

    // Use pluralization functions
    const suspeitos = pluralizationTerms.suspeito(numero_pessoas_envolvidas);
    const pessoas = pluralizationTerms.pessoa(numero_pessoas_envolvidas);
    const individuos = pluralizationTerms.individuo(numero_pessoas_envolvidas);
    const foram = pluralizationTerms.foi(numero_pessoas_envolvidas);
    const seEncontravam = pluralizationTerms.se_encontrava(numero_pessoas_envolvidas);

    let report = `RELATÓRIO POLICIAL

═══════════════════════════════════════════════════════

DADOS DO CRIME:
Crime: ${crime}
Local de Início: ${local_inicio}
Local de Fim: ${local_fim}
Número de Pessoas Envolvidas: ${numero_pessoas_envolvidas} ${pessoas}

═══════════════════════════════════════════════════════

RELATO DOS FATOS:

Durante patrulhamento de rotina, ${foram} acionado(s) para uma ocorrência de ${crime.toLowerCase()} na região de ${local_inicio}. 

Ao chegar no local, ${individuos} ${seEncontravam} em atitude suspeita. Após abordagem policial, foi constatado que ${suspeitos} ${numero_pessoas_envolvidas > 1 ? 'estavam' : 'estava'} envolvido(s) no crime de ${crime.toLowerCase()}.

A ocorrência teve início em ${local_inicio} e término em ${local_fim}.`;

    if (observacoes && observacoes.trim()) {
      report += `\n\nOBSERVAÇÕES ADICIONAIS:\n${observacoes}`;
    }

    report += `

═══════════════════════════════════════════════════════

PENALIDADES APLICADAS:`;

    if (multas > 0) {
      report += `\nMulta: $${multas.toLocaleString('pt-BR')}`;
    }

    if (tempo_pena > 0) {
      const horas = Math.floor(tempo_pena / 60);
      const minutos = tempo_pena % 60;
      let tempoTexto = '';
      
      if (horas > 0) {
        tempoTexto += `${horas} hora${horas > 1 ? 's' : ''}`;
      }
      
      if (minutos > 0) {
        if (horas > 0) tempoTexto += ' e ';
        tempoTexto += `${minutos} minuto${minutos > 1 ? 's' : ''}`;
      }
      
      report += `\nTempo de Detenção: ${tempoTexto}`;
    }

    if (multas === 0 && tempo_pena === 0) {
      report += `\nNenhuma penalidade aplicada.`;
    }

    report += `\n\n═══════════════════════════════════════════════════════

Relatório elaborado pelo sistema GRA-BOT
Data: ${new Date().toLocaleDateString('pt-BR')} às ${new Date().toLocaleTimeString('pt-BR')}`;

    return report;
  }, [formData]);

  const isEmpty = !formData.crime || !formData.local_inicio || !formData.local_fim || !formData.numero_pessoas_envolvidas;

  return (
    <Paper 
      elevation={2} 
      sx={{ 
        p: 3, 
        backgroundColor: isEmpty ? 'grey.50' : 'background.paper',
        border: '1px solid',
        borderColor: isEmpty ? 'grey.300' : 'primary.light',
      }}
    >
      <Typography variant="h6" gutterBottom color="primary">
        Pré-visualização do Relatório
      </Typography>
      <Divider sx={{ mb: 2 }} />
      
      <Box
        component="pre"
        sx={{
          fontFamily: 'monospace',
          fontSize: '0.875rem',
          lineHeight: 1.4,
          color: isEmpty ? 'text.secondary' : 'text.primary',
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
          backgroundColor: isEmpty ? 'transparent' : 'grey.100',
          p: isEmpty ? 0 : 2,
          borderRadius: 1,
          maxHeight: '400px',
          overflow: 'auto',
        }}
      >
        {reportText}
      </Box>
    </Paper>
  );
};