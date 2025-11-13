#!/bin/bash
export SECURITY_DISABLE=true
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/distrischool_grades
export SPRING_DATASOURCE_USERNAME=distrischool
export SPRING_DATASOURCE_PASSWORD=distrischool123

# Java 17
export JAVA_HOME=/Users/ccastro/Library/Java/JavaVirtualMachines/temurin-17.0.16/Contents/Home 2>/dev/null || export JAVA_HOME=$(/usr/libexec/java_home -v 17)

echo "🚀 Iniciando Grade Service..."
echo "   SECURITY_DISABLE=$SECURITY_DISABLE"
echo "   Porta: 8083"
echo ""

if [ -f "mvnw" ]; then
    ./mvnw spring-boot:run
elif command -v mvn > /dev/null; then
    mvn spring-boot:run
else
    echo "❌ Maven não encontrado"
    exit 1
fi
