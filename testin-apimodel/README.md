# testin-apimodel

The half of Testin that turns an API call's JSON into Java: New → Testin API
Model from JSON on a package writes the request as a Lombok class and the
response as a record (UC-CODEGEN-022).

It is a content module of its own, loaded only in IDEs that have the Java
plugin, so the Plugin Verifier checks it only there.

## Credit

Testin API Model from JSON is designed on the idea of
[RoboPOJOGenerator](https://github.com/robohorse/RoboPOJOGenerator), the
IntelliJ plugin by Vadim Shchenev (MIT license): select a package, choose New,
paste a JSON payload, and get its classes. No code was taken from it; this
module is Testin's own, written in Java on the Jackson and the IntelliJ Java
APIs the plugin already ships.
