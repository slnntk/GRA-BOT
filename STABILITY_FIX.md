# 🔧 Correção de Estabilidade do Bot Discord - GRA-BOT

## 📋 Problema Identificado

O bot estava desconectando frequentemente (a cada 3-4 horas) com o erro:
```
WebSocket closed: 1006 Abnormal closure caused by discord4j.gateway.retry.GatewayException
```

### Causa Raiz
A otimização agressiva de memória implementada anteriormente estava causando instabilidade:
- **Garbage Collection agressivo**: 3 chamadas de GC a cada 3 minutos
- **Cache muito pequeno**: Expirando dados muito rápido (2 minutos)
- **Standby muito frequente**: Entrando em modo standby a cada 3 minutos
- **Recursos limitados**: Pool de conexões e threads muito pequenos

Essas otimizações extremas estavam interrompendo a conexão WebSocket do Discord4J, causando desconexões.

## ✅ Soluções Implementadas

### 1. **Configuração do Gateway Discord** (`DiscordConfig.java`)
```java
.setMaxMissedHeartbeatAck(3) // Permite até 3 heartbeats perdidos antes de reconectar
```
- **Benefício**: Conexão mais resiliente a problemas temporários de rede
- **Impacto**: Reduz desconexões desnecessárias

### 2. **Otimização do StandbyService** (`StandbyService.java`)

**ANTES:**
```java
STANDBY_TIMEOUT = 3 * 60 * 1000; // 3 minutos
ACTIVITY_CHECK_INTERVAL = 1 * 60 * 1000; // 1 minuto

// Garbage collection agressivo
System.gc();
System.runFinalization();
System.gc();
```

**DEPOIS:**
```java
STANDBY_TIMEOUT = 10 * 60 * 1000; // 10 minutos
ACTIVITY_CHECK_INTERVAL = 2 * 60 * 1000; // 2 minutos

// GC suave - apenas uma vez
System.gc();
```

- **Benefício**: Reduz estresse no sistema e evita interrupções na conexão Discord
- **Impacto**: Modo standby menos agressivo, conexão mais estável

### 3. **Balanceamento de Cache** (`PerformanceConfig.java` & `application.properties`)

| Cache | Antes | Depois | Mudança |
|-------|-------|--------|---------|
| Discord Messages | 50 / 2min | 100 / 5min | +100% tamanho, +150% TTL |
| Guild Config | 20 / 30min | 50 / 30min | +150% tamanho |
| User Cache | 100 / 1h | 200 / 1h | +100% tamanho |
| Main Cache | 50 / 2min | 200 / 5min | +300% tamanho, +150% TTL |

- **Benefício**: Menos limpezas de cache = menos GC = conexão mais estável
- **Impacto**: Memória adicional de ~50-100MB, mas estabilidade muito maior

### 4. **Connection Pool** (`application.properties`)

**ANTES:**
```properties
spring.datasource.hikari.maximum-pool-size=3
spring.datasource.hikari.minimum-idle=1
```

**DEPOIS:**
```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
```

- **Benefício**: Melhor handling de conexões simultâneas
- **Impacto**: Mais recursos disponíveis para operações assíncronas

### 5. **Thread Pool** (`application.properties`)

**ANTES:**
```properties
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=4
spring.task.execution.pool.queue-capacity=10
```

**DEPOIS:**
```properties
spring.task.execution.pool.core-size=3
spring.task.execution.pool.max-size=6
spring.task.execution.pool.queue-capacity=20
```

- **Benefício**: Melhor processamento de eventos Discord assíncronos
- **Impacto**: Menos bloqueios e timeouts

### 6. **Memória JVM** (`pom.xml`)

**ANTES:**
```
-Xms64m -Xmx256m
```

**DEPOIS:**
```
-Xms96m -Xmx384m
```

- **Benefício**: Mais espaço para operações sem forçar GC constantemente
- **Impacto**: +128MB de memória, mas evita GC frequente que causava desconexões

## 📊 Comparação de Recursos

| Recurso | Antes | Depois | Diferença |
|---------|-------|--------|-----------|
| **Memória Inicial** | 64 MB | 96 MB | +50% |
| **Memória Máxima** | 256 MB | 384 MB | +50% |
| **Cache Total** | ~220 entradas | ~550 entradas | +150% |
| **TTL Médio** | 2 minutos | 5 minutos | +150% |
| **DB Pool** | 3 conexões | 5 conexões | +67% |
| **Thread Pool** | 2-4 threads | 3-6 threads | +50% |
| **Standby Timeout** | 3 minutos | 10 minutos | +233% |
| **GC Calls** | 3x por ciclo | 1x por ciclo | -67% |

## 🎯 Resultados Esperados

### ✅ Melhorias
- **Conexão estável**: Sem mais desconexões 1006
- **Menos GC**: Redução de 67% nas chamadas de garbage collection
- **Standby inteligente**: Entra em modo economia apenas após 10 minutos de inatividade
- **Resiliente**: Tolera até 3 heartbeats perdidos antes de reconectar
- **Performance**: Melhor throughput com thread pool maior

### ⚠️ Trade-offs
- **Memória**: Uso aumentará de ~320MB para ~400-500MB
- **Custos**: Pequeno aumento (mas ainda muito otimizado comparado à versão original que usava 1GB+)

## 📈 Monitoramento Recomendado

### Métricas para Observar:
1. **Uptime da conexão Discord**: Deve ser >99.9%
2. **Desconexões WebSocket**: Deve ser <1 por dia (idealmente 0)
3. **Uso de memória**: Deve estabilizar em ~400-500MB
4. **Garbage Collection**: Deve ocorrer com menos frequência
5. **Tempo de resposta**: Deve permanecer baixo (<500ms)

### Endpoints de Monitoramento:
```bash
# Health check
curl http://localhost:8080/actuator/health

# Métricas de memória
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Métricas de cache
curl http://localhost:8080/actuator/caches

# Métricas Prometheus
curl http://localhost:8080/actuator/prometheus
```

## 🔍 Troubleshooting

### Se o bot ainda desconectar:
1. **Verificar logs**: Procurar por erros de rede ou autenticação
2. **Aumentar heartbeat tolerance**: Mudar de 3 para 5 em `setMaxMissedHeartbeatAck()`
3. **Verificar memória**: Se OOM, aumentar `-Xmx` para 512m
4. **Network issues**: Verificar se há problemas de conectividade com Discord API

### Se o uso de memória for muito alto:
1. **Reduzir cache sizes**: Diminuir `maximumSize` nos caches
2. **Reduzir TTL**: Diminuir `expireAfterWrite` de 5m para 3m
3. **Ajustar JVM**: Usar `-XX:MaxRAMPercentage=75.0` no lugar de `-Xmx`

## 🚀 Deploy

### Próximos Passos:
1. ✅ Build bem-sucedido (`mvn clean compile`)
2. ✅ Mudanças commitadas
3. ⏳ Deploy para ambiente de produção
4. ⏳ Monitorar por 24-48 horas
5. ⏳ Verificar se desconexões pararam

### Rollback (se necessário):
```bash
git revert HEAD
git push origin copilot/start-container-logging
```

## 📝 Notas Finais

Este fix busca um **equilíbrio entre otimização de recursos e estabilidade**. A versão anterior era extremamente otimizada (320MB), mas causava instabilidade. Esta versão usa um pouco mais de memória (~400-500MB), mas deve manter o bot conectado 24/7 sem problemas.

**Filosofia**: É melhor usar 500MB e ter 100% de uptime do que usar 320MB e ficar desconectando a cada 3 horas.

---

**Data da Correção**: 04/11/2025  
**Versão**: 0.0.1-SNAPSHOT  
**Status**: ✅ Implementado, aguardando validação em produção
