#!/bin/bash

# Script de deploy para produção
# Uso: ./deploy.sh

set -e

echo "🚀 Iniciando deploy do Physio Manager..."

# Verifica se o arquivo .env existe
if [ ! -f .env ]; then
    echo "❌ Erro: Arquivo .env não encontrado!"
    echo "📝 Crie um arquivo .env com as variáveis necessárias (veja DEPLOYMENT.md)"
    exit 1
fi

# Para os containers existentes
echo "🛑 Parando containers existentes..."
docker-compose -f docker-compose.prod.yml down

# Nota sobre backup do banco
echo "ℹ️  Lembre-se: O banco de dados está em servidor externo."
echo "   Faça backup manualmente se necessário antes do deploy."

# Build da imagem
echo "🔨 Construindo imagem Docker..."
docker-compose -f docker-compose.prod.yml build --no-cache

# Inicia os serviços
echo "▶️  Iniciando serviços..."
docker-compose -f docker-compose.prod.yml up -d

# Aguarda a aplicação iniciar
echo "⏳ Aguardando aplicação iniciar..."
sleep 10

# Verifica o status
echo "📊 Status dos containers:"
docker-compose -f docker-compose.prod.yml ps

# Mostra os logs
echo ""
echo "📋 Últimas linhas dos logs:"
docker-compose -f docker-compose.prod.yml logs --tail=20

echo ""
echo "✅ Deploy concluído!"
echo "🌐 Aplicação disponível em: http://localhost:8080"
echo "📚 Swagger UI: http://localhost:8080/swagger-ui.html"

