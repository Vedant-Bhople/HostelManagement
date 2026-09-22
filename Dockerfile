FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw 2>/dev/null || true

RUN if [ -f "./mvnw" ]; then ./mvnw clean package -DskipTests; else apt-get update && apt-get install -y maven && mvn clean package -DskipTests; fi

CMD ["sh", "-c", "java -jar target/*.jar"]
