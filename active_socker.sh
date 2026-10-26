#!/bin/bash

PODMAN_SOCK="${XDG_RUNTIME_DIR}/podman/podman.sock"
DOCKER_SOCK="${XDG_RUNTIME_DIR}/docker.sock"

if [ -S "$PODMAN_SOCK" ]; then
    ACTIVE_SOCK="$PODMAN_SOCK"
    echo "Found Podman socket: $ACTIVE_SOCK"
elif [ -S "$DOCKER_SOCK" ]; then
    ACTIVE_SOCK="$DOCKER_SOCK"
    echo "Found Docker socket: $ACTIVE_SOCK"
else
    ACTIVE_SOCK="/var/run/docker.sock"
    echo "Using default rootful socket: $ACTIVE_SOCK"
fi

export ACTIVE_SOCK