FROM eclipse-temurin:8-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw 2>/dev/null || true

RUN if [ -f "./mvnw" ]; then ./mvnw clean package -Dmaven.test.skip=true; else apt-get update && apt-get install -y maven && mvn clean package -Dmaven.test.skip=true; fi

CMD ["sh", "-c", "java -jar target/*.jar"]