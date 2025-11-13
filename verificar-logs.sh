#!/bin/bash

echo "=== VERIFICANDO LOGS DO GRADE MANAGEMENT SERVICE ==="
echo ""

cd "$(dirname "$0")"

# Verifica se o Docker está rodando
if ! docker ps > /dev/null 2>&1; then
    echo "❌ Docker não está rodando!"
    echo "   Inicie o Docker primeiro: open -a Docker"
    exit 1
fi

echo "✅ Docker está rodando"
echo ""

# Verifica se o container está rodando
if ! docker-compose ps grade-management-service-dev | grep -q "Up"; then
    echo "⚠️  Container não está rodando. Iniciando..."
    docker-compose up -d grade-management-service-dev
    echo "   Aguardando 10 segundos para o serviço iniciar..."
    sleep 10
fi

echo "📋 Status do container:"
docker-compose ps grade-management-service-dev | tail -2
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📋 ÚLTIMOS 50 LOGS:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
docker-compose logs --tail=50 grade-management-service-dev 2>&1 | tail -50
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔍 VERIFICANDO CONFIGURAÇÃO DE SEGURANÇA:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
docker-compose logs --tail=200 grade-management-service-dev 2>&1 | grep -i -E "(security|SECURITY|disable|jwtDecoder|Started|Tomcat|port|8080)" | tail -20
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🧪 TESTANDO HEALTH CHECK:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if curl -s http://localhost:8083/api/v1/health > /dev/null 2>&1; then
    echo "✅ Serviço está respondendo!"
    curl -s http://localhost:8083/api/v1/health | python3 -m json.tool 2>/dev/null || curl -s http://localhost:8083/api/v1/health
else
    echo "⏳ Serviço ainda não está respondendo"
    echo "   Verifique os logs acima para mais detalhes"
fi
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📝 COMANDOS ÚTEIS:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "   Ver logs em tempo real:"
echo "   docker-compose logs -f grade-management-service-dev"
echo ""
echo "   Ver logs com filtro de segurança:"
echo "   docker-compose logs grade-management-service-dev | grep -i security"
echo ""
echo "   Reiniciar o serviço:"
echo "   docker-compose restart grade-management-service-dev"
echo ""

