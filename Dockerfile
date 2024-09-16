FROM gradle:8.5.0-jdk21

WORKDIR /app

COPY /app .

WORKDIR /app

RUN gradle installDist

CMD ./build/install/app/bin/app
