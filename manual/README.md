## Introduction
This is a simple command-line interface application for demonstration purposes of Scalar DL.
Users can use this application to create asset categories, create new assets and borrow/return assets.
All states of assets are shared with all the other users on the same Scalar DL network.

## Build the the application

Please install JDK 8 before building and launching this application.

To launch the application, two things are necessary :
- a self-contained jar file containing the Scalar DL client libraries and the source code of this application
- a folder named *contract* which contains the contract files used by the application

To generate these files, please run the following command. A contract
folder will be created in the application's root directory.
```
./gradlew prepare
```

## Command-line interface

A executable jar file `am-1.0.jar` can be generated in the root folder using `./gradlew prepare`.
We can use this pattern to execute different commands.

```
java -jar am-1.0.jar <command> [<command options>|<command arguments>]
```

### `init` command
`init` command is used to generate a config file `client.properties` so that the other commands can use it to connect to a Scalar DL network afterwards.

```
java -jar am-1.0.jar init [-h] [-tls] [-credential=<credential>] [-host=<host>] [-p=<port>] private-key certificate
```

#### Connect to Scalar DL Sandbox
We can test this application with Scalar DL Sandbox provided by Scalar, Inc.
Please refer [this](./use-scalar-dl-sandbox.md) to apply for your holder id, private key, certificate and authentication credential.
When you have your `key`, `certificate` and `credential`, use following `init` command example to generate the `client.properties` for connecting to Scalar DL Sandbox.
```
java -jar am-1.0.jar init -tls -credential='Basic xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx==
' -host='sandbox.scalar-labs.com' -p=443 /tmp/foo-key.pem /tmp/foo.pem
```

#### Options
|name|usage|
|----|-----|
|-h|Display help|
|-host=`<host>`|Use the host address to connect to one server of the Scalar DL network. Default: `localhost`|
|-p=`<port>`|Use the port to connect to the one server of the Scalar DL network. Default: `50051`|
|-tls|Enable TLS connection|
|-credential=`<credential>`|Specify the authentication token to connect to the Scalar DL network|

#### Arguments
|name|usage|
|----|-----|
|private-key|The path of private key file|
|certificate|The path of certificate file|

### `list-type` command
To list existing asset categories.

```
java -jar am-1.0.jar list-type
```

### `add-type` command
To add a new asset category.

```
java -jar am-1.0.jar add-type [-h] asset_type
```

#### Options
|name|usage|
|----|-----|
|-h|Display help|

#### Arguments
|name|usage|
|----|-----|
|asset_type|New category's name|


### `list` command
To list all existing assets and their borrowing status for the specified category.

```
java -jar am-1.0.jar list [-h] asset_type
```

#### Options
|name|usage|
|----|-----|
|-h|Display help|

#### Arguments
|name|usage|
|----|-----|
|asset_type|The category's name to list all assets|


### `add` command
To add a new asset to the specified category.

```
java -jar am-1.0.jar add [-h] asset_type asset
```

#### Options
|name|usage|
|----|-----|
|-h|Display help|

#### Arguments
|name|usage|
|----|-----|
|asset_type|The category's name to add new asset|
|asset|New asset's name|

### `borrow` command
To borrow an asset.

```
java -jar am-1.0.jar borrow [-h] arguments...
```

#### Options
|name|usage|
|----|-----|
|-h|Display help|

#### Arguments
Can be either format of `<type> <name>`

|name|usage|
|----|-----|
|type|The category's name of the borrowing asset|
|name|The borrowing asset's name|

or format of `<id>`

|name|usage|
|----|-----|
|id|The borrowing asset's id|

### `return` command

To return an asset.

```
java -jar am-1.0.jar return [-h] arguments...
```

#### Options
|name|usage|
|----|-----|
|-h|Display help|

#### Arguments
Can be either format of `<type> <name>`

|name|usage|
|----|-----|
|type|The category's name of the returning asset|
|name|The returning asset's name|

or format of `<id>`

|name|usage|
|----|-----|
|id|The returning asset's id|

### `asset-history` command
To display the borrowing status for the specified asset.

```
java -jar am-1.0.jar asset-history [-h] arguments...
```

#### Options
|name|usage|
|----|-----|
|-h|Display help|

#### Arguments
Can be either format of `<type> <name>`

|name|usage|
|----|-----|
|type|The category's name of the asset|
|name|The asset's name|

or format of `<id>`

|name|usage|
|----|-----|
|id|The asset's id|

### `validate` command
To validate tamper-evidence for the specified asset.

```
java -jar am-1.0.jar validate [-h] asset...
```
#### Options
|name|usage|
|----|-----|
|-h|Display help|

#### Arguments
Can be either format of `<type> <name>`

|name|usage|
|----|-----|
|type|The category's name of the asset|
|name|The asset's name|

or format of `<id>`

|name|usage|
|----|-----|
|id|The asset's id|
