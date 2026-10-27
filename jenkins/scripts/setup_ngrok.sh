#!/usr/bin/env bash
set -e

# --- Configuration ---
JENKINS_PORT=8080
INSTALL_DIR="$HOME/.local/bin"
# Update your PATH for the current session if not already present
export PATH="$INSTALL_DIR:$PATH"

echo "=== 1. Checking / Installing ngrok ==="
if command -v ngrok &>/dev/null; then
    echo "ngrok is already installed at: $(which ngrok)"
else
    echo "Downloading ngrok binary..."
    mkdir -p "$INSTALL_DIR"
    
    # Download and extract binary directly into ~/.local/bin
    curl -fsSL https://bin.ngrok.com/c/bNyj1mQVY4c/ngrok-v3-stable-linux-amd64.tgz | tar -xz -C "$INSTALL_DIR"
    chmod +x "$INSTALL_DIR/ngrok"
    
    echo "ngrok successfully installed to $INSTALL_DIR/ngrok"
fi

echo "=== 2. Authtoken Setup ==="
# Prompt for auth token if it hasn't been set yet
read -rp "Enter your ngrok Authtoken (press Enter to skip if already saved): " AUTHTOKEN
if [ -n "$AUTHTOKEN" ]; then
    ngrok config add-authtoken "$AUTHTOKEN"
    echo "Authtoken configured."
fi

echo ""
echo "=== 3. Launching ngrok Tunnel for Jenkins (Port $JENKINS_PORT) ==="
echo "Starting ngrok... Press Ctrl+C to stop."
echo "----------------------------------------------------"

ngrok http "$JENKINS_PORT"