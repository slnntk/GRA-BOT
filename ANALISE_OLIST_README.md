# Análise Principal dos Dados Olist

Este repositório contém o notebook principal para análise dos dados da Olist, focando em 4 perguntas estratégicas específicas.

## 📊 Perguntas Analisadas

1. **Qual o percentual de pedidos entregues após a data estimada pela Olist?**
2. **Qual o método de pagamento mais utilizado em pedidos acima de R$ 150,00?**
3. **Qual é a relação entre o tempo de entrega e a nota de avaliação do cliente?**
4. **Quais são as 5 categorias de produtos mais vendidas e qual a receita total gerada por cada uma?**

## 📁 Arquivos Necessários

Para executar o notebook `olist_analysis_principal.ipynb`, você precisa dos seguintes datasets da Olist na pasta `data/`:

```
data/
├── olist_orders_dataset.csv
├── olist_order_items_dataset.csv
├── olist_order_payments_dataset.csv
├── olist_order_reviews_dataset.csv
├── olist_products_dataset.csv
└── product_category_name_translation.csv
```

## 🚀 Como Usar

### Pré-requisitos
```bash
pip install pandas numpy matplotlib seaborn jupyter
```

### Executar a Análise
1. Clone o repositório
2. Baixe os datasets da Olist e coloque na pasta `data/`
3. Execute o notebook:
```bash
jupyter notebook olist_analysis_principal.ipynb
```

## 📈 Estrutura da Análise

### 1. Configuração Inicial
- Importação de bibliotecas necessárias
- Configuração de gráficos e visualizações

### 2. Carregamento dos Dados
- Leitura dos 6 datasets principais
- Validação da estrutura dos dados

### 3. Pré-processamento
- Conversão de datas
- Merge de tabelas de tradução
- Tratamento de dados faltantes

### 4. Análises Específicas

#### Análise 1: Pontualidade das Entregas
- Cálculo do percentual de pedidos entregues em atraso
- Visualizações com gráficos de pizza e barras
- **Output**: Percentual exato de entregas atrasadas

#### Análise 2: Métodos de Pagamento Premium
- Filtragem de pedidos acima de R$ 150,00
- Análise dos métodos de pagamento preferenciais
- **Output**: Método mais utilizado e sua participação percentual

#### Análise 3: Tempo vs Satisfação
- Correlação entre tempo de entrega e notas de avaliação
- Análise estatística detalhada
- **Output**: Coeficiente de correlação e insights comportamentais

#### Análise 4: Top Categorias
- Ranking das 5 categorias mais vendidas
- Cálculo de receita total por categoria
- **Output**: Lista detalhada com quantidades e receitas

### 5. Resumo Executivo
- Síntese das principais descobertas
- Recomendações estratégicas baseadas nos dados

## 🎯 Visualizações Incluídas

- **Gráficos de Pizza**: Distribuições percentuais
- **Gráficos de Barras**: Comparações quantitativas
- **Boxplots**: Análise de distribuições
- **Scatter Plots**: Correlações
- **Gráficos Empilhados**: Análises multidimensionais

## 📋 Saídas Esperadas

Cada análise fornece:
1. **Resposta direta** à pergunta específica
2. **Métricas quantitativas** precisas
3. **Visualizações** complementares
4. **Insights** estratégicos

## 🔧 Customizações

O notebook pode ser facilmente adaptado para:
- Análise de diferentes períodos temporais
- Filtros por regiões específicas
- Análises de outras categorias de produtos
- Diferentes faixas de valores

## 💡 Dicas de Uso

1. **Performance**: Para datasets grandes, considere usar `chunksize` no `pd.read_csv()`
2. **Memória**: Execute as análises uma por vez se houver limitações de RAM
3. **Personalização**: Modifique os thresholds (R$ 150,00, top 5, etc.) conforme necessário
4. **Exportação**: Os gráficos podem ser salvos usando `plt.savefig()`

## 📊 Exemplo de Resultados

O notebook produz resultados no formato:

```
✨ RESULTADOS PRINCIPAIS:
- X.X% dos pedidos foram entregues após a data estimada
- [Método] é o pagamento preferido em pedidos premium
- Correlação de -0.XXX entre tempo e satisfação
- Top 5 categorias geram R$ XX.XM em receita
```

---

*Notebook otimizado para análise eficiente e insights acionáveis dos dados Olist.*