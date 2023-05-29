FROM ubuntu:18.04
RUN apt-get update && apt-get install -y \
 openjdk-11-jdk \
 maven \
 make
WORKDIR /home/fresco-outsourcing
ADD . /home/fresco-outsourcing
RUN mvn clean package -DskipTests
COPY . /app
EXPOSE 8041
EXPOSE 8042
EXPOSE 8043
EXPOSE 8044
EXPOSE 8045
EXPOSE 8046
ENTRYPOINT ["java","-jar","/app/demo/target/demo.jar"]