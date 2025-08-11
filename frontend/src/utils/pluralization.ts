/**
 * Pluralization utility function for Portuguese
 */
export const pluralize = (singular: string, plural: string, count: number): string => {
  return count === 1 ? singular : plural;
};

/**
 * Predefined pluralization rules for common police report terms
 */
export const pluralizationTerms = {
  suspeito: (count: number) => pluralize('o suspeito', 'os suspeitos', count),
  pessoa: (count: number) => pluralize('pessoa', 'pessoas', count),
  envolvido: (count: number) => pluralize('envolvido', 'envolvidos', count),
  individuo: (count: number) => pluralize('o indivíduo', 'os indivíduos', count),
  criminoso: (count: number) => pluralize('o criminoso', 'os criminosos', count),
  foi: (count: number) => pluralize('foi', 'foram', count),
  estava: (count: number) => pluralize('estava', 'estavam', count),
  se_encontrava: (count: number) => pluralize('se encontrava', 'se encontravam', count),
};