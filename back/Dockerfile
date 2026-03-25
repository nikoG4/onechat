# Usa una imagen base de Java
FROM openjdk:17-jdk-slim

# Establece el directorio de trabajo en el contenedor
WORKDIR /app

# Copia el archivo JAR de tu proyecto al contenedor
COPY target/*.jar app.jar

# Expone el puerto en el que se ejecutará tu aplicación
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
