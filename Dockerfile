FROM adoptopenjdk:11.0.6_10-jre-hotspot-bionic
ARG path
ADD $path /home/konsul
ENV PATH="/home/konsul/bin:${PATH}"
