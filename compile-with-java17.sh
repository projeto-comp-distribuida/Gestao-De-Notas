#!/bin/bash

# Script para compilar o projeto usando Java 17
export JAVA_HOME=/Users/ccastro/Library/Java/JavaVirtualMachines/temurin-17.0.16/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "Usando Java 17:"
java -version

echo ""
echo "Compilando projeto..."
mvn clean compile -DskipTests "$@"

