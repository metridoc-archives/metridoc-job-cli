#!/bin/sh

INSTALL_DIR="$HOME/.metridoc/cli/install"

if [[ -d $INSTALL_DIR ]];
then
    echo "previous installation exists, deleting now"
    rm -rf $INSTALL_DIR
fi

mkdir -p $INSTALL_DIR

if [[ $1 ]];
then
    echo "searching for version $1"
else
    MDOC_VERSION=`curl -s https://raw.github.com/metridoc/metridoc-job-cli/master/src/main/resources/MDOC_VERSION`
    echo "no version provided, installing latest version [$MDOC_VERSION]"
fi

GITHUB_URL="https://github.com/metridoc/metridoc-job-cli/archive/v$MDOC_VERSION.zip"
SOURCE_FILE="metridoc-job-cli-$MDOC_VERSION.zip"
SOURCE_LOCATION="$INSTALL_DIR/$SOURCE_FILE"
echo "downloading source file from [$GITHUB_URL] to [$SOURCE_LOCATION]"
curl -L "https://github.com/metridoc/metridoc-job-cli/archive/v$MDOC_VERSION.zip" > "$SOURCE_LOCATION"
cd "$INSTALL_DIR"
unzip -q "$SOURCE_FILE"
mv "metridoc-job-cli-$MDOC_VERSION" "metridoc-job-cli"
cd metridoc-job-cli
chmod 744 gradlew
./gradlew installApp > "$INSTALL_DIR/install.log" 2>&1 &
PID=$!

printf "Installing Application"
while kill -0 $PID >/dev/null 2>&1
do
    printf "."
    sleep 1
done

echo ""

MDOC_BIN="$INSTALL_DIR/metridoc-job-cli/build/install/mdoc/bin"
if ! grep -q metridoc-job-cli "$HOME/.bash_profile"; then
    echo "export PATH=$MDOC_BIN:\$PATH" >> "$HOME/.bash_profile"
fi
cd "$MDOC_BIN"
./mdoc install-deps

# go back to where we were
cd "$CURRENT_LOCATION"