FROM openjdk:11
ENV PROJECT_NAME=discord_bot_for_freezye
WORKDIR /app
COPY $PROJECT_NAME-1.0.jar .
CMD java -jar $PROJECT_NAME-1.0.jar