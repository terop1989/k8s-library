FROM ubuntu:18.04 AS downloader
ARG HELM_VERSION=3.0.0
RUN apt-get update && \
    apt install curl -y && \
    curl -fsSL -o /tmp/helm-v${HELM_VERSION}-linux-amd64.tar.gz https://get.helm.sh/helm-v${HELM_VERSION}-linux-amd64.tar.gz && \
    tar -zxvf /tmp/helm-v${HELM_VERSION}-linux-amd64.tar.gz -C /tmp 

FROM ubuntu:18.04
COPY --from=downloader --chmod=755 /tmp/linux-amd64/helm /usr/local/bin/helm
CMD tail -f /dev/null