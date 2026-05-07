# Reproducer for the jammy x86_64 chrome-headless-shell startup SIGSEGV.
#
# Build:
#   docker build -t chs-repro -f Dockerfile .
# Run:
#   docker run --rm --platform=linux/amd64 chs-repro
#
# Pin a specific Chrome for Testing version:
#   docker build --build-arg CHROME_VERSION=148.0.7778.96 -t chs-repro -f Dockerfile .
#
# Cross-check it does NOT reproduce on noble by changing the FROM line below.

FROM ubuntu:jammy

ARG CHROME_VERSION=148.0.7778.96

ENV DEBIAN_FRONTEND=noninteractive

# Chromium runtime deps + node 20 (jammy's default node is too old).
RUN apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates curl wget unzip gnupg \
        libnss3 libnspr4 libdbus-1-3 \
        libatk1.0-0 libatk-bridge2.0-0 libatspi2.0-0 \
        libxcomposite1 libxdamage1 libxfixes3 libxrandr2 libxkbcommon0 \
        libdrm2 libgbm1 libasound2 libpango-1.0-0 libcairo2 libcups2 \
        libxext6 libx11-6 libxcb1 libxshmfence1 \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

# Download chrome-headless-shell directly from Chrome for Testing — no
# Playwright in the loop.
RUN curl -fL -o /tmp/chs.zip \
        "https://storage.googleapis.com/chrome-for-testing-public/${CHROME_VERSION}/linux64/chrome-headless-shell-linux64.zip" \
    && unzip -q /tmp/chs.zip -d /opt \
    && rm /tmp/chs.zip \
    && /opt/chrome-headless-shell-linux64/chrome-headless-shell --version

ENV CHROME=/opt/chrome-headless-shell-linux64/chrome-headless-shell
ENV ITERATIONS=200
ENV STARTUP_WAIT_MS=2500

COPY repro.js /repro.js

CMD ["node", "/repro.js"]
