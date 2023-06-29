FROM maven:3.8.1-adoptopenjdk-11
USER root
WORKDIR /home/fresco-outsourcing
ADD . /home/fresco-outsourcing
RUN mvn clean package -DskipTests
COPY . /app