#!/bin/bash

# Script para monitorar uso de memória do bot Discord
echo "🔍 Monitor de Memória - Bot Discord"
echo "=================================="
echo ""

# Função para monitorar processo específico
monitor_bot_memory() {
    local bot_pid=$(ps aux | grep "BotAttendanceApplication" | grep -v grep | awk '{print $2}')
    
    if [ -z "$bot_pid" ]; then
        echo "❌ Bot não está rodando"
        return 1
    fi
    
    echo "🤖 Bot PID: $bot_pid"
    echo ""
    
    while true; do
        # Obtém informações de memória do processo
        local mem_info=$(ps -p $bot_pid -o pid,rss,vsz,pmem,pcpu,comm 2>/dev/null)
        
        if [ $? -eq 0 ]; then
            clear
            echo "🔍 Monitor de Memória - Bot Discord"
            echo "=================================="
            echo "⏰ $(date '+%H:%M:%S')"
            echo ""
            echo "📊 Informações do Processo:"
            echo "$mem_info"
            echo ""
            
            # Converte RSS para MB
            local rss_mb=$(ps -p $bot_pid -o rss= 2>/dev/null | awk '{print $1/1024}')
            echo "💾 Memória RSS: ${rss_mb}MB"
            
            # Verifica se está em stand-by
            local standby_check=$(ps -p $bot_pid -o comm= 2>/dev/null)
            if [[ "$standby_check" == *"java"* ]]; then
                echo "🟢 Status: Ativo"
            else
                echo "🔴 Status: Inativo"
            fi
            
            echo ""
            echo "Pressione Ctrl+C para sair"
            sleep 2
        else
            echo "❌ Bot parou de executar"
            break
        fi
    done
}

# Função para monitorar todos os processos Java
monitor_all_java() {
    echo "📊 Todos os Processos Java:"
    echo "========================="
    ps aux | grep java | grep -v grep | awk '{
        printf "PID: %s | CPU: %s%% | MEM: %s%% | RSS: %.1fMB | VSZ: %.1fMB | %s\n", 
        $2, $3, $4, $6/1024, $5/1024, $11
    }' | head -10
    echo ""
}

# Função para monitorar uso de memória do sistema
monitor_system_memory() {
    echo "🖥️  Memória do Sistema:"
    echo "====================="
    vm_stat | awk '{
        if ($1 == "Pages") {
            pages_free = $3
            pages_active = $4
            pages_inactive = $5
            pages_speculative = $6
            pages_wired = $7
        }
    } END {
        page_size = 4096
        free_mb = (pages_free * page_size) / 1024 / 1024
        active_mb = (pages_active * page_size) / 1024 / 1024
        inactive_mb = (pages_inactive * page_size) / 1024 / 1024
        wired_mb = (pages_wired * page_size) / 1024 / 1024
        total_used = active_mb + inactive_mb + wired_mb
        total_free = free_mb
        
        printf "💾 Memória Livre: %.1fMB\n", free_mb
        printf "🔄 Memória Ativa: %.1fMB\n", active_mb
        printf "⏸️  Memória Inativa: %.1fMB\n", inactive_mb
        printf "🔒 Memória Wired: %.1fMB\n", wired_mb
        printf "📊 Total Usado: %.1fMB\n", total_used
        printf "📊 Total Livre: %.1fMB\n", total_free
    }'
    echo ""
}

# Menu principal
case "$1" in
    "bot")
        monitor_bot_memory
        ;;
    "all")
        monitor_all_java
        ;;
    "system")
        monitor_system_memory
        ;;
    *)
        echo "🔍 Monitor de Memória - Bot Discord"
        echo "=================================="
        echo ""
        echo "Uso: $0 [opção]"
        echo ""
        echo "Opções:"
        echo "  bot     - Monitora apenas o bot Discord"
        echo "  all     - Mostra todos os processos Java"
        echo "  system  - Mostra memória do sistema"
        echo ""
        echo "Exemplos:"
        echo "  $0 bot      # Monitora bot em tempo real"
        echo "  $0 all      # Lista todos os processos Java"
        echo "  $0 system   # Mostra memória do sistema"
        ;;
esac
