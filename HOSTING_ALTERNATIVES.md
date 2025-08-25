# Guia de Hospedagem para Discord Bots - GRA-BOT

## 🔍 Situação Atual: Railway

**Problema**: Bot consumindo 1GB de RAM → Custo alto (~$0.79/período)  
**Solução**: Otimizações implementadas + alternativas de hospedagem

## 💰 Comparativo Detalhado de Hosts

### 1. Railway (Atual - Otimizado)
- **Custo**: $5-8/mês
- **RAM**: 512MB-1GB 
- **Prós**: 
  - Deploy automático via GitHub
  - Interface amigável
  - Monitoramento integrado
- **Contras**: 
  - Mais caro para apps simples
  - Cobrança por uso de memória
- **Com otimizações**: ~$3-5/mês

### 2. Fly.io ⭐ (Melhor custo-benefício)
- **Custo**: $0-5/mês
- **RAM**: 256MB grátis, 512MB pagos
- **Prós**:
  - Tier gratuito generoso (160 horas/mês)
  - Edge computing (baixa latência)
  - Boa performance
- **Contras**: 
  - Configuração via CLI
  - Tier gratuito para apps inativos
- **Setup**:
```bash
# Instalar flyctl
curl -L https://fly.io/install.sh | sh

# Deploy
fly launch
fly deploy
```

### 3. Render
- **Custo**: $7/mês (Web Service)
- **RAM**: 512MB
- **Prós**:
  - Deploy automático
  - SSL gratuito
  - Backup automático
- **Contras**: 
  - Sem tier gratuito para sempre ativo
  - Preço fixo
- **Ideal para**: Produção com budget fixo

### 4. Heroku
- **Custo**: $7/mês (Basic Dyno)
- **RAM**: 512MB
- **Prós**:
  - Plataforma madura
  - Muitos add-ons
  - Documentação extensa
- **Contras**: 
  - Removeu tier gratuito
  - Preço alto para recursos básicos
- **Status**: Não recomendado para novos projetos

### 5. DigitalOcean App Platform
- **Custo**: $5/mês (Basic)
- **RAM**: 512MB
- **Prós**:
  - Preço fixo e previsível
  - Boa performance
  - Interface simples
- **Contras**: 
  - Menor flexibilidade
  - Sem tier gratuito

### 6. VPS Tradicionais

#### AWS Lightsail ⭐ (Melhor para controle)
- **Custo**: $3.50/mês
- **RAM**: 512MB, 1 vCPU
- **Prós**:
  - Controle total
  - AWS reliability
  - Preço baixo
- **Contras**: 
  - Requer configuração manual
  - Sem deploy automático

#### DigitalOcean Droplet
- **Custo**: $4/mês
- **RAM**: 512MB, 1 vCPU
- **Similar ao Lightsail**, interface mais amigável

#### OVH VPS (Europa)
- **Custo**: €3.50/mês (~$3.80)
- **RAM**: 2GB, 1 vCPU
- **Melhor custo/benefício para RAM**

## 🎯 Recomendações por Cenário

### Para Desenvolvimento/Teste:
1. **Fly.io** (tier gratuito)
2. **Railway** (com otimizações)

### Para Produção (Budget Limitado):
1. **AWS Lightsail** ($3.50/mês) ⭐
2. **OVH VPS** (€3.50/mês, mais RAM)
3. **Fly.io** ($2-5/mês)

### Para Produção (Facilidade):
1. **Railway** (com otimizações, $3-5/mês)
2. **Render** ($7/mês)
3. **DigitalOcean App Platform** ($5/mês)

### Para Múltiplos Bots:
1. **VPS (AWS Lightsail/DigitalOcean)** - Host vários bots
2. **OVH VPS** - 2GB RAM para múltiplos projetos

## 🚀 Guia de Migração

### Para Fly.io (Recomendado para economia)

1. **Instalar Fly CLI**:
```bash
curl -L https://fly.io/install.sh | sh
```

2. **Configurar fly.toml**:
```toml
app = "gra-bot"

[env]
  DISCORD_BOT_TOKEN = "your_token"
  JAVA_TOOL_OPTIONS = "-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

[build]
  buildpacks = ["heroku/java"]

[[services]]
  http_checks = []
  internal_port = 8080
  processes = ["app"]

  [[services.ports]]
    force_https = true
    handlers = ["http"]
    port = 80

  [[services.ports]]
    handlers = ["tls", "http"]
    port = 443

[build.env]
  BP_JVM_VERSION = "17"
```

3. **Deploy**:
```bash
fly launch --no-deploy
fly deploy
```

### Para AWS Lightsail

1. **Criar instância Ubuntu 22.04**
2. **Instalar Java 17**:
```bash
sudo apt update
sudo apt install openjdk-17-jre-headless
```

3. **Upload e executar**:
```bash
# Upload do JAR otimizado
scp target/aviation-discord-bot-0.0.1-SNAPSHOT.jar user@ip:~/

# Executar com otimizações
java -jar aviation-discord-bot-0.0.1-SNAPSHOT.jar
```

4. **Configurar systemd** (auto-start):
```bash
sudo nano /etc/systemd/system/gra-bot.service
```

```ini
[Unit]
Description=GRA Discord Bot
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu
ExecStart=/usr/bin/java -jar aviation-discord-bot-0.0.1-SNAPSHOT.jar
Environment="DISCORD_BOT_TOKEN=your_token"
Environment="JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable gra-bot
sudo systemctl start gra-bot
```

## 📊 Comparação Final de Custos (Mensal)

| Host | Custo | RAM | Setup | Auto-Deploy | Total Score |
|------|-------|-----|-------|-------------|-------------|
| **Fly.io** | $0-5 | 256-512MB | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **AWS Lightsail** | $3.50 | 512MB | ⭐⭐ | ⭐ | ⭐⭐⭐⭐ |
| **Railway (otim)** | $3-5 | 512MB | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **DigitalOcean** | $4-5 | 512MB | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **Render** | $7 | 512MB | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Heroku** | $7 | 512MB | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |

## 🎯 Decisão Final

**Para economia máxima**: Migre para **Fly.io** ou **AWS Lightsail**  
**Para manter facilidade**: Continue no **Railway com otimizações**  
**Para múltiplos projetos**: VPS **OVH** ou **DigitalOcean**

Com as otimizações implementadas, mesmo o Railway deve ter redução de ~60% nos custos de memória.