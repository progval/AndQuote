AndQuote is an Android application that browses quotes from various websites,
according to their availability on the OpenQuoteApi server.

AndQuote dynamically loads the list of sites from the server, so when a site
is added you do not need to update AndQuote, it will be applied automatically.

# How to build

## Get the source code

```
git clone git://github.com/ProgVal/AndQuote.git
cd AndQuote
git submodule init
git submodule update
```

##Compile and install on your device

Signed binary:

```
ant release
ant installr
```

Non-signed binary:

```
ant debug
ant installd
```

# Donate

I hate Java, writting this application has been painful to me ;)

Bitcoin: 1LC9fN12dgb9jLTQSupJGAxc4v2WjKAVud
