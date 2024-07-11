# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jre-alpine
WORKDIR /reatmetric
COPY target/deps .
CMD ["java", "-Djava.rmi.server.hostname=127.0.0.1", "-Dreatmetric.core.config=/etc/reatmetric/configuration.xml", "-Dreatmetric.remoting.rmi.export.port=4000", "--module-path=/reatmetric",  "-m", "eu.dariolucia.reatmetric.remoting/eu.dariolucia.reatmetric.remoting.ReatmetricRemotingServer", "3000"]
# Java RMI registry
EXPOSE 3000
# Java RMI exported objects
EXPOSE 4000
# HTTP server driver port
EXPOSE 8081

#  docker build -t reatmetric-1.1.0-snapshot .
#  docker run -it --mount type=bind,src="C:\Users\dario\Reatmetric\reatmetric_docker",target="/etc/reatmetric" -d -p 127.0.0.1:3000:3000 -p 127.0.0.1:4000:4000 -p 127.0.0.1:8081:8081 reatmetric-1.1.0-snapshot