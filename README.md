# Grimaldo
An android client app (and cross platform command line server) to remotely unlock your Linux desktop PC

## How it works

The system is made of an Android app, containing a c++ binary acting as client, which performs
a public key challenge-response authentication against the same binary running as server on a Linux PC. Upon successful authentication, the server issues the command:

```shell
loginctl unlock-session <current-session>
```

This may be useful in a company or, in general, a public environment, in order to avoid having to insert password on every PC unlock.

The native client/server binary source is included as submodule of this project, and available [here](https://github.com/pgp/GrimaldoNative)
