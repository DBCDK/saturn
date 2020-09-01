FROM docker.dbc.dk/dbc-python3

ENV USER python
RUN useradd -m $USER
RUN pip install pysocks pysftp && apt update && apt install curl -y
USER $USER
WORKDIR /home/$USER

COPY --chown=python src src
