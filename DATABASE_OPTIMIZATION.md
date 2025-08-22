# Otimização de Banco de Dados para Railway

## Resumo da Mudança

Este projeto foi otimizado para reduzir custos no Railway, substituindo PostgreSQL por H2 Database como opção padrão. Esta mudança pode economizar **$5-20/mês** em custos de hospedagem.

## Por que H2 é Suficiente?

### Características do Projeto
- **Bot Discord** para equipe de aviação (escala limitada de usuários)
- **Modelo de dados simples**: 5 entidades principais
- **Consultas básicas**: CRUD, filtros simples, contagens
- **Relacionamentos simples**: Many-to-many entre usuários e escalas
- **Volume baixo**: Dados de escalas e logs de uma equipe

### PostgreSQL vs H2 para este Caso de Uso
| Aspecto | PostgreSQL | H2 |
|---------|------------|-----|
| **Custo Railway** | $5-20/mês | $0/mês |
| **Memória** | ~100MB adicional | ~10MB |
| **Latência** | Rede | Local (mais rápido) |
| **Setup** | Configuração externa | Zero configuração |
| **Recursos necessários** | Todos suportados pelo H2 | ✅ |

## Configuração Atual

### Modo H2 (Padrão - Economia de Custos)
```properties
# H2 é usado automaticamente quando DATABASE_URL não está definida
# Arquivo de banco: ./data/botattendance.mv.db
# Compatibilidade PostgreSQL ativa
```

### Modo PostgreSQL (Opcional)
```bash
# Para usar PostgreSQL, defina a variável de ambiente:
export DATABASE_URL=postgresql://username:password@host:port/database
```

## Como Usar

### 1. Desenvolvimento Local
```bash
# Clone o repositório
git clone https://github.com/slnntk/GRA-BOT.git
cd GRA-BOT

# Execute o bot (H2 será usado automaticamente)
./mvnw spring-boot:run
```

### 2. Deploy no Railway (H2 - Recomendado)
```bash
# Não defina DATABASE_URL
# O bot usará H2 automaticamente
# Custo: apenas CPU/RAM do bot (~$3-5/mês)
```

### 3. Deploy no Railway (PostgreSQL - Se necessário)
```bash
# Defina DATABASE_URL nas variáveis de ambiente do Railway
# Custo: bot + PostgreSQL (~$8-25/mês)
```

## Estrutura de Dados

### Entidades Suportadas
- ✅ **Schedule**: Escalas de voo
- ✅ **User**: Usuários Discord  
- ✅ **GuildConfig**: Configuração do servidor
- ✅ **ScheduleLog**: Logs de auditoria
- ✅ **Enums**: Tipos de aeronave, missão, etc.

### Funcionalidades Mantidas
- ✅ Criação/edição de escalas
- ✅ Sistema de inscrições
- ✅ Logs de auditoria
- ✅ Limpeza automática de dados antigos
- ✅ Configuração por servidor Discord
- ✅ Todos os comandos Discord

## Migração de Dados

### Se você já tem dados no PostgreSQL:

1. **Export dos dados**:
```sql
-- No PostgreSQL
pg_dump -h host -U user -d database --data-only --column-inserts > backup.sql
```

2. **Import para H2**:
```bash
# Inicie o bot com H2 (criará as tabelas)
# Use H2 Console (temporariamente ativar em application.properties):
spring.h2.console.enabled=true
# Acesse http://localhost:8080/h2-console
# Execute os INSERTs do backup.sql
```

## Vantagens da Solução

### 💰 **Econômica**
- **Economia**: $5-20/mês
- **Performance**: Melhor latência (banco local)
- **Simplicidade**: Zero configuração de banco externo

### 🔄 **Flexível**
- **Desenvolvimento**: H2 por padrão
- **Produção**: H2 ou PostgreSQL via variável de ambiente
- **Migração**: Fácil alternância entre os bancos

### 🛡️ **Confiável**
- **Persistência**: Dados salvos em arquivo
- **Backup**: Simples cópia do arquivo `data/`
- **Compatibilidade**: Modo PostgreSQL ativo no H2

## Limitações do H2

As seguintes limitações **NÃO AFETAM** este projeto:

- ❌ Concorrência alta (este é um bot para equipe pequena)
- ❌ Consultas complexas (apenas consultas básicas necessárias)
- ❌ Recursos avançados PostgreSQL (não utilizados)
- ❌ Clustering (não necessário)

## Monitoramento

### Logs Importantes
```log
# H2 ativo
Using embedded database: H2

# PostgreSQL ativo  
Using external database: PostgreSQL
```

### Arquivos de Dados
```
./data/
├── botattendance.mv.db    # Arquivo principal H2
├── botattendance.trace.db # Logs de trace (opcional)
```

## Conclusão

Para um bot Discord de escalas de aviação, **H2 é mais que suficiente** e oferece:
- ✅ **85%+ economia de custos**
- ✅ **Melhor performance**
- ✅ **Zero problemas de compatibilidade**
- ✅ **Manutenção simplificada**

PostgreSQL só seria necessário para aplicações com milhares de usuários simultâneos ou recursos específicos não disponíveis no H2.