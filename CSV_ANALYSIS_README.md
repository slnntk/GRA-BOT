# CSV Analysis System - Sistema de Análise de CSV

Este sistema fornece uma solução completa para análise de arquivos CSV com dados de pessoal militar, seguindo padrões de design orientados a objetos e arquitetura escalável.

## Características Principais

### 🏗️ Arquitetura
- **Padrão Factory**: Para criação de parsers CSV flexíveis
- **Padrão Strategy**: Para diferentes tipos de análise
- **Padrão Builder**: Para construção de objetos complexos
- **Service Layer**: Separação clara de responsabilidades
- **REST API**: Endpoints bem definidos para integração

### 🚀 Funcionalidades

#### Parsing de CSV
- Suporte a formato específico de dados militares
- Validação de dados robusta
- Tratamento de campos opcionais e vazios
- Parsing inteligente de datas e valores booleanos

#### Análise Abrangente
- **Distribuição de Patentes**: Análise hierárquica por categoria
- **Análise de Unidades**: Distribuição por unidades organizacionais
- **Certificações**: Contagem e estatísticas de cursos/certificações
- **Funções Administrativas**: Análise de responsabilidades administrativas
- **Análise Temporal**: Tendências de entrada e promoções
- **Top Performers**: Identificação de pessoal mais qualificado
- **Qualidade dos Dados**: Avaliação de completude e consistência

#### Insights Automáticos
- Geração automática de insights relevantes
- Identificação de padrões nos dados
- Métricas de performance e qualidade

## Estrutura do Projeto

```
src/main/java/com/gra/paradise/botattendance/
├── controller/
│   └── CsvAnalysisController.java          # REST endpoints
├── service/csv/
│   ├── CsvAnalysisService.java             # Serviço principal
│   ├── CsvParserFactory.java               # Factory para parsers
│   ├── CsvParserFactoryImpl.java           # Implementação da factory
│   ├── CsvAnalysisStrategy.java            # Interface Strategy
│   ├── MilitaryPersonnelAnalysisStrategy.java # Análise militar
│   └── MilitaryPersonnelCsvParser.java     # Parser específico
├── model/csv/
│   ├── MilitaryPersonnel.java              # Modelo de domínio
│   └── CsvAnalysisResult.java              # Resultado da análise
└── demo/
    └── CsvAnalysisDemo.java                # Aplicação de demonstração
```

## API Endpoints

### 🔍 Análise Completa
```http
POST /api/csv/analyze
Content-Type: multipart/form-data

Parâmetros:
- file: arquivo CSV
- analysisType: tipo de análise (padrão: COMPREHENSIVE_MILITARY_ANALYSIS)
```

### 📊 Parsing Simples
```http
POST /api/csv/parse
Content-Type: multipart/form-data

Parâmetros:
- file: arquivo CSV
```

### 📋 Tipos de Análise Disponíveis
```http
GET /api/csv/analysis-types
```

### ❤️ Status de Saúde
```http
GET /api/csv/health
```

## Exemplo de Uso

### Via API REST
```bash
# Análise completa
curl -X POST -F "file=@dados-militar.csv" \
     -F "analysisType=COMPREHENSIVE_MILITARY_ANALYSIS" \
     http://localhost:8080/api/csv/analyze

# Parsing simples
curl -X POST -F "file=@dados-militar.csv" \
     http://localhost:8080/api/csv/parse
```

### Via Demo Application
```bash
# Executar demo
mvn spring-boot:run -Dspring-boot.run.arguments="--csv.demo.enabled=true"
```

## Formato do CSV

O sistema suporta o formato específico de dados militares com as seguintes colunas:

```
ID,Patente,PTT,Nome,Unidade,GIC,PER,GOT,GRA,GTM,SPD,SASP,AB,AC,CO,BO,SUL,HC,P1,P2,P3,P4,AET,CMD,INST,CRS,CRE,CLG,ADVs,Status,Medalhas,Entrada,Última promoção
```

### Campos Principais:
- **Identificação**: ID, Patente, PTT, Nome, Unidade
- **Certificações**: GIC, PER, GOT, GRA, GTM, SPD, SASP
- **Administrativo**: AB, AC, CO, BO, SUL, HC, P1, P2, P3, P4, AET
- **Comando**: CMD, INST, CRS, CRE, CLG, ADVs
- **Status**: Status, Medalhas, Entrada, Última promoção

## Resultados da Análise

### Métricas Incluídas:
- **Estatísticas Básicas**: Total de registros, registros válidos, completude dos dados
- **Distribuições**: Por patente, unidade, status, categoria de patente
- **Análise de Certificações**: Contagens, médias, certificação mais comum
- **Funções Administrativas**: Contagens e médias por pessoa
- **Análise Temporal**: Distribuição por ano de entrada e promoção
- **Top Performers**: Pessoal mais certificado e com mais funções administrativas
- **Insights**: Observações automáticas sobre os dados
- **Qualidade dos Dados**: Issues identificados e porcentagem de completude

## Exemplo de Resultado

```json
{
  "fileName": "dados-militar.csv",
  "analysisDateTime": "2024-01-15T10:30:00",
  "totalRecords": 150,
  "validRecords": 148,
  "dataCompletenessPercentage": 87.5,
  "rankDistribution": {
    "Soldado 1ª C": 45,
    "Cabo": 32,
    "3º Sargento": 28
  },
  "rankCategoryDistribution": {
    "SOLDIER": 45,
    "CORPORAL": 32,
    "SERGEANT": 28
  },
  "certificationCounts": {
    "GIC": 23,
    "PER": 18,
    "GOT": 31
  },
  "averageCertificationsPerPerson": 1.8,
  "topCertifiedPersonnel": [...],
  "keyInsights": [
    "Most common rank category: SOLDIER (45 personnel)",
    "Average certifications per person: 1.80",
    "Most common certification: GOT",
    "Active personnel: 148 (98.7%)"
  ]
}
```

## Testes

### Executar todos os testes:
```bash
mvn test
```

### Testes específicos:
```bash
# Testes de análise
mvn test -Dtest="*Analysis*Test"

# Testes de parsing
mvn test -Dtest="*Parser*Test"

# Testes de integração
mvn test -Dtest="*Integration*Test"
```

## Extensibilidade

### Adicionar Novo Tipo de Análise:
1. Implementar `CsvAnalysisStrategy`
2. Registrar como `@Component`
3. O sistema detectará automaticamente

### Adicionar Novo Formato de CSV:
1. Implementar `CsvParser`
2. Registrar como `@Component`
3. A factory detectará automaticamente

## Dependências

- **Spring Boot**: Framework base
- **OpenCSV**: Parsing de arquivos CSV
- **Lombok**: Redução de boilerplate
- **JUnit 5**: Testes unitários
- **H2 Database**: Banco de dados (já configurado no projeto)

## Configuração

O sistema utiliza as configurações existentes do projeto. Não são necessárias configurações adicionais além das já presentes no `application.properties`.

---

**Desenvolvido seguindo princípios SOLID e padrões de design para máxima escalabilidade e manutenibilidade.**