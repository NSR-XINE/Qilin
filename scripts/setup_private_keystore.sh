#!/bin/bash
set -e

# Change to repository root
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

KEYSTORE_FILE="$REPO_ROOT/release.keystore"
PROPS_FILE="$REPO_ROOT/keystore.properties"

echo "=========================================="
echo "  Private Release Keystore Setup"
echo "=========================================="
echo "This will create a private keystore on your device."
echo "Neither git, nor GitHub, nor anyone else will know your password."
echo ""

# Prompt for password securely (hidden input)
read -s -p "Enter a secure password for your keystore: " PASS1
echo ""
read -s -p "Confirm your password: " PASS2
echo ""

if [ "$PASS1" != "$PASS2" ]; then
    echo "Error: Passwords do not match!"
    exit 1
fi

if [ -z "$PASS1" ]; then
    echo "Error: Password cannot be empty!"
    exit 1
fi

read -p "Enter key alias [default: qilin]: " ALIAS
ALIAS=${ALIAS:-qilin}

echo ""
echo "Generating private release keystore..."

if [ -f "$KEYSTORE_FILE" ]; then
    read -p "release.keystore already exists. Overwrite? (y/N): " CONFIRM
    if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 0
    fi
    rm -f "$KEYSTORE_FILE"
fi

keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Qilin, OU=Personal, O=Qilin, L=Private, S=Private, C=US" \
    -storepass "$PASS1" \
    -keypass "$PASS1" > /dev/null 2>&1

chmod 600 "$KEYSTORE_FILE"

# Create keystore.properties
cat > "$PROPS_FILE" <<EOF
storeFile=release.keystore
storePassword=$PASS1
keyAlias=$ALIAS
keyPassword=$PASS1
EOF

chmod 600 "$PROPS_FILE"

echo "✓ Private keystore created at: $KEYSTORE_FILE"
echo "✓ keystore.properties created at: $PROPS_FILE"
echo ""
echo "Security verification:"
if git check-ignore -q "$KEYSTORE_FILE" && git check-ignore -q "$PROPS_FILE"; then
    echo "✓ Keystore and properties are protected by .gitignore and will never be tracked by git."
else
    echo "⚠ Warning: Ensure .gitignore contains release.keystore and keystore.properties!"
fi

echo ""
echo "--------------------------------------------------"
echo "Optional: For private GitHub Actions Cloud Builds"
echo "1. Export base64 string to a secure file or clipboard:"
echo "   base64 -w 0 release.keystore"
echo ""
echo "2. Add Repository Secrets in GitHub (Settings -> Secrets and variables -> Actions):"
echo "   - KEYSTORE_BASE64:    (paste base64 output)"
echo "   - KEYSTORE_PASSWORD:  (your password)"
echo "   - KEY_ALIAS:          $ALIAS"
echo "   - KEY_PASSWORD:       (your password)"
echo "--------------------------------------------------"
echo "Done! Running ./gradlew assembleRelease will now sign using your private key."
