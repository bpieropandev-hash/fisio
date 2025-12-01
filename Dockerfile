# ============================================
# Stage 1: Build da aplicação
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS build

# Define diretório de trabalho
WORKDIR /app

# Copia apenas os arquivos de configuração do Maven primeiro (para cache de dependências)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Baixa dependências (esta camada será cacheada se pom.xml não mudar)
RUN mvn dependency:go-offline -B

# Copia o código fonte
COPY src ./src

# Compila e empacota a aplicação
RUN mvn clean package -DskipTests -B

# ============================================
# Stage 2: Imagem de produção (runtime)
# ============================================
FROM eclipse-temurin:17-jre-jammy

# Metadados da imagem
LABEL maintainer="Physio Manager"
LABEL description="Sistema de gerenciamento financeiro e agenda para fisioterapia"

# Instala wget para healthcheck (leve e útil)
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget && \
    rm -rf /var/lib/apt/lists/*

# Cria usuário não-privilegiado para executar a aplicação
RUN groupadd -r physio && useradd -r -g physio physio

# Define diretório de trabalho
WORKDIR /app

# Copia o JAR da aplicação do stage de build
COPY --from=build /app/target/*.jar app.jar

# Cria diretório para logs e define permissões
RUN mkdir -p /app/logs && \
    chown -R physio:physio /app

# Muda para usuário não-privilegiado
USER physio

# Expõe a porta da aplicação
EXPOSE 8080

# Variáveis de ambiente padrão (podem ser sobrescritas)
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    SPRING_PROFILES_ACTIVE=prod

# Comando para executar a aplicação
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

